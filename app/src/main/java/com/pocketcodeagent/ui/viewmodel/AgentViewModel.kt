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
import com.pocketcodeagent.domain.agent.registry.AgentRegistry
import com.pocketcodeagent.domain.agent.registry.RichAgentRole
import com.pocketcodeagent.domain.skill.Skill
import com.pocketcodeagent.domain.skill.SkillPromptBuilder
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

    // Registry role selection
    var selectedRegistryRole by mutableStateOf<RichAgentRole>(AgentRegistry.PLANNER)

    // Skill selection
    var selectedSkill by mutableStateOf<Skill?>(null)

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

        val skill = selectedSkill
        val finalMessage = if (skill != null) {
            SkillPromptBuilder.build(
                skill = skill,
                userTask = userText,
                workspaceSummary = rootUriString ?: "No workspace",
                openFile = null,
                role = selectedRegistryRole,
                agentMode = agentMode
            )
        } else {
            userText
        }

        val userMsg = ChatMessage(sender = "User", message = finalMessage, isAgent = false)
        chatMessages.add(userMsg)
        userInput = ""

        runAgentRole(provider, selectedRegistryRole, rootUriString)
    }

    /** Applies a skill: sets recommended role and mode, clears selection on re-tap. */
    fun applySkill(skill: Skill) {
        if (selectedSkill?.id == skill.id) {
            selectedSkill = null
            return
        }
        selectedSkill = skill
        AgentRegistry.findById(skill.recommendedRoleId)?.let { role ->
            selectedRegistryRole = role
        }
        agentMode = skill.mode
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

    fun runAgentRole(provider: Provider, role: RichAgentRole, rootUriString: String?) {
        val legacyRole = AgentRegistry.toLegacyOrPlanner(role)
        agentJob?.cancel()
        agentJob = viewModelScope.launch {
            isExecuting = true
            currentAgentRole = legacyRole
            activeStreamingText = ""
            runState = if (legacyRole == AgentRole.PLANNER) AgentRunState.Planning else AgentRunState.Streaming

            val history = chatMessages.toList()
            var lastAgentMessage: ChatMessage? = null

            var bufferedText = ""
            var lastUpdate = System.currentTimeMillis()

            agentRepository.runAgent(provider, legacyRole, agentMode, history, rootUriString) { chunk ->
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

    /** Legacy overload for old AgentRole enum callers (ChatPanel, ChatAgentScreen). */
    fun runAgentRole(provider: Provider, role: AgentRole, rootUriString: String?) {
        runAgentRole(provider, AgentRegistry.fromLegacy(role), rootUriString)
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
