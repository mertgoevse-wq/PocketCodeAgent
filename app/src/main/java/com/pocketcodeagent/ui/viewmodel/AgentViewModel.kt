package com.pocketcodeagent.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.repository.AgentRepository
import com.pocketcodeagent.data.repository.ProviderRepository
import com.pocketcodeagent.data.repository.SessionRepository
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.AgentRunState
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.agent.CommandRiskScanner
import com.pocketcodeagent.domain.security.OwnerSecurityManager
import com.pocketcodeagent.domain.terminal.CommandSource
import com.pocketcodeagent.domain.terminal.CommandStatus
import com.pocketcodeagent.domain.terminal.TerminalCommand
import com.pocketcodeagent.domain.agent.registry.AgentRegistry
import com.pocketcodeagent.domain.agent.registry.RichAgentRole
import com.pocketcodeagent.domain.skill.Skill
import com.pocketcodeagent.domain.skill.SkillPromptBuilder
import com.pocketcodeagent.domain.skill.SkillRegistry
import com.pocketcodeagent.domain.context.WorkspaceContext
import com.pocketcodeagent.domain.preview.PreviewTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgentViewModel(
    private val agentRepository: AgentRepository,
    private val providerRepository: ProviderRepository,
    private val sessionRepository: SessionRepository,
    private val ownerSecurityManager: OwnerSecurityManager
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

    // Context tracking
    var lastContext by mutableStateOf<WorkspaceContext?>(null)
    var lastContextFileCount by mutableStateOf(0)
    var lastContextWarnings by mutableStateOf<List<String>>(emptyList())

    // Session tracking
    var sessionId by mutableStateOf(0)
        private set
    private var sessionInitialized = false

    // Active agent coroutine job for cancellation (Stop Button)
    private var agentJob: Job? = null

    // Terminal Commands State
    val terminalCommands = mutableStateListOf<TerminalCommand>()
    val executedCommands = mutableStateListOf<String>()

    // Diagnostic logs
    val recentLogsFlow: StateFlow<List<com.pocketcodeagent.data.local.entity.LogEntity>> =
        providerRepository.getRecentLogsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val session = sessionRepository.loadLastSession()
            if (session != null) {
                sessionId = session.id
                sessionInitialized = true

                // Restore agent mode
                agentMode = sessionRepository.restoreAgentMode(session.agentMode)

                // Restore role selection
                session.selectedRoleId?.let { roleId ->
                    AgentRegistry.findById(roleId)?.let { selectedRegistryRole = it }
                }

                // Restore skill selection
                session.selectedSkillId?.let { skillId ->
                    selectedSkill = SkillRegistry.findById(skillId)
                }

                // Restore messages
                val messageEntities = sessionRepository.loadMessages(sessionId)
                chatMessages.clear()
                messageEntities.forEach { entity ->
                    chatMessages.add(
                        ChatMessage(
                            id = entity.id,
                            sender = entity.sender,
                            message = entity.content,
                            timestamp = entity.timestamp,
                            isAgent = entity.role == "assistant"
                        )
                    )
                }

                // Restore terminal commands
                val commandEntities = sessionRepository.loadTerminalCommands(sessionId)
                terminalCommands.clear()
                terminalCommands.addAll(commandEntities)
            }
        }
    }

    fun newSession() {
        viewModelScope.launch {
            if (sessionId > 0) {
                sessionRepository.deleteSession(sessionId)
            }
            chatMessages.clear()
            terminalCommands.clear()
            executedCommands.clear()
            activeStreamingText = ""
            runState = AgentRunState.Idle
            lastContext = null
            lastContextFileCount = 0
            lastContextWarnings = emptyList()
            sessionId = 0
            sessionInitialized = false
        }
    }

    fun sendMessage(
        provider: Provider,
        rootUriString: String?,
        userText: String,
        pendingChanges: List<FilePatch> = emptyList(),
        activeFileName: String? = null,
        previewTarget: PreviewTarget = PreviewTarget.None
    ) {
        if (userText.trim().isEmpty()) return

        val userMsg = ChatMessage(sender = "User", message = userText, isAgent = false)
        chatMessages.add(userMsg)
        userInput = ""

        runAgentRole(provider, selectedRegistryRole, rootUriString, userText, pendingChanges, activeFileName, previewTarget, userMsg)
    }

    private suspend fun ensureSession(title: String, workspaceUri: String?, provider: Provider?) {
        if (sessionId > 0) return
        val session = sessionRepository.createSession(
            title = title.take(80),
            workspaceUri = workspaceUri,
            providerId = provider?.id,
            model = provider?.modelName,
            roleId = selectedRegistryRole.id,
            skillId = selectedSkill?.id,
            mode = agentMode.name
        )
        sessionId = session.id
        sessionInitialized = true
    }

    /** Applies a skill: sets recommended role and mode, clears selection on re-tap. */
    fun applySkill(skill: Skill) {
        if (selectedSkill?.id == skill.id) {
            selectedSkill = null
            persistSessionState()
            return
        }
        selectedSkill = skill
        AgentRegistry.findById(skill.recommendedRoleId)?.let { role ->
            selectedRegistryRole = role
        }
        agentMode = skill.mode
        persistSessionState()
    }

    private fun persistSessionState() {
        if (sessionId > 0) {
            viewModelScope.launch {
                try {
                    sessionRepository.updateSessionState(
                        sessionId = sessionId,
                        roleId = selectedRegistryRole.id,
                        skillId = selectedSkill?.id,
                        mode = agentMode.name
                    )
                } catch (_: Exception) { }
            }
        }
    }

    fun stopAgent() {
        agentJob?.cancel()
        agentJob = null
        isExecuting = false
        currentAgentRole = null
        runState = AgentRunState.Cancelled
        if (activeStreamingText.isNotEmpty()) {
            val cancelledMsg = ChatMessage(
                sender = "Agent",
                message = activeStreamingText + "\n\n[Vom Benutzer gestoppt]",
                isAgent = true,
                agentRole = AgentRole.PLANNER
            )
            chatMessages.add(cancelledMsg)
            if (sessionId > 0) {
                viewModelScope.launch { sessionRepository.saveMessage(cancelledMsg, sessionId) }
            }
        }
        activeStreamingText = ""
    }

    fun runAgentRole(
        provider: Provider,
        role: RichAgentRole,
        rootUriString: String?,
        userTask: String = "",
        pendingChanges: List<FilePatch> = emptyList(),
        activeFileName: String? = null,
        previewTarget: PreviewTarget = PreviewTarget.None,
        userMessage: ChatMessage? = null
    ) {
        val legacyRole = AgentRegistry.toLegacyOrPlanner(role)

        if (ownerSecurityManager.isApiCallBlocked() && provider.id != 999) {
            val blockedMsg = ChatMessage(
                sender = "System",
                message = "API-Aufrufe sind durch Emergency Stop deaktiviert. Deaktiviere den Emergency Stop in den Settings, um fortzufahren.",
                isAgent = true,
                agentRole = legacyRole
            )
            chatMessages.add(blockedMsg)
            return
        }

        agentJob?.cancel()
        agentJob = viewModelScope.launch {
            // Ensure session exists before saving anything
            ensureSession(userTask, rootUriString, provider)

            // Save user message now that session is guaranteed to exist
            userMessage?.let { sessionRepository.saveMessage(it, sessionId) }

            isExecuting = true
            currentAgentRole = legacyRole
            activeStreamingText = ""
            runState = if (legacyRole == AgentRole.PLANNER) AgentRunState.Planning else AgentRunState.Streaming

            // Build workspace context (builder runs on Dispatchers.IO internally)
            val context = agentRepository.buildWorkspaceContext(
                userTask = userTask,
                selectedRoleId = role.id,
                selectedSkillId = selectedSkill?.id,
                activeFilePath = activeFileName,
                pendingChanges = pendingChanges,
                terminalCommands = terminalCommands.toList(),
                previewTarget = previewTarget
            )
            lastContext = context
            lastContextFileCount = context.relevantFiles.size + context.buildFiles.size
            lastContextWarnings = context.warnings

            // Build skill-augmented user message if skill is selected
            val skillAugmentedMessage = selectedSkill?.let { skill ->
                SkillPromptBuilder.build(skill, userTask, context, role, agentMode)
            }

            // Replace last user message content with skill-augmented version for API
            val history = if (skillAugmentedMessage != null) {
                chatMessages.dropLast(1) + ChatMessage(
                    sender = "User",
                    message = skillAugmentedMessage,
                    isAgent = false
                )
            } else {
                chatMessages.toList()
            }
            var lastAgentMessage: ChatMessage? = null

            var bufferedText = ""
            var lastUpdate = System.currentTimeMillis()

            agentRepository.runAgent(
                provider = provider,
                role = legacyRole,
                agentMode = agentMode,
                history = history,
                rootUriString = rootUriString,
                workspaceContext = context,
                onChunk = { chunk ->
                    runState = AgentRunState.Streaming
                    bufferedText += chunk
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 150) {
                        activeStreamingText = bufferedText
                        lastUpdate = now
                    }
                }
            )
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

                // Persist agent message + artifacts
                if (sessionId > 0) {
                    sessionRepository.saveMessage(it, sessionId)
                    it.artifacts.forEach { artifact ->
                        sessionRepository.saveArtifact(artifact, sessionId)
                    }
                }

                if (it.proposedCommands.isNotEmpty()) {
                    it.proposedCommands.forEach { cmd ->
                        val risk = CommandRiskScanner.scan(cmd.command)
                        val tc = TerminalCommand(
                            command = cmd.command,
                            reason = cmd.reason ?: "Vom Agenten vorgeschlagen",
                            riskLevel = risk,
                            source = CommandSource.AGENT
                        )
                        terminalCommands.add(tc)
                        if (sessionId > 0) {
                            sessionRepository.saveTerminalCommand(tc, sessionId)
                        }
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
        val cmd = TerminalCommand(
            command = command,
            reason = null,
            riskLevel = risk,
            source = CommandSource.USER
        )
        terminalCommands.add(cmd)
        if (sessionId > 0) {
            viewModelScope.launch { sessionRepository.saveTerminalCommand(cmd, sessionId) }
        }
    }

    fun markCommandCopied(commandId: String) {
        val idx = terminalCommands.indexOfFirst { it.id == commandId }
        if (idx >= 0) {
            terminalCommands[idx] = terminalCommands[idx].markCopied()
            persistCommandStatus(commandId, CommandStatus.COPIED)
        }
    }

    fun markCommandDone(commandId: String) {
        val idx = terminalCommands.indexOfFirst { it.id == commandId }
        if (idx >= 0) {
            val cmd = terminalCommands[idx]
            terminalCommands[idx] = cmd.markDone()
            persistCommandStatus(commandId, CommandStatus.MARKED_DONE)
            if (!executedCommands.contains(cmd.command)) {
                executedCommands.add(cmd.command)
            }
        }
    }

    fun rejectCommand(commandId: String) {
        val idx = terminalCommands.indexOfFirst { it.id == commandId }
        if (idx >= 0) {
            terminalCommands[idx] = terminalCommands[idx].reject()
            persistCommandStatus(commandId, CommandStatus.REJECTED)
        }
    }

    private fun persistCommandStatus(commandId: String, status: CommandStatus) {
        if (sessionId > 0) {
            viewModelScope.launch {
                try { sessionRepository.updateCommandStatus(commandId, status) } catch (_: Exception) { }
            }
        }
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
