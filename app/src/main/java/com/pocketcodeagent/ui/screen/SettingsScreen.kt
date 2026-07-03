package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.GlowPink
import com.pocketcodeagent.ui.theme.NeonPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onResetWorkspace: () -> Unit,
    onClearLogs: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Info card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A33)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Android Keystore Security",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your API Keys are encrypted using the hardware-backed Android Keystore system. The key alias 'PocketCodeAgentKey' is generated inside the secure execution environment on this device. The encrypted keys are stored locally in a SQLite database and can never be decrypted without the local device Keystore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E98B5)
                    )
                }
            }

            // System Configurations
            Text("Global Controls", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)

            // Reset Workspace Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161324)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reset Folder Workspace", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Revoke SAF persistent permissions to choose a new directory.", color = Color.Gray, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onResetWorkspace,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = Color.White)
                    }
                }
            }

            // Clear Log History Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161324)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Debug Log History", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Flush all local database records of agent execution steps.", color = Color.Gray, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.buttonColors(containerColor = GlowPink)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Version Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PocketCodeAgent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Version 1.0.0 (MVP Build)", color = Color.Gray, fontSize = 12.sp)
                Text("Developed locally without root dependencies", color = Color.DarkGray, fontSize = 10.sp)
            }
        }
    }
}
