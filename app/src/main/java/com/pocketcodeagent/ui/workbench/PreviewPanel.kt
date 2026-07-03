package com.pocketcodeagent.ui.workbench

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.preview.StaticPreviewBundler
import com.pocketcodeagent.domain.preview.StaticPreviewResult
import com.pocketcodeagent.data.repository.WorkspaceRepository
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_CONSOLE_LOGS = 200
private const val DEFAULT_DEV_URL = "http://127.0.0.1:5173"

// Secrets-Patterns fuer Console-Log-Sanitization
private val SECRET_PATTERNS = listOf(
    Regex("""(?i)bearer\s+[A-Za-z0-9._\-]{20,}""") to "Bearer [redacted]",
    Regex("""(?i)sk-[A-Za-z0-9]{20,}""") to "sk-[redacted]",
    Regex("""(?i)nvapi-[A-Za-z0-9]{20,}""") to "nvapi-[redacted]",
    Regex("""(?i)(api[_-]?key|apikey|api_secret|token|secret|password|authorization)\s*[:=]\s*[^\s,;)]{8,}""") to "$1=[redacted]"
)

enum class PreviewMode { WORKSPACE, FILE, URL }

@Composable
fun PreviewPanel(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    previewTarget: PreviewTarget,
    onTargetChanged: (PreviewTarget) -> Unit,
    repository: WorkspaceRepository,
    workspacePreviewReady: Boolean = false,
    onPreviewReadyConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = repository.context
    val bundler = remember { StaticPreviewBundler(context) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var previewMode by remember { mutableStateOf(PreviewMode.WORKSPACE) }
    var urlInput by remember { mutableStateOf(DEFAULT_DEV_URL) }
    var webError by remember { mutableStateOf<String?>(null) }
    val consoleLogs = remember { mutableStateListOf<String>() }
    var showConsole by remember { mutableStateOf(false) }
    var showWarnings by remember { mutableStateOf(false) }
    var showTermux by remember { mutableStateOf(false) }

    var manualReloadToken by remember { mutableStateOf(0) }
    var lastReloadTime by remember { mutableStateOf<Long?>(null) }

    var bundleResult by remember { mutableStateOf<StaticPreviewResult?>(null) }
    var isBundling by remember { mutableStateOf(false) }

    // Workspace preview ready hint (set by MainViewModel after diff apply)
    var showPreviewReadyHint by remember { mutableStateOf(false) }

    // Sync from MainViewModel workspacePreviewReady state
    LaunchedEffect(workspacePreviewReady) {
        if (workspacePreviewReady) {
            showPreviewReadyHint = true
            onPreviewReadyConsumed()
        }
    }

    // Remember scope for coroutines
    val scope = rememberCoroutineScope()

    // React to previewTarget changes
    LaunchedEffect(previewTarget) {
        when (previewTarget) {
            is PreviewTarget.WorkspaceStatic -> {
                previewMode = PreviewMode.WORKSPACE
                showPreviewReadyHint = false
                loadWorkspaceBundleAsync(scope, workspaceUriString, bundler, repository) { result ->
                    bundleResult = result
                    webViewRef?.loadDataWithBaseURL(null, result.html, "text/html", "UTF-8", null)
                    lastReloadTime = System.currentTimeMillis()
                }
            }
            is PreviewTarget.File -> {
                previewMode = PreviewMode.FILE
                loadWorkspaceBundleAsync(scope, workspaceUriString, bundler, repository, previewTarget.path) { result ->
                    bundleResult = result
                    webViewRef?.loadDataWithBaseURL(null, result.html, "text/html", "UTF-8", null)
                    lastReloadTime = System.currentTimeMillis()
                }
            }
            is PreviewTarget.Url -> {
                previewMode = PreviewMode.URL
                urlInput = previewTarget.url
                webError = null
                webViewRef?.loadUrl(previewTarget.url)
            }
            is PreviewTarget.None -> { /* keep current state */ }
        }
    }

    // Manual reload
    LaunchedEffect(manualReloadToken) {
        if (manualReloadToken > 0) {
            webError = null
            when (previewMode) {
                PreviewMode.WORKSPACE -> {
                    loadWorkspaceBundleAsync(scope, workspaceUriString, bundler, repository) { result ->
                        bundleResult = result
                        webViewRef?.loadDataWithBaseURL(null, result.html, "text/html", "UTF-8", null)
                        lastReloadTime = System.currentTimeMillis()
                    }
                }
                PreviewMode.FILE -> {
                    bundleResult?.let { br ->
                        loadWorkspaceBundleAsync(scope, workspaceUriString, bundler, repository, br.sourcePath) { result ->
                            bundleResult = result
                            webViewRef?.loadDataWithBaseURL(null, result.html, "text/html", "UTF-8", null)
                            lastReloadTime = System.currentTimeMillis()
                        }
                    }
                }
                PreviewMode.URL -> webViewRef?.reload()
            }
        }
    }

    // Auto-reload on file writes when workspace preview is active
    var lastWriteTimestamp by remember { mutableStateOf(0L) }
    LaunchedEffect(viewModel.lastFileWriteTimestamp) {
        if (viewModel.lastFileWriteTimestamp > 0 && viewModel.lastFileWriteTimestamp != lastWriteTimestamp) {
            lastWriteTimestamp = viewModel.lastFileWriteTimestamp
            if (previewMode == PreviewMode.WORKSPACE || previewMode == PreviewMode.FILE) {
                loadWorkspaceBundleAsync(scope, workspaceUriString, bundler, repository,
                    if (previewMode == PreviewMode.FILE) bundleResult?.sourcePath else null
                ) { result ->
                    bundleResult = result
                    webViewRef?.loadDataWithBaseURL(null, result.html, "text/html", "UTF-8", null)
                    lastReloadTime = System.currentTimeMillis()
                }
            }
        }
    }

    // WebView lifecycle cleanup
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13131A))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Web, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Preview", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            // Clear cache
            IconButton(onClick = { webViewRef?.clearCache(true) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteSweep, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            // Reload
            IconButton(onClick = { manualReloadToken++ }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

        // Segmented Mode Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111116))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedChip("Workspace", Icons.Default.FolderOpen, previewMode == PreviewMode.WORKSPACE) {
                previewMode = PreviewMode.WORKSPACE
                onTargetChanged(PreviewTarget.WorkspaceStatic)
            }
            SegmentedChip("File", Icons.Default.Code, previewMode == PreviewMode.FILE) {
                previewMode = PreviewMode.FILE
            }
            SegmentedChip("URL", Icons.Default.Link, previewMode == PreviewMode.URL) {
                previewMode = PreviewMode.URL
            }
        }

        // Mode-specific controls
        when (previewMode) {
            PreviewMode.WORKSPACE -> {
                WorkspaceControls(
                    workspaceUriString = workspaceUriString,
                    bundleResult = bundleResult,
                    isBundling = isBundling,
                    onLoadClick = {
                        onTargetChanged(PreviewTarget.WorkspaceStatic)
                    }
                )
            }
            PreviewMode.FILE -> {
                FileControls(
                    bundleResult = bundleResult,
                    isBundling = isBundling,
                    onLoadClick = {
                        bundleResult?.let { br ->
                            val fileName = br.sourcePath.substringAfterLast('/')
                            onTargetChanged(PreviewTarget.File(br.sourcePath, fileName))
                        }
                    },
                    onPickFile = { /* file picker not yet integrated */ }
                )
            }
            PreviewMode.URL -> {
                UrlControls(
                    urlInput = urlInput,
                    onUrlChange = { urlInput = it },
                    onLoadClick = {
                        onTargetChanged(PreviewTarget.Url(urlInput))
                    },
                    onClearCache = { webViewRef?.clearCache(true) }
                )
            }
        }

        HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)

        // Status bar
        StatusBar(
            bundleResult = bundleResult,
            previewMode = previewMode,
            lastReloadTime = lastReloadTime
        )

        // Workspace preview ready hint
        AnimatedVisibility(visible = showPreviewReadyHint) {
            Surface(color = SlateBlue.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = CalmSage, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Workspace preview ready", color = CalmSage, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        showPreviewReadyHint = false
                        onTargetChanged(PreviewTarget.WorkspaceStatic)
                    }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Jetzt laden", color = SlateBlue, fontSize = 11.sp)
                    }
                    IconButton(onClick = { showPreviewReadyHint = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Error banner
        webError?.let { err ->
            Surface(color = Color(0xFF240D0D), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = WarmCopper, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(err, color = WarmCopper, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { webError = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = WarmCopper, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                val msg = when (error?.errorCode) {
                                    -2 -> "Server nicht erreichbar (${error.description})"
                                    -3 -> "SSL / HTTP Fehler (${error.description})"
                                    -6 -> "Timeout — Verbindung zu langsam"
                                    else -> "Ladefehler: ${error?.description ?: "Unbekannt"}"
                                }
                                webError = msg
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                            msg?.let {
                                val raw = "[${it.messageLevel()}] ${it.message()}"
                                val sanitized = sanitizeConsoleLog(raw)
                                if (consoleLogs.size >= MAX_CONSOLE_LOGS) consoleLogs.removeAt(0)
                                consoleLogs.add(sanitized)
                            }
                            return true
                        }
                    }
                    webViewRef = this
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        // Collapsible bottom panels
        BottomPanels(
            showConsole = showConsole,
            showWarnings = showWarnings,
            showTermux = showTermux,
            onToggleConsole = { showConsole = !showConsole },
            onToggleWarnings = { showWarnings = !showWarnings },
            onToggleTermux = { showTermux = !showTermux },
            consoleLogs = consoleLogs,
            bundleResult = bundleResult,
            onClearLogs = { consoleLogs.clear() },
            onCopyLogs = {
                val sanitized = consoleLogs.joinToString("\n")
                copyText(context, sanitized)
            }
        )
    }
}

// ─── Helper Composable: Segmented Control Chip ────────────────────────────

@Composable
private fun RowScope.SegmentedChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) SlateBlue.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        modifier = Modifier.weight(1f).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (selected) SlateBlue else Color(0xFF555560), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) SlateBlue else Color(0xFF777783))
        }
    }
}

