package com.pocketcodeagent.domain.skill

import com.pocketcodeagent.domain.agent.AgentMode

enum class SkillCategory(val displayName: String) {
    ANDROID("Android"),
    WEB("Web"),
    DEBUGGING("Debugging"),
    REFACTORING("Refactoring"),
    SECURITY("Security"),
    PERFORMANCE("Performance"),
    PREVIEW("Preview"),
    RELEASE("Release")
}

data class Skill(
    val id: String,
    val displayName: String,
    val category: SkillCategory,
    val description: String,
    val promptTemplate: String,
    val recommendedRoleId: String,
    val mode: AgentMode
)
