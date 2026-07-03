package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.repository.DiffLine
import com.pocketcodeagent.data.repository.DiffLineType
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.GlowPink
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffReviewScreen(
    viewModel: WorkspaceViewModel,
    proposedPatches: List<FilePatch>,
    currentIndex: Int,
    rootWorkspaceUriString: String?,
    onApplyChange: (FilePatch) -> Unit,
    onRejectChange: () -> Unit,
    onApplyAll: () -> Unit,
    onBackClick: () -> Unit
) {
    val activeChange = proposedPatches.getOrNull(currentIndex)

    // Compute diff when active change updates
    LaunchedEffect(activeChange, rootWorkspaceUriString) {
        if (activeChange != null && rootWorkspaceUriString != null) {
            viewModel.prepareDiff(rootWorkspaceUriString, activeChange)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review Code Changes", color = Color.White)
                        activeChange?.let {
                            Text(
                                text = "${it.path} (${currentIndex + 1}/${proposedPatches.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricTeal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (proposedPatches.isNotEmpty()) {
                        IconButton(onClick = {
                            if (rootWorkspaceUriString != null) {
                                viewModel.applyAllPatches(rootWorkspaceUriString, proposedPatches) { success ->
                                    if (success) {
                                        onApplyAll()
                                    }
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Apply All", tint = ElectricTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        bottomBar = {
            if (activeChange != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0C1B))
                ) {
                    // Visual error banner if patch fails (e.g. missing write permissions)
                    viewModel.workspaceError?.let { error ->
                        Surface(
                            color = Color.Red.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Fehler: $error",
                                color = Color(0xFFFF8888),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onRejectChange,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reject", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                if (rootWorkspaceUriString != null) {
                                    viewModel.applyPatch(rootWorkspaceUriString, activeChange) { success ->
                                        if (success) {
                                            onApplyChange(activeChange)
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF0C0A14))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept & Apply", color = Color(0xFF0C0A14), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0C0A14)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeChange == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Alle vorgeschlagenen Änderungen geprüft!", color = Color.White, fontSize = 16.sp)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = Color(0xFF131024),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Aktion: ${activeChange.action.uppercase()}",
                                color = when (activeChange.action.lowercase()) {
                                    "create" -> Color(0xFF88FF88)
                                    "delete" -> Color(0xFFFF8888)
                                    else -> ElectricTeal
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF07050E))
                            .padding(8.dp)
                    ) {
                        items(viewModel.activeDiffLines) { line ->
                            val backgroundColor = when (line.type) {
                                DiffLineType.ADDED -> Color(0xFF0E2C1A)
                                DiffLineType.REMOVED -> Color(0xFF381414)
                                DiffLineType.UNCHANGED -> Color.Transparent
                            }
                            val textColor = when (line.type) {
                                DiffLineType.ADDED -> Color(0xFF88FF88)
                                DiffLineType.REMOVED -> Color(0xFFFF8888)
                                DiffLineType.UNCHANGED -> Color.LightGray
                            }
                            val prefix = when (line.type) {
                                DiffLineType.ADDED -> "+"
                                DiffLineType.REMOVED -> "-"
                                DiffLineType.UNCHANGED -> " "
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(horizontal = 8.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = String.format("%3d", line.lineNumber),
                                    color = Color.DarkGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(30.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = prefix,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(12.dp)
                                )
                                Text(
                                    text = line.text,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
