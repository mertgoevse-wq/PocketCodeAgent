package com.pocketcodeagent.ui.screen

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewScreen(
    viewModel: WorkspaceViewModel,
    workspaceUriString: String?,
    onBackClick: () -> Unit
) {
    var previewUrl by remember { mutableStateOf("http://127.0.0.1:5173") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var indexHtmlUriString by remember { mutableStateOf<String?>(null) }
    var staticHtmlContent by remember { mutableStateOf<String?>(null) }
    var isStaticMode by remember { mutableStateOf(false) }

    // Search for index.html in the workspace
    LaunchedEffect(workspaceUriString) {
        if (workspaceUriString != null) {
            val fileUri = viewModel.repository.getFileUriByRelativePath(workspaceUriString, "index.html")
            if (fileUri != null) {
                indexHtmlUriString = fileUri.toString()
                staticHtmlContent = viewModel.repository.readFile(fileUri.toString())
                isStaticMode = true
            } else {
                isStaticMode = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Preview Panel", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
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
            // Mode Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1A33))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Web, contentDescription = null, tint = ElectricTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isStaticMode) "Static HTML Preview (index.html)" else "Local Dev Server Preview",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (isStaticMode) "Rendering local index.html directly" else "Enter your Termux local server address below",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Address bar for non-static mode
            if (!isStaticMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = previewUrl,
                        onValueChange = { previewUrl = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPurple
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { webViewInstance?.loadUrl(previewUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("Go", color = Color.White)
                    }
                }
            }

            // WebView Render Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
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
                        if (isStaticMode && staticHtmlContent != null) {
                            webView.loadDataWithBaseURL("", staticHtmlContent!!, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
