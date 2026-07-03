package com.pocketcodeagent.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.AgentViewModel

private val TerminalBg   = Color(0xFF090909)
private val TerminalText = Color(0xFFCCCCCC)
private val PromptColor  = Color(0xFF50FA7B)

@Composable
fun TerminalPanel(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.executedCommands.size) {
        if (viewModel.executedCommands.isNotEmpty()) {
            listState.scrollToItem(viewModel.executedCommands.size - 1)
        }
    }

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
            Text("Queue: ${viewModel.recommendedCommands.size}", color = TextSecondary, fontSize = 11.sp)
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
                Icon(Icons.Default.Info, null, tint = SlateBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Befehle werden hier nur gesammelt. Kopiere sie nach Termux und fuehre sie dort bewusst aus.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 0.5.dp)

        // Pending commands
        if (viewModel.recommendedCommands.isNotEmpty()) {
            Text(
                "  Warteschlange",
                color = Color(0xFF555560),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            viewModel.recommendedCommands.forEach { cmd ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ $cmd",
                        color = Color(0xFFFFB86C),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.executeTerminalCommand(cmd) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PromptColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Extern erledigt", fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.rejectTerminalCommand(cmd) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = WarmCopper, modifier = Modifier.size(14.dp))
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 0.5.dp)
        }

        // Executed commands log
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (viewModel.executedCommands.isEmpty()) {
                item(key = "empty-terminal") {
                    Text(
                        "Noch keine extern erledigten Befehle.",
                        color = Color(0xFF333340),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            items(viewModel.executedCommands, key = { it }) { cmd ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$ ", color = PromptColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text(cmd, color = TerminalText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
