package com.blacksquircle.ui.core.data.storage.database.dao.workspace

import androidx.room.Dao
import androidx.room.Query
import com.blacksquircle.ui.core.data.storage.database.dao.base.BaseDao
import com.blacksquircle.ui.core.data.storage.database.entity.workspace.WorkspaceEntity
import com.blacksquircle.ui.core.data.storage.database.utils.Tables

@Dao
abstract class WorkspaceDao : BaseDao<WorkspaceEntity> {

    @Query("SELECT * FROM `${Tables.WORKSPACES}` ORDER BY `dateOpened` ASC")
    abstract suspend fun loadAll(): List<WorkspaceEntity>

    @Query("DELETE FROM `${Tables.WORKSPACES}`")
    abstract suspend fun deleteAll()
}