// ─── Helper: Workspace Mode Controls ───────────────────────────────────────

@Composable
private fun WorkspaceControls(
    workspaceUriString: String?,
    bundleResult: StaticPreviewResult?,
    isBundling: Boolean,
    onLoadClick: () -> Unit
) {
    Surface(color = Color(0xFF13131A), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("index.html aus Workspace laden", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onLoadClick,
                    enabled = workspaceUriString != null && !isBundling,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Load Workspace Preview", color = Color.White, fontSize = 11.sp)
                }
            }
            bundleResult?.sourcePath?.let { path ->
                Text("Source: $path", color = Color(0xFF555560), fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ─── Helper: File Mode Controls ────────────────────────────────────────────

@Composable
private fun FileControls(
    bundleResult: StaticPreviewResult?,
    isBundling: Boolean,
    onLoadClick: () -> Unit,
    onPickFile: () -> Unit
) {
    Surface(color = Color(0xFF13131A), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            bundleResult?.sourcePath?.let { path ->
                if (path.endsWith(".html", ignoreCase = true) || path.endsWith(".htm", ignoreCase = true)) {
                    Text("Preview: $path", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Preview: $path", color = WarmCopper, fontSize = 11.sp)
                    Text("Nur .html/.htm-Dateien werden unterstuetzt.", color = TextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.height(6.dp))
            } ?: Text("Keine HTML-Datei ausgewaehlt — oeffne eine .html Datei im Editor.",
                color = TextSecondary, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(
                    onClick = onLoadClick,
                    enabled = !isBundling && bundleResult != null,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                    shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Preview current file", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

// ─── Helper: URL Mode Controls ─────────────────────────────────────────────

@Composable
private fun UrlControls(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onLoadClick: () -> Unit,
    onClearCache: () -> Unit
) {
    Surface(color = Color(0xFF13131A), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        focusedBorderColor = SlateBlue,
                        unfocusedBorderColor = BorderGrey
                    ),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onLoadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                    shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Load", color = Color.White, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onClearCache, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Clear WebView Cache", color = Color(0xFF777783), fontSize = 10.sp)
                }
                Text("Für React/Vite/Node-Projekte muss ein lokaler Dev-Server z.B. in Termux laufen.",
                    color = TextSecondary, fontSize = 9.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─── Helper: Status Bar ────────────────────────────────────────────────────

@Composable
private fun StatusBar(
    bundleResult: StaticPreviewResult?,
    previewMode: PreviewMode,
    lastReloadTime: Long?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0C))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val targetText = when (previewMode) {
            PreviewMode.WORKSPACE -> bundleResult?.sourcePath ?: "Workspace"
            PreviewMode.FILE -> bundleResult?.sourcePath ?: "File"
            PreviewMode.URL -> "URL"
        }
        Text("Target: $targetText", color = Color(0xFF555560), fontSize = 10.sp, maxLines = 1)

        lastReloadTime?.let {
            val secs = (System.currentTimeMillis() - it) / 1000
            Text("Last reload: ${secs}s ago", color = Color(0xFF444450), fontSize = 10.sp)
        }

        bundleResult?.let { br ->
            val warnCount = br.warnings.size + br.errors.size
            if (warnCount > 0) {
                Text("⚠ $warnCount", color = if (br.errors.isNotEmpty()) WarmCopper else Color(0xFFF0AD4E), fontSize = 10.sp)
            } else {
                Text("✓", color = CalmSage, fontSize = 10.sp)
            }
        }
    }
    HorizontalDivider(color = Color(0xFF1A1A22), thickness = 0.5.dp)
}

// ─── Helper: Collapsible Bottom Panels ─────────────────────────────────────

@Composable
private fun BottomPanels(
    showConsole: Boolean,
    showWarnings: Boolean,
    showTermux: Boolean,
    onToggleConsole: () -> Unit,
    onToggleWarnings: () -> Unit,
    onToggleTermux: () -> Unit,
    consoleLogs: List<String>,
    bundleResult: StaticPreviewResult?,
    onClearLogs: () -> Unit,
    onCopyLogs: () -> Unit
) {
    Column {
        // Panel toggle buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111116))
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PanelToggle("Console", Icons.Default.Terminal, consoleLogs.size, showConsole, onToggleConsole)
            val warnCount = (bundleResult?.warnings?.size ?: 0) + (bundleResult?.errors?.size ?: 0)
            PanelToggle("Warnings", Icons.Default.Warning, warnCount, showWarnings, onToggleWarnings)
            PanelToggle("Termux", Icons.Default.PhoneAndroid, 0, showTermux, onToggleTermux)
        }

        // Console panel
        AnimatedVisibility(visible = showConsole, enter = expandVertically(), exit = shrinkVertically()) {
            Surface(color = Color(0xFF0A0A0C), modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("WebView Konsole (max $MAX_CONSOLE_LOGS, sanitized)", color = Color(0xFF555560), fontSize = 10.sp)
                        Row {
                            IconButton(onClick = onCopyLogs, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            }
                            IconButton(onClick = onClearLogs, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Delete, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        items(consoleLogs.takeLast(MAX_CONSOLE_LOGS)) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("[ERROR]")) Color(0xFFFF8888)
                                else if (log.contains("[WARNING]")) Color(0xFFFFCC80)
                                else Color(0xFF8B949E),
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Warnings panel
        AnimatedVisibility(visible = showWarnings, enter = expandVertically(), exit = shrinkVertically()) {
            Surface(color = Color(0xFF0A0A0C), modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    bundleResult?.errors?.forEach { error ->
                        item { WarningEntry(error, isError = true) }
                    }
                    bundleResult?.warnings?.forEach { warning ->
                        item { WarningEntry(warning, isError = false) }
                    }
                    if ((bundleResult?.errors?.size ?: 0) + (bundleResult?.warnings?.size ?: 0) == 0) {
                        item { Text("Keine Warnungen", color = Color(0xFF555560), fontSize = 11.sp) }
                    }
                }
            }
        }

        // Termux help panel
        AnimatedVisibility(visible = showTermux, enter = expandVertically(), exit = shrinkVertically()) {
            TermuxHelpPanel()
        }
    }
}

@Composable
private fun WarningEntry(text: String, isError: Boolean) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isError) Icons.Default.Error else Icons.Default.Warning, null,
            tint = if (isError) WarmCopper else Color(0xFFF0AD4E), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (isError) WarmCopper else Color(0xFFCCB860), fontSize = 10.sp)
    }
}

@Composable
private fun RowScope.PanelToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (expanded) Color(0xFF1E1E26) else Color.Transparent,
        onClick = onClick,
        modifier = Modifier.weight(1f).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (expanded) SlateBlue else Color(0xFF555560), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("$label${if (count > 0) " ($count)" else ""}", fontSize = 10.sp,
                color = if (expanded) SlateBlue else Color(0xFF777783))
        }
    }
}

