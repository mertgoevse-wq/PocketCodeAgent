package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    viewModel: WorkspaceViewModel,
    fileUriString: String?,
    fileName: String?,
    onBackClick: () -> Unit
) {
    var editedContent by remember { mutableStateOf("") }
    
    // Load content when URI changes
    LaunchedEffect(fileUriString) {
        fileUriString?.let {
            viewModel.loadFileContent(it)
        }
    }

    // Sync content once loaded
    LaunchedEffect(viewModel.openFileContent, viewModel.isOpenFileLoading) {
        if (!viewModel.isOpenFileLoading) {
            editedContent = viewModel.openFileContent
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName ?: "Code Editor", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            fileUriString?.let {
                                viewModel.saveFileContent(it, editedContent)
                            }
                        },
                        enabled = !viewModel.isOpenFileSaving && fileUriString != null
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = ElectricTeal)
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
        ) {
            if (viewModel.isOpenFileLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ElectricTeal
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Small path display banner
                    Text(
                        text = fileUriString ?: "No file active",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161324))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Code Editor Text Field
                    TextField(
                        value = editedContent,
                        onValueChange = { editedContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C0A14),
                            unfocusedContainerColor = Color(0xFF0C0A14),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        )
                    )
                }
            }

            if (viewModel.isOpenFileSaving) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = ElectricTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving file...", color = Color.White)
                    }
                }
            }
        }
    }
}
