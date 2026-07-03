package com.pocketcodeagent.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.ProviderPresets
import com.pocketcodeagent.data.model.ProviderTestStatus
import com.pocketcodeagent.domain.agent.AgentRunState
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.ui.chat.ChatPanel
import com.pocketcodeagent.ui.screen.ProviderSetupScreen
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.AgentStatus
import com.pocketcodeagent.ui.viewmodel.AgentViewModel
import com.pocketcodeagent.ui.viewmodel.MainViewModel
import com.pocketcodeagent.ui.viewmodel.ProviderViewModel
import com.pocketcodeagent.ui.viewmodel.WorkspaceViewModel
import com.pocketcodeagent.ui.workbench.*

// ─── Status badge colors ─────────────────────────────────────────────────────
private val StatusColors = mapOf(
    AgentStatus.IDLE             to Color(0xFF3A3A44),
    AgentStatus.PLANNING         to Color(0xFF5E72E4),
    AgentStatus.STREAMING        to Color(0xFF2DCE89),
    AgentStatus.PARSING          to Color(0xFF11CDEF),
    AgentStatus.WAITING_APPROVAL to Color(0xFFF0AD4E),
    AgentStatus.APPLYING         to Color(0xFF11CDEF),
    AgentStatus.ERROR            to Color(0xFFF5365C),
    AgentStatus.DONE             to Color(0xFF2DCE89),
    AgentStatus.CANCELLED        to Color(0xFFF0AD4E)
)

