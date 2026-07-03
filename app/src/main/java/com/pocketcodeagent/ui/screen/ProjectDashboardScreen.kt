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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.GlowPink
import com.pocketcodeagent.ui.theme.NeonPurple

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
                title = { Text("PocketCodeAgent Hub", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = Color(0xFF0C0A14)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0C1B),
                            Color(0xFF0C0A14)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Workspace & AI Info Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A33)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = workspaceName ?: "No Active Workspace Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = null, tint = GlowPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (providerName != null) "$providerName ($modelName)" else "No Provider Active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Text(
                text = "Developer Workspaces",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Navigation Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DashboardTile(
                        icon = Icons.Default.Chat,
                        title = "Chat Agents",
                        subtitle = "Planner, Coder, Reviewer",
                        tint = NeonPurple,
                        onClick = { onNavigate("chat") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.FolderOpen,
                        title = "File Explorer",
                        subtitle = "Browse project tree",
                        tint = ElectricTeal,
                        onClick = { onNavigate("explorer") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.PlayCircleOutline,
                        title = "Live Preview",
                        subtitle = "Render HTML/Vite preview",
                        tint = GlowPink,
                        onClick = { onNavigate("preview") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.Terminal,
                        title = "Terminal Bridge",
                        subtitle = "Run shell instructions",
                        tint = Color.Yellow,
                        onClick = { onNavigate("terminal") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.ListAlt,
                        title = "System Logs",
                        subtitle = "Debug logs & outputs",
                        tint = Color.Cyan,
                        onClick = { onNavigate("logs") }
                    )
                }

                item {
                    DashboardTile(
                        icon = Icons.Default.Settings,
                        title = "App Settings",
                        subtitle = "Keystore, API key configs",
                        tint = Color.LightGray,
                        onClick = { onNavigate("settings") }
                    )
                }
            }
            
            // Bottom quick action
            Button(
                onClick = { onNavigate("chat") },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(top = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch AI Agent Chat", color = Color.White, fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A33).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
