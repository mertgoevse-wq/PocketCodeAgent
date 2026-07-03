package com.pocketcodeagent.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
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
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.GlowPink
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: AgentViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val recommended = viewModel.terminalCommands
    val executed = viewModel.executedCommands
    var manualCommand by remember { mutableStateOf("") }

    // Dialog state for confirming execution
    var commandToConfirm by remember { mutableStateOf<String?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    // Check if Termux is installed on the system
    val isTermuxInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Helper function to check for dangerous commands
    fun getDangerousWarning(command: String): String? {
        val cmdLower = command.lowercase().trim()
        if (cmdLower.contains("rm -rf") || cmdLower.contains("rm -r")) {
            return "⚠️ GEFAHR: Dieser Befehl löscht Verzeichnisse rekursiv und unwiderruflich!"
        }
        if (cmdLower.contains("chmod 777")) {
            return "⚠️ GEFAHR: Setzt maximale Berechtigungen (777). Dies ist ein Sicherheitsrisiko!"
        }
        if (cmdLower.contains("curl") && (cmdLower.contains("| sh") || cmdLower.contains("| bash"))) {
            return "⚠️ GEFAHR: Lädt ein externes Skript herunter und führt es sofort aus (Remote Code Execution)!"
        }
        if (cmdLower.contains("sudo")) {
            return "ℹ️ INFO: Android Sandbox blockiert Root-Befehle. 'sudo' wird fehlschlagen."
        }
        return null
    }

    // Helper function to copy to clipboard
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("terminal_command", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Befehl kopiert! 📋", Toast.LENGTH_SHORT).show()
    }

    // Helper function to open Termux
    fun openTermux() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Termux App nicht installiert! 📱", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Fehler beim Öffnen von Termux: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Bridge 💻", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .padding(16.dp)
        ) {
            // Termux Bridge Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isTermuxInstalled) Color(0xFF1E3A24) else Color(0xFF331E1E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = ElectricTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTermuxInstalled) "Status: Termux installiert ✅" else "Status: Termux nicht installiert ⚠️",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (isTermuxInstalled) {
                            Button(
                                onClick = { openTermux() },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Öffnen", color = Color(0xFF0C0A14), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTermuxInstalled) {
                            "Termux wurde auf deinem Android-Gerät erkannt. PocketCodeAgent führt Befehle in einer isolierten App-Sandbox aus; kopiere empfohlene Befehle und führe sie in Termux aus."
                        } else {
                            "Termux wurde nicht gefunden. Für npm, Node-Dienste und Server-Previews installiere Termux über F-Droid oder GitHub."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6D0EB),
                        fontSize = 11.sp
                    )
                }
            }

            // Recommended Commands (Needs Approval)
            if (recommended.isNotEmpty()) {
                Text(
                    text = "Befehlsvorschläge vom Agenten (Bestätigung nötig):",
                    style = MaterialTheme.typography.titleSmall,
                    color = GlowPink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recommended) { cmd ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161324)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$ ${cmd.safeDisplay}",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Copy shortcut
                                    IconButton(
                                        onClick = { copyToClipboard(cmd.command) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = ElectricTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    TextButton(
                                        onClick = { viewModel.rejectCommand(cmd.id) },
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Text("Reject", color = Color.Red, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            warningMessage = getDangerousWarning(cmd.command)
                                            commandToConfirm = cmd.command
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Run", color = Color(0xFF0C0A14), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Executed Console Log
            Text(
                text = "Terminal Verlauf & Ausgabe:",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (executed.isEmpty()) {
                        item {
                            Text("Verlauf ist leer. Keine Befehle ausgeführt.", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    } else {
                        items(executed) { cmd ->
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "> $cmd",
                                        color = ElectricTeal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { copyToClipboard(cmd) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Befehlsausführung simuliert in Sandbox.\nAusgabe: Done (Ergebnis in Termux prüfen)",
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual prompt input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualCommand,
                    onValueChange = { manualCommand = it },
                    placeholder = { Text("Eigener Befehl...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonPurple
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (manualCommand.isNotEmpty()) {
                            warningMessage = getDangerousWarning(manualCommand)
                            commandToConfirm = manualCommand
                        }
                    },
                    modifier = Modifier
                        .background(NeonPurple, shape = RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White)
                }
            }
        }
    }

    // Confirmation Dialog for Command Execution
    if (commandToConfirm != null) {
        AlertDialog(
            onDismissRequest = { 
                commandToConfirm = null 
                warningMessage = null
            },
            title = { Text("Befehlsausführung bestätigen ⚠️") },
            text = {
                Column {
                    Text("Möchtest du folgenden Befehl ausführen/kopieren?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = commandToConfirm!!,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(8.dp)
                    )
                    
                    // Show warning warning if dangerous
                    warningMessage?.let { warn ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = warn, color = Color(0xFFFF8888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cmd = commandToConfirm!!
                        // Copy to clipboard
                        copyToClipboard(cmd)
                        // Log simulated execution
                        viewModel.queueTerminalCommand(cmd)
                        val tc = viewModel.terminalCommands.lastOrNull()
                        if (tc != null) viewModel.markCommandDone(tc.id)
                        
                        // Clear input
                        if (cmd == manualCommand) {
                            manualCommand = ""
                        }
                        
                        // Open Termux automatically to let user paste it
                        if (isTermuxInstalled) {
                            openTermux()
                        }
                        
                        commandToConfirm = null
                        warningMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text(
                        text = if (isTermuxInstalled) "Kopieren & Termux öffnen" else "Befehl kopieren",
                        color = Color(0xFF0C0A14),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        commandToConfirm = null 
                        warningMessage = null
                    }
                ) {
                    Text("Abbrechen", color = Color.LightGray)
                }
            }
        )
    }
}
