package com.pocketcodeagent.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class FilePatchAction {
    @SerializedName("create")
    CREATE,

    @SerializedName("modify")
    MODIFY,

    @SerializedName("delete")
    DELETE
}

enum class FilePatchStatus {
    PENDING,
    APPLIED,
    REJECTED,
    CONFLICT,
    FAILED
}

enum class FilePatchSource {
    AGENT,
    USER,
    IMPORT
}

data class FilePatch(
    val id: String = "patch-${UUID.randomUUID()}",
    val path: String,
    val action: FilePatchAction,
    val oldText: String? = null,
    val newText: String? = null,
    val status: FilePatchStatus = FilePatchStatus.PENDING,
    val source: FilePatchSource = FilePatchSource.AGENT,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val requiresSecondConfirmation: Boolean = action == FilePatchAction.DELETE,
    val deleteConfirmed: Boolean = false,
    val replaceWholeFile: Boolean = false
)

data class AgentCommand(
    val command: String,
    val reason: String,
    val requiresConfirmation: Boolean
)

data class AgentResponse(
    val summary: String,
    val patches: List<FilePatch>,
    val commands: List<AgentCommand>
)
