package com.pocketcodeagent.ui.screen

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.ui.theme.CalmSage
import com.pocketcodeagent.ui.theme.DarkSurface
import com.pocketcodeagent.ui.theme.DeepSlateBackground
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.theme.TextPrimary
import com.pocketcodeagent.ui.theme.TextSecondary
import com.pocketcodeagent.ui.theme.WarmCopper
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    onFileClick: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Create Dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isDirectoryCreation by remember { mutableStateOf(false) }

    // Delete Dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<WorkspaceFile?>(null) }

    // Load workspace files when screen displays
    LaunchedEffect(workspaceUriString) {
        workspaceUriString?.let { viewModel.loadWorkspace(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projekt-Dateibaum 📁", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SlateBlue,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Erstellen")
            }
        },
        containerColor = DeepSlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Dateien durchsuchen...", color = Color.Gray) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SlateBlue,
                    unfocusedBorderColor = Color(0xFF2E2D34)
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (viewModel.isLoadingFiles) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CalmSage
                    )
                } else if (viewModel.files.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Verzeichnis ist leer.", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    // Flatten the file list for tree representation
                    val flatList = remember(viewModel.files, expandedFolders) {
                        val list = mutableListOf<Pair<WorkspaceFile, Int>>()
                        fun buildFlatList(files: List<WorkspaceFile>, depth: Int) {
                            for (file in files) {
                                list.add(Pair(file, depth))
                                if (file.isDirectory && expandedFolders.contains(file.uriString)) {
                                    buildFlatList(file.children, depth + 1)
                                }
                            }
                        }
                        buildFlatList(viewModel.files, 0)
                        list
                    }

                    // Apply Search Query filter
                    val filteredList = remember(flatList, searchQuery) {
                        if (searchQuery.isEmpty()) {
                            flatList
                        } else {
                            flatList.filter { it.first.name.contains(searchQuery, ignoreCase = true) }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredList) { (file, depth) ->
                            FileTreeItem(
                                file = file,
                                depth = depth,
                                isExpanded = expandedFolders.contains(file.uriString),
                                onClick = {
                                    if (file.isDirectory) {
                                        expandedFolders = if (expandedFolders.contains(file.uriString)) {
                                            expandedFolders - file.uriString
                                        } else {
                                            expandedFolders + file.uriString
                                        }
                                    } else {
                                        onFileClick(file.uriString, file.name)
                                    }
                                },
                                onDeleteClick = {
                                    fileToDelete = file
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create File/Folder Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Neues Element erstellen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Dateiname") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SlateBlue,
                            unfocusedBorderColor = Color(0xFF2E2D34)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isDirectoryCreation,
                            onCheckedChange = { isDirectoryCreation = it },
                            colors = CheckboxDefaults.colors(checkedColor = SlateBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verzeichnis (Ordner) erstellen?", color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotEmpty() && workspaceUriString != null) {
                            if (isDirectoryCreation) {
                                viewModel.createDirectory(workspaceUriString, newFileName)
                            } else {
                                viewModel.createFile(workspaceUriString, "text/plain", newFileName)
                            }
                            // Reload directory tree
                            viewModel.loadWorkspace(workspaceUriString)
                        }
                        showCreateDialog = false
                        newFileName = ""
                        isDirectoryCreation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue)
                ) {
                    Text("Erstellen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Abbrechen", color = Color.LightGray)
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Löschen bestätigen ⚠️") },
            text = {
                Text(
                    text = "Möchtest du '${fileToDelete!!.name}' wirklich unwiderruflich löschen?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = fileToDelete
                        if (file != null && workspaceUriString != null) {
                            viewModel.deleteFile(file.uriString) { success ->
                                if (success) {
                                    Toast.makeText(context, "Datei gelöscht", Toast.LENGTH_SHORT).show()
                                    viewModel.loadWorkspace(workspaceUriString)
                                } else {
                                    Toast.makeText(context, "Löschen fehlgeschlagen", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCopper)
                ) {
                    Text("Löschen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
fun FileTreeItem(
    file: WorkspaceFile,
    depth: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = (depth * 16).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Extension icon resolution
                val icon = if (file.isDirectory) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else {
                    val ext = file.name.substringAfterLast('.', "").lowercase()
                    when (ext) {
                        "html", "htm" -> Icons.Default.Code
                        "js", "ts", "jsx", "tsx" -> Icons.Default.Terminal
                        "css" -> Icons.Default.Web
                        "json" -> Icons.Default.Description
                        else -> Icons.Default.InsertDriveFile
                    }
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (file.isDirectory) SlateBlue else CalmSage,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Delete button
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = WarmCopper, modifier = Modifier.size(16.dp))
                }
                if (file.isDirectory) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
