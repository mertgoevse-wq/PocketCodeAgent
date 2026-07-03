package com.pocketcodeagent.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
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
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.GlowPink
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewScreen(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Preview, 1: Console Logs, 2: Termux Bridge
    
    var previewUrl by remember { mutableStateOf("http://127.0.0.1:5173") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var indexHtmlUriString by remember { mutableStateOf<String?>(null) }
    var staticHtmlContent by remember { mutableStateOf<String?>(null) }
    var isStaticMode by remember { mutableStateOf(false) }
    
    // React/Vite project detection state
    var isViteProject by remember { mutableStateOf(false) }
    var isReactProject by remember { mutableStateOf(false) }

    // Console logs captured from WebView
    val consoleLogs = remember { mutableStateListOf<String>() }

    // Dialog state for custom URL configuration
    var showUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(previewUrl) }

    // 1. Scan workspace for index.html and package.json
    LaunchedEffect(workspaceUriString) {
        if (workspaceUriString != null) {
            // Check for index.html
            val indexUri = viewModel.repository.getFileUriByRelativePath(workspaceUriString, "index.html")
            if (indexUri != null) {
                indexHtmlUriString = indexUri.toString()
                staticHtmlContent = viewModel.repository.readFile(indexUri.toString())
                isStaticMode = true
            } else {
                isStaticMode = false
            }

            // Check for package.json to detect Vite/React
            val packageJsonUri = viewModel.repository.getFileUriByRelativePath(workspaceUriString, "package.json")
            if (packageJsonUri != null) {
                val content = viewModel.repository.readFile(packageJsonUri.toString())
                if (content.isNotEmpty()) {
                    isViteProject = content.contains("\"vite\"")
                    isReactProject = content.contains("\"react\"")
                    if (isViteProject) {
                        isStaticMode = false // Dev server has priority over raw static file
                    }
                }
            }
        }
    }

    // 2. React to workspace file edits to auto-reload WebView
    LaunchedEffect(viewModel.lastFileWriteTimestamp) {
        if (viewModel.lastFileWriteTimestamp > 0) {
            consoleLogs.add("[System] 🔄 File edit detected. Reloading preview...")
            if (isStaticMode && indexHtmlUriString != null) {
                staticHtmlContent = viewModel.repository.readFile(indexHtmlUriString!!)
                webViewInstance?.loadDataWithBaseURL("", staticHtmlContent ?: "", "text/html", "UTF-8", null)
            } else {
                webViewInstance?.reload()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Preview Panel 👁️", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Set custom preview URL button
                    if (!isStaticMode) {
                        IconButton(onClick = { 
                            customUrlInput = previewUrl
                            showUrlDialog = true 
                        }) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = "Set URL", tint = ElectricTeal)
                        }
                    }

                    // Reload button
                    IconButton(onClick = {
                        consoleLogs.add("[System] 🔄 Manual reload triggered.")
                        if (isStaticMode && indexHtmlUriString != null) {
                            staticHtmlContent = viewModel.repository.readFile(indexHtmlUriString!!)
                            webViewInstance?.loadDataWithBaseURL("", staticHtmlContent ?: "", "text/html", "UTF-8", null)
                        } else {
                            webViewInstance?.reload()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload", tint = ElectricTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = Color(0xFF0C0A14)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F0C1B),
                contentColor = ElectricTeal
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Preview", fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Web, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Console (${consoleLogs.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.ListAlt, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Termux Bridge", fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Terminal, contentDescription = null) }
                )
            }

            // Tab rendering
            when (selectedTab) {
                0 -> {
                    // Preview Renderer Area
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Project detection banner
                        Surface(
                            color = Color(0xFF1E1A33),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bannerTitle = if (isViteProject) {
                                    "⚡ Vite project detected"
                                } else if (isStaticMode) {
                                    "📁 Static HTML Preview (index.html)"
                                } else {
                                    "🌐 Custom Server Preview"
                                }
                                
                                val bannerSubtitle = if (isViteProject) {
                                    "Active on: $previewUrl"
                                } else if (isStaticMode) {
                                    "Rendering index.html directly with auto-reload"
                                } else {
                                    "Listening on: $previewUrl"
                                }

                                Column {
                                    Text(text = bannerTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = bannerSubtitle, color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                consoleLogs.add("[System] Loaded page: $url")
                                            }
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                consoleMessage?.let {
                                                    val level = it.messageLevel().name
                                                    val log = "[$level] ${it.message()} (${it.sourceId()}:${it.lineNumber()})"
                                                    consoleLogs.add(log)
                                                }
                                                return true
                                            }
                                        }
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewInstance = this
                                        
                                        if (isStaticMode && staticHtmlContent != null) {
                                            loadDataWithBaseURL("", staticHtmlContent!!, "text/html", "UTF-8", null)
                                        } else {
                                            loadUrl(previewUrl)
                                        }
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

                1 -> {
                    // Console Logs Output console
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
                            Text("Captured Console Outputs", color = Color.White, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { consoleLogs.clear() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f))
                            ) {
                                Text("Clear Logs", color = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (consoleLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No console logs captured yet.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    items(consoleLogs) { log ->
                                        val color = if (log.contains("ERROR")) {
                                            Color(0xFFFF8888)
                                        } else if (log.contains("WARNING")) {
                                            Color(0xFFFFCC80)
                                        } else if (log.contains("System")) {
                                            ElectricTeal
                                        } else {
                                            Color.LightGray
                                        }
                                        Text(
                                            text = log,
                                            color = color,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Termux Bridge Instructions panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val termuxScript = """
                            cd /sdcard/Android/media/com.pocketcodeagent/
                            # OR cd into your specific Storage Access Framework workspace folder
                            npm install
                            npm run dev -- --host 127.0.0.1
                        """.trimIndent()

                        Text(
                            text = "📱 Local Dev Runtime Integration",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Android blocks executing Node.js runtimes inside standard applications. To preview React/Vite projects, you must run a dev server inside Termux (on the same device) and connect this WebView preview panel to it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A33)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Termux commands:", color = ElectricTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("termux_bridge", termuxScript)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Commands copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = ElectricTeal)
                                    }
                                }
                                Text(
                                    text = termuxScript,
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

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // Copy instructions
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("termux_bridge", termuxScript)
                                clipboard.setPrimaryClip(clip)
                                
                                // Attempt to launch Termux App via Intent
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        Toast.makeText(context, "Termux opened! Paste the commands there.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Termux app not found. Please install Termux from F-Droid or GitHub.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open Termux: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Termux Bridge 🚀", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog to set custom preview URLs
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Set Preview Server URL Link") },
            text = {
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    label = { Text("Server URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonPurple
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        previewUrl = customUrlInput
                        showUrlDialog = false
                        consoleLogs.add("[System] 🔗 Preview URL changed to: $previewUrl")
                        webViewInstance?.loadUrl(previewUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Apply", color = Color(0xFF0C0A14))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}
