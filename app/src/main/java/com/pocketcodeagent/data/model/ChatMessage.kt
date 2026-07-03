package com.pocketcodeagent.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAgent: Boolean = false,
    val agentRole: AgentRole? = null,
    val proposedPatches: List<FilePatch> = emptyList(),
    val proposedCommands: List<AgentCommand> = emptyList(),
    var isCommandExecuted: Boolean = false,
    var isApplied: Boolean = false
)
