package com.pocketcodeagent.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.local.AppDatabase
import com.pocketcodeagent.data.repository.AgentRepository
import com.pocketcodeagent.data.repository.ProviderRepository
import com.pocketcodeagent.data.repository.WorkspaceRepository
import com.pocketcodeagent.ui.screen.*
import com.pocketcodeagent.ui.theme.PocketCodeAgentTheme
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

                // Render screens based on current state
                when (mainViewModel.currentScreen) {
                    "welcome" -> WelcomeScreen(
                        onGetStartedClick = { mainViewModel.navigateTo("providers") }
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
                        onFileSelected = { fileUri, fileName ->
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
