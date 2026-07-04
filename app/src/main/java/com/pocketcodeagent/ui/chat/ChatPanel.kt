package com.pocketcodeagent.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.pocketcodeagent.domain.context.WorkspaceContext
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.skill.Skill
import com.pocketcodeagent.domain.skill.SkillCategory
import com.pocketcodeagent.domain.skill.SkillRegistry
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.AgentViewModel

private val PanelBg = Color(0xFF0E0E10)
private val InputBg  = Color(0xFF18181C)
private val ChipBg   = Color(0xFF1E1E24)
private val StreamBg = Color(0xFF14141A)

@Composable
fun ChatPanel(
    viewModel: AgentViewModel,
    provider: Provider?,
    workspaceUriString: String?,
    pendingChanges: List<FilePatch>,
    activeFileName: String?,
    previewTarget: PreviewTarget,
    onReviewDiff: (List<FilePatch>) -> Unit,
    onAddFileAction: (AgentAction) -> Unit,
    onQueueCommand: (AgentAction.RunCommand) -> Unit,
    onOpenPreview: (AgentAction.OpenPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    val messages = viewModel.chatMessages
    val listState = rememberLazyListState()
    val canRunProvider = provider?.id == 999 || provider?.hasRequiredConfiguration() == true
    val hasPendingChanges = pendingChanges.isNotEmpty()

    var showContextDialog by remember { mutableStateOf(false) }
    var showNewSessionConfirm by remember { mutableStateOf(false) }
    var showSkillSheet by remember { mutableStateOf(false) }
    var showRoleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(PanelBg)
    ) {
        // ── Message List ──────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty() && !viewModel.isExecuting) {
                item(key = "empty-hint") { ChatEmptyHint() }
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
            if (viewModel.isExecuting) {
                item(key = "streaming-bubble") {
                    StreamingBubble(
                        roleName = viewModel.currentAgentRole?.displayName ?: "Agent",
                        text = viewModel.activeStreamingText
                    )
                }
            }
        }

        // ── Demo badge ────────────────────────────────────────────────────
        if (provider?.id == 999) {
            Surface(
                color = SlateBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, tint = SlateBlue, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Demo — keine echte API", color = SlateBlue, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Row A: Context Chip + New Session ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().background(InputBg).padding(horizontal = 6.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContextChipRow(
                context = viewModel.lastContext,
                fileCount = viewModel.lastContextFileCount,
                warnings = viewModel.lastContextWarnings,
                onShowDialog = { showContextDialog = true },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { showNewSessionConfirm = true },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Add, "New Session", tint = TextSecondary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(1.dp))
                Text("New", color = TextSecondary, fontSize = 9.sp)
            }
        }

        // ── Row B: Discuss/Build + Skill Chip ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().background(InputBg).padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactModeChip("Discuss", selected = viewModel.agentMode == AgentMode.DISCUSS, modifier = Modifier.weight(1f)) {
                    viewModel.agentMode = AgentMode.DISCUSS
                }
                CompactModeChip("Build", selected = viewModel.agentMode == AgentMode.BUILD, modifier = Modifier.weight(1f)) {
                    viewModel.agentMode = AgentMode.BUILD
                }
            }
            val skill = viewModel.selectedSkill
            Surface(
                color = if (skill != null) Color(0xFF26324A) else ChipBg,
                shape = RoundedCornerShape(4.dp),
                onClick = { showSkillSheet = true }
            ) {
                Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        skill?.displayName ?: "Skill",
                        color = if (skill != null) SlateBlue else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                }
            }
        }

        // ── Row C: Role Chip + Run/Stop + Apply ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().background(InputBg).padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedRole = viewModel.selectedRegistryRole
            Surface(
                color = ChipBg,
                shape = RoundedCornerShape(5.dp),
                onClick = { showRoleSheet = true },
                modifier = Modifier.weight(1f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, tint = SlateBlue, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(selectedRole.displayName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                }
            }

            if (viewModel.isExecuting) {
                Surface(
                    onClick = { viewModel.stopAgent() },
                    color = WarmCopper,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Icon(Icons.Default.Stop, "Stop", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            } else {
                Surface(
                    onClick = {
                        provider?.let { viewModel.runAgentRole(it, selectedRole, workspaceUriString, "", pendingChanges, activeFileName, previewTarget) }
                    },
                    enabled = canRunProvider,
                    color = if (canRunProvider) SlateBlue else Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Run", color = if (canRunProvider) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (hasPendingChanges) {
                Surface(
                    onClick = { onReviewDiff(pendingChanges) },
                    color = CalmSage,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Apply", color = Color(0xFF0E0E10), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Row D: Input + Send ───────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().background(InputBg).navigationBarsPadding()
        ) {
            if (!canRunProvider && provider?.id != 999) {
                Text("Provider nicht konfiguriert", color = WarmCopper, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.userInput,
                    onValueChange = { viewModel.userInput = it },
                    placeholder = { Text("Beschreibe, was der Agent bauen soll...", color = TextSecondary, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SlateBlue,
                        unfocusedBorderColor = BorderGrey
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp, max = 100.dp),
                    shape = RoundedCornerShape(18.dp),
                    maxLines = 4
                )
                Spacer(Modifier.width(6.dp))
                val canSend = canRunProvider && viewModel.userInput.isNotBlank()
                Surface(
                    onClick = {
                        provider?.let { viewModel.sendMessage(it, workspaceUriString, viewModel.userInput, pendingChanges, activeFileName, previewTarget) }
                    },
                    enabled = canSend,
                    color = if (canSend) SlateBlue else Color(0xFF2A2A2A),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Send, "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // ── Skill BottomSheet ─────────────────────────────────────────────────
    if (showSkillSheet) {
        SkillBottomSheet(
            selected = viewModel.selectedSkill,
            onSelect = { viewModel.applySkill(it) },
            onClear = { viewModel.selectedSkill = null },
            onDismiss = { showSkillSheet = false }
        )
    }

    // ── Role BottomSheet ─────────────────────────────────────────────────
    if (showRoleSheet) {
        RoleBottomSheet(
            selected = viewModel.selectedRegistryRole,
            onSelect = { viewModel.selectedRegistryRole = it },
            onDismiss = { showRoleSheet = false }
        )
    }

    // ── Context Preview Dialog ────────────────────────────────────────────
    if (showContextDialog) {
        ContextPreviewDialog(
            context = viewModel.lastContext,
            fileCount = viewModel.lastContextFileCount,
            onDismiss = { showContextDialog = false }
        )
    }

    if (showNewSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showNewSessionConfirm = false },
            containerColor = Color(0xFF1A1A22),
            title = { Text("Neue Session", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Alle Chat-Nachrichten und Commands der aktuellen Session werden unwiderruflich geloescht.", color = TextSecondary, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.newSession(); showNewSessionConfirm = false }) {
                    Text("Loeschen", color = WarmCopper, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showNewSessionConfirm = false }) { Text("Abbrechen", color = TextSecondary) } }
        )
    }
}

// ─── Skill BottomSheet ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillBottomSheet(
    selected: Skill?,
    onSelect: (Skill) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text("Skill auswählen", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            HorizontalDivider(color = Color(0xFF2A2A34), thickness = 0.5.dp)

            Surface(
                color = Color.Transparent,
                onClick = { onClear(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("None (ohne Skill)", color = TextSecondary, fontSize = 12.sp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(SkillRegistry.ALL, key = { it.id }) { skill ->
                    val isSelected = skill.id == selected?.id
                    Surface(
                        color = if (isSelected) Color(0xFF26324A) else Color.Transparent,
                        onClick = { onSelect(skill); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(skill.displayName, color = if (isSelected) SlateBlue else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${skill.category.displayName} · ${skill.mode.name}", color = TextSecondary, fontSize = 10.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Role BottomSheet ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleBottomSheet(
    selected: RichAgentRole,
    onSelect: (RichAgentRole) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text("Agent-Rolle wählen", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            HorizontalDivider(color = Color(0xFF2A2A34), thickness = 0.5.dp)

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(AgentRegistry.ALL, key = { it.id }) { role ->
                    val isSelected = role.id == selected.id
                    Surface(
                        color = if (isSelected) Color(0xFF26324A) else Color.Transparent,
                        onClick = { onSelect(role); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(role.displayName, color = if (isSelected) SlateBlue else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(role.shortDescription, color = TextSecondary, fontSize = 10.sp, maxLines = 2)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Compact Mode Chip ──────────────────────────────────────────────────────
@Composable
private fun CompactModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF26324A) else ChipBg,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
            Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

// ─── Streaming Bubble ────────────────────────────────────────────────────────
@Composable
private fun StreamingBubble(roleName: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(StreamBg, shape = RoundedCornerShape(10.dp)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CalmSage, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("$roleName …", color = CalmSage, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        if (text.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(text = text, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

// ─── Empty Hint ───────────────────────────────────────────────────────────────
@Composable
private fun ChatEmptyHint() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Psychology, null, tint = Color(0xFF3A3A44), modifier = Modifier.size(48.dp))
        Text("Beschreibe deine Aufgabe", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("Der Agent plant, kodiert und prüft automatisch.", color = Color(0xFF505058), fontSize = 12.sp)
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
            colors = CardDefaults.cardColors(containerColor = if (isUser) Color(0xFF1A2535) else Color(0xFF18181C)),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = if (isUser) 12.dp else 2.dp, bottomEnd = if (isUser) 2.dp else 12.dp),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.9f else 1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.message, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)

                if (message.artifacts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    message.artifacts.forEach { artifact ->
                        ArtifactCard(artifact = artifact, onAddFileAction = onAddFileAction, onQueueCommand = onQueueCommand, onOpenPreview = onOpenPreview)
                        Spacer(Modifier.height(8.dp))
                    }
                }

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
                        Text("${message.proposedPatches.size} Änderung(en) prüfen", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (!hasArtifacts && message.proposedCommands.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Befehle:", color = TextSecondary, fontSize = 11.sp)
                    message.proposedCommands.forEach { cmd ->
                        Spacer(Modifier.height(4.dp))
                        Surface(color = Color(0xFF0E0E10), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = cmd.command, color = Color(0xFFBBBBBB), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onExecuteCommand(cmd.command) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text("Queue", fontSize = 10.sp) }
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
    val fileActions = artifact.actions.filter { it is AgentAction.CreateFile || it is AgentAction.ModifyFile || it is AgentAction.DeleteFile }
    Surface(color = Color(0xFF101014), shape = RoundedCornerShape(8.dp), border = ButtonDefaults.outlinedButtonBorder, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, null, tint = SlateBlue, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(artifact.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (fileActions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { fileActions.forEach(onAddFileAction) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage)
                    ) { Text("Review Changes", fontSize = 10.sp) }
                }
            }
            artifact.parseWarnings.forEach { warning -> Text(warning, color = WarmCopper, fontSize = 10.sp, lineHeight = 14.sp) }
            artifact.actions.forEach { action -> AgentActionCard(action = action, onAddFileAction = onAddFileAction, onQueueCommand = onQueueCommand, onOpenPreview = onOpenPreview) }
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
    Surface(color = Color(0xFF18181C), shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(action.typeLabel, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp))
                Spacer(Modifier.width(7.dp))
                Text(action.userVisibleTitle, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (action is AgentAction.RunCommand) { RiskBadge(action.riskLevel) }
            }
            Text(action.safeSummary, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            when (action) {
                is AgentAction.CreateFile, is AgentAction.ModifyFile -> {
                    OutlinedButton(onClick = { onAddFileAction(action) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text("Add to Diff", fontSize = 10.sp) }
                }
                is AgentAction.DeleteFile -> {
                    OutlinedButton(onClick = { onAddFileAction(action) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCopper), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text("Review Delete", fontSize = 10.sp) }
                }
                is AgentAction.RunCommand -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(action.command)) }, enabled = action.riskLevel != CommandRiskLevel.BLOCKED, colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text("Copy Command", fontSize = 10.sp) }
                        OutlinedButton(onClick = { onQueueCommand(action) }, enabled = action.riskLevel != CommandRiskLevel.BLOCKED, colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text("Add to Queue", fontSize = 10.sp) }
                    }
                    if (action.riskLevel == CommandRiskLevel.BLOCKED) { Text("Blockiert: Command wird nicht kopiert oder in die Queue gelegt.", color = WarmCopper, fontSize = 10.sp) }
                }
                is AgentAction.OpenPreview -> {
                    OutlinedButton(onClick = { onOpenPreview(action) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text("Open Preview", fontSize = 10.sp) }
                }
                is AgentAction.Note -> Unit
            }
        }
    }
}

@Composable
private fun RiskBadge(riskLevel: CommandRiskLevel) {
    val color = riskColor(riskLevel)
    Text(riskLevel.name, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
}

private fun riskColor(riskLevel: CommandRiskLevel): Color = when (riskLevel) {
    CommandRiskLevel.SAFE -> CalmSage
    CommandRiskLevel.CAUTION -> Color(0xFFF0AD4E)
    CommandRiskLevel.BLOCKED -> WarmCopper
}

// ─── Context Chip ────────────────────────────────────────────────────────────
@Composable
private fun ContextChipRow(
    context: WorkspaceContext?,
    fileCount: Int,
    warnings: List<String>,
    onShowDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (context == null) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = ChipBg, shape = RoundedCornerShape(4.dp), onClick = onShowDialog, modifier = Modifier.height(20.dp)) {
            Row(modifier = Modifier.padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = SlateBlue, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(2.dp))
                Text("$fileCount files", color = SlateBlue, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("~${formatContextChars(context.estimatedChars)}", color = TextSecondary, fontSize = 8.sp)
        if (warnings.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = WarmCopper, modifier = Modifier.size(9.dp))
                Text("${warnings.size}", color = WarmCopper, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ContextPreviewDialog(context: WorkspaceContext?, fileCount: Int, onDismiss: () -> Unit) {
    if (context == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        title = { Text("Workspace Context", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Workspace", context.workspaceName ?: "None", SlateBlue)
                InfoRow("Files", "$fileCount relevant", CalmSage)
                InfoRow("Est. chars", "~${formatContextChars(context.estimatedChars)}", TextSecondary)
                if (context.activeFilePath != null) { InfoRow("Active file", context.activeFilePath, CalmSage) }
                if (context.buildFiles.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Build files:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    context.buildFiles.forEach { bf -> Text("  ${bf.path} (${bf.reason})", color = Color(0xFF666670), fontSize = 10.sp) }
                }
                if (context.relevantFiles.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Relevant files:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    context.relevantFiles.forEach { rf -> Text("  ${rf.path} (${rf.reason})", color = Color(0xFF666670), fontSize = 10.sp) }
                }
                if (context.warnings.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Warnings:", color = WarmCopper, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    context.warnings.forEach { w -> Text("  ⚠ $w", color = WarmCopper, fontSize = 10.sp) }
                }
                if (context.pendingChangesSummary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Pending changes:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(context.pendingChangesSummary, color = Color(0xFF666670), fontSize = 10.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = SlateBlue, fontWeight = FontWeight.SemiBold) } }
    )
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = TextSecondary, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatContextChars(chars: Int): String = when {
    chars >= 1000 -> "${chars / 1000}K"
    else -> "$chars"
}