// ─── Helper: Termux Help ───────────────────────────────────────────────────

@Composable
private fun TermuxHelpPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(color = Color(0xFF0A0A0C), modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
        LazyColumn(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Termux Dev-Server einrichten", color = SlateBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("Für React/Vite/Node-Projekte brauchst du einen Dev-Server. Installiere Termux und führe diese Commands aus:",
                    color = TextSecondary, fontSize = 10.sp)
            }
            item { TermuxCmd("1. pkg update") { copyText(context, "pkg update") } }
            item { TermuxCmd("2. pkg install nodejs git") { copyText(context, "pkg install nodejs git") } }
            item { TermuxCmd("3. cd /sdcard/Download/PocketCodeAgent") { copyText(context, "cd /sdcard/Download/DEIN_PROJEKT") } }
            item { TermuxCmd("4. npm install") { copyText(context, "npm install") } }
            item { TermuxCmd("5. npm run dev -- --host 127.0.0.1") { copyText(context, "npm run dev -- --host 127.0.0.1") } }
            item {
                TextButton(
                    onClick = { copyText(context, "pkg update && pkg install nodejs git") },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("📋 Alle Commands kopieren", color = CalmSage, fontSize = 11.sp)
                }
            }
            item {
                Text("Keine Fake-Ausführung. Starte Termux manuell und gib die Commands ein.",
                    color = Color(0xFF555560), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun TermuxCmd(command: String, onClick: () -> Unit) {
    Surface(color = Color(0xFF13131A), shape = RoundedCornerShape(6.dp), onClick = onClick,
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Text(command, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                modifier = Modifier.weight(1f))
            Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF555560), modifier = Modifier.size(12.dp))
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────

private fun sanitizeConsoleLog(log: String): String {
    var sanitized = log
    for ((pattern, replacement) in SECRET_PATTERNS) {
        sanitized = pattern.replace(sanitized, replacement)
    }
    return sanitized
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Termux", text))
}

private fun loadWorkspaceBundleAsync(
    scope: kotlinx.coroutines.CoroutineScope,
    workspaceUriString: String?,
    bundler: StaticPreviewBundler,
    repository: WorkspaceRepository,
    startFileName: String? = null,
    onResult: (StaticPreviewResult) -> Unit
) {
    if (workspaceUriString == null) {
        onResult(StaticPreviewResult(
            html = "<html><body><h3 style='color:#888;text-align:center;padding-top:100px;font-family:sans-serif;'>Kein Workspace geoeffnet</h3></body></html>",
            sourcePath = "", errors = listOf("Kein Workspace ausgewaehlt.")))
        return
    }
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            repository.workspace.setRootUri(workspaceUriString)
            bundler.bundleFromWorkspace(repository.workspace, workspaceUriString, startFileName)
        }
        onResult(result)
    }
}


