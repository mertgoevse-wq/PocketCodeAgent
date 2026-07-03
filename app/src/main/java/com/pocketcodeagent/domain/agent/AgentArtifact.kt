package com.pocketcodeagent.domain.agent

data class AgentArtifact(
    val id: String,
    val title: String,
    val actions: List<AgentAction>,
    val rawText: String,
    val parseWarnings: List<String> = emptyList()
)
