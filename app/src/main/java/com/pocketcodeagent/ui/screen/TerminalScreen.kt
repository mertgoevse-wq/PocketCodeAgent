package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val recommended = viewModel.recommendedCommands
    val executed = viewModel.executedCommands
    var manualCommand by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Bridge", color = Color.White) },
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
            // Termux Bridge Guide
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A33)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Termux Bridge Status: Configured",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Because Android sandboxes prevent running node.js or servers directly, React/Vite builds should be executed in Termux. PocketCodeAgent communicates commands via intent. Set up Termux-tasker or copy commands to execute them locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E98B5),
                        fontSize = 11.sp
                    )
                }
            }

            // Recommended Commands (Needs Approval)
            if (recommended.isNotEmpty()) {
                Text(
                    "Recommended by Agent (Requires Approval):",
                    style = MaterialTheme.typography.titleSmall,
                    color = GlowPink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
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
                                    text = "$ $cmd",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Row {
                                    TextButton(onClick = { viewModel.rejectTerminalCommand(cmd) }) {
                                        Text("Reject", color = Color.Red, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.executeTerminalCommand(cmd) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Approve", color = Color(0xFF0C0A14), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Executed Console Log
            Text(
                "Terminal Console Output:",
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
                            Text("Console is idle. No commands executed yet.", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    } else {
                        items(executed) { cmd ->
                            Text(
                                text = "> $cmd",
                                color = ElectricTeal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Command executed successfully in environment.\nTask output: Done (mocked runtime)",
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
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
                    placeholder = { Text("Type command...", color = Color.Gray) },
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
                            viewModel.executeTerminalCommand(manualCommand)
                            manualCommand = ""
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
}
