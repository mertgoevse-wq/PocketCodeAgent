package com.pocketcodeagent.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun CodeEditorScreen(
    viewModel: WorkspaceViewModel,
    fileUriString: String?,
    fileName: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var editedContent by remember { mutableStateOf("") }
    
    // Undo stack
    val undoStack = remember { mutableStateListOf<String>() }
    var showLineNumbers by remember { mutableStateOf(true) }

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
            undoStack.clear()
        }
    }

    // Function to push to undo stack (max 30 items)
    fun pushUndoState(state: String) {
        if (undoStack.isEmpty() || undoStack.last() != state) {
            if (undoStack.size >= 30) {
                undoStack.removeAt(0)
            }
            undoStack.add(state)
        }
    }

    // Helper to copy to clipboard
    fun copyAllToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("editor_code", editedContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Code in Zwischenablage kopiert! 📋", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName ?: "Code Editor", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Line numbers toggle
                    IconButton(onClick = { showLineNumbers = !showLineNumbers }) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "Zahlen",
                            tint = if (showLineNumbers) CalmSage else Color.Gray
                        )
                    }
                    
                    // Copy All
                    IconButton(onClick = { copyAllToClipboard() }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Kopieren", tint = TextPrimary)
                    }

                    // Undo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                editedContent = undoStack.removeLast()
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) SlateBlue else Color.Gray
                        )
                    }

                    // Save
                    IconButton(
                        onClick = {
                            fileUriString?.let {
                                viewModel.saveFileContent(it, editedContent) {
                                    Toast.makeText(context, "Datei erfolgreich gespeichert! 💾", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !viewModel.isOpenFileSaving && fileUriString != null
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Speichern", tint = CalmSage)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = DeepSlateBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isOpenFileLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CalmSage
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Path banner
                    Text(
                        text = fileUriString ?: "Keine aktive Datei",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Line numbers display
                        if (showLineNumbers) {
                            val lineCount = editedContent.split("\n").size
                            val lineNumbersText = (1..lineCount).joinToString("\n") { it.toString() }
                            Text(
                                text = lineNumbersText,
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                                    .widthIn(min = 28.dp),
                                style = TextStyle(lineHeight = 18.sp)
                            )
                        }

                        // BasicTextField for high-performance monospace writing
                        BasicTextField(
                            value = editedContent,
                            onValueChange = {
                                pushUndoState(editedContent)
                                editedContent = it
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState()),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 18.sp
                            ),
                            cursorBrush = SolidColor(CalmSage),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            )
                        )
                    }
                }
            }

            if (viewModel.isOpenFileSaving) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = CalmSage, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Speichere Datei...", color = Color.White)
                    }
                }
            }
        }
    }
}
