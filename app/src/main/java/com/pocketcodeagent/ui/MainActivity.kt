package com.pocketcodeagent.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.local.AppDatabase
import com.pocketcodeagent.data.repository.AgentRepository
import com.pocketcodeagent.data.repository.ProviderRepository
import com.pocketcodeagent.data.repository.SessionRepository
import com.pocketcodeagent.data.repository.WorkspaceRepository
import com.pocketcodeagent.domain.language.LanguageMode
import com.pocketcodeagent.domain.security.OwnerSecurityManager
import com.pocketcodeagent.ui.screen.*
import com.pocketcodeagent.ui.shell.MainShellScreen
import com.pocketcodeagent.ui.theme.DeepSlateBackground
import com.pocketcodeagent.ui.theme.PocketCodeAgentTheme
import com.pocketcodeagent.ui.viewmodel.AgentViewModel
import com.pocketcodeagent.ui.viewmodel.MainViewModel
import com.pocketcodeagent.ui.viewmodel.ProviderViewModel
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var ownerSecurityManager: OwnerSecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyStoredLocale()

        // 1. Initialize local Room database
        val database = AppDatabase.getDatabase(applicationContext)

        // 2. Initialize security manager
        val ownerSecurityManager = OwnerSecurityManager(applicationContext)

        // 3. Initialize repositories
        val providerRepo = ProviderRepository(database)
        val workspaceRepo = WorkspaceRepository(applicationContext)
        val agentRepo = AgentRepository(providerRepo, workspaceRepo)
        val sessionRepo = SessionRepository(database)

        // 4. Create view models
        val mainViewModel = MainViewModel(sessionRepo, ownerSecurityManager)
        // Restore persisted language choice
        val prefs = getSharedPreferences("pca_prefs", MODE_PRIVATE)
        val savedLangTag = prefs.getString("language_mode", null)
        if (savedLangTag != null) {
            mainViewModel.languageMode = LanguageMode.entries.firstOrNull { it.localeTag == savedLangTag }
                ?: LanguageMode.System
        }
        val providerViewModel = ProviderViewModel(providerRepo)
        val workspaceViewModel = WorkspaceViewModel(workspaceRepo)
        val agentViewModel = AgentViewModel(agentRepo, providerRepo, sessionRepo, ownerSecurityManager)

        setContent {
            PocketCodeAgentTheme(themeMode = mainViewModel.themeMode) {
                // Folder picker launcher
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    uri?.let {
                        val doc = DocumentFile.fromTreeUri(applicationContext, it)
                        val name = doc?.name ?: "Selected Folder"
                        mainViewModel.selectWorkspace(it.toString(), name)
                        workspaceViewModel.initializeWorkspacePermission(it)
                        // Navigate to shell after workspace is selected
                        mainViewModel.navigateTo("shell")
                    }
                }

                when (mainViewModel.currentScreen) {
                    // ── Onboarding Flow ──────────────────────────────────────
                    "welcome" -> WelcomeScreen(
                        onGetStartedClick = { mainViewModel.navigateTo("providers") },
                        onStartDemoModeClick = {
                            val demoProvider = com.pocketcodeagent.data.model.Provider(
                                id = 999,
                                name = "Demo Mode",
                                baseUrl = "",
                                apiKey = "",
                                modelName = "demo-model"
                            )
                            mainViewModel.selectedProvider = demoProvider
                            mainViewModel.selectWorkspace("demo://workspace", "Demo Workspace")
                            mainViewModel.navigateTo("shell")
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
                        onProceedClick = { mainViewModel.navigateTo("shell") }
                    )

                    // ── Main Shell (Bolt-like Workbench) ──────────────────────
                    "shell" -> MainShellScreen(
                        mainViewModel = mainViewModel,
                        agentViewModel = agentViewModel,
                        workspaceViewModel = workspaceViewModel,
                        providerViewModel = providerViewModel,
                        onPickWorkspace = { folderPickerLauncher.launch(null) }
                    )

                    // ── Fallback: legacy screens still reachable if needed ──
                    else -> WelcomeScreen(
                        onGetStartedClick = { mainViewModel.navigateTo("providers") },
                        onStartDemoModeClick = { mainViewModel.navigateTo("shell") }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::ownerSecurityManager.isInitialized) {
            ownerSecurityManager.onActivityPaused()
        }
    }

    private fun applyStoredLocale() {
        val prefs = getSharedPreferences("pca_prefs", MODE_PRIVATE)
        val langTag = prefs.getString("language_mode", null) ?: return
        val locale = Locale.forLanguageTag(langTag)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    companion object {
        const val PREFS_NAME = "pca_prefs"
        const val KEY_LANGUAGE_MODE = "language_mode"

        fun saveLanguageMode(context: android.content.Context, mode: LanguageMode) {
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE_MODE, mode.localeTag)
                .apply()
        }
    }
}
