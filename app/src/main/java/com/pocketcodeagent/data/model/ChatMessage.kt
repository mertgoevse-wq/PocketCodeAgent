package com.pocketcodeagent.data.model

data class ProposedFileChange(
    val relativePath: String,
    val originalContent: String,
    val newContent: String,
    var isApplied: Boolean = false
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAgent: Boolean = false,
    val agentRole: AgentRole? = null,
    val proposedChanges: List<ProposedFileChange> = emptyList(),
    val proposedCommands: List<String> = emptyList(),
    var isCommandExecuted: Boolean = false
)
