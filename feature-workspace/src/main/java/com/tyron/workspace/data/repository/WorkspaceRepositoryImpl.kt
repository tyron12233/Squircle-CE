package com.tyron.workspace.data.repository

import androidx.core.net.toUri
import com.blacksquircle.ui.core.data.factory.FilesystemFactory
import com.blacksquircle.ui.core.data.storage.database.AppDatabase
import com.blacksquircle.ui.core.data.storage.database.entity.workspace.WorkspaceEntity
import com.blacksquircle.ui.core.domain.coroutine.DispatcherProvider
import com.tyron.workspace.data.converter.WorkspaceConverter
import com.tyron.workspace.domain.model.Workspace
import com.tyron.workspace.domain.repository.WorkspaceRepository
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class WorkspaceRepositoryImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val appDatabase: AppDatabase,
) : WorkspaceRepository {

    override suspend fun recentWorkspaces(): List<Workspace> {
        return withContext(dispatcherProvider.io()) {
            appDatabase.workspaceDao().loadAll()
                .map(WorkspaceConverter::toModel)
        }
    }

    override suspend fun workspaceOpened(path: String) {
        return withContext(dispatcherProvider.io()) {
            val file = File(path)
            val uri = file.toUri()
            val entity = WorkspaceEntity(
                uuid = UUID.fromString(path).toString(),
                rootUri = uri.toString(),
                dateOpened = System.currentTimeMillis()
            )

            appDatabase.workspaceDao().insert(entity)
        }
    }


}