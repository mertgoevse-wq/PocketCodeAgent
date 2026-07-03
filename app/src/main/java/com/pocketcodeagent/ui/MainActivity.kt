package com.pocketcodeagent.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.local.AppDatabase
import com.pocketcodeagent.data.repository.AgentRepository
import com.pocketcodeagent.data.repository.ProviderRepository
import com.pocketcodeagent.data.repository.WorkspaceRepository
import com.pocketcodeagent.ui.screen.*
import com.pocketcodeagent.ui.theme.DeepSlateBackground
import com.pocketcodeagent.ui.theme.PocketCodeAgentTheme
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.viewmodel.AgentViewModel
import com.pocketcodeagent.ui.viewmodel.MainViewModel
import com.pocketcodeagent.ui.viewmodel.ProviderViewModel
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize local Room database
        val database = AppDatabase.getDatabase(applicationContext)

        // 2. Initialize repositories
        val providerRepo = ProviderRepository(database)
        val workspaceRepo = WorkspaceRepository(applicationContext)
        val agentRepo = AgentRepository(providerRepo, workspaceRepo)

        // 3. Create view models
        val mainViewModel = MainViewModel()
        val providerViewModel = ProviderViewModel(providerRepo)
        val workspaceViewModel = WorkspaceViewModel(workspaceRepo)
        val agentViewModel = AgentViewModel(agentRepo, providerRepo)

        setContent {
            PocketCodeAgentTheme {
                // Folder picker launcher
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    uri?.let {
                        val doc = DocumentFile.fromTreeUri(applicationContext, it)
                        val name = doc?.name ?: "Selected Folder"
                        mainViewModel.selectWorkspace(it.toString(), name)
                        workspaceViewModel.initializeWorkspacePermission(it)
                        mainViewModel.navigateTo("dashboard")
                    }
                }

                val tabScreens = listOf("dashboard", "chat", "explorer", "preview", "terminal", "settings")
                val isTabScreen = mainViewModel.currentScreen in tabScreens

                Scaffold(
                    bottomBar = {
                        if (isTabScreen) {
                            NavigationBar(
                                containerColor = Color(0xFF0F0C1B),
                                contentColor = SlateBlue
                            ) {
                                NavigationBarItem(
                                    selected = mainViewModel.currentScreen == "dashboard",
                                    onClick = { mainViewModel.navigateTo("dashboard") },
                                    label = { Text("Dashboard", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") }
                                )
                                NavigationBarItem(
                                    selected = mainViewModel.currentScreen == "chat",
                                    onClick = { mainViewModel.navigateTo("chat") },
                                    label = { Text("Agent", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Agent") }
                                )
                                NavigationBarItem(
                                    selected = mainViewModel.currentScreen == "explorer",
                                    onClick = { mainViewModel.navigateTo("explorer") },
                                    label = { Text("Files", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Files") }
                                )
                                NavigationBarItem(
                                    selected = mainViewModel.currentScreen == "preview",
                                    onClick = { mainViewModel.navigateTo("preview") },
                                    label = { Text("Preview", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Web, contentDescription = "Preview") }
                                )
                                NavigationBarItem(
                                    selected = mainViewModel.currentScreen == "terminal",
                                    onClick = { mainViewModel.navigateTo("terminal") },
                                    label = { Text("Terminal", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") }
                                )
                                NavigationBarItem(
                                    selected = mainViewModel.currentScreen == "settings",
                                    onClick = { mainViewModel.navigateTo("settings") },
                                    label = { Text("Settings", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                                )
                            }
                        }
                    },
                    containerColor = DeepSlateBackground
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (mainViewModel.currentScreen) {
                            "welcome" -> WelcomeScreen(
                                onGetStartedClick = { mainViewModel.navigateTo("providers") },
                                onStartDemoModeClick = {
                                    val demoProvider = com.pocketcodeagent.data.model.Provider(
                                        id = 999,
                                        name = "Demo Provider (Simuliert)",
                                        baseUrl = "http://localhost",
                                        apiKey = "demo",
                                        modelName = "demo-model"
                                    )
                                    mainViewModel.selectedProvider = demoProvider
                                    mainViewModel.selectWorkspace("demo://workspace", "Demo Workspace")
                                    workspaceViewModel.repository.workspace.setRootUri("demo://workspace")
                                    mainViewModel.navigateTo("dashboard")
                                }
                            )

                            "providers" -> ProviderSetupScreen(
                                viewModel = providerViewModel,
                                activeProvider = mainViewModel.selectedProvider,
                                onSelectProvider = { mainViewModel.selectedProvider = it },
                                onBackClick = { mainViewModel.navigateTo("welcome") },
                                onNextClick = { mainViewModel.navigateTo("workspace") }
                            )

                            "workspace" -> WorkspacePickerScreen(
                                selectedWorkspaceName = mainViewModel.selectedWorkspaceName,
                                onPickWorkspaceClick = { folderPickerLauncher.launch(null) },
                                onBackClick = { mainViewModel.navigateTo("providers") },
                                onProceedClick = { mainViewModel.navigateTo("dashboard") }
                            )

                            "dashboard" -> ProjectDashboardScreen(
                                workspaceName = mainViewModel.selectedWorkspaceName,
                                providerName = mainViewModel.selectedProvider?.name,
                                modelName = mainViewModel.selectedProvider?.modelName,
                                onNavigate = { route -> mainViewModel.navigateTo(route) }
                            )

                            "chat" -> ChatAgentScreen(
                                viewModel = agentViewModel,
                                provider = mainViewModel.selectedProvider,
                                workspaceUriString = mainViewModel.selectedWorkspaceUri,
                                onReviewDiff = { changes ->
                                    mainViewModel.pendingFileChanges = changes
                                    mainViewModel.currentDiffFileIndex = 0
                                    mainViewModel.navigateTo("diff")
                                },
                                onBackClick = { mainViewModel.navigateTo("dashboard") }
                            )

                            "explorer" -> FileExplorerScreen(
                                viewModel = workspaceViewModel,
                                workspaceUriString = mainViewModel.selectedWorkspaceUri,
                                onFileClick = { fileUri, fileName ->
                                    mainViewModel.selectedFileUri = fileUri
                                    mainViewModel.selectedFileName = fileName
                                    mainViewModel.navigateTo("editor")
                                },
                                onBackClick = { mainViewModel.navigateTo("dashboard") }
                            )

                            "editor" -> CodeEditorScreen(
                                viewModel = workspaceViewModel,
                                fileUriString = mainViewModel.selectedFileUri,
                                fileName = mainViewModel.selectedFileName,
                                onBackClick = { mainViewModel.navigateTo("explorer") }
                            )

                            "diff" -> DiffReviewScreen(
                                viewModel = workspaceViewModel,
                                proposedPatches = mainViewModel.pendingFileChanges,
                                currentIndex = mainViewModel.currentDiffFileIndex,
                                rootWorkspaceUriString = mainViewModel.selectedWorkspaceUri,
                                onApplyChange = { appliedChange ->
                                    val updated = mainViewModel.pendingFileChanges.filter { it.path != appliedChange.path }
                                    mainViewModel.pendingFileChanges = updated
                                    if (updated.isNotEmpty()) {
                                        mainViewModel.currentDiffFileIndex = 0
                                    } else {
                                        mainViewModel.navigateTo("chat")
                                    }
                                },
                                onRejectChange = {
                                    val updated = mainViewModel.pendingFileChanges.filterIndexed { idx, _ -> idx != mainViewModel.currentDiffFileIndex }
                                    mainViewModel.pendingFileChanges = updated
                                    if (mainViewModel.currentDiffFileIndex >= updated.size) {
                                        mainViewModel.currentDiffFileIndex = maxOf(0, updated.size - 1)
                                    }
                                    if (updated.isEmpty()) {
                                        mainViewModel.navigateTo("chat")
                                    }
                                },
                                onApplyAll = {
                                    mainViewModel.pendingFileChanges = emptyList()
                                    mainViewModel.navigateTo("chat")
                                },
                                onBackClick = { mainViewModel.navigateTo("chat") }
                            )

                            "terminal" -> TerminalScreen(
                                viewModel = agentViewModel,
                                onBackClick = { mainViewModel.navigateTo("dashboard") }
                            )

                            "preview" -> LivePreviewScreen(
                                viewModel = workspaceViewModel,
                                workspaceUriString = mainViewModel.selectedWorkspaceUri,
                                onBackClick = { mainViewModel.navigateTo("dashboard") }
                            )

                            "settings" -> SettingsScreen(
                                onResetWorkspace = {
                                    mainViewModel.selectedWorkspaceUri = null
                                    mainViewModel.selectedWorkspaceName = null
                                    mainViewModel.navigateTo("workspace")
                                },
                                onClearLogs = {
                                    agentViewModel.clearLogs()
                                },
                                onBackClick = { mainViewModel.navigateTo("dashboard") }
                            )

                            "logs" -> LogsScreen(
                                viewModel = agentViewModel,
                                onBackClick = { mainViewModel.navigateTo("dashboard") }
                            )
                        }
                    }
                }
            }
        }
    }
}
