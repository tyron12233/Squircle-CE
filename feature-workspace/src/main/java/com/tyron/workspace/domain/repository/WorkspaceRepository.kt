package com.tyron.workspace.domain.repository

import com.tyron.workspace.domain.model.Workspace

interface WorkspaceRepository {

    suspend fun recentWorkspaces(): List<Workspace>

    suspend fun workspaceOpened(path: String)
}