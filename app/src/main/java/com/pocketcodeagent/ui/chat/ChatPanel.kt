package com.pocketcodeagent.ui.chat

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.domain.agent.AgentAction
import com.pocketcodeagent.domain.agent.AgentArtifact
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.agent.registry.AgentRegistry
import com.pocketcodeagent.domain.agent.registry.RichAgentRole
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.AgentViewModel

// ─── Colors / Design tokens ──────────────────────────────────────────────────
private val PanelBg = Color(0xFF0E0E10)
private val InputBg  = Color(0xFF18181C)
private val ChipBg   = Color(0xFF1E1E24)
private val StreamBg = Color(0xFF14141A)

// ─── Main Composable ─────────────────────────────────────────────────────────
@Composable
fun ChatPanel(
    viewModel: AgentViewModel,
    provider: Provider?,
    workspaceUriString: String?,
    onReviewDiff: (List<FilePatch>) -> Unit,
    onAddFileAction: (AgentAction) -> Unit,
    onQueueCommand: (AgentAction.RunCommand) -> Unit,
    onOpenPreview: (AgentAction.OpenPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    val messages = viewModel.chatMessages
    val listState = rememberLazyListState()
    val canRunProvider = provider?.id == 999 || provider?.hasRequiredConfiguration() == true

    // Throttled auto-scroll: only on new final message or every 500 ms during streaming
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBg)
    ) {
        // ── Message List ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty() && !viewModel.isExecuting) {
                item(key = "empty-hint") {
                    ChatEmptyHint()
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onReviewDiff = { onReviewDiff(message.proposedPatches) },
                    onExecuteCommand = { viewModel.queueTerminalCommand(it) },
                    onAddFileAction = onAddFileAction,
                    onQueueCommand = onQueueCommand,
                    onOpenPreview = onOpenPreview
                )
            }

            // Streaming bubble – single stable item, no jitter
            if (viewModel.isExecuting) {
                item(key = "streaming-bubble") {
                    StreamingBubble(
                        roleName = viewModel.currentAgentRole?.displayName ?: "Agent",
                        text = viewModel.activeStreamingText
                    )
                }
            }
        }

        AgentModeRow(
            currentMode = viewModel.agentMode,
            onModeSelected = { viewModel.agentMode = it }
        )

        AgentActionRow(
            provider = provider,
            canRunProvider = canRunProvider,
            workspaceUriString = workspaceUriString,
            messages = messages,
            viewModel = viewModel,
            onReviewDiff = onReviewDiff
        )

        // ── Input Row ─────────────────────────────────────────────────────
        ChatInputRow(
            provider = provider,
            canRunProvider = canRunProvider,
            workspaceUriString = workspaceUriString,
            viewModel = viewModel
        )
    }
}

