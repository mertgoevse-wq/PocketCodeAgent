package com.pocketcodeagent.ui.screen

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
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    onFileSelected: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isDirectoryCreation by remember { mutableStateOf(false) }

    // Load workspace files when screen displays
    LaunchedEffect(workspaceUriString) {
        workspaceUriString?.let { viewModel.loadWorkspace(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project File Explorer", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NeonPurple,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create File/Folder")
            }
        },
        containerColor = Color(0xFF0C0A14)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isLoadingFiles) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ElectricTeal
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
                    Text("Workspace is empty.", color = Color.Gray, fontSize = 16.sp)
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(flatList) { (file, depth) ->
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
                                    onFileSelected(file.uriString, file.name)
                                }
                            }
                        )
                    }
                }
            }

            // Create File/Folder Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("Create New Item", color = Color.White) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newFileName,
                                onValueChange = { newFileName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonPurple
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isDirectoryCreation,
                                    onCheckedChange = { isDirectoryCreation = it },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Is Directory?", color = Color.White)
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
                                    viewModel.loadWorkspace(workspaceUriString)
                                }
                                showCreateDialog = false
                                newFileName = ""
                                isDirectoryCreation = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                        ) {
                            Text("Create", color = Color(0xFF0C0A14))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.LightGray)
                        }
                    },
                    containerColor = Color(0xFF1E1A33)
                )
            }
        }
    }
}

@Composable
fun FileTreeItem(
    file: WorkspaceFile,
    depth: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = (depth * 20).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else {
                    Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (file.isDirectory) NeonPurple else ElectricTeal,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = file.name,
                color = Color.White,
                fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            if (file.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
