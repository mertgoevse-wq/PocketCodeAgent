package com.pocketcodeagent.data.model

/** Legacy enum used as parameter type by AgentRepository, ChatMessage, and AgentViewModel.
 *  New role system: com.pocketcodeagent.domain.agent.registry.RichAgentRole via AgentRegistry. */
enum class AgentRole(val displayName: String) {
    PLANNER("Planner"),
    CODER("Coder"),
    REVIEWER("Reviewer"),
    FIXER("Fixer"),
    PREVIEW("Preview"),
    TERMINAL("Terminal")
}
