package com.pocketcodeagent.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
fun LivePreviewScreen(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeModeTab by remember { mutableStateOf(0) } // 0: Static Preview, 1: Local Server, 2: Console, 3: Termux Help

    var previewUrl by remember { mutableStateOf("http://127.0.0.1:5173") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var indexHtmlUriString by remember { mutableStateOf<String?>(null) }
    var selectedHtmlName by remember { mutableStateOf("index.html") }
    var staticHtmlContent by remember { mutableStateOf<String?>(null) }
    
    // WebView Settings states
    var jsEnabled by remember { mutableStateOf(true) }
    var domStorageEnabled by remember { mutableStateOf(true) }
    var allowFileAccess by remember { mutableStateOf(true) }
    
    // Error state
    var webErrorState by remember { mutableStateOf<String?>(null) }

    // Dialogs
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFilePickerDialog by remember { mutableStateOf(false) }

    // Captured console logs
    val consoleLogs = remember { mutableStateListOf<String>() }

    // Cleanup WebView on exit
    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    // 1. Initial file check
    LaunchedEffect(workspaceUriString) {
        if (workspaceUriString != null) {
            val indexUri = viewModel.repository.getFileUriByRelativePath(workspaceUriString, "index.html")
            if (indexUri != null) {
                indexHtmlUriString = indexUri.toString()
                selectedHtmlName = "index.html"
                staticHtmlContent = viewModel.repository.readFile(indexUri.toString())
            }
        }
    }

    // 2. Auto-reload trigger on file edits
    LaunchedEffect(viewModel.lastFileWriteTimestamp) {
        if (viewModel.lastFileWriteTimestamp > 0) {
            consoleLogs.add("[System] 🔄 Dateiänderung erkannt. Lade neu...")
            webErrorState = null
            if (activeModeTab == 0 && indexHtmlUriString != null) {
                staticHtmlContent = viewModel.repository.readFile(indexHtmlUriString!!)
                webViewInstance?.loadDataWithBaseURL("file:///", staticHtmlContent ?: "", "text/html", "UTF-8", null)
            } else if (activeModeTab == 1) {
                webViewInstance?.reload()
            }
        }
    }

    // Helper to copy to clipboard
    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Kopiert! 📋", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Preview Panel 👁️", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Settings button
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = SlateBlue)
                    }
                    // Reload button
                    IconButton(onClick = {
                        webErrorState = null
                        consoleLogs.add("[System] 🔄 Manuelles Neuladen ausgelöst.")
                        if (activeModeTab == 0 && indexHtmlUriString != null) {
                            staticHtmlContent = viewModel.repository.readFile(indexHtmlUriString!!)
                            webViewInstance?.loadDataWithBaseURL("file:///", staticHtmlContent ?: "", "text/html", "UTF-8", null)
                        } else {
                            webViewInstance?.reload()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload", tint = CalmSage)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = DeepSlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row selectors
            TabRow(
                selectedTabIndex = activeModeTab,
                containerColor = Color(0xFF0F0C1B),
                contentColor = SlateBlue
            ) {
                Tab(
                    selected = activeModeTab == 0,
                    onClick = { activeModeTab = 0; webErrorState = null },
                    text = { Text("Static Web", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeModeTab == 1,
                    onClick = { activeModeTab = 1; webErrorState = null },
                    text = { Text("Local Server", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Web, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeModeTab == 2,
                    onClick = { activeModeTab = 2 },
                    text = { Text("Logs (${consoleLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeModeTab == 3,
                    onClick = { activeModeTab = 3 },
                    text = { Text("Termux Hilfe", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (activeModeTab) {
                0 -> {
                    // MODUS A: Static Web Preview
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Geladene Datei:", color = TextSecondary, fontSize = 10.sp)
                                    Text(
                                        text = selectedHtmlName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Button(
                                    onClick = { showFilePickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Wähle Datei", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        // WebView Render Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            if (webErrorState != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF2D1818))
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("⚠️ WebView Fehler", color = WarmCopper, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(webErrorState!!, color = Color.LightGray, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            webErrorState = null
                                            webViewInstance?.loadDataWithBaseURL("file:///", staticHtmlContent ?: "", "text/html", "UTF-8", null)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue)
                                    ) {
                                        Text("Wiederholen", color = Color.White)
                                    }
                                }
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            webViewClient = object : WebViewClient() {
                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    request: WebResourceRequest?,
                                                    error: WebResourceError?
                                                ) {
                                                    super.onReceivedError(view, request, error)
                                                    webErrorState = error?.description?.toString() ?: "Fehler beim Laden"
                                                }
                                            }
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                    consoleMessage?.let {
                                                        consoleLogs.add("[${it.messageLevel().name}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                                                    }
                                                    return true
                                                }
                                            }
                                            settings.javaScriptEnabled = jsEnabled
                                            settings.domStorageEnabled = domStorageEnabled
                                            settings.allowFileAccess = allowFileAccess
                                            webViewInstance = this
                                            
                                            if (staticHtmlContent != null) {
                                                loadDataWithBaseURL("file:///", staticHtmlContent!!, "text/html", "UTF-8", null)
                                            } else {
                                                loadDataWithBaseURL("file:///", "<html><body><h3 style='color:#666;text-align:center;padding-top:100px;'>Keine index.html im Workspace gefunden.</h3></body></html>", "text/html", "UTF-8", null)
                                            }
                                        }
                                    },
                                    update = { webView ->
                                        webViewInstance = webView
                                        webView.settings.javaScriptEnabled = jsEnabled
                                        webView.settings.domStorageEnabled = domStorageEnabled
                                        webView.settings.allowFileAccess = allowFileAccess
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // MODUS B: Local Server Preview
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = previewUrl,
                                        onValueChange = { previewUrl = it },
                                        label = { Text("Server URL Link") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = SlateBlue,
                                            unfocusedBorderColor = Color(0xFF2E2D34)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            webErrorState = null
                                            webViewInstance?.loadUrl(previewUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue)
                                    ) {
                                        Text("Load", color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ℹ️ Für React/Vite/Node-Projekte muss ein lokaler Server z.B. über Termux laufen.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // WebView Render Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            if (webErrorState != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF2D1818))
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("⚠️ Server nicht erreichbar", color = WarmCopper, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Die Adresse $previewUrl konnte nicht geladen werden. Läuft dein Termux-Server?", color = Color.LightGray, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            webErrorState = null
                                            webViewInstance?.loadUrl(previewUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateBlue)
                                    ) {
                                        Text("Wiederholen", color = Color.White)
                                    }
                                }
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            webViewClient = object : WebViewClient() {
                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    request: WebResourceRequest?,
                                                    error: WebResourceError?
                                                ) {
                                                    super.onReceivedError(view, request, error)
                                                    // Filter out sub-resources to avoid breaking layout on minor assets
                                                    if (request?.isForMainFrame == true) {
                                                        webErrorState = error?.description?.toString() ?: "Verbindung verweigert"
                                                    }
                                                }
                                            }
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                    consoleMessage?.let {
                                                        consoleLogs.add("[${it.messageLevel().name}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                                                    }
                                                    return true
                                                }
                                            }
                                            settings.javaScriptEnabled = jsEnabled
                                            settings.domStorageEnabled = domStorageEnabled
                                            settings.allowFileAccess = allowFileAccess
                                            webViewInstance = this
                                            loadUrl(previewUrl)
                                        }
                                    },
                                    update = { webView ->
                                        webViewInstance = webView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Console logs
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("WebView Konsolenausgaben", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { consoleLogs.clear() },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmCopper.copy(alpha = 0.8f))
                            ) {
                                Text("Clear", color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (consoleLogs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Keine Konsolenlogs aufgefangen.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    items(consoleLogs) { log ->
                                        Text(
                                            text = log,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (log.contains("ERROR")) Color(0xFFFF8888) else if (log.contains("WARNING")) Color(0xFFFFCC80) else Color.LightGray,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Termux Help Instructions
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "Vollwertige Runtimes wie Node.js/Vite können auf Android aufgrund von Sandbox-Sperren nicht in normalen Apps laufen. Führe den Server in Termux aus und binde ihn hier ein.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        item {
                            TermuxCommandCard("1. Update & Repositories", "pkg update") { copyToClipboard("update", "pkg update") }
                        }
                        item {
                            TermuxCommandCard("2. Node & Git installieren", "pkg install nodejs git") { copyToClipboard("install", "pkg install nodejs git") }
                        }
                        item {
                            TermuxCommandCard("3. In Projektordner wechseln", "cd /sdcard/Android/media/com.pocketcodeagent") { copyToClipboard("cd", "cd /sdcard/Android/media/com.pocketcodeagent") }
                        }
                        item {
                            TermuxCommandCard("4. NPM-Pakete installieren", "npm install") { copyToClipboard("npm_install", "npm install") }
                        }
                        item {
                            TermuxCommandCard("5. Dev-Server starten", "npm run dev -- --host 127.0.0.1") { copyToClipboard("run", "npm run dev -- --host 127.0.0.1") }
                        }
                    }
                }
            }
        }
    }

    // Preview Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("WebView Einstellungen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("JavaScript aktivieren", color = TextPrimary)
                        Switch(checked = jsEnabled, onCheckedChange = { jsEnabled = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DOM Storage aktivieren", color = TextPrimary)
                        Switch(checked = domStorageEnabled, onCheckedChange = { domStorageEnabled = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lokalem Dateizugriff erlauben", color = TextPrimary)
                        Switch(checked = allowFileAccess, onCheckedChange = { allowFileAccess = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            webViewInstance?.clearCache(true)
                            Toast.makeText(context, "WebView Cache geleert", Toast.LENGTH_SHORT).show()
                            showSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCopper),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("WebView Cache leeren", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue)
                ) {
                    Text("Schließen", color = Color.White)
                }
            }
        )
    }

    // File Picker Dialog (for pick index.html in Workspace)
    if (showFilePickerDialog) {
        val htmlFiles = viewModel.files.filter { it.name.endsWith(".html", ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showFilePickerDialog = false },
            title = { Text("Wähle HTML Startdatei") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (htmlFiles.isEmpty()) {
                        Text("Keine HTML-Dateien im Workspace gefunden.", color = TextSecondary)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(htmlFiles) { file ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            indexHtmlUriString = file.uriString
                                            selectedHtmlName = file.name
                                            staticHtmlContent = viewModel.repository.readFile(file.uriString)
                                            showFilePickerDialog = false
                                            webErrorState = null
                                            webViewInstance?.loadDataWithBaseURL("file:///", staticHtmlContent ?: "", "text/html", "UTF-8", null)
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = file.name,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilePickerDialog = false }) {
                    Text("Abbrechen", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
fun TermuxCommandCard(title: String, command: String, onCopyClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = SlateBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = onCopyClick, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CalmSage, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = command,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
        }
    }
}
