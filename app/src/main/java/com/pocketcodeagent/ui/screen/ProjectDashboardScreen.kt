package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
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
                .padding(16.dp)
        ) {
            // Workspace & AI Info Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = SlateBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = workspaceName ?: "Kein Workspace ausgewählt ⚠️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = null, tint = CalmSage)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (providerName != null) "Provider: $providerName ($modelName)" else "Kein API-Provider aktiv ⚠️ (Demo-Modus)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Text(
                text = "Zentrale Steuerungsmodule:",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Navigation Grid (8 tiles)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DashboardTile(
                        icon = Icons.Default.Chat,
                        title = "Agent starten",
                        subtitle = "Planen, coden & fixen",
                        tint = SlateBlue,
                        onClick = { onNavigate("chat") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.FolderOpen,
                        title = "Dateien ansehen",
                        subtitle = "Projekt-Dateibaum",
                        tint = CalmSage,
                        onClick = { onNavigate("explorer") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.Web,
                        title = "Preview öffnen",
                        subtitle = "HTML/Vite-Vorschau",
                        tint = SlateBlue,
                        onClick = { onNavigate("preview") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.Terminal,
                        title = "Terminal öffnen",
                        subtitle = "Termux-Befehle",
                        tint = CalmSage,
                        onClick = { onNavigate("terminal") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.Lock,
                        title = "Provider einrichten",
                        subtitle = "API-Key & Endpoints",
                        tint = WarmCopper,
                        onClick = { onNavigate("providers") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.CreateNewFolder,
                        title = "Workspace öffnen",
                        subtitle = "Ordner per SAF picken",
                        tint = SlateBlue,
                        onClick = { onNavigate("workspace") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.Settings,
                        title = "Einstellungen",
                        subtitle = "App-Daten zurücksetzen",
                        tint = TextSecondary,
                        onClick = { onNavigate("settings") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.ListAlt,
                        title = "Logs öffnen",
                        subtitle = "Laufzeit-Protokolle",
                        tint = CalmSage,
                        onClick = { onNavigate("logs") }
                    )
                }
            }
            
            // Bottom quick action for quick agent launching
            Button(
                onClick = { onNavigate("chat") },
                colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Agenten-Chat starten 🤖", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DashboardTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
