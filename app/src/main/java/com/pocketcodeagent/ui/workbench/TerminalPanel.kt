package com.pocketcodeagent.ui.workbench

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.terminal.CommandSource
import com.pocketcodeagent.domain.terminal.CommandStatus
import com.pocketcodeagent.domain.terminal.TerminalCommand
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.AgentViewModel

private val TerminalBg = Color(0xFF090909)
private val PromptColor = Color(0xFF50FA7B)

@Composable
fun TerminalPanel(
    viewModel: AgentViewModel,
    onOpenTermux: () -> Unit = {},
    onSetPreviewUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val commands = viewModel.terminalCommands
    var showCautionDialog by remember { mutableStateOf<TerminalCommand?>(null) }
    var showTermuxSetup by remember { mutableStateOf(false) }
    var showGitWorkflow by remember { mutableStateOf(false) }
    val termuxInstalled = isTermuxInstalled(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Terminal, null, tint = PromptColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Terminal", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            val queuedCount = commands.count { it.status == CommandStatus.QUEUED }
            if (queuedCount > 0) {
                Text("Queue: $queuedCount", color = TextSecondary, fontSize = 11.sp)
            }
        }

        HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 0.5.dp)

        // Termux notice
        Surface(
            color = Color(0xFF0D1117),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (termuxInstalled) Icons.Default.CheckCircle else Icons.Default.Info,
                    null,
                    tint = if (termuxInstalled) CalmSage else SlateBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (termuxInstalled) "Termux installiert — Commands kopieren und dort ausfuehren."
                    else "Termux nicht erkannt. Installiere Termux manuell (F-Droid / GitHub).",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (termuxInstalled) {
                    TextButton(
                        onClick = {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
                                if (intent != null) context.startActivity(intent) else onOpenTermux()
                            } catch (_: Exception) { onOpenTermux() }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Open", color = CalmSage, fontSize = 10.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 0.5.dp)

        // Global action buttons
        val activeCommands = commands.filter { it.status != CommandStatus.REJECTED }
        if (activeCommands.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D12))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val safeCommands = activeCommands.filter {
                    it.status == CommandStatus.QUEUED && it.riskLevel == CommandRiskLevel.SAFE
                }
                if (safeCommands.isNotEmpty()) {
                    TextButton(
                        onClick = { safeCommands.forEach { copyCommand(context, it) } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Copy all safe", color = CalmSage, fontSize = 10.sp)
                    }
                }

                TextButton(
                    onClick = { viewModel.clearCompletedCommands() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear done", color = Color(0xFF777783), fontSize = 10.sp)
                }

                TextButton(
                    onClick = { viewModel.clearRejectedCommands() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear rejected", color = Color(0xFF777783), fontSize = 10.sp)
                }

                TextButton(
                    onClick = { viewModel.rejectAllCommands() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Reject all", color = WarmCopper, fontSize = 10.sp)
                }

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = { showTermuxSetup = !showTermuxSetup },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid, null,
                        tint = SlateBlue, modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Setup", color = SlateBlue, fontSize = 10.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 0.5.dp)
        }

        // Termux setup card
        AnimatedVisibility(visible = showTermuxSetup, enter = expandVertically(), exit = shrinkVertically()) {
            TermuxSetupCard(context, onSetPreviewUrl)
        }

        // Git workflow toggle button
        Surface(
            onClick = { showGitWorkflow = !showGitWorkflow },
            color = Color(0xFF0D1117),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Code,
                    null,
                    tint = SlateBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Push via Termux (Git Workflow)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (showGitWorkflow) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 0.5.dp)

        // Git workflow card
        AnimatedVisibility(visible = showGitWorkflow, enter = expandVertically(), exit = shrinkVertically()) {
            GitWorkflowCard(context)
        }

        // Command queue
        if (activeCommands.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Terminal, null, tint = Color(0xFF222230), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Keine Commands vorgeschlagen.", color = Color(0xFF444450), fontSize = 12.sp)
                    Text("Agenten koennen Commands vorschlagen, die hier gesammelt werden.", color = Color(0xFF333340), fontSize = 10.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(activeCommands, key = { it.id }) { cmd ->
                    CommandCard(
                        command = cmd,
                        onCopy = {
                            when (cmd.riskLevel) {
                                CommandRiskLevel.SAFE -> {
                                    copyCommand(context, cmd)
                                    viewModel.markCommandCopied(cmd.id)
                                }
                                CommandRiskLevel.CAUTION -> showCautionDialog = cmd
                                CommandRiskLevel.BLOCKED -> { /* blocked — no copy */ }
                            }
                        },
                        onMarkDone = { viewModel.markCommandDone(cmd.id) },
                        onReject = { viewModel.rejectCommand(cmd.id) },
                        onSetPreviewUrl = onSetPreviewUrl
                    )
                }
            }
        }

        // CAUTION warning dialog
        showCautionDialog?.let { cmd ->
            AlertDialog(
                onDismissRequest = { showCautionDialog = null },
                containerColor = Color(0xFF1A1A24),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF0AD4E), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vorsicht", color = Color(0xFFF0AD4E), fontSize = 15.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Dieses Kommando kann Dateien, Dependencies oder Build-Zustaende veraendern.",
                            color = TextSecondary, fontSize = 12.sp
                        )
                        cmd.reason?.let { reason ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("Grund: ", color = Color(0xFF777783), fontSize = 11.sp)
                                Text(reason, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                        }
                        Surface(color = Color(0xFF0E0E10), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                cmd.safeDisplay,
                                color = Color(0xFFFFB86C),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            copyCommand(context, cmd)
                            viewModel.markCommandCopied(cmd.id)
                            showCautionDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0AD4E))
                    ) {
                        Text("Copy anyway", color = Color.Black, fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCautionDialog = null }) {
                        Text("Cancel", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            )
        }
    }
}

// ─── Command Card ─────────────────────────────────────────────────────────

@Composable
private fun CommandCard(
    command: TerminalCommand,
    onCopy: () -> Unit,
    onMarkDone: () -> Unit,
    onReject: () -> Unit,
    onSetPreviewUrl: (String) -> Unit
) {
    Surface(
        color = Color(0xFF111118),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Risk + Status badges
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RiskBadge(command.riskLevel)
                StatusBadge(command.status)
                Spacer(Modifier.weight(1f))
                Text(
                    command.source.name,
                    color = Color(0xFF444450),
                    fontSize = 9.sp
                )
            }

            // Command text
            Surface(color = Color(0xFF0A0A0A), shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    command.safeDisplay,
                    color = Color(0xFFCCCCCC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Reason
            command.reason?.let { reason ->
                Text(reason.take(120), color = Color(0xFF555560), fontSize = 10.sp, lineHeight = 14.sp)
            }

            // Action buttons — depends on risk level
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (command.riskLevel) {
                    CommandRiskLevel.SAFE -> {
                        CompactButton("Copy", CalmSage, command.status == CommandStatus.QUEUED) { onCopy() }
                        CompactButton("Done", Color(0xFF777783), command.status == CommandStatus.QUEUED || command.status == CommandStatus.COPIED) { onMarkDone() }
                    }
                    CommandRiskLevel.CAUTION -> {
                        CompactButton("Copy (verify)", Color(0xFFF0AD4E), command.status == CommandStatus.QUEUED) { onCopy() }
                        CompactButton("Done", Color(0xFF777783), true) { onMarkDone() }
                    }
                    CommandRiskLevel.BLOCKED -> {
                        Text(
                            "Blockiert — kann nicht kopiert werden.",
                            color = WarmCopper,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (command.riskLevel != CommandRiskLevel.BLOCKED) {
                    Spacer(Modifier.weight(1f))
                }
                CompactButton("Reject", WarmCopper, command.status != CommandStatus.REJECTED) { onReject() }

                // Preview hint for npm run dev
                if (command.command.contains("npm run dev", ignoreCase = true) && command.riskLevel != CommandRiskLevel.BLOCKED) {
                    CompactButton("Set Preview", SlateBlue, true) {
                        onSetPreviewUrl("http://127.0.0.1:5173")
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(risk: CommandRiskLevel) {
    val color = when (risk) {
        CommandRiskLevel.SAFE -> CalmSage
        CommandRiskLevel.CAUTION -> Color(0xFFF0AD4E)
        CommandRiskLevel.BLOCKED -> WarmCopper
    }
    Surface(color = color.copy(alpha = 0.13f), shape = RoundedCornerShape(4.dp)) {
        Text(risk.name, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

@Composable
private fun StatusBadge(status: CommandStatus) {
    val (label, color) = when (status) {
        CommandStatus.QUEUED -> "Queued" to Color(0xFF777783)
        CommandStatus.COPIED -> "Copied" to SlateBlue
        CommandStatus.MARKED_DONE -> "Done" to CalmSage
        CommandStatus.REJECTED -> "Rejected" to WarmCopper
        CommandStatus.BLOCKED -> "Blocked" to WarmCopper
    }
    Surface(color = color.copy(alpha = 0.10f), shape = RoundedCornerShape(4.dp)) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

@Composable
private fun RowScope.CompactButton(label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, color = if (enabled) color else Color(0xFF444450), fontSize = 10.sp)
    }
}

// ─── Termux Setup Card ────────────────────────────────────────────────────

@Composable
private fun TermuxSetupCard(context: Context, onSetPreviewUrl: (String) -> Unit) {
    Surface(
        color = Color(0xFF0D1117),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Termux Dev-Server Setup", color = SlateBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                "Fuer React/Vite/Node-Projekte laeuft der Dev-Server in Termux. PocketCodeAgent zeigt die Preview per WebView ueber http://127.0.0.1:5173.",
                color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp
            )

            val setupCmds = listOf(
                "pkg update" to "Paketliste aktualisieren",
                "pkg install nodejs git" to "Node.js & Git installieren",
                "termux-setup-storage" to "Speicherzugriff erlauben",
                "cd /sdcard/Download/DEIN_PROJEKT" to "In Projektverzeichnis wechseln",
                "npm install" to "Dependencies installieren",
                "npm run dev -- --host 127.0.0.1" to "Dev-Server starten (Port 5173)"
            )

            setupCmds.forEach { (cmd, desc) ->
                Surface(
                    color = Color(0xFF13131A),
                    shape = RoundedCornerShape(6.dp),
                    onClick = { copyText(context, cmd) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cmd, color = Color(0xFFCCCCCC), fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(desc, color = Color(0xFF444450), fontSize = 9.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF444450), modifier = Modifier.size(10.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                TextButton(
                    onClick = { copyText(context, setupCmds.joinToString(" && ") { it.first }) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Alles kopieren", color = CalmSage, fontSize = 10.sp)
                }
                TextButton(
                    onClick = { onSetPreviewUrl("http://127.0.0.1:5173") },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Set Preview URL", color = SlateBlue, fontSize = 10.sp)
                }
            }
        }
    }
}

// ─── Git Workflow Card ────────────────────────────────────────────────────

@Composable
private fun GitWorkflowCard(context: Context) {
    Surface(color = Color(0xFF0D1117), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Git Workflow (Push via Termux)", color = SlateBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)

            Text(
                "PocketCodeAgent fuehrt nichts automatisch aus. Pruefe Dateien und Secrets vor git push. Kopiere die Commands und fuehre sie in Termux aus.",
                color = WarmCopper,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            val gitCommands = listOf(
                "git status" to "Geaenderte Dateien anzeigen",
                "git add ." to "Alle Aenderungen stagen",
                "git commit -m \"Update PocketCodeAgent project\"" to "Commit erstellen",
                "git remote -v" to "Remote-URL pruefen",
                "git push" to "Push ausfuehren"
            )

            gitCommands.forEach { (cmd, desc) ->
                Surface(
                    color = Color(0xFF13131A),
                    shape = RoundedCornerShape(6.dp),
                    onClick = { copyText(context, cmd) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cmd, color = Color(0xFFCCCCCC), fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(desc, color = Color(0xFF444450), fontSize = 9.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF444450), modifier = Modifier.size(10.dp))
                    }
                }
            }

            TextButton(
                onClick = { copyText(context, gitCommands.joinToString(" && ") { it.first }) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Alle Git-Commands kopieren", color = CalmSage, fontSize = 10.sp)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────

private fun isTermuxInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.termux", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

private fun copyCommand(context: Context, command: TerminalCommand) {
    copyText(context, command.command)
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text))
}
