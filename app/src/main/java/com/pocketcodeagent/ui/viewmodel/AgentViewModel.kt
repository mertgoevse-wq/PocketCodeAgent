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

    // Terminal Commands State
    val recommendedCommands = mutableStateListOf<String>()
    val executedCommands = mutableStateListOf<String>()

    // Diagnostic logs
    val recentLogsFlow: StateFlow<List<com.pocketcodeagent.data.local.entity.LogEntity>> = providerRepository.getRecentLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(provider: Provider, rootUriString: String?, userText: String) {
        if (userText.trim().isEmpty()) return
        
        val userMsg = ChatMessage(sender = "User", message = userText, isAgent = false)
        chatMessages.add(userMsg)
        userInput = ""

        runAgentRole(provider, AgentRole.PLANNER, rootUriString)
    }

    fun runAgentRole(provider: Provider, role: AgentRole, rootUriString: String?) {
        viewModelScope.launch {
            isExecuting = true
            currentAgentRole = role
            activeStreamingText = ""

            val history = chatMessages.toList()
            var lastAgentMessage: ChatMessage? = null

            agentRepository.runAgent(provider, role, history, rootUriString) { chunk ->
                activeStreamingText += chunk
            }
                .collect { finalMessage ->
                    lastAgentMessage = finalMessage
                }

            isExecuting = false
            currentAgentRole = null
            activeStreamingText = ""

            lastAgentMessage?.let {
                chatMessages.add(it)
                if (it.proposedCommands.isNotEmpty()) {
                    recommendedCommands.addAll(it.proposedCommands.map { cmd -> cmd.command })
                }
            }
        }
    }

    fun executeTerminalCommand(command: String) {
        recommendedCommands.remove(command)
        executedCommands.add(command)
        viewModelScope.launch {
            providerRepository.log("TERMINAL", "Executing command (mocked): $command")
        }
    }

    fun rejectTerminalCommand(command: String) {
        recommendedCommands.remove(command)
    }

    fun clearChat() {
        chatMessages.clear()
        recommendedCommands.clear()
    }

    fun clearLogs() {
        viewModelScope.launch {
            providerRepository.clearLogs()
        }
    }
}
