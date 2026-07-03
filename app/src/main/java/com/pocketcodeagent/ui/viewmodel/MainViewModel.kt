package com.pocketcodeagent.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchSource
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.domain.agent.AgentAction
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.ui.shell.AppTab

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

class MainViewModel : ViewModel() {
    var currentScreen by mutableStateOf("welcome")
    var selectedWorkspaceUri by mutableStateOf<String?>(null)
    var selectedWorkspaceName by mutableStateOf<String?>(null)

    var selectedProvider by mutableStateOf<Provider?>(null)

    // Shell navigation
    var activeTab by mutableStateOf(AppTab.CHAT)
    var agentStatus by mutableStateOf(AgentStatus.IDLE)

    // Settings sheet
    var showSettingsSheet by mutableStateOf(false)

    // Editor State
    var selectedFileUri by mutableStateOf<String?>(null)
    var selectedFileName by mutableStateOf<String?>(null)

    // Diff Review State
    var pendingFileChanges by mutableStateOf<List<FilePatch>>(emptyList())
    var currentDiffFileIndex by mutableStateOf(0)

    // Agent action integration state
    var queuedCommandActions by mutableStateOf<List<AgentAction.RunCommand>>(emptyList())
    var activePreviewTarget by mutableStateOf<PreviewTarget>(PreviewTarget.None)

    // Workspace preview ready hint (set when index.html is created/modified via diff apply)
    var workspacePreviewReady by mutableStateOf(false)

    fun setPreviewTarget(target: PreviewTarget) {
        activePreviewTarget = target
        openPreview()
    }

    fun setPreviewUrl(url: String) {
        activePreviewTarget = PreviewTarget.Url(url)
        openPreview()
    }

    fun loadWorkspacePreview() {
        activePreviewTarget = PreviewTarget.WorkspaceStatic
        openPreview()
    }

    fun loadPreviewFile(path: String, fileName: String) {
        activePreviewTarget = PreviewTarget.File(path, fileName)
        openPreview()
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
    }

    fun openDiff(changes: List<FilePatch>) {
        pendingFileChanges = changes
        currentDiffFileIndex = 0
        activeTab = AppTab.DIFF
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
    }

    fun updatePatch(updatedPatch: FilePatch) {
        pendingFileChanges = pendingFileChanges.map { patch ->
            if (patch.id == updatedPatch.id) updatedPatch else patch
        }
    }

    fun rejectPatch(patchId: String) {
        pendingFileChanges = pendingFileChanges.map { patch ->
            if (patch.id == patchId) {
                patch.copy(status = FilePatchStatus.REJECTED)
            } else {
                patch
            }
        }
    }

    fun removeResolvedPatches() {
        pendingFileChanges = pendingFileChanges.filter {
            it.status == FilePatchStatus.PENDING || it.status == FilePatchStatus.CONFLICT || it.status == FilePatchStatus.FAILED
        }
    }

    fun confirmDeletePatch(patchId: String) {
        pendingFileChanges = pendingFileChanges.map { patch ->
            if (patch.id == patchId && patch.action == FilePatchAction.DELETE) {
                patch.copy(deleteConfirmed = true)
            } else {
                patch
            }
        }
    }

    fun openFileInEditor(uri: String, name: String) {
        selectedFileUri = uri
        selectedFileName = name
        activeTab = AppTab.CODE
    }
}
