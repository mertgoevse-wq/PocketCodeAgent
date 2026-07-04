package com.pocketcodeagent.ui.viewmodel

import android.net.Uri
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.data.repository.DiffLine
import com.pocketcodeagent.data.repository.WorkspaceRepository
import com.pocketcodeagent.domain.export.WorkspaceExporter
import com.pocketcodeagent.domain.workspace.PatchApplyResult
import com.pocketcodeagent.domain.workspace.UndoResult
import com.pocketcodeagent.domain.workspace.WorkspacePatchApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceViewModel(val repository: WorkspaceRepository) : ViewModel() {
    private val patchApplier = WorkspacePatchApplier(repository.workspace)

    var files by mutableStateOf<List<WorkspaceFile>>(emptyList())
    var isLoadingFiles by mutableStateOf(false)
    var isApplyingPatch by mutableStateOf(false)
    var isCheckingConflicts by mutableStateOf(false)
    var isExportingWorkspace by mutableStateOf(false)
    var exportProgress by mutableStateOf<WorkspaceExporter.ExportProgress?>(null)
    var exportStatusMessage by mutableStateOf<String?>(null)

    // Current open file editing
    var openFileContent by mutableStateOf("")
    var originalFileContent by mutableStateOf("")
    var editorContent by mutableStateOf("")
    var openFileUri by mutableStateOf<String?>(null)
    var openFileName by mutableStateOf<String?>(null)
    var openFileRelativePath by mutableStateOf<String?>(null)
    var isOpenFileLoading by mutableStateOf(false)
    var isOpenFileSaving by mutableStateOf(false)
    var openFileSizeBytes by mutableStateOf(0L)
    val isModified: Boolean get() = editorContent != originalFileContent
    val isLargeFile: Boolean get() = openFileSizeBytes > 200_000L
    val isHugeFile: Boolean get() = openFileSizeBytes > 500_000L
    val hasConflict: Boolean get() = openFileContent != originalFileContent && openFileContent.isNotEmpty()

    // Diff view lines
    var activeDiffLines by mutableStateOf<List<DiffLine>>(emptyList())

    // History of applied patches (to support Undo!)
    val appliedPatchesHistory = mutableStateListOf<FilePatch>()

    // Current operation logs/status for UI
    var workspaceError by mutableStateOf<String?>(null)

    // Timestamp of the last file write to trigger Live Preview auto-reload
    var lastFileWriteTimestamp by mutableStateOf(0L)

    fun initializeWorkspacePermission(uri: Uri) {
        repository.persistWorkspacePermission(uri)
    }

    fun loadWorkspace(uriString: String) {
        viewModelScope.launch {
            isLoadingFiles = true
            val uri = Uri.parse(uriString)
            val result = withContext(Dispatchers.IO) {
                repository.loadWorkspaceFiles(uri)
            }
            files = result
            isLoadingFiles = false
        }
    }

    fun loadFileContent(uriString: String, relativePath: String? = null, fileName: String? = null) {
        viewModelScope.launch {
            isOpenFileLoading = true
            openFileUri = uriString
            openFileRelativePath = relativePath
            openFileName = fileName ?: uriString.substringAfterLast("/")
            val content = withContext(Dispatchers.IO) {
                repository.readFile(uriString)
            }
            openFileContent = content
            originalFileContent = content
            editorContent = content
            openFileSizeBytes = content.encodeToByteArray().size.toLong()
            isOpenFileLoading = false
        }
    }

    fun checkForConflicts(onComplete: (Boolean) -> Unit) {
        val uri = openFileUri
        if (uri == null) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            isCheckingConflicts = true
            val diskContent = withContext(Dispatchers.IO) {
                try {
                    repository.readFile(uri)
                } catch (_: Exception) {
                    ""
                }
            }
            isCheckingConflicts = false
            if (diskContent.isNotEmpty() && diskContent != openFileContent) {
                openFileContent = diskContent
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun reloadFromDisk() {
        val uri = openFileUri ?: return
        viewModelScope.launch {
            isOpenFileLoading = true
            val content = withContext(Dispatchers.IO) {
                repository.readFile(uri)
            }
            openFileContent = content
            originalFileContent = content
            editorContent = content
            openFileSizeBytes = content.encodeToByteArray().size.toLong()
            isOpenFileLoading = false
        }
    }

    fun updateEditorContent(newContent: String) {
        editorContent = newContent
    }

    fun revertEditorContent() {
        editorContent = originalFileContent
    }

    fun saveFileContent(uriString: String, content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isOpenFileSaving = true
            val success = withContext(Dispatchers.IO) {
                repository.writeFile(uriString, content)
            }
            isOpenFileSaving = false
            if (success) {
                originalFileContent = content
                openFileContent = content
                openFileSizeBytes = content.encodeToByteArray().size.toLong()
                lastFileWriteTimestamp = System.currentTimeMillis()
                onSuccess()
            }
        }
    }

    fun prepareDiff(rootUriString: String, patch: FilePatch) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            
            val original = when (patch.action) {
                FilePatchAction.CREATE -> ""
                FilePatchAction.MODIFY -> patch.oldText ?: withContext(Dispatchers.IO) {
                    repository.workspace.readFile(patch.path) ?: ""
                }
                FilePatchAction.DELETE -> withContext(Dispatchers.IO) {
                    repository.workspace.readFile(patch.path) ?: patch.oldText.orEmpty()
                }
            }
            
            val modified = when (patch.action) {
                FilePatchAction.DELETE -> ""
                else -> patch.newText.orEmpty()
            }
            
            val diff = withContext(Dispatchers.IO) {
                com.pocketcodeagent.data.util.DiffGenerator.computeDiff(original, modified)
            }
            activeDiffLines = diff
        }
    }

    fun applyPatch(rootUriString: String, patch: FilePatch, onComplete: (Boolean) -> Unit) {
        applyWorkspacePatch(rootUriString, patch) { result ->
            onComplete(result.success)
        }
    }

    fun applyWorkspacePatch(rootUriString: String, patch: FilePatch, onComplete: (PatchApplyResult) -> Unit) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            isApplyingPatch = true
            
            val result = withContext(Dispatchers.IO) {
                patchApplier.applyPatch(patch)
            }
            isApplyingPatch = false

            if (result.success) {
                appliedPatchesHistory.add(patch)
                lastFileWriteTimestamp = System.currentTimeMillis()
                loadWorkspace(rootUriString)
            } else {
                workspaceError = result.message
            }
            onComplete(result)
        }
    }

    fun applyAllPatches(rootUriString: String, patches: List<FilePatch>, onComplete: (Boolean) -> Unit) {
        applyWorkspacePatches(rootUriString, patches) { results ->
            onComplete(results.all { it.success })
        }
    }

    fun applyWorkspacePatches(rootUriString: String, patches: List<FilePatch>, onComplete: (List<PatchApplyResult>) -> Unit) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            isApplyingPatch = true
            
            val results = withContext(Dispatchers.IO) {
                patchApplier.applyPatches(patches)
            }
            isApplyingPatch = false

            if (results.any { it.success }) {
                lastFileWriteTimestamp = System.currentTimeMillis()
                appliedPatchesHistory.addAll(patches.filter { patch -> results.any { it.patchId == patch.id && it.success } })
                loadWorkspace(rootUriString)
            }
            results.firstOrNull { !it.success }?.let {
                workspaceError = it.message
            }
            onComplete(results)
        }
    }

    fun undoLastPatch(rootUriString: String, onComplete: (Boolean) -> Unit) {
        undoLastApply(rootUriString) { result ->
            onComplete(result.success)
        }
    }

    fun undoLastApply(rootUriString: String, onComplete: (UndoResult) -> Unit) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            isApplyingPatch = true
            
            val result = withContext(Dispatchers.IO) {
                patchApplier.undoLastApply()
            }
            isApplyingPatch = false

            if (result.success) {
                if (appliedPatchesHistory.isNotEmpty()) {
                    appliedPatchesHistory.clear()
                }
                lastFileWriteTimestamp = System.currentTimeMillis()
                loadWorkspace(rootUriString)
            } else {
                workspaceError = result.message
            }
            onComplete(result)
        }
    }

    fun createFile(parentUriString: String, mimeType: String, fileName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.createFile(parentUriString, mimeType, fileName)
            }
        }
    }

    fun createDirectory(parentUriString: String, dirName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.createDirectory(parentUriString, dirName)
            }
        }
    }

    fun deleteFile(fileUriString: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.deleteFile(fileUriString)
            }
            onComplete(success)
        }
    }

    fun exportWorkspaceZip(context: Context, rootUriString: String, outputUriString: String) {
        viewModelScope.launch {
            isExportingWorkspace = true
            exportStatusMessage = null
            exportProgress = WorkspaceExporter.ExportProgress()

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(Uri.parse(outputUriString), "w")?.use { output ->
                        WorkspaceExporter.exportToZip(
                            context = context,
                            rootUri = Uri.parse(rootUriString),
                            outputStream = output,
                            onProgress = { progress ->
                                viewModelScope.launch { exportProgress = progress }
                            }
                        )
                    } ?: listOf("Exportziel konnte nicht geoeffnet werden.")
                }
            }

            isExportingWorkspace = false
            result.onSuccess { warnings ->
                exportStatusMessage = if (warnings.isNullOrEmpty()) {
                    "Workspace ZIP exportiert."
                } else {
                    "Workspace ZIP exportiert, ${warnings.size} Warnung(en)."
                }
            }.onFailure {
                exportStatusMessage = "ZIP Export fehlgeschlagen."
            }
        }
    }
}
