package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.pocketcodeagent.domain.security.EmergencyStopState
import com.pocketcodeagent.domain.security.OwnerAuthState
import com.pocketcodeagent.domain.security.OwnerSecurityManager
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.theme.PcaThemeMode
import com.pocketcodeagent.domain.language.LanguageMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ownerSecurity: OwnerSecurityManager,
    onResetWorkspace: () -> Unit,
    onClearLogs: () -> Unit,
    onBackClick: () -> Unit,
    compactMode: Boolean = false,
    onToggleCompactMode: () -> Unit = {},
    themeMode: PcaThemeMode = PcaThemeMode.DarkPremium,
    onThemeModeSelected: (PcaThemeMode) -> Unit = {},
    languageMode: LanguageMode = LanguageMode.System,
    onLanguageModeSelected: (LanguageMode) -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenDiff: () -> Unit = {},
    onOpenPreview: () -> Unit = {},
    onOpenTerminal: () -> Unit = {}
) {
    val authState by ownerSecurity.authState.collectAsState()
    val emergencyStop by ownerSecurity.emergencyStop.collectAsState()

    var showPinSetup by remember { mutableStateOf(false) }
    var showPinUnlock by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0E0E10))
            )
        },
        containerColor = Color(0xFF0E0E10)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Security Info ────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CalmSage)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Android Keystore Security",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your API Keys are encrypted using the hardware-backed Android Keystore system. The key alias 'PocketCodeAgentKey' is generated inside the secure execution environment on this device. The encrypted keys are stored locally in a SQLite database and can never be decrypted without the local device Keystore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // ── Owner Security ────────────────────────────────────────────────
            Text("Owner Security", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Auth state indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (stateColor, stateLabel) = when (authState) {
                            is OwnerAuthState.NotConfigured -> Color(0xFF777783) to "Nicht eingerichtet"
                            is OwnerAuthState.Locked -> WarmCopper to "Gesperrt"
                            is OwnerAuthState.Unlocked -> CalmSage to "Entsperrt"
                            is OwnerAuthState.Failed -> WarmCopper to "Fehlgeschlagen"
                            is OwnerAuthState.TemporarilyBlocked -> Color(0xFFF5365C) to "Temporär gesperrt"
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(stateColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stateLabel, color = stateColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Failed reason
                    (authState as? OwnerAuthState.Failed)?.let {
                        Text(it.reason, color = WarmCopper, fontSize = 11.sp)
                    }

                    Text(
                        "Schützt nur lokale sensible Aktionen. Kein Remote-Zugriff.",
                        color = Color(0xFF555560),
                        fontSize = 10.sp
                    )

                    // Buttons
                    when (authState) {
                        is OwnerAuthState.NotConfigured -> {
                            Button(
                                onClick = { showPinSetup = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Owner-Schutz einrichten", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        is OwnerAuthState.Locked -> {
                            OutlinedButton(
                                onClick = { showPinUnlock = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mit PIN entsperren", fontSize = 12.sp)
                            }
                        }
                        is OwnerAuthState.Unlocked -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { ownerSecurity.lock() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sperren", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        ownerSecurity.resetPinSetup()
                                        showPinSetup = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCopper)
                                ) {
                                    Text("PIN ändern", fontSize = 12.sp)
                                }
                            }
                        }
                        is OwnerAuthState.TemporarilyBlocked -> {
                            Text(
                                "Zu viele Fehlversuche. Warte 1 Minute.",
                                color = Color(0xFFF5365C),
                                fontSize = 12.sp
                            )
                        }
                        is OwnerAuthState.Failed -> {
                            OutlinedButton(
                                onClick = { showPinUnlock = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Erneut versuchen", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ── Emergency Stop ────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF5365C), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Emergency Stop", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    val currentLabel = emergencyStop.label
                    val currentDesc = emergencyStop.description
                    Text("Aktuell: $currentLabel", color = TextSecondary, fontSize = 12.sp)
                    Text(currentDesc, color = Color(0xFF555560), fontSize = 11.sp)

                    Spacer(Modifier.height(4.dp))

                    // Protected: requires owner unlock
                    Button(
                        onClick = { showEmergencyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5365C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Emergency Stop ändern", color = Color.White, fontSize = 13.sp)
                    }

                    if (emergencyStop != EmergencyStopState.NORMAL) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                ownerSecurity.setEmergencyStop(EmergencyStopState.NORMAL)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Emergency Stop deaktivieren", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Language ──────────────────────────────────────────────────────
            Text("Sprache", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageMode.entries.forEach { mode ->
                        val isSelected = mode == languageMode
                        Surface(
                            color = if (isSelected) SlateBlue.copy(alpha = 0.15f) else Color(0xFF1E1E28),
                            shape = RoundedCornerShape(6.dp),
                            onClick = { onLanguageModeSelected(mode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.label, color = if (isSelected) SlateBlue else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    val desc = when (mode) {
                                        LanguageMode.System -> "Folgt Android System-Einstellung"
                                        LanguageMode.German -> "Deutsch"
                                        LanguageMode.English -> "English"
                                    }
                                    Text(desc, color = TextSecondary, fontSize = 10.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Text("Sprachänderung wird nach App-Neustart wirksam.", color = Color(0xFF777783), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Appearance ───────────────────────────────────────────────────
            Text("Appearance", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    PcaThemeMode.entries.forEach { mode ->
                        val isSelected = mode == themeMode
                        Surface(
                            color = if (isSelected) SlateBlue.copy(alpha = 0.15f) else Color(0xFF1E1E28),
                            shape = RoundedCornerShape(6.dp),
                            onClick = { onThemeModeSelected(mode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.label, color = if (isSelected) SlateBlue else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(mode.description, color = TextSecondary, fontSize = 10.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── UI Preferences ────────────────────────────────────────────────
            Text("UI Preferences", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("One-Hand Compact Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Slim controls, more chat space, advanced actions in bottom sheet.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = compactMode,
                        onCheckedChange = { onToggleCompactMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SlateBlue,
                            checkedTrackColor = SlateBlue.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color(0xFF555560),
                            uncheckedTrackColor = Color(0xFF2A2A34)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Global Controls ───────────────────────────────────────────────
            Text("Global Controls", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)

            // ── Smoke Test Helper ────────────────────────────────────────────
            SmokeTestCard(
                onOpenChat = onOpenChat,
                onOpenFiles = onOpenFiles,
                onOpenDiff = onOpenDiff,
                onOpenPreview = onOpenPreview,
                onOpenTerminal = onOpenTerminal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reset Folder Workspace", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Revoke SAF persistent permissions.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onResetWorkspace,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue)
                    ) {
                        Text("Reset", color = Color.White)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Debug Log History", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Flush agent execution logs.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCopper)
                    ) {
                        Text("Clear", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PocketCodeAgent", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Version 1.0.0 (MVP Build)", color = TextSecondary, fontSize = 12.sp)
                Text("Developed locally without root dependencies", color = Color(0xFF50505C), fontSize = 10.sp)
            }
        }
    }

    // ── PIN Setup Dialog ────────────────────────────────────────────────────
    if (showPinSetup) {
        val isNewSetup = !ownerSecurity.hasPinConfigured()
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinSetup = false; newPin = ""; confirmPin = "" },
            containerColor = Color(0xFF1A1A22),
            title = {
                Text(
                    if (isNewSetup) "Owner-PIN einrichten" else "PIN ändern",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Lege eine lokale PIN (min. 4 Zeichen) fest. Diese PIN wird nur als Hash gespeichert und nie übertragen.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("Neue PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("PIN bestätigen") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = confirmPin.isNotEmpty() && confirmPin != newPin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length >= 4 && newPin == confirmPin) {
                            ownerSecurity.setPin(newPin)
                            showPinSetup = false
                            newPin = ""
                            confirmPin = ""
                        }
                    },
                    enabled = newPin.length >= 4 && newPin == confirmPin
                ) {
                    Text("Speichern", color = SlateBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetup = false; newPin = ""; confirmPin = "" }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            }
        )
    }

    // ── PIN Unlock Dialog ───────────────────────────────────────────────────
    if (showPinUnlock) {
        var unlockPin by remember { mutableStateOf("") }
        var unlockError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinUnlock = false; unlockPin = ""; unlockError = null },
            containerColor = Color(0xFF1A1A22),
            title = {
                Text("Owner entsperren", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Gib deine Owner-PIN ein, um sensible Aktionen freizuschalten.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = unlockPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) unlockPin = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = unlockError != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    unlockError?.let {
                        Text(it, color = WarmCopper, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (ownerSecurity.attemptPin(unlockPin)) {
                            showPinUnlock = false
                            unlockPin = ""
                            unlockError = null
                        } else {
                            unlockError = "Falsche PIN"
                            unlockPin = ""
                        }
                    },
                    enabled = unlockPin.length >= 4
                ) {
                    Text("Entsperren", color = SlateBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinUnlock = false; unlockPin = ""; unlockError = null }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            }
        )
    }

    // ── Emergency Stop Dialog ────────────────────────────────────────────────
    if (showEmergencyDialog) {
        EmergencyStopDialog(
            current = emergencyStop,
            onSelect = { state ->
                ownerSecurity.setEmergencyStop(state)
                showEmergencyDialog = false
            },
            onDismiss = { showEmergencyDialog = false }
        )
    }
}

@Composable
private fun EmergencyStopDialog(
    current: EmergencyStopState,
    onSelect: (EmergencyStopState) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        title = {
            Text("Emergency Stop", color = Color(0xFFF5365C), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Wähle, welche Funktionen deaktiviert werden sollen:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                EmergencyStopState.entries.forEach { state ->
                    val isActive = current == state
                    Surface(
                        color = if (isActive) SlateBlue.copy(alpha = 0.15f) else Color(0xFF1E1E28),
                        shape = RoundedCornerShape(6.dp),
                        onClick = { onSelect(state) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                state.label,
                                color = if (isActive) SlateBlue else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                state.description,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", color = SlateBlue)
            }
        }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = SlateBlue,
    unfocusedBorderColor = BorderGrey,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextSecondary,
    cursorColor = SlateBlue
)

// ─── Smoke Test Helper Card ─────────────────────────────────────────────────
private val smokeTestPrompt =
    "Erstelle eine kleine statische Test-Web-App mit index.html, styles.css und app.js. " +
    "Dunkles Premium-Design, keine Neonfarben, Button mit Klickzaehler. " +
    "Gib alle Aenderungen als pocketArtifact mit pocketAction type=file aus."

@Composable
private fun SmokeTestCard(
    onOpenChat: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenDiff: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BugReport, null,
                    tint = SlateBlue, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "PocketCodeAgent Smoke Test",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                "Ein einfacher Ablauf zum Testen aller Kern-Funktionen. Keine automatische Ausfuehrung.",
                color = TextSecondary,
                fontSize = 11.sp
            )

            // Steps
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                StepRow(1, "Provider pruefen", "Settings oeffnen, Provider auswaehlen, API-Key eintragen, Test-Button.")
                StepRow(2, "Workspace oeffnen", "Oben links auf Workspace tippen, lokalen Ordner auswaehlen.")
                StepRow(3, "Build Mode waehlen", "Im Chat 'Build' statt 'Discuss' aktivieren.")
                StepRow(4, "Testprompt kopieren", "Den Button unten nutzen, dann in Chat einfuegen.")
                StepRow(5, "Diff pruefen", "Nach Agent-Antwort im Diff-Tab Aenderungen durchsehen.")
                StepRow(6, "Apply safe", "Nur ungefaehrliche Dateien uebernehmen, keine Loeschungen blind bestaetigen.")
                StepRow(7, "Preview oeffnen", "Preview-Tab prueft Workspace-HTML oder URL 127.0.0.1:5173.")
                StepRow(8, "Terminal Queue pruefen", "Im Terminal-Tab siehst du gepuffte Commands aus Agent-Antworten.")
            }

            HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

            // Test prompt display
            Surface(
                color = Color(0xFF0E0E10),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    smokeTestPrompt,
                    color = Color(0xFF9999A3),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(10.dp),
                    maxLines = 5
                )
            }

            // Copy prompt button
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(smokeTestPrompt))
                    copied = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue)
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (copied) "Kopiert!" else "Testprompt kopieren", fontSize = 12.sp)
            }

            LaunchedEffect(copied) {
                if (copied) {
                    kotlinx.coroutines.delay(2000)
                    copied = false
                }
            }

            HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

            // Navigation buttons
            Text("Navigation", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NavChip("Chat", Icons.Default.Chat, onOpenChat, modifier = Modifier.weight(1f))
                NavChip("Files", Icons.Default.Folder, onOpenFiles, modifier = Modifier.weight(1f))
                NavChip("Diff", Icons.Default.Difference, onOpenDiff, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NavChip("Preview", Icons.Default.PlayArrow, onOpenPreview, modifier = Modifier.weight(1f))
                NavChip("Terminal", Icons.Default.Terminal, onOpenTerminal, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StepRow(step: Int, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = SlateBlue.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("$step", color = SlateBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color(0xFF666670), fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun NavChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateBlue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = SlateBlue, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = SlateBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
