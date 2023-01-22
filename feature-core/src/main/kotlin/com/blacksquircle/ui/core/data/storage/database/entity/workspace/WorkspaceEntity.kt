package com.blacksquircle.ui.core.data.storage.database.entity.workspace

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.blacksquircle.ui.core.data.storage.database.utils.Tables

@Entity(tableName = Tables.WORKSPACES)
data class WorkspaceEntity (

    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "path")
    val rootUri: String,

    @ColumnInfo(name = "dateOpened")
    val dateOpened: Long
)