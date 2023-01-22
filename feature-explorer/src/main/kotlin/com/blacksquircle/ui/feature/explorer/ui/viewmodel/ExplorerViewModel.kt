/*
 * Copyright 2022 Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blacksquircle.ui.feature.explorer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blacksquircle.ui.core.data.storage.keyvalue.SettingsManager
import com.blacksquircle.ui.core.domain.resources.StringProvider
import com.blacksquircle.ui.core.ui.extensions.appendList
import com.blacksquircle.ui.core.ui.extensions.replaceList
import com.blacksquircle.ui.core.ui.viewstate.ViewEvent
import com.blacksquircle.ui.feature.explorer.R
import com.blacksquircle.ui.feature.explorer.data.utils.FileSorter
import com.blacksquircle.ui.feature.explorer.data.utils.Operation
import com.blacksquircle.ui.feature.explorer.domain.repository.ExplorerRepository
import com.blacksquircle.ui.feature.explorer.ui.navigation.ExplorerScreen
import com.blacksquircle.ui.feature.explorer.ui.viewstate.DirectoryViewState
import com.blacksquircle.ui.feature.explorer.ui.viewstate.ExplorerViewState
import com.blacksquircle.ui.filesystem.base.exception.DirectoryExpectedException
import com.blacksquircle.ui.filesystem.base.exception.PermissionException
import com.blacksquircle.ui.filesystem.base.model.FileModel
import com.blacksquircle.ui.filesystem.base.model.FileType
import com.blacksquircle.ui.filesystem.base.utils.isValidFileName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val stringProvider: StringProvider,
    private val settingsManager: SettingsManager,
    private val explorerRepository: ExplorerRepository,
) : ViewModel() {

    private val _explorerViewState = MutableStateFlow<ExplorerViewState>(ExplorerViewState.Stub)
    val explorerViewState: StateFlow<ExplorerViewState> = _explorerViewState.asStateFlow()

    private val _directoryViewState = MutableStateFlow<DirectoryViewState>(DirectoryViewState.Stub)
    val directoryViewState: StateFlow<DirectoryViewState> = _directoryViewState.asStateFlow()

    private val _refreshState = MutableStateFlow(false)
    val refreshState: StateFlow<Boolean> = _refreshState.asStateFlow()

    private val _viewEvent = Channel<ViewEvent>(Channel.BUFFERED)
    val viewEvent: Flow<ViewEvent> = _viewEvent.receiveAsFlow()

    private val _customEvent = MutableSharedFlow<ExplorerViewEvent>()
    val customEvent: SharedFlow<ExplorerViewEvent> = _customEvent.asSharedFlow()

    val serverState = explorerRepository.serverFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var viewMode: Int = settingsManager.viewMode.toInt()
        private set
    var sortMode: Int = settingsManager.sortMode.toInt()
        private set
    var showHidden: Boolean = settingsManager.showHidden
        private set
    var query: String = ""
        private set
    var dropdownPosition: Int = -1
        private set

    var rootFileModel : FileModel? = null
        private set

    private val breadcrumbs = mutableListOf<FileModel>()
    private val selection = mutableListOf<FileModel>()
    private val buffer = mutableListOf<FileModel>()
    private val files = mutableListOf<FileModel>()
    private var operation = Operation.CREATE

    fun obtainEvent(event: ExplorerIntent) {
        when (event) {
            is ExplorerIntent.SearchFiles -> searchFiles(event)
            is ExplorerIntent.SelectFiles -> selectFiles(event)
            is ExplorerIntent.SelectTab -> selectTab(event)
            is ExplorerIntent.SelectFilesystem -> selectFilesystem(event)
            is ExplorerIntent.Refresh -> refreshList()

            is ExplorerIntent.Cut -> cutButton()
            is ExplorerIntent.Copy -> copyButton()
            is ExplorerIntent.Create -> createButton()
            is ExplorerIntent.Rename -> renameButton()
            is ExplorerIntent.Delete -> deleteButton()
            is ExplorerIntent.SelectAll -> selectAllButton()
            is ExplorerIntent.Properties -> propertiesButton()
            is ExplorerIntent.CopyPath -> copyPathButton()
            is ExplorerIntent.Compress -> compressButton()

            is ExplorerIntent.OpenFolder -> listFiles(event)
            is ExplorerIntent.OpenFileWith -> openFileAs(event)
            is ExplorerIntent.OpenFile -> openFile(event)
            is ExplorerIntent.CreateFile -> createFile(event)
            is ExplorerIntent.RenameFile -> renameFile(event)
            is ExplorerIntent.DeleteFile -> deleteFile()
            is ExplorerIntent.CutFile -> cutFile()
            is ExplorerIntent.CopyFile -> copyFile()
            is ExplorerIntent.CompressFile -> compressFile(event)
            is ExplorerIntent.ExtractFile -> extractFile(event)

            is ExplorerIntent.ShowHidden -> showHidden()
            is ExplorerIntent.HideHidden -> hideHidden()
            is ExplorerIntent.SortByName -> sortByName()
            is ExplorerIntent.SortBySize -> sortBySize()
            is ExplorerIntent.SortByDate -> sortByDate()

            is ExplorerIntent.SetWorkspaceRoot -> setRoot(event)
        }
    }

    private fun setRoot(event: ExplorerIntent.SetWorkspaceRoot) {
        rootFileModel = event.root
    }

    fun handleOnBackPressed(): Boolean {
        return when {
            selection.isNotEmpty() -> {
                _explorerViewState.value = ExplorerViewState.ActionBar(
                    breadcrumbs = breadcrumbs,
                    selection = selection.replaceList(emptyList()),
                    operation = operation,
                )
                true
            }
            breadcrumbs.size > 1 -> {
                _explorerViewState.value = ExplorerViewState.ActionBar(
                    breadcrumbs = breadcrumbs.replaceList(breadcrumbs - breadcrumbs.last()),
                    selection = selection.replaceList(emptyList()),
                    operation = operation,
                )
                listFiles(ExplorerIntent.OpenFolder(breadcrumbs.lastOrNull()))
                true
            }
            else -> false
        }
    }

    private fun listFiles(event: ExplorerIntent.OpenFolder) {
        viewModelScope.launch {
            try {
                if (!refreshState.value && query.isEmpty()) { // SwipeRefresh
                    _directoryViewState.value = DirectoryViewState.Loading
                }

                val fileModel = event.fileModel ?: rootFileModel
                val fileTree = explorerRepository.listFiles(fileModel)
                _explorerViewState.value = ExplorerViewState.ActionBar(
                    breadcrumbs = breadcrumbs.appendList(fileTree.parent),
                    selection = selection,
                    operation = operation,
                )
                if (fileTree.children.isNotEmpty()) {
                    _directoryViewState.value = DirectoryViewState.Files(fileTree.children)
                } else {
                    _directoryViewState.value = DirectoryViewState.Error(
                        image = R.drawable.ic_file_find,
                        title = stringProvider.getString(R.string.message_no_result),
                        subtitle = "",
                    )
                }
                files.replaceList(fileTree.children)
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                initialState()
                errorState(e)
            } finally {
                _refreshState.value = false
            }
        }
    }

    private fun searchFiles(event: ExplorerIntent.SearchFiles) {
        viewModelScope.launch {
            query = event.query
            val searchList = files.filter { it.name.contains(query, ignoreCase = true) }
            if (searchList.isNotEmpty()) {
                _directoryViewState.value = DirectoryViewState.Files(searchList)
            } else {
                _directoryViewState.value = DirectoryViewState.Error(
                    image = R.drawable.ic_file_find,
                    title = stringProvider.getString(R.string.message_no_result),
                    subtitle = "",
                )
            }
        }
    }

    private fun selectFiles(event: ExplorerIntent.SelectFiles) {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                selection = selection.replaceList(event.selection),
                operation = operation,
            )
        }
    }

    private fun selectTab(event: ExplorerIntent.SelectTab) {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs.replaceList(breadcrumbs.take(event.position + 1)),
                selection = selection.replaceList(emptyList()),
                operation = operation,
            )
            listFiles(ExplorerIntent.OpenFolder(breadcrumbs.lastOrNull()))
        }
    }

    private fun selectFilesystem(event: ExplorerIntent.SelectFilesystem) {
        viewModelScope.launch {
            try {
                if (dropdownPosition != event.position) {
                    dropdownPosition = event.position
                    explorerRepository.filesystem(event.position)
                    breadcrumbs.replaceList(emptyList())
                    files.replaceList(emptyList())
                    initialState()
                    listFiles(ExplorerIntent.OpenFolder())
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
    }

    private fun refreshList() {
        viewModelScope.launch {
            _refreshState.value = true
            listFiles(ExplorerIntent.OpenFolder(breadcrumbs.lastOrNull()))
        }
    }

    private fun cutButton() {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                operation = Operation.CUT.also { type ->
                    buffer.replaceList(selection)
                    operation = type
                },
                selection = selection.replaceList(emptyList()),
            )
        }
    }

    private fun copyButton() {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                operation = Operation.COPY.also { type ->
                    buffer.replaceList(selection)
                    operation = type
                },
                selection = selection.replaceList(emptyList()),
            )
        }
    }

    private fun createButton() {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                operation = Operation.CREATE.also { type ->
                    buffer.replaceList(emptyList()) // empty buffer for Operation.CREATE
                    operation = type
                },
                selection = selection.replaceList(emptyList()),
            )
            val screen = ExplorerScreen.CreateDialog
            _viewEvent.send(ViewEvent.Navigation(screen))
        }
    }

    private fun renameButton() {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                operation = Operation.RENAME.also { type ->
                    buffer.replaceList(selection)
                    operation = type
                },
                selection = selection.replaceList(emptyList()),
            )
            val screen = ExplorerScreen.RenameDialog(buffer.first().name)
            _viewEvent.send(ViewEvent.Navigation(screen))
        }
    }

    private fun deleteButton() {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                operation = Operation.DELETE.also { type ->
                    buffer.replaceList(selection)
                    operation = type
                },
                selection = selection.replaceList(emptyList()),
            )
            val screen = ExplorerScreen.DeleteDialog(buffer.first().name, buffer.size)
            _viewEvent.send(ViewEvent.Navigation(screen))
        }
    }

    private fun selectAllButton() {
        viewModelScope.launch {
            _customEvent.emit(ExplorerViewEvent.SelectAll)
        }
    }

    private fun propertiesButton() {
        viewModelScope.launch {
            try {
                val fileModel = selection.first()
                val screen = ExplorerScreen.PropertiesDialog(fileModel)
                _viewEvent.send(ViewEvent.Navigation(screen))
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun copyPathButton() {
        viewModelScope.launch {
            val fileModel = selection.first()
            _customEvent.emit(ExplorerViewEvent.CopyPath(fileModel))
            initialState()
        }
    }

    private fun compressButton() {
        viewModelScope.launch {
            _explorerViewState.value = ExplorerViewState.ActionBar(
                breadcrumbs = breadcrumbs,
                operation = Operation.COMPRESS.also { type ->
                    buffer.replaceList(selection)
                    operation = type
                },
                selection = selection.replaceList(emptyList()),
            )
            val screen = ExplorerScreen.CompressDialog
            _viewEvent.send(ViewEvent.Navigation(screen))
        }
    }

    private fun openFileAs(event: ExplorerIntent.OpenFileWith) {
        viewModelScope.launch {
            val fileModel = event.fileModel ?: selection.first()
            _customEvent.emit(ExplorerViewEvent.OpenFileWith(fileModel))
            initialState()
        }
    }

    private fun openFile(event: ExplorerIntent.OpenFile) {
        viewModelScope.launch {
            when (event.fileModel.type) {
                FileType.ARCHIVE -> extractFile(ExplorerIntent.ExtractFile(event.fileModel))
                FileType.DEFAULT,
                FileType.TEXT -> _customEvent.emit(ExplorerViewEvent.OpenFile(event.fileModel))
                else -> openFileAs(ExplorerIntent.OpenFileWith(event.fileModel))
            }
        }
    }

    private fun createFile(event: ExplorerIntent.CreateFile) {
        viewModelScope.launch {
            try {
                val isValid = event.fileName.isValidFileName()
                if (!isValid) {
                    _viewEvent.send(
                        ViewEvent.Toast(stringProvider.getString(R.string.message_invalid_file_name))
                    )
                    return@launch
                }
                val parent = breadcrumbs.last()
                val child = parent.copy(
                    fileUri = parent.fileUri + "/" + event.fileName,
                    directory = event.directory
                )
                explorerRepository.createFile(child)
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(1, Operation.CREATE)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun renameFile(event: ExplorerIntent.RenameFile) {
        viewModelScope.launch {
            try {
                val isValid = event.fileName.isValidFileName()
                if (!isValid) {
                    _viewEvent.send(
                        ViewEvent.Toast(stringProvider.getString(R.string.message_invalid_file_name))
                    )
                    return@launch
                }

                val originalFile = buffer.first()
                val renamedFile = originalFile.copy(
                    fileUri = originalFile.fileUri.substringBeforeLast('/') + "/" + event.fileName,
                    directory = originalFile.directory
                )
                explorerRepository.renameFile(originalFile, renamedFile)
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(1, Operation.RENAME)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun deleteFile() {
        viewModelScope.launch {
            try {
                explorerRepository.deleteFiles(buffer)
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(buffer.size, Operation.DELETE)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun cutFile() {
        viewModelScope.launch {
            try {
                explorerRepository.cutFiles(buffer, breadcrumbs.last())
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(buffer.size, Operation.CUT)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun copyFile() {
        viewModelScope.launch {
            try {
                explorerRepository.copyFiles(buffer, breadcrumbs.last())
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(buffer.size, Operation.COPY)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun compressFile(event: ExplorerIntent.CompressFile) {
        viewModelScope.launch {
            try {
                val isValid = event.fileName.isValidFileName()
                if (!isValid) {
                    _viewEvent.send(
                        ViewEvent.Toast(stringProvider.getString(R.string.message_invalid_file_name))
                    )
                    return@launch
                }
                val parent = breadcrumbs.last()
                val child = parent.copy(parent.path + "/" + event.fileName)
                explorerRepository.compressFiles(buffer, child)
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(buffer.size, Operation.COMPRESS)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun extractFile(event: ExplorerIntent.ExtractFile) {
        viewModelScope.launch {
            try {
                explorerRepository.extractFiles(event.fileModel, breadcrumbs.last())
                _viewEvent.send(
                    ViewEvent.Navigation(
                        ExplorerScreen.ProgressDialog(-1, Operation.EXTRACT)
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
                if (e is PermissionException) {
                    _directoryViewState.value = DirectoryViewState.Permission
                }
            } finally {
                initialState()
            }
        }
    }

    private fun showHidden() {
        settingsManager.showHidden = true
        showHidden = true
        refreshList()
    }

    private fun hideHidden() {
        settingsManager.showHidden = false
        showHidden = false
        refreshList()
    }

    private fun sortByName() {
        sortMode = FileSorter.SORT_BY_NAME.also {
            settingsManager.sortMode = it.toString()
            refreshList()
        }
    }

    private fun sortBySize() {
        sortMode = FileSorter.SORT_BY_SIZE.also {
            settingsManager.sortMode = it.toString()
            refreshList()
        }
    }

    private fun sortByDate() {
        sortMode = FileSorter.SORT_BY_DATE.also {
            settingsManager.sortMode = it.toString()
            refreshList()
        }
    }

    private fun initialState() {
        _explorerViewState.value = ExplorerViewState.ActionBar(
            breadcrumbs = breadcrumbs,
            operation = Operation.CREATE.also { type ->
                buffer.replaceList(emptyList())
                operation = type
            },
            selection = selection.replaceList(emptyList()),
        )
    }

    private fun errorState(e: Throwable) {
        when (e) {
            is PermissionException -> {
                _directoryViewState.value = DirectoryViewState.Permission
            }
            is DirectoryExpectedException -> {
                _directoryViewState.value = DirectoryViewState.Error(
                    image = R.drawable.ic_file_error,
                    title = stringProvider.getString(R.string.message_error_occurred),
                    subtitle = stringProvider.getString(R.string.message_directory_expected),
                )
            }
            else -> {
                _directoryViewState.value = DirectoryViewState.Error(
                    image = R.drawable.ic_file_error,
                    title = stringProvider.getString(R.string.message_error_occurred),
                    subtitle = e.message.orEmpty(),
                )
            }
        }
    }

    companion object {
        private const val TAG = "ExplorerViewModel"
    }
}