// ─── Main Shell ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    mainViewModel: MainViewModel,
    agentViewModel: AgentViewModel,
    workspaceViewModel: WorkspaceViewModel,
    providerViewModel: ProviderViewModel,
    onPickWorkspace: () -> Unit
) {
    val providers by providerViewModel.providers.collectAsState()

    LaunchedEffect(providers) {
        val selected = mainViewModel.selectedProvider
        if (selected != null) {
            providers.firstOrNull { it.id == selected.id }?.let { latest ->
                mainViewModel.selectedProvider = latest
            }
        } else if (providers.size == 1) {
            mainViewModel.selectedProvider = providers.first()
        }
    }

    // Keep agent status in sync with agentViewModel
    val openPatchCount = mainViewModel.pendingFileChanges.count {
        it.status == FilePatchStatus.PENDING || it.status == FilePatchStatus.CONFLICT || it.status == FilePatchStatus.FAILED
    }

    LaunchedEffect(agentViewModel.runState, agentViewModel.isExecuting, workspaceViewModel.isApplyingPatch, openPatchCount) {
        mainViewModel.agentStatus = when {
            workspaceViewModel.isApplyingPatch -> AgentStatus.APPLYING
            agentViewModel.runState is AgentRunState.Planning -> AgentStatus.PLANNING
            agentViewModel.runState is AgentRunState.Streaming -> AgentStatus.STREAMING
            agentViewModel.runState is AgentRunState.ParsingActions -> AgentStatus.PARSING
            agentViewModel.runState is AgentRunState.WaitingForApproval -> AgentStatus.WAITING_APPROVAL
            agentViewModel.runState is AgentRunState.Applying -> AgentStatus.APPLYING
            agentViewModel.runState is AgentRunState.Error -> AgentStatus.ERROR
            openPatchCount > 0 -> AgentStatus.WAITING_APPROVAL
            agentViewModel.runState is AgentRunState.Done -> AgentStatus.DONE
            agentViewModel.runState is AgentRunState.Cancelled -> AgentStatus.CANCELLED
            else -> AgentStatus.IDLE
        }
    }

    Scaffold(
        containerColor = Color(0xFF0E0E10),
        topBar = {
            Column {
                ShellTopBar(
                    workspaceName = mainViewModel.selectedWorkspaceName,
                    agentStatus = mainViewModel.agentStatus,
                    onSettingsClick = { mainViewModel.showSettingsSheet = true }
                )
                ProviderModelBar(
                    providers = providers,
                    provider = mainViewModel.selectedProvider,
                    isExecuting = agentViewModel.isExecuting,
                    isTesting = providerViewModel.isTesting &&
                        providerViewModel.testingProviderId == mainViewModel.selectedProvider?.id,
                    onProviderSelected = { mainViewModel.selectedProvider = it },
                    onModelSelected = { provider, model ->
                        providerViewModel.updateProviderModel(provider, model) { updated ->
                            mainViewModel.selectedProvider = updated
                        }
                    },
                    onTestClick = {
                        mainViewModel.selectedProvider?.let { provider ->
                            providerViewModel.testSavedProvider(provider) { updated ->
                                mainViewModel.selectedProvider = updated
                            }
                        }
                    },
                    onSettingsClick = { mainViewModel.showSettingsSheet = true }
                )
                WorkspaceStatusBar(
                    workspaceName = mainViewModel.selectedWorkspaceName,
                    fileCount = workspaceViewModel.files.size,
                    isLoading = workspaceViewModel.isLoadingFiles,
                    onPickWorkspace = onPickWorkspace,
                    onReload = {
                        mainViewModel.selectedWorkspaceUri?.let { workspaceViewModel.loadWorkspace(it) }
                    }
                )
                HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            ShellBottomNav(
                activeTab = mainViewModel.activeTab,
                onTabSelected = { mainViewModel.activeTab = it },
                diffBadge = openPatchCount
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (mainViewModel.activeTab) {
                AppTab.CHAT -> ChatPanel(
                    viewModel = agentViewModel,
                    provider = mainViewModel.selectedProvider,
                    workspaceUriString = mainViewModel.selectedWorkspaceUri,
                    onReviewDiff = { patches ->
                        mainViewModel.openDiff(patches)
                    },
                    onAddFileAction = { action ->
                        mainViewModel.addPendingFileAction(action)
                        mainViewModel.openDiff()
                    },
                    onQueueCommand = { action ->
                        if (action.riskLevel != CommandRiskLevel.BLOCKED) {
                            mainViewModel.addCommandAction(action)
                            agentViewModel.queueTerminalCommand(action.command)
                            mainViewModel.openTerminal()
                        }
                    },
                    onOpenPreview = { action ->
                        mainViewModel.setPreviewUrl(action.target)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.FILES -> FileTreePanel(
                    viewModel = workspaceViewModel,
                    workspaceUriString = mainViewModel.selectedWorkspaceUri,
                    onFileClick = { uri, name ->
                        mainViewModel.openFileInEditor(uri, name)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.CODE -> CodeEditorPanel(
                    viewModel = workspaceViewModel,
                    fileUri = mainViewModel.selectedFileUri,
                    fileName = mainViewModel.selectedFileName,
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.DIFF -> DiffPanel(
                    viewModel = workspaceViewModel,
                    patches = mainViewModel.pendingFileChanges,
                    onApplyChange = { patch ->
                        val root = mainViewModel.selectedWorkspaceUri ?: return@DiffPanel
                        workspaceViewModel.applyWorkspacePatch(root, patch) { result ->
                            mainViewModel.updatePatch(
                                patch.copy(
                                    status = result.status,
                                    errorMessage = if (result.success) null else result.message
                                )
                            )
                            mainViewModel.agentStatus = if (result.success) AgentStatus.DONE else AgentStatus.ERROR
                            mainViewModel.selectedFileUri?.let { workspaceViewModel.loadFileContent(it) }
                        }
                    },
                    onRejectChange = { patch ->
                        mainViewModel.updatePatch(patch.copy(status = FilePatchStatus.REJECTED))
                    },
                    onApplyAll = {
                        val root = mainViewModel.selectedWorkspaceUri ?: return@DiffPanel
                        val safePatches = mainViewModel.pendingFileChanges.filter { patch ->
                            patch.status == FilePatchStatus.PENDING &&
                                (patch.action != FilePatchAction.DELETE || patch.deleteConfirmed)
                        }
                        if (safePatches.isEmpty()) return@DiffPanel
                        workspaceViewModel.applyWorkspacePatches(root, safePatches) { results ->
                            results.forEach { result ->
                                mainViewModel.pendingFileChanges.firstOrNull { it.id == result.patchId }?.let { patch ->
                                    mainViewModel.updatePatch(
                                        patch.copy(
                                            status = result.status,
                                            errorMessage = if (result.success) null else result.message
                                        )
                                    )
                                }
                            }
                            mainViewModel.agentStatus = if (results.all { it.success }) AgentStatus.DONE else AgentStatus.ERROR
                            mainViewModel.selectedFileUri?.let { workspaceViewModel.loadFileContent(it) }
                        }
                    },
                    onRejectAll = {
                        mainViewModel.pendingFileChanges = mainViewModel.pendingFileChanges.map {
                            if (it.status == FilePatchStatus.APPLIED) it else it.copy(status = FilePatchStatus.REJECTED)
                        }
                    },
                    onUndoLastApply = {
                        val root = mainViewModel.selectedWorkspaceUri ?: return@DiffPanel
                        workspaceViewModel.undoLastApply(root) { result ->
                            mainViewModel.agentStatus = if (result.success) AgentStatus.DONE else AgentStatus.ERROR
                            mainViewModel.selectedFileUri?.let { workspaceViewModel.loadFileContent(it) }
                        }
                    },
                    onConfirmDelete = { patch ->
                        mainViewModel.confirmDeletePatch(patch.id)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.PREVIEW -> PreviewPanel(
                    viewModel = workspaceViewModel,
                    workspaceUriString = mainViewModel.selectedWorkspaceUri,
                    previewTarget = mainViewModel.activePreviewTarget,
                    onTargetChanged = { mainViewModel.setPreviewTarget(it) },
                    repository = workspaceViewModel.repository,
                    workspacePreviewReady = mainViewModel.workspacePreviewReady,
                    onPreviewReadyConsumed = { mainViewModel.workspacePreviewReady = false },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.TERMINAL -> TerminalPanel(
                    viewModel = agentViewModel,
                    onSetPreviewUrl = { url ->
                        mainViewModel.setPreviewUrl(url)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Settings bottom sheet
    if (mainViewModel.showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { mainViewModel.showSettingsSheet = false },
            containerColor = Color(0xFF13131A),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ProviderSetupScreen(
                viewModel = providerViewModel,
                activeProvider = mainViewModel.selectedProvider,
                onSelectProvider = { mainViewModel.selectedProvider = it },
                onBackClick = { mainViewModel.showSettingsSheet = false },
                onNextClick = { mainViewModel.showSettingsSheet = false }
            )
        }
    }
}

// ─── Shell Top Bar ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopBar(
    workspaceName: String?,
    agentStatus: AgentStatus,
    onSettingsClick: () -> Unit
) {
    val statusColor = StatusColors[agentStatus] ?: Color(0xFF3A3A44)

    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PocketCodeAgent",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    // Status badge
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            agentStatus.label,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    workspaceName ?: "No workspace",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, null, tint = TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0E0E10))
    )
}

// ─── Provider Model Bar ───────────────────────────────────────────────────────
@Composable
private fun ProviderModelBar(
    providers: List<Provider>,
    provider: Provider?,
    isExecuting: Boolean,
    isTesting: Boolean,
    onProviderSelected: (Provider) -> Unit,
    onModelSelected: (Provider, String) -> Unit,
    onTestClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val modelOptions = provider?.let {
        ProviderPresets.modelOptionsFor(it.providerType, it.modelName)
    }.orEmpty()
    val status = when {
        isTesting -> ProviderTestStatus.TESTING
        provider == null || !provider.hasRequiredConfiguration() -> ProviderTestStatus.NOT_CONFIGURED
        provider.lastTestStatus == ProviderTestStatus.READY -> ProviderTestStatus.READY
        provider.lastTestStatus == ProviderTestStatus.ERROR -> ProviderTestStatus.ERROR
        else -> ProviderTestStatus.NOT_CONFIGURED
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131A))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Surface(
                color = Color(0xFF1E1E26),
                shape = RoundedCornerShape(6.dp),
                onClick = { providerMenuExpanded = true },
                modifier = Modifier.widthIn(min = 96.dp, max = 132.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Api, null, tint = SlateBlue, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = provider?.name ?: "Provider",
                        color = if (provider != null) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                }
            }
            DropdownMenu(
                expanded = providerMenuExpanded,
                onDismissRequest = { providerMenuExpanded = false },
                containerColor = Color(0xFF1E1E26)
            ) {
                if (providers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Provider hinzufuegen", color = TextSecondary, fontSize = 12.sp) },
                        onClick = {
                            providerMenuExpanded = false
                            onSettingsClick()
                        }
                    )
                } else {
                    providers.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                onProviderSelected(item)
                                providerMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Surface(
                color = Color(0xFF1E1E26),
                shape = RoundedCornerShape(6.dp),
                onClick = { if (provider != null) modelMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Psychology, null, tint = CalmSage, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = provider?.modelName ?: "Modell",
                        color = if (provider != null) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                }
            }
            DropdownMenu(
                expanded = modelMenuExpanded,
                onDismissRequest = { modelMenuExpanded = false },
                containerColor = Color(0xFF1E1E26),
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                provider?.let { activeProvider ->
                    modelOptions.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                onModelSelected(activeProvider, model)
                                modelMenuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Settings / API-Key", color = SlateBlue, fontSize = 12.sp) },
                        onClick = {
                            modelMenuExpanded = false
                            onSettingsClick()
                        }
                    )
                }
            }
        }

        ProviderStatusPill(status)

        IconButton(
            onClick = onTestClick,
            enabled = !isExecuting && !isTesting && provider?.hasRequiredConfiguration() == true,
            modifier = Modifier.size(30.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SlateBlue, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = "Test provider", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        IconButton(onClick = onSettingsClick, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "Provider settings", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ProviderStatusPill(status: ProviderTestStatus) {
    val color = when (status) {
        ProviderTestStatus.READY -> CalmSage
        ProviderTestStatus.TESTING -> SlateBlue
        ProviderTestStatus.ERROR -> WarmCopper
        ProviderTestStatus.NOT_CONFIGURED -> Color(0xFF777783)
    }
    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(4.dp))
            Text(
                status.label,
                color = color,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

// ─── Workspace Status Bar ─────────────────────────────────────────────────────
@Composable
private fun WorkspaceStatusBar(
    workspaceName: String?,
    fileCount: Int,
    isLoading: Boolean,
    onPickWorkspace: () -> Unit,
    onReload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111116))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = onPickWorkspace, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.FolderOpen, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
        Text(
            text = workspaceName ?: "Workspace öffnen …",
            color = if (workspaceName != null) TextSecondary else Color(0xFF444450),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = CalmSage,
                strokeWidth = 1.5.dp
            )
        } else if (fileCount > 0) {
            Text("$fileCount", color = Color(0xFF444450), fontSize = 10.sp)
        }
        IconButton(onClick = onReload, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Refresh, null, tint = Color(0xFF444450), modifier = Modifier.size(14.dp))
        }
    }
}

// ─── Bottom Navigation ─────────────────────────────────────────────────────────
@Composable
private fun ShellBottomNav(
    activeTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    diffBadge: Int
) {
    NavigationBar(
        containerColor = Color(0xFF0E0E10),
        contentColor = SlateBlue,
        tonalElevation = 0.dp
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == AppTab.DIFF && diffBadge > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = WarmCopper) {
                                Text("$diffBadge", fontSize = 9.sp, color = Color.White)
                            }
                        }) {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SlateBlue,
                    selectedTextColor = SlateBlue,
                    unselectedIconColor = Color(0xFF444450),
                    unselectedTextColor = Color(0xFF444450),
                    indicatorColor = Color(0xFF1A1A26)
                )
            )
        }
    }
}
