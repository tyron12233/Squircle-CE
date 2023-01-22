package com.tyron.workspace.domain.model

data class Workspace(
    val uuid: String,
    val rootUri: String,
    val dateOpened: Long
)