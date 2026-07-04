package com.pocketcodeagent.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchSource
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.domain.context.ContextSanitizer
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

private val EditorBg = Color(0xFF0D0D0F)
private val LineNrBg = Color(0xFF111115)
private val LineNrColor = Color(0xFF50505C)
private val ToolbarBg = Color(0xFF13131A)

@Composable
fun CodeEditorPanel(
    viewModel: WorkspaceViewModel,
    fileUri: String?,
    fileName: String?,
    modifier: Modifier = Modifier,
    filePath: String? = null,
    onOpenFiles: (() -> Unit)? = null,
    onPreviewFile: ((String, String) -> Unit)? = null,
    onCreateDiffPatch: ((FilePatch) -> Unit)? = null,
    onShareFile: ((String, String) -> Unit)? = null
) {
    val editorContent = viewModel.editorContent
    val isModified = viewModel.isModified
    val isLarge = viewModel.isLargeFile
    val isHuge = viewModel.isHugeFile
    val sizeKb = viewModel.openFileSizeBytes / 1024L
    var showLineNumbers by remember { mutableStateOf(true) }
    var showFind by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var editingEnabled by remember { mutableStateOf(!isLarge) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var showSecretShareDialog by remember { mutableStateOf(false) }
    var showLargeShareDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val editorLines = remember(editorContent) { editorContent.lines() }
    val isHtmlFile = fileName?.let { it.endsWith(".html", ignoreCase = true) || it.endsWith(".htm", ignoreCase = true) } == true
    val fileType = remember(fileName) { detectFileType(fileName) }
    val relativePath = filePath ?: viewModel.openFileRelativePath
    val isCheckingConflicts = viewModel.isCheckingConflicts

    LaunchedEffect(fileUri) {
        fileUri?.let { viewModel.loadFileContent(it, relativePath ?: fileName, fileName) }
    }

    LaunchedEffect(isLarge) {
        if (isLarge) editingEnabled = false
    }

    val previewLambda: (() -> Unit)? = if (isHtmlFile && onPreviewFile != null) {
        val currentFileName = fileName.orEmpty()
        val currentPath = relativePath ?: currentFileName
        ({ onPreviewFile(currentPath, currentFileName) })
    } else null

    val previewDisabledReason: String? = when {
        isHtmlFile -> null
        fileName != null && !isHuge -> "Nur .html/.htm Dateien"
        else -> null
    }

    fun createPatchFromCurrentEditor() {
        onCreateDiffPatch?.invoke(
            FilePatch(
                path = relativePath ?: fileName ?: "unknown",
                action = FilePatchAction.MODIFY,
                oldText = viewModel.originalFileContent,
                newText = editorContent,
                source = FilePatchSource.USER,
                status = FilePatchStatus.PENDING,
                additions = editorContent.lines().size,
                deletions = viewModel.originalFileContent.lines().size,
                replaceWholeFile = true
            )
        )
    }

    val patchLambda: (() -> Unit)? = if (isModified && onCreateDiffPatch != null && !isHuge && !isCheckingConflicts) {
        {
            viewModel.checkForConflicts { hasDiskConflict ->
                if (hasDiskConflict) {
                    showConflictDialog = true
                } else {
                    createPatchFromCurrentEditor()
                }
            }
        }
    } else null

    val patchDisabledReason: String? = when {
        !isModified -> null
        isCheckingConflicts -> "Pruefe Datei ..."
        isHuge -> "Patch blockiert (>500 KB)"
        else -> null
    }

    fun shareEditorContent(content: String) {
        val name = fileName ?: return
        onShareFile?.invoke(name, content)
    }

    fun requestShare() {
        if (fileName == null) return
        val sizeBytes = editorContent.encodeToByteArray().size
        when {
            sizeBytes > 500_000 -> showLargeShareDialog = true
            ContextSanitizer.hasPotentialSecrets(editorContent) -> showSecretShareDialog = true
            sizeBytes > 200_000 -> showLargeShareDialog = true
            else -> shareEditorContent(editorContent)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBg)
    ) {
        EditorHeader(
            fileName = fileName,
            relativePath = relativePath ?: fileName,
            fileType = fileType,
            isModified = isModified,
            isLarge = isLarge,
            isHuge = isHuge,
            sizeKb = sizeKb,
            isLoading = viewModel.isOpenFileLoading,
            isSaving = viewModel.isOpenFileSaving,
            isCheckingConflicts = isCheckingConflicts,
            showLineNumbers = showLineNumbers,
            onToggleLineNumbers = { showLineNumbers = !showLineNumbers },
            showFind = showFind,
            onToggleFind = { showFind = !showFind },
            onSave = {
                viewModel.checkForConflicts { hasDiskConflict ->
                    if (hasDiskConflict) {
                        showConflictDialog = true
                    } else {
                        fileUri?.let { uri ->
                            viewModel.saveFileContent(uri, editorContent)
                        }
                    }
                }
            },
            onRevert = { viewModel.revertEditorContent() },
            onCopyAll = { clipboard.setText(AnnotatedString(editorContent)) },
            onPreview = previewLambda,
            previewDisabledReason = previewDisabledReason,
            onCreatePatch = patchLambda,
            patchDisabledReason = patchDisabledReason,
            onShare = if (fileUri != null && fileName != null && onShareFile != null && !isHuge) {
                { requestShare() }
            } else null,
            shareDisabledReason = if (isHuge) "Datei zu gross zum Teilen" else null
        )

        HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

        if (showFind) {
            FindBar(query = findQuery, onQueryChange = { findQuery = it })
            HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)
        }

        if (fileUri == null) {
            PanelPlaceholder(
                icon = Icons.Default.Code,
                title = "Keine Datei offen",
                subtitle = "Wähle im Files-Tab eine Datei aus.",
                actionLabel = if (onOpenFiles != null) "Open Files" else null,
                onAction = onOpenFiles
            )
            return@Column
        }

        if (viewModel.isOpenFileLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CalmSage)
            }
            return@Column
        }

        if (isHuge) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, tint = WarmCopper, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Datei zu groß (${sizeKb} KB)", color = WarmCopper, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Dateien > 500 KB werden nicht im Editor geöffnet.", color = TextSecondary, fontSize = 12.sp)
                }
            }
            return@Column
        }

        if (isLarge && !editingEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmCopper.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = WarmCopper, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Read-only: ${sizeKb} KB — ", color = WarmCopper, fontSize = 11.sp)
                TextButton(
                    onClick = { editingEnabled = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("Edit trotzdem", color = WarmCopper, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (editingEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    if (showLineNumbers) {
                        LineNumberGutter(lines = editorLines)
                    }
                    BasicTextField(
                        value = editorContent,
                        onValueChange = { viewModel.updateEditorContent(it) },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(CalmSage),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (showLineNumbers) 8.dp else 12.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (editorContent.isEmpty()) {
                                Box {
                                    Text(
                                        "// Code hier bearbeiten ...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color(0xFF3A3A44)
                                    )
                                    innerTextField()
                                }
                            } else {
                                innerTextField()
                            }
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    if (showLineNumbers) {
                        LineNumberGutter(lines = editorLines)
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = if (showLineNumbers) 8.dp else 12.dp, vertical = 8.dp)
                    ) {
                        editorLines.forEach { line ->
                            Text(
                                text = line.ifEmpty { " " },
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        if (showFind && findQuery.isNotBlank()) {
            val matchCount = editorContent.split(findQuery, ignoreCase = true).size - 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A24))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("$matchCount Treffer", color = CalmSage, fontSize = 10.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111115))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${editorLines.size} lines - ${editorContent.length} chars",
                color = Color(0xFF50505C),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                if (isModified) "Modified" else "",
                color = if (isModified) CalmSage else Color.Transparent,
                fontSize = 10.sp
            )
        }
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            containerColor = Color(0xFF1A1A22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = WarmCopper, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Datei-Konflikt", color = WarmCopper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Die Datei wurde auf dem Datentraeger geaendert, seit sie geoeffnet wurde. " +
                    "Deine Aenderungen koennten fremde Aenderungen ueberschreiben.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        viewModel.reloadFromDisk()
                        showConflictDialog = false
                    }) {
                        Text("Von Disk neu laden", color = TextSecondary, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        showConflictDialog = false
                        fileUri?.let { uri ->
                            viewModel.saveFileContent(uri, editorContent)
                        }
                    }) {
                        Text("Trotzdem speichern", color = WarmCopper, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConflictDialog = false }) {
                    Text("Abbrechen", color = TextSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    if (showSecretShareDialog) {
        AlertDialog(
            onDismissRequest = { showSecretShareDialog = false },
            containerColor = Color(0xFF1A1A22),
            title = {
                Text("Moegliche Secrets erkannt", color = WarmCopper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Diese Datei enthaelt moeglicherweise API-Keys, Tokens oder Secrets.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSecretShareDialog = false
                    shareEditorContent(ContextSanitizer.redactSummary(editorContent))
                }) {
                    Text("Share sanitized copy", color = CalmSage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showSecretShareDialog = false }) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        showSecretShareDialog = false
                        shareEditorContent(editorContent)
                    }) {
                        Text("Share anyway", color = WarmCopper, fontSize = 12.sp)
                    }
                }
            }
        )
    }

    if (showLargeShareDialog) {
        val isBlocked = editorContent.encodeToByteArray().size > 500_000
        AlertDialog(
            onDismissRequest = { showLargeShareDialog = false },
            containerColor = Color(0xFF1A1A22),
            title = {
                Text("Grosse Datei teilen", color = WarmCopper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    if (isBlocked) {
                        "Dateien ueber 500 KB werden nicht ueber den Editor geteilt."
                    } else {
                        "Diese Datei ist groesser als 200 KB. Pruefe vor dem Teilen, ob der Inhalt wirklich raus soll."
                    },
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                if (!isBlocked) {
                    TextButton(onClick = {
                        showLargeShareDialog = false
                        shareEditorContent(editorContent)
                    }) {
                        Text("Share anyway", color = WarmCopper, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLargeShareDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                }
            }
        )
    }
}

// ─── Editor Header ────────────────────────────────────────────────────────────
@Composable
private fun EditorHeader(
    fileName: String?,
    relativePath: String?,
    fileType: String,
    isModified: Boolean,
    isLarge: Boolean,
    isHuge: Boolean,
    sizeKb: Long,
    isLoading: Boolean,
    isSaving: Boolean,
    isCheckingConflicts: Boolean,
    showLineNumbers: Boolean,
    onToggleLineNumbers: () -> Unit,
    showFind: Boolean,
    onToggleFind: () -> Unit,
    onSave: () -> Unit,
    onRevert: () -> Unit,
    onCopyAll: () -> Unit,
    onPreview: (() -> Unit)?,
    previewDisabledReason: String?,
    onCreatePatch: (() -> Unit)?,
    patchDisabledReason: String?,
    onShare: (() -> Unit)? = null,
    shareDisabledReason: String? = null
) {
    val statusLabel = when {
        isHuge -> "Read-only"
        isLarge && isModified -> "Unsaved (large)"
        isLarge -> "Read-only (large)"
        isModified -> "Unsaved"
        else -> "Saved"
    }
    val statusColor = when {
        isHuge -> WarmCopper
        isLarge -> Color(0xFFF0AD4E)
        isModified -> CalmSage
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ToolbarBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, null, tint = SlateBlue, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                text = fileName ?: "Keine Datei",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (fileType.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Surface(color = SlateBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(3.dp)) {
                    Text(
                        fileType.uppercase(),
                        color = SlateBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            if (isModified) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CalmSage, shape = RoundedCornerShape(4.dp))
                )
            }
            if (isLoading) {
                Spacer(Modifier.width(6.dp))
                CircularProgressIndicator(Modifier.size(12.dp), color = CalmSage, strokeWidth = 1.5.dp)
            }
            Spacer(Modifier.weight(1f))
            if (fileName != null) {
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(3.dp)) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("${sizeKb} KB", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        if (relativePath != null && relativePath != fileName) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = relativePath,
                color = Color(0xFF50505C),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }

        if (fileName != null && !isHuge) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(Icons.Default.Save, "Save", isModified && !isSaving && !isCheckingConflicts, CalmSage, onSave)
                if (isSaving || isCheckingConflicts) {
                    CircularProgressIndicator(Modifier.size(12.dp), color = CalmSage, strokeWidth = 1.5.dp)
                }

                ToolbarIconButton(Icons.Default.Undo, "Revert", isModified, WarmCopper, onRevert)
                ToolbarIconButton(Icons.Default.ContentCopy, "Copy all", true, SlateBlue, onCopyAll)

                if (onShare != null || shareDisabledReason != null) {
                    ToolbarTextButton(
                        label = "Share", icon = Icons.Default.Share,
                        enabled = onShare != null, activeColor = Color(0xFFF0AD4E),
                        bgColor = Color(0xFF2E241A), disabledReason = shareDisabledReason,
                        clickAction = onShare ?: {}
                    )
                }
                ToolbarIconButton(Icons.Default.Search, "Find", true, if (showFind) CalmSage else TextSecondary, onToggleFind)
                ToolbarIconButton(Icons.Default.FormatListNumbered, "Line numbers", true, if (showLineNumbers) CalmSage else TextSecondary, onToggleLineNumbers)

                Spacer(Modifier.weight(1f))

                if (onPreview != null || previewDisabledReason != null) {
                    ToolbarTextButton(
                        label = "Preview", icon = Icons.Default.Visibility,
                        enabled = onPreview != null, activeColor = CalmSage,
                        bgColor = Color(0xFF1E2E1E), disabledReason = previewDisabledReason,
                        clickAction = onPreview ?: {}
                    )
                }

                if (onCreatePatch != null || patchDisabledReason != null) {
                    ToolbarTextButton(
                        label = "Diff", icon = Icons.Default.Compare,
                        enabled = onCreatePatch != null, activeColor = SlateBlue,
                        bgColor = Color(0xFF1E242E), disabledReason = patchDisabledReason,
                        clickAction = onCreatePatch ?: {}
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(28.dp)) {
        Icon(icon, contentDescription, tint = if (enabled) activeColor else Color(0xFF3A3A44), modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    activeColor: Color,
    bgColor: Color,
    disabledReason: String?,
    clickAction: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { if (enabled) clickAction() },
            color = if (enabled) bgColor else Color(0xFF2A2A2A),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = if (enabled) activeColor else Color(0xFF555555), modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(label, color = if (enabled) activeColor else Color(0xFF555555), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (!enabled && disabledReason != null) {
            Text(
                disabledReason,
                color = Color(0xFF6A6A6A),
                fontSize = 8.sp,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 100.dp)
            )
        }
    }
}

private fun detectFileType(fileName: String?): String {
    if (fileName == null) return ""
    return when {
        fileName.endsWith(".kt", ignoreCase = true) -> "kotlin"
        fileName.endsWith(".kts", ignoreCase = true) -> "gradle kotlin"
        fileName.endsWith(".java", ignoreCase = true) -> "java"
        fileName.endsWith(".xml", ignoreCase = true) -> "xml"
        fileName.endsWith(".json", ignoreCase = true) -> "json"
        fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true) -> "html"
        fileName.endsWith(".css", ignoreCase = true) -> "css"
        fileName.endsWith(".js", ignoreCase = true) -> "javascript"
        fileName.endsWith(".ts", ignoreCase = true) -> "typescript"
        fileName.endsWith(".py", ignoreCase = true) -> "python"
        fileName.endsWith(".md", ignoreCase = true) -> "markdown"
        fileName.endsWith(".gradle", ignoreCase = true) -> "gradle"
        fileName.endsWith(".properties", ignoreCase = true) -> "properties"
        fileName.endsWith(".yaml", ignoreCase = true) || fileName.endsWith(".yml", ignoreCase = true) -> "yaml"
        fileName.endsWith(".sh", ignoreCase = true) -> "shell"
        fileName.endsWith(".sql", ignoreCase = true) -> "sql"
        else -> ""
    }
}

// ─── Line Number Gutter ───────────────────────────────────────────────────────
@Composable
private fun LineNumberGutter(lines: List<String>) {
    Column(
        modifier = Modifier
            .background(LineNrBg)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        lines.forEachIndexed { i, _ ->
            Text(
                text = "${i + 1}",
                color = LineNrColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ─── Find Bar ─────────────────────────────────────────────────────────────────
@Composable
private fun FindBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A24))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Suchen ...", color = Color(0xFF3A3A44), fontSize = 11.sp) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f).height(32.dp),
            singleLine = true
        )
    }
}
