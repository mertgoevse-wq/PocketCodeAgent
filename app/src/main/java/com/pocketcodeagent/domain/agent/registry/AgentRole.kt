package com.pocketcodeagent.domain.agent.registry

import com.pocketcodeagent.domain.agent.AgentMode

enum class RoleRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class RichAgentRole(
    val id: String,
    val displayName: String,
    val shortDescription: String,
    val systemInstructions: String,
    val allowedModes: Set<AgentMode>,
    val defaultTemperature: Double = 0.3,
    val riskLevel: RoleRiskLevel = RoleRiskLevel.LOW
)
