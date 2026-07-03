package com.pocketcodeagent.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

private val PanelBg = Color(0xFF0E0E10)
private val FolderColor = Color(0xFF8B949E)
private val FileColor   = Color(0xFFADB5BD)

@Composable
fun FileTreePanel(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    onFileClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(workspaceUriString) {
        workspaceUriString?.let { viewModel.loadWorkspace(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBg)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13131A))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FolderOpen, null, tint = CalmSage, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Files", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (viewModel.isLoadingFiles) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = CalmSage,
                    strokeWidth = 2.dp
                )
            } else {
                Text("${viewModel.files.size}", color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { workspaceUriString?.let { viewModel.loadWorkspace(it) } },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

        if (workspaceUriString == null) {
            PanelPlaceholder(
                icon = Icons.Default.FolderOpen,
                title = "Kein Workspace geöffnet",
                subtitle = "Wähle einen Ordner über die Status-Leiste aus."
            )
            return@Column
        }

        if (viewModel.isLoadingFiles) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CalmSage)
            }
            return@Column
        }

        if (viewModel.files.isEmpty()) {
            PanelPlaceholder(
                icon = Icons.Default.FolderOff,
                title = "Leer",
                subtitle = "Der Workspace enthält keine sichtbaren Dateien."
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(viewModel.files, key = { it.uriString }) { file ->
                FileTreeItem(
                    file = file,
                    depth = 0,
                    isExpanded = expandedFolders.contains(file.uriString),
                    onToggle = {
                        expandedFolders = if (expandedFolders.contains(file.uriString))
                            expandedFolders - file.uriString
                        else
                            expandedFolders + file.uriString
                    },
                    onFileClick = onFileClick
                )
                // Show children if expanded
                if (file.isDirectory && expandedFolders.contains(file.uriString)) {
                    file.children.forEach { child ->
                        FileTreeItem(
                            file = child,
                            depth = 1,
                            isExpanded = expandedFolders.contains(child.uriString),
                            onToggle = {
                                expandedFolders = if (expandedFolders.contains(child.uriString))
                                    expandedFolders - child.uriString
                                else
                                    expandedFolders + child.uriString
                            },
                            onFileClick = onFileClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    file: WorkspaceFile,
    depth: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onFileClick: (String, String) -> Unit
) {
    val indent = (depth * 16).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (file.isDirectory) onToggle()
                else onFileClick(file.uriString, file.name)
            }
            .padding(start = 16.dp + indent, end = 16.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.isDirectory) {
            Icon(
                if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                null,
                tint = FolderColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Folder, null, tint = Color(0xFFF0AD4E), modifier = Modifier.size(16.dp))
        } else {
            Spacer(Modifier.width(20.dp))
            Icon(Icons.Default.Description, null, tint = FileColor, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = file.name,
            color = if (file.isDirectory) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal
        )
    }
    HorizontalDivider(color = Color(0xFF1A1A22), thickness = 0.5.dp)
}
