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
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.GlowPink
import com.pocketcodeagent.ui.theme.NeonPurple
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
    LaunchedEffect(messages.size, viewModel.activeStreamingText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Coding Agents Chat", color = Color.White)
                        Text(
                            text = provider?.name?.let { "Active: $it" } ?: "No Active Provider",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricTeal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = Color(0xFF0C0A14)
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
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onReviewDiff = { onReviewDiff(message.proposedPatches) },
                        onExecuteCommand = { viewModel.executeTerminalCommand(it) }
                    )
                }

                // Streaming indicator
                if (viewModel.isExecuting) {
                    item {
                        val activeRole = viewModel.currentAgentRole?.displayName ?: "AI Agent"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(Color(0xFF1E1A33).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ElectricTeal,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$activeRole is generating...",
                                    color = ElectricTeal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (viewModel.activeStreamingText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.activeStreamingText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Quick Agent Role Actions Bar
            if (provider != null && !viewModel.isExecuting && messages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161324))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.runAgentRole(provider, AgentRole.PLANNER, workspaceUriString) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A47)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Plan", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.runAgentRole(provider, AgentRole.CODER, workspaceUriString) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Code", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.runAgentRole(provider, AgentRole.REVIEWER, workspaceUriString) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A47)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review", color = Color.White, fontSize = 12.sp)
                    }
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
                    placeholder = { Text("Ask the agents to build or fix something...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0xFF2E2A47)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        provider?.let {
                            viewModel.sendMessage(it, workspaceUriString, viewModel.userInput)
                        }
                    },
                    enabled = provider != null && viewModel.userInput.isNotEmpty() && !viewModel.isExecuting,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (provider != null && viewModel.userInput.isNotEmpty() && !viewModel.isExecuting) NeonPurple else Color.DarkGray)
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
            color = if (isUser) ElectricTeal else GlowPink,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Bubble
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF1E1A33) else Color(0xFF161324)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Diffs Banner
                if (message.proposedPatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onReviewDiff,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Compare, contentDescription = null, tint = Color(0xFF0C0A14), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Review proposed code changes (${message.proposedPatches.size})",
                            color = Color(0xFF0C0A14),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Command Executor list
                if (message.proposedCommands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Recommended Commands:", style = MaterialTheme.typography.titleSmall, color = ElectricTeal)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Run", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                                if (cmd.reason.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reason: ${cmd.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
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
