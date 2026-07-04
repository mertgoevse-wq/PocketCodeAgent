package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.CalmSage
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.theme.WarmCopper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspacePickerScreen(
    selectedWorkspaceName: String?,
    onPickWorkspaceClick: () -> Unit,
    onBackClick: () -> Unit,
    onProceedClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Workspace Folder", color = Color.White) },
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
        Box(
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
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = CalmSage,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Local Project Workspace",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select a directory where your projects reside. The app uses Android's Storage Access Framework to securely read/write code inside this folder without needing root access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9E98B5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Workspace Card
                if (selectedWorkspaceName != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A36)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Currently Active Directory:",
                                style = MaterialTheme.typography.bodySmall,
                                color = CalmSage
                            )
                            Text(
                                selectedWorkspaceName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Picker Button
                Button(
                    onClick = onPickWorkspaceClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedWorkspaceName == null) "Select Folder" else "Change Folder",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Explanatory note
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color(0xFF1E1A33).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = WarmCopper,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Your files are kept 100% locally. PocketCodeAgent asks for persistable permissions to keep directory access active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E98B5),
                        fontSize = 11.sp
                    )
                }

                if (selectedWorkspaceName != null) {
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = onProceedClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CalmSage),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Go to Dashboard", color = Color(0xFF0C0A14), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0C0A14))
                    }
                }
            }
        }
    }
}
