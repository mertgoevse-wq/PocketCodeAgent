package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.ui.theme.CalmSage
import com.pocketcodeagent.ui.theme.DarkSurface
import com.pocketcodeagent.ui.theme.DeepSlateBackground
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.theme.TextPrimary
import com.pocketcodeagent.ui.theme.TextSecondary
import com.pocketcodeagent.ui.theme.WarmCopper
import com.pocketcodeagent.ui.viewmodel.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAgentScreen(
    viewModel: AgentViewModel,
    provider: Provider?,
    workspaceUriString: String?,
    onReviewDiff: (List<FilePatch>) -> Unit,
    onBackClick: () -> Unit
) {
    val messages = viewModel.chatMessages
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom
    LaunchedEffect(messages.size, viewModel.activeStreamingText.length) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Coding Agents Chat 🤖", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            text = provider?.name?.let { "Active: $it" } ?: "Simulierter Demo-Modus",
                            style = MaterialTheme.typography.bodySmall,
                            color = CalmSage
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = WarmCopper)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = DeepSlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Initial onboarding prompt helper if empty
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Gib eine Aufgabe ein und starte die Agenten-Rolle!",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onReviewDiff = { onReviewDiff(message.proposedPatches) },
                        onExecuteCommand = { viewModel.queueTerminalCommand(it) }
                    )
                }

                // Streaming indicator
                if (viewModel.isExecuting) {
                    item {
                        val activeRole = viewModel.currentAgentRole?.displayName ?: "Agent"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CalmSage,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$activeRole generiert Antwort...",
                                    color = CalmSage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (viewModel.activeStreamingText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.activeStreamingText,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Quick Agent Role Actions Bar (Always show role templates for fast workflows)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0C1B))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.runAgentRole(provider ?: com.pocketcodeagent.data.model.Provider(id = 999, name = "Demo", baseUrl = "", apiKey = "", modelName = ""), AgentRole.PLANNER, workspaceUriString) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Plan", color = TextPrimary, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.runAgentRole(provider ?: com.pocketcodeagent.data.model.Provider(id = 999, name = "Demo", baseUrl = "", apiKey = "", modelName = ""), AgentRole.CODER, workspaceUriString) },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Code", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.runAgentRole(provider ?: com.pocketcodeagent.data.model.Provider(id = 999, name = "Demo", baseUrl = "", apiKey = "", modelName = ""), AgentRole.REVIEWER, workspaceUriString) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Review", color = TextPrimary, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.runAgentRole(provider ?: com.pocketcodeagent.data.model.Provider(id = 999, name = "Demo", baseUrl = "", apiKey = "", modelName = ""), AgentRole.FIXER, workspaceUriString) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Fix", color = TextPrimary, fontSize = 11.sp)
                }

                val lastMessageHasPatches = messages.lastOrNull()?.proposedPatches?.isNotEmpty() == true
                Button(
                    onClick = { 
                        messages.lastOrNull()?.proposedPatches?.let { onReviewDiff(it) }
                    },
                    enabled = lastMessageHasPatches,
                    colors = ButtonDefaults.buttonColors(containerColor = CalmSage, disabledContainerColor = Color.DarkGray),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Apply", color = if (lastMessageHasPatches) Color(0xFF0F0C1B) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0C1B))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.userInput,
                    onValueChange = { viewModel.userInput = it },
                    placeholder = { Text("Aufgabe an Agenten formulieren...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SlateBlue,
                        unfocusedBorderColor = Color(0xFF2E2D34)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val activeProv = provider ?: com.pocketcodeagent.data.model.Provider(id = 999, name = "Demo", baseUrl = "", apiKey = "", modelName = "")
                        viewModel.sendMessage(activeProv, workspaceUriString, viewModel.userInput)
                    },
                    enabled = viewModel.userInput.isNotEmpty() && !viewModel.isExecuting,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (viewModel.userInput.isNotEmpty() && !viewModel.isExecuting) SlateBlue else Color.DarkGray)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onReviewDiff: () -> Unit,
    onExecuteCommand: (String) -> Unit
) {
    val isUser = !message.isAgent
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Name tag
        Text(
            text = message.sender,
            color = if (isUser) SlateBlue else CalmSage,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Bubble Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF1B2A36) else DarkSurface
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 12.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Diffs Banner
                if (message.proposedPatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onReviewDiff,
                        colors = ButtonDefaults.buttonColors(containerColor = CalmSage)
                    ) {
                        Icon(imageVector = Icons.Default.Compare, contentDescription = null, tint = Color(0xFF0F0C1B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Änderungen prüfen (${message.proposedPatches.size})",
                            color = Color(0xFF0F0C1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Command Executor list
                if (message.proposedCommands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Empfohlene Befehle:", style = MaterialTheme.typography.titleSmall, color = CalmSage)
                    for (cmd in message.proposedCommands) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cmd.command,
                                        color = Color.LightGray,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onExecuteCommand(cmd.command) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Run", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                                if (cmd.reason.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Grund: ${cmd.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
