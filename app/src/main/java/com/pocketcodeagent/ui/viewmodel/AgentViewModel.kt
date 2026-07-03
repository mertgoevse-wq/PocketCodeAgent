package com.pocketcodeagent.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.repository.AgentRepository
import com.pocketcodeagent.data.repository.ProviderRepository
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.AgentRunState
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.agent.CommandRiskScanner
import com.pocketcodeagent.domain.terminal.CommandSource
import com.pocketcodeagent.domain.terminal.CommandStatus
import com.pocketcodeagent.domain.terminal.TerminalCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgentViewModel(
    private val agentRepository: AgentRepository,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    val chatMessages = mutableStateListOf<ChatMessage>()

    var userInput by mutableStateOf("")
    var isExecuting by mutableStateOf(false)
    var currentAgentRole by mutableStateOf<AgentRole?>(null)
    var activeStreamingText by mutableStateOf("")
    var agentMode by mutableStateOf(AgentMode.DISCUSS)
    var runState by mutableStateOf<AgentRunState>(AgentRunState.Idle)

    // Active agent coroutine job for cancellation (Stop Button)
    private var agentJob: Job? = null

    // Terminal Commands State
    val terminalCommands = mutableStateListOf<TerminalCommand>()
    val executedCommands = mutableStateListOf<String>()

    // Diagnostic logs
    val recentLogsFlow: StateFlow<List<com.pocketcodeagent.data.local.entity.LogEntity>> =
        providerRepository.getRecentLogsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(provider: Provider, rootUriString: String?, userText: String) {
        if (userText.trim().isEmpty()) return

        val userMsg = ChatMessage(sender = "User", message = userText, isAgent = false)
        chatMessages.add(userMsg)
        userInput = ""

        runAgentRole(provider, AgentRole.PLANNER, rootUriString)
    }

    fun stopAgent() {
        agentJob?.cancel()
        agentJob = null
        isExecuting = false
        currentAgentRole = null
        runState = AgentRunState.Cancelled
        if (activeStreamingText.isNotEmpty()) {
            chatMessages.add(
                ChatMessage(
                    sender = "Agent",
                    message = activeStreamingText + "\n\n[Vom Benutzer gestoppt]",
                    isAgent = true,
                    agentRole = AgentRole.PLANNER
                )
            )
        }
        activeStreamingText = ""
    }

    fun runAgentRole(provider: Provider, role: AgentRole, rootUriString: String?) {
        agentJob?.cancel()
        agentJob = viewModelScope.launch {
            isExecuting = true
            currentAgentRole = role
            activeStreamingText = ""
            runState = if (role == AgentRole.PLANNER) AgentRunState.Planning else AgentRunState.Streaming

            val history = chatMessages.toList()
            var lastAgentMessage: ChatMessage? = null

            var bufferedText = ""
            var lastUpdate = System.currentTimeMillis()

            agentRepository.runAgent(provider, role, agentMode, history, rootUriString) { chunk ->
                runState = AgentRunState.Streaming
                bufferedText += chunk
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 150) {
                    activeStreamingText = bufferedText
                    lastUpdate = now
                }
            }
                .collect { finalMessage ->
                    activeStreamingText = bufferedText
                    lastAgentMessage = finalMessage
                }

            isExecuting = false
            currentAgentRole = null
            activeStreamingText = ""

            lastAgentMessage?.let {
                runState = AgentRunState.ParsingActions
                chatMessages.add(it)
                if (it.proposedCommands.isNotEmpty()) {
                    it.proposedCommands.forEach { cmd ->
                        val risk = CommandRiskScanner.scan(cmd.command)
                        terminalCommands.add(
                            TerminalCommand(
                                command = cmd.command,
                                reason = cmd.reason ?: "Vom Agenten vorgeschlagen",
                                riskLevel = risk,
                                source = CommandSource.AGENT
                            )
                        )
                    }
                }
                runState = if (it.proposedPatches.isNotEmpty() || it.proposedCommands.isNotEmpty()) {
                    AgentRunState.WaitingForApproval
                } else {
                    AgentRunState.Done
                }
            } ?: run {
                runState = AgentRunState.Done
            }
        }
    }

    fun queueTerminalCommand(command: String) {
        if (command.isBlank()) return
        if (terminalCommands.any { it.command == command && it.status == CommandStatus.QUEUED }) return
        val risk = CommandRiskScanner.scan(command)
        terminalCommands.add(
            TerminalCommand(
                command = command,
                reason = null,
                riskLevel = risk,
                source = CommandSource.USER
            )
        )
    }

    fun markCommandCopied(commandId: String) {
        val idx = terminalCommands.indexOfFirst { it.id == commandId }
        if (idx >= 0) terminalCommands[idx] = terminalCommands[idx].markCopied()
    }

    fun markCommandDone(commandId: String) {
        val idx = terminalCommands.indexOfFirst { it.id == commandId }
        if (idx >= 0) {
            val cmd = terminalCommands[idx]
            terminalCommands[idx] = cmd.markDone()
            if (!executedCommands.contains(cmd.command)) {
                executedCommands.add(cmd.command)
            }
        }
    }

    fun rejectCommand(commandId: String) {
        val idx = terminalCommands.indexOfFirst { it.id == commandId }
        if (idx >= 0) terminalCommands[idx] = terminalCommands[idx].reject()
    }

    fun rejectAllCommands() {
        val updated = terminalCommands.map { if (it.status != CommandStatus.REJECTED) it.reject() else it }
        terminalCommands.clear()
        terminalCommands.addAll(updated)
    }

    fun clearCompletedCommands() {
        terminalCommands.removeAll { it.status == CommandStatus.MARKED_DONE }
    }

    fun clearRejectedCommands() {
        terminalCommands.removeAll { it.status == CommandStatus.REJECTED }
    }

    fun clearChat() {
        chatMessages.clear()
        terminalCommands.clear()
    }

    fun clearLogs() {
        viewModelScope.launch {
            providerRepository.clearLogs()
        }
    }
}
