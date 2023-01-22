package com.tyron.workspace.data.converter

import com.blacksquircle.ui.core.data.storage.database.entity.workspace.WorkspaceEntity
import com.tyron.workspace.domain.model.Workspace

object WorkspaceConverter {

    fun toModel(entity: WorkspaceEntity) : Workspace {
        return Workspace(
            uuid = entity.uuid,
            rootUri = entity.rootUri,
            dateOpened = entity.dateOpened
        )
    }
}