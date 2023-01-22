package com.tyron.workspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tyron.workspace.domain.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WorkspaceSelectorViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
}