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
import com.pocketcodeagent.data.model.ProposedFileChange
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
    proposedChanges: List<ProposedFileChange>,
    currentIndex: Int,
    rootWorkspaceUriString: String?,
    onApplyChange: (ProposedFileChange) -> Unit,
    onRejectChange: () -> Unit,
    onBackClick: () -> Unit
) {
    val activeChange = proposedChanges.getOrNull(currentIndex)

    // Compute diff when active change updates
    LaunchedEffect(activeChange) {
        activeChange?.let {
            viewModel.prepareDiff(it)
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
                                text = "${it.relativePath} (${currentIndex + 1}/${proposedChanges.size})",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        bottomBar = {
            if (activeChange != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0C1B))
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
                                viewModel.applyProposedChange(rootWorkspaceUriString, activeChange) {
                                    onApplyChange(activeChange)
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
                    Text("All proposed changes have been reviewed!", color = Color.White, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07050E))
                ) {
                    items(viewModel.activeDiffLines) { diffLine ->
                        DiffLineItem(diffLine)
                    }
                }
            }
        }
    }
}

@Composable
fun DiffLineItem(diffLine: DiffLine) {
    val backgroundColor = when (diffLine.type) {
        DiffLineType.ADDED -> Color(0xFF1E3A24) // Soft Green
        DiffLineType.REMOVED -> Color(0xFF4A1A1E) // Soft Red
        DiffLineType.UNCHANGED -> Color.Transparent
    }

    val textColor = when (diffLine.type) {
        DiffLineType.ADDED -> Color(0xFF88FF88)
        DiffLineType.REMOVED -> Color(0xFFFF8888)
        DiffLineType.UNCHANGED -> Color(0xFFC5C0DB)
    }

    val prefix = when (diffLine.type) {
        DiffLineType.ADDED -> "+"
        DiffLineType.REMOVED -> "-"
        DiffLineType.UNCHANGED -> " "
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Line number
        Text(
            text = diffLine.lineNumber.toString().padStart(3, ' '),
            color = Color.DarkGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(28.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Prefix (+ / - / ' ')
        Text(
            text = prefix,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(12.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Content
        Text(
            text = diffLine.text,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}
