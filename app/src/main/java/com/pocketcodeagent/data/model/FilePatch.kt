package com.pocketcodeagent.data.model

data class FilePatch(
    val path: String,
    val action: String, // "create", "modify", "delete"
    val oldText: String,
    val newText: String
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
