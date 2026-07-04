package com.pocketcodeagent.ui.viewmodel

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchSource
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.repository.SessionRepository
import com.pocketcodeagent.domain.agent.AgentAction
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.security.EmergencyStopState
import com.pocketcodeagent.domain.security.OwnerSecurityManager
import com.pocketcodeagent.domain.security.SensitiveAction
import com.pocketcodeagent.domain.security.SensitiveActionGuard
import com.pocketcodeagent.ui.shell.AppTab
import kotlinx.coroutines.launch

enum class AgentStatus(val label: String) {
    IDLE("Idle"),
    PLANNING("Planning"),
    STREAMING("Streaming"),
    PARSING("Parsing"),
    WAITING_APPROVAL("Waiting"),
    APPLYING("Applying"),
    ERROR("Error"),
    DONE("Done"),
    CANCELLED("Cancelled")
}

class MainViewModel(
    private val sessionRepository: SessionRepository,
    val ownerSecurityManager: OwnerSecurityManager
) : ViewModel() {
    var currentScreen by mutableStateOf("welcome")
    var selectedWorkspaceUri by mutableStateOf<String?>(null)
    var selectedWorkspaceName by mutableStateOf<String?>(null)

    var selectedProvider by mutableStateOf<Provider?>(null)

    val isDemoMode: Boolean get() = selectedProvider?.id == 999

    fun persistSelectedProvider(provider: Provider) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.loadLastSession() ?: return@launch
                sessionRepository.updateSessionState(
                    sessionId = session.id,
                    providerId = provider.id,
                    model = provider.modelName
                )
            } catch (_: Exception) { }
        }
    }

    // Shell navigation
    var activeTab by mutableStateOf(AppTab.CHAT)
    var agentStatus by mutableStateOf(AgentStatus.IDLE)

    // Settings sheet
    var showSettingsSheet by mutableStateOf(false)
    var showOwnerSettings by mutableStateOf(false)

    // Editor State
    var selectedFileUri by mutableStateOf<String?>(null)
    var selectedFileName by mutableStateOf<String?>(null)
    var selectedFileRelativePath by mutableStateOf<String?>(null)

    // Diff Review State
    var pendingFileChanges by mutableStateOf<List<FilePatch>>(emptyList())
    var currentDiffFileIndex by mutableStateOf(0)

    // Agent action integration state
    var queuedCommandActions by mutableStateOf<List<AgentAction.RunCommand>>(emptyList())
    var activePreviewTarget by mutableStateOf<PreviewTarget>(PreviewTarget.None)

    // Workspace preview ready hint (set when index.html is created/modified via diff apply)
    var workspacePreviewReady by mutableStateOf(false)

    // Share intent for file sharing (triggered from composable via LaunchedEffect)
    var shareIntent by mutableStateOf<Intent?>(null)

    var sessionRestoreNotice by mutableStateOf<String?>(null)
    private var restoredSessionId = 0

    init {
        restoreLastSessionState()
    }

    fun restoreLastSessionState() {
        viewModelScope.launch {
            try {
                val session = sessionRepository.loadLastSession() ?: return@launch
                applyRestoredSession(session.id)
            } catch (_: Exception) {
                sessionRestoreNotice = "Session konnte nicht vollstaendig wiederhergestellt werden."
            }
        }
    }

    fun restoreSessionState(sessionId: Int) {
        if (sessionId <= 0 || sessionId == restoredSessionId) return
        viewModelScope.launch {
            try {
                applyRestoredSession(sessionId)
            } catch (_: Exception) {
                sessionRestoreNotice = "Session konnte nicht vollstaendig wiederhergestellt werden."
            }
        }
    }

    private suspend fun applyRestoredSession(sessionId: Int) {
        val session = sessionRepository.loadSession(sessionId) ?: return
        restoredSessionId = session.id
        selectedWorkspaceUri = session.workspaceUri
        selectedWorkspaceName = session.workspaceName
        activePreviewTarget = sessionRepository.deserializePreviewTarget(
            session.previewTargetType,
            session.previewTargetData
        )
        selectedFileUri = session.activeFileUri
        selectedFileName = session.activeFileName
        selectedFileRelativePath = session.activeFileName
        pendingFileChanges = sessionRepository.loadPendingPatches(session.id)
        currentDiffFileIndex = 0

        if (!session.workspaceUri.isNullOrBlank()) {
            currentScreen = "shell"
        }
        if (pendingFileChanges.isNotEmpty() ||
            activePreviewTarget !is PreviewTarget.None ||
            !selectedFileUri.isNullOrBlank()
        ) {
            sessionRestoreNotice = "Session wurde wiederhergestellt."
        }
    }

    fun clearSessionRestoreNotice() {
        sessionRestoreNotice = null
    }

    fun setPreviewTarget(target: PreviewTarget) {
        activePreviewTarget = target
        persistSessionPreviewTarget(target)
        openPreview()
    }

    fun setPreviewUrl(url: String) {
        setPreviewTarget(PreviewTarget.Url(url))
    }

    fun loadWorkspacePreview() {
        setPreviewTarget(PreviewTarget.WorkspaceStatic)
    }

    fun loadPreviewFile(path: String, fileName: String) {
        setPreviewTarget(PreviewTarget.File(path, fileName))
    }

    fun clearPreviewTarget() {
        activePreviewTarget = PreviewTarget.None
    }

    fun navigateTo(screen: String) {
        currentScreen = screen
    }

    fun selectWorkspace(uri: String, name: String) {
        selectedWorkspaceUri = uri
        selectedWorkspaceName = name
        viewModelScope.launch {
            try {
                val session = sessionRepository.loadLastSession() ?: return@launch
                sessionRepository.updateSessionState(
                    sessionId = session.id,
                    workspaceUri = uri,
                    workspaceName = name
                )
            } catch (_: Exception) { }
        }
    }

    fun openDiff(changes: List<FilePatch>) {
        pendingFileChanges = changes
        currentDiffFileIndex = 0
        activeTab = AppTab.DIFF
        changes.forEach(::persistPatch)
    }

    fun openDiff() {
        activeTab = AppTab.DIFF
    }

    fun openTerminal() {
        activeTab = AppTab.TERMINAL
    }

    fun openPreview() {
        activeTab = AppTab.PREVIEW
    }

    fun addAgentActions(actions: List<AgentAction>) {
        actions.forEach { action ->
            when (action) {
                is AgentAction.CreateFile,
                is AgentAction.ModifyFile,
                is AgentAction.DeleteFile -> addPendingFileAction(action)
                is AgentAction.RunCommand -> addCommandAction(action)
                is AgentAction.OpenPreview -> setPreviewUrl(action.target)
                is AgentAction.Note -> Unit
            }
        }
    }

    fun addPendingFileAction(action: AgentAction) {
        val patch = when (action) {
            is AgentAction.CreateFile -> FilePatch(
                path = action.path,
                action = FilePatchAction.CREATE,
                newText = action.content,
                additions = action.content.lines().size,
                deletions = 0
            )
            is AgentAction.ModifyFile -> FilePatch(
                path = action.path,
                action = FilePatchAction.MODIFY,
                oldText = action.oldText,
                newText = action.newText,
                additions = action.newText.lines().size,
                deletions = action.oldText?.lines()?.size,
                replaceWholeFile = action.oldText.isNullOrBlank()
            )
            is AgentAction.DeleteFile -> FilePatch(
                path = action.path,
                action = FilePatchAction.DELETE,
                source = FilePatchSource.AGENT,
                status = FilePatchStatus.PENDING,
                requiresSecondConfirmation = true,
                additions = 0
            )
            else -> return
        }

        pendingFileChanges = (pendingFileChanges.filterNot { it.path == patch.path } + patch)
        currentDiffFileIndex = 0
        persistPatch(patch)

        // Detect index.html creation/modification for preview ready hint
        if (patch.path.contains("index.html", ignoreCase = true)) {
            workspacePreviewReady = true
        }
    }

    fun addCommandAction(action: AgentAction.RunCommand) {
        if (queuedCommandActions.none { it.command == action.command }) {
            queuedCommandActions = queuedCommandActions + action
        }
    }

    fun addPendingPatch(patch: FilePatch) {
        pendingFileChanges = (pendingFileChanges.filterNot { it.id == patch.id || it.path == patch.path } + patch)
        currentDiffFileIndex = 0
        persistPatch(patch)
    }

    fun updatePatch(updatedPatch: FilePatch) {
        pendingFileChanges = pendingFileChanges.map { patch ->
            if (patch.id == updatedPatch.id) updatedPatch else patch
        }
        persistPatch(updatedPatch)
    }

    private fun persistPatch(patch: FilePatch) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.loadLastSession() ?: return@launch
                sessionRepository.savePendingPatch(patch, session.id)
            } catch (_: Exception) { }
        }
    }

    fun rejectPatch(patchId: String) {
        val updated = pendingFileChanges.map { patch ->
            if (patch.id == patchId) {
                patch.copy(status = FilePatchStatus.REJECTED)
            } else {
                patch
            }
        }
        pendingFileChanges = updated
        updated.firstOrNull { it.id == patchId }?.let(::persistPatch)
    }

    fun removeResolvedPatches() {
        pendingFileChanges = pendingFileChanges.filter {
            it.status == FilePatchStatus.PENDING || it.status == FilePatchStatus.CONFLICT || it.status == FilePatchStatus.FAILED
        }
    }

    fun confirmDeletePatch(patchId: String) {
        val updated = pendingFileChanges.map { patch ->
            if (patch.id == patchId && patch.action == FilePatchAction.DELETE) {
                patch.copy(deleteConfirmed = true)
            } else {
                patch
            }
        }
        pendingFileChanges = updated
        updated.firstOrNull { it.id == patchId }?.let(::persistPatch)
    }

    fun openFileInEditor(uri: String, name: String, relativePath: String? = null) {
        selectedFileUri = uri
        selectedFileName = name
        selectedFileRelativePath = relativePath ?: name
        activeTab = AppTab.CODE
        persistSessionActiveFile(uri, name)
    }

    private fun persistSessionPreviewTarget(target: PreviewTarget) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.loadLastSession() ?: return@launch
                sessionRepository.updateSessionState(
                    sessionId = session.id,
                    previewTarget = target
                )
            } catch (_: Exception) { }
        }
    }

    private fun persistSessionActiveFile(uri: String, name: String) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.loadLastSession() ?: return@launch
                sessionRepository.updateSessionState(
                    sessionId = session.id,
                    activeFileUri = uri,
                    activeFileName = name
                )
            } catch (_: Exception) { }
        }
    }
}