@Composable
private fun AgentModeRow(
    currentMode: AgentMode,
    onModeSelected: (AgentMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InputBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ModeChip("Discuss", selected = currentMode == AgentMode.DISCUSS, modifier = Modifier.weight(1f)) {
            onModeSelected(AgentMode.DISCUSS)
        }
        ModeChip("Build", selected = currentMode == AgentMode.BUILD, modifier = Modifier.weight(1f)) {
            onModeSelected(AgentMode.BUILD)
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF26324A) else ChipBg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
            Text(
                label,
                color = if (selected) TextPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ─── Streaming Bubble ────────────────────────────────────────────────────────
@Composable
private fun StreamingBubble(roleName: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StreamBg, shape = RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = CalmSage,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$roleName …",
                color = CalmSage,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (text.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

// ─── Agent Action Chips ───────────────────────────────────────────────────────
@Composable
private fun AgentActionRow(
    provider: Provider?,
    canRunProvider: Boolean,
    workspaceUriString: String?,
    messages: List<ChatMessage>,
    viewModel: AgentViewModel,
    onReviewDiff: (List<FilePatch>) -> Unit
) {
    val lastMessageHasPatches = messages.lastOrNull()?.proposedPatches?.isNotEmpty() == true
    var roleMenuExpanded by remember { mutableStateOf(false) }
    val selectedRole = viewModel.selectedRegistryRole

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InputBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Compact Role Selector ──────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                color = ChipBg,
                shape = RoundedCornerShape(6.dp),
                onClick = { roleMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = SlateBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        selectedRole.displayName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = roleMenuExpanded,
                onDismissRequest = { roleMenuExpanded = false },
                containerColor = Color(0xFF1E1E26),
                modifier = Modifier.heightIn(max = 380.dp)
            ) {
                AgentRegistry.ALL.forEach { role ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(role.displayName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(role.shortDescription, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                            }
                        },
                        onClick = {
                            viewModel.selectedRegistryRole = role
                            roleMenuExpanded = false
                        },
                        leadingIcon = {
                            Icon(
                                if (role.id == selectedRole.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (role.id == selectedRole.id) SlateBlue else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // ── Run Button ─────────────────────────────────────────────────────
        Surface(
            onClick = {
                provider?.let { viewModel.runAgentRole(it, selectedRole, workspaceUriString) }
            },
            enabled = canRunProvider,
            color = if (canRunProvider) SlateBlue else Color(0xFF2A2A2A),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.weight(0.6f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Text(
                    "Run",
                    color = if (canRunProvider) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Apply Button ───────────────────────────────────────────────────
        Surface(
            onClick = { messages.lastOrNull()?.proposedPatches?.let { onReviewDiff(it) } },
            enabled = lastMessageHasPatches,
            color = if (lastMessageHasPatches) CalmSage else Color(0xFF2A2A2A),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.weight(0.6f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    "Apply",
                    color = if (lastMessageHasPatches) Color(0xFF0E0E10) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ─── Chat Input Row ───────────────────────────────────────────────────────────
@Composable
private fun ChatInputRow(
    provider: Provider?,
    canRunProvider: Boolean,
    workspaceUriString: String?,
    viewModel: AgentViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InputBg)
            .navigationBarsPadding()
    ) {
        if (!canRunProvider) {
            Text(
                "Provider nicht konfiguriert",
                color = WarmCopper,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.userInput,
                onValueChange = { viewModel.userInput = it },
                placeholder = { Text("Aufgabe eingeben …", color = TextSecondary, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = SlateBlue,
                    unfocusedBorderColor = BorderGrey
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(Modifier.width(8.dp))

            if (viewModel.isExecuting) {
                IconButton(
                    onClick = { viewModel.stopAgent() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(WarmCopper)
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
            } else {
                val canSend = canRunProvider && viewModel.userInput.isNotBlank()
                IconButton(
                    onClick = {
                        provider?.let { viewModel.sendMessage(it, workspaceUriString, viewModel.userInput) }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (canSend) SlateBlue else Color(0xFF2A2A2A))
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

// ─── Empty Hint ───────────────────────────────────────────────────────────────
@Composable
private fun ChatEmptyHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            tint = Color(0xFF3A3A44),
            modifier = Modifier.size(48.dp)
        )
        Text(
            "Beschreibe deine Aufgabe",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "Der Agent plant, kodiert und prüft automatisch.",
            color = Color(0xFF505058),
            fontSize = 12.sp
        )
    }
}

// ─── Chat Bubble ─────────────────────────────────────────────────────────────
@Composable
fun ChatBubble(
    message: ChatMessage,
    onReviewDiff: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onAddFileAction: (AgentAction) -> Unit,
    onQueueCommand: (AgentAction.RunCommand) -> Unit,
    onOpenPreview: (AgentAction.OpenPreview) -> Unit
) {
    val isUser = !message.isAgent
    val hasArtifacts = message.artifacts.isNotEmpty()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = message.sender,
            color = if (isUser) SlateBlue else CalmSage,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF1A2535) else Color(0xFF18181C)
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.9f else 1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                if (message.artifacts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    message.artifacts.forEach { artifact ->
                        ArtifactCard(
                            artifact = artifact,
                            onAddFileAction = onAddFileAction,
                            onQueueCommand = onQueueCommand,
                            onOpenPreview = onOpenPreview
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Legacy fallback button for old parser output
                if (!hasArtifacts && message.proposedPatches.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onReviewDiff,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage),
                        border = ButtonDefaults.outlinedButtonBorder,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Compare, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${message.proposedPatches.size} Änderung(en) prüfen",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Command cards
                if (!hasArtifacts && message.proposedCommands.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Befehle:", color = TextSecondary, fontSize = 11.sp)
                    message.proposedCommands.forEach { cmd ->
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFF0E0E10),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cmd.command,
                                    color = Color(0xFFBBBBBB),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onExecuteCommand(cmd.command) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Queue", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactCard(
    artifact: AgentArtifact,
    onAddFileAction: (AgentAction) -> Unit,
    onQueueCommand: (AgentAction.RunCommand) -> Unit,
    onOpenPreview: (AgentAction.OpenPreview) -> Unit
) {
    val fileActions = artifact.actions.filter {
        it is AgentAction.CreateFile || it is AgentAction.ModifyFile || it is AgentAction.DeleteFile
    }

    Surface(
        color = Color(0xFF101014),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = SlateBlue, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    artifact.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (fileActions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { fileActions.forEach(onAddFileAction) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage)
                    ) {
                        Text("Review Changes", fontSize = 10.sp)
                    }
                }
            }

            artifact.parseWarnings.forEach { warning ->
                Text(warning, color = WarmCopper, fontSize = 10.sp, lineHeight = 14.sp)
            }

            artifact.actions.forEach { action ->
                AgentActionCard(
                    action = action,
                    onAddFileAction = onAddFileAction,
                    onQueueCommand = onQueueCommand,
                    onOpenPreview = onOpenPreview
                )
            }
        }
    }
}

@Composable
private fun AgentActionCard(
    action: AgentAction,
    onAddFileAction: (AgentAction) -> Unit,
    onQueueCommand: (AgentAction.RunCommand) -> Unit,
    onOpenPreview: (AgentAction.OpenPreview) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val accent = when (action) {
        is AgentAction.Note -> TextSecondary
        is AgentAction.CreateFile -> CalmSage
        is AgentAction.ModifyFile -> SlateBlue
        is AgentAction.DeleteFile -> WarmCopper
        is AgentAction.RunCommand -> riskColor(action.riskLevel)
        is AgentAction.OpenPreview -> CalmSage
    }

    Surface(
        color = Color(0xFF18181C),
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    action.typeLabel,
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    action.userVisibleTitle,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (action is AgentAction.RunCommand) {
                    RiskBadge(action.riskLevel)
                }
            }

            Text(action.safeSummary, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)

            when (action) {
                is AgentAction.CreateFile,
                is AgentAction.ModifyFile -> {
                    OutlinedButton(
                        onClick = { onAddFileAction(action) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("Add to Diff", fontSize = 10.sp)
                    }
                }
                is AgentAction.DeleteFile -> {
                    OutlinedButton(
                        onClick = { onAddFileAction(action) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCopper),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("Review Delete", fontSize = 10.sp)
                    }
                }
                is AgentAction.RunCommand -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(action.command)) },
                            enabled = action.riskLevel != CommandRiskLevel.BLOCKED,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Copy Command", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { onQueueCommand(action) },
                            enabled = action.riskLevel != CommandRiskLevel.BLOCKED,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Add to Queue", fontSize = 10.sp)
                        }
                    }
                    if (action.riskLevel == CommandRiskLevel.BLOCKED) {
                        Text("Blockiert: Command wird nicht kopiert oder in die Queue gelegt.", color = WarmCopper, fontSize = 10.sp)
                    }
                }
                is AgentAction.OpenPreview -> {
                    OutlinedButton(
                        onClick = { onOpenPreview(action) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("Open Preview", fontSize = 10.sp)
                    }
                }
                is AgentAction.Note -> Unit
            }
        }
    }
}

@Composable
private fun RiskBadge(riskLevel: CommandRiskLevel) {
    val color = riskColor(riskLevel)
    Text(
        riskLevel.name,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

private fun riskColor(riskLevel: CommandRiskLevel): Color {
    return when (riskLevel) {
        CommandRiskLevel.SAFE -> CalmSage
        CommandRiskLevel.CAUTION -> Color(0xFFF0AD4E)
        CommandRiskLevel.BLOCKED -> WarmCopper
    }
}
