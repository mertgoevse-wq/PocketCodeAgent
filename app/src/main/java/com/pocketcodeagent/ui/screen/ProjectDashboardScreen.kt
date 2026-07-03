package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.CalmSage
import com.pocketcodeagent.ui.theme.DarkSurface
import com.pocketcodeagent.ui.theme.DeepSlateBackground
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.theme.TextPrimary
import com.pocketcodeagent.ui.theme.TextSecondary
import com.pocketcodeagent.ui.theme.WarmCopper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    workspaceName: String?,
    providerName: String?,
    modelName: String?,
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PocketCodeAgent Hub 👁️", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = DeepSlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Card: Active Provider Info
            DashboardCard(
                title = "Aktivierter AI-Provider 🔑",
                icon = Icons.Default.Lock,
                iconColor = WarmCopper
            ) {
                Column {
                    if (providerName != null) {
                        Text("Anbieter: $providerName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Modell: $modelName", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        Text("Kein API-Provider konfiguriert", color = TextSecondary, fontSize = 14.sp)
                        Text("Läuft im simulierten Demo-Modus", color = CalmSage, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNavigate("providers") },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Verwalten", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // 2. Card: Current Workspace Info
            DashboardCard(
                title = "Projekt-Workspace 📂",
                icon = Icons.Default.Folder,
                iconColor = SlateBlue
            ) {
                Column {
                    Text(
                        text = workspaceName ?: "Kein Workspace ausgewählt",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (workspaceName?.contains("Demo") == true) "demo://workspace (Virtueller Speicher)" else "Android Storage Access Framework (SAF)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onNavigate("workspace") }) {
                            Text("Ändern", color = SlateBlue, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onNavigate("explorer") },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Dateien öffnen", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 3. Card: Last Agent Run Status
            DashboardCard(
                title = "Letzter Agenten-Lauf 🤖",
                icon = Icons.Default.SmartToy,
                iconColor = CalmSage
            ) {
                Column {
                    Text("Status: Bereit für Eingabe", color = CalmSage, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Ausgewählte Rolle: Planner / Coder / Reviewer", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNavigate("chat") },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Chat öffnen", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // 4. Card: Build/Preview Status
            DashboardCard(
                title = "Build & LivePreview Status 👁️",
                icon = Icons.Default.Web,
                iconColor = SlateBlue
            ) {
                Column {
                    val isDemo = workspaceName?.contains("Demo") == true
                    Text(
                        text = if (isDemo) "Projektstruktur: Vite (Demo) erkannt" else "Projektstruktur: Statisches HTML",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDemo) "Vorschau-Server: http://127.0.0.1:5173" else "Dateibasiertes WebView-Rendering aktiv",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNavigate("preview") },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Vorschau laden", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // 5. Grid Header & Quick Navigation Actions
            Text(
                text = "Zentrale Schnellaktionen:",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Terminal,
                    title = "Terminal",
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("terminal")
                }
                QuickActionButton(
                    icon = Icons.Default.ListAlt,
                    title = "System-Logs",
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("logs")
                }
                QuickActionButton(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("settings")
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            content()
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SlateBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
