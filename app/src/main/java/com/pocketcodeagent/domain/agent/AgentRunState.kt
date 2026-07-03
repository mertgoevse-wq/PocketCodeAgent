package com.pocketcodeagent.domain.agent

sealed class AgentRunState {
    data object Idle : AgentRunState()
    data object Planning : AgentRunState()
    data object Streaming : AgentRunState()
    data object ParsingActions : AgentRunState()
    data object WaitingForApproval : AgentRunState()
    data object Applying : AgentRunState()
    data object Error : AgentRunState()
    data object Done : AgentRunState()
    data object Cancelled : AgentRunState()
}
