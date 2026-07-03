package com.pocketcodeagent.data.model

data class WorkspaceFile(
    val name: String,
    val uriString: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val children: List<WorkspaceFile> = emptyList()
)
