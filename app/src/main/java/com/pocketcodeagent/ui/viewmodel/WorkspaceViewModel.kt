package com.pocketcodeagent.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.data.repository.DiffLine
import com.pocketcodeagent.data.repository.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceViewModel(val repository: WorkspaceRepository) : ViewModel() {

    var files by mutableStateOf<List<WorkspaceFile>>(emptyList())
    var isLoadingFiles by mutableStateOf(false)

    // Current open file editing
    var openFileContent by mutableStateOf("")
    var isOpenFileLoading by mutableStateOf(false)
    var isOpenFileSaving by mutableStateOf(false)

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

    fun loadFileContent(uriString: String) {
        viewModelScope.launch {
            isOpenFileLoading = true
            val content = withContext(Dispatchers.IO) {
                repository.readFile(uriString)
            }
            openFileContent = content
            isOpenFileLoading = false
        }
    }

    fun saveFileContent(uriString: String, content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isOpenFileSaving = true
            val success = withContext(Dispatchers.IO) {
                repository.writeFile(uriString, content)
            }
            isOpenFileSaving = false
            if (success) {
                lastFileWriteTimestamp = System.currentTimeMillis()
                onSuccess()
            }
        }
    }

    fun prepareDiff(rootUriString: String, patch: FilePatch) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            
            val original = if (patch.action.lowercase() == "create") {
                ""
            } else {
                withContext(Dispatchers.IO) {
                    repository.workspace.readFile(patch.path) ?: ""
                }
            }
            
            val modified = if (patch.action.lowercase() == "delete") {
                ""
            } else {
                patch.newText
            }
            
            val diff = withContext(Dispatchers.IO) {
                com.pocketcodeagent.data.util.DiffGenerator.computeDiff(original, modified)
            }
            activeDiffLines = diff
        }
    }

    fun applyPatch(rootUriString: String, patch: FilePatch, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            
            val result = withContext(Dispatchers.IO) {
                com.pocketcodeagent.data.util.PatchApplier.applyPatch(repository.workspace, patch)
            }
            
            when (result) {
                is com.pocketcodeagent.data.util.PatchApplier.PatchResult.Success -> {
                    appliedPatchesHistory.add(patch)
                    lastFileWriteTimestamp = System.currentTimeMillis()
                    onComplete(true)
                }
                is com.pocketcodeagent.data.util.PatchApplier.PatchResult.Error -> {
                    workspaceError = result.message
                    onComplete(false)
                }
            }
        }
    }

    fun applyAllPatches(rootUriString: String, patches: List<FilePatch>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            
            val success = withContext(Dispatchers.IO) {
                var allOk = true
                for (patch in patches) {
                    val result = com.pocketcodeagent.data.util.PatchApplier.applyPatch(repository.workspace, patch)
                    if (result is com.pocketcodeagent.data.util.PatchApplier.PatchResult.Success) {
                        appliedPatchesHistory.add(patch)
                    } else if (result is com.pocketcodeagent.data.util.PatchApplier.PatchResult.Error) {
                        workspaceError = result.message
                        allOk = false
                        break
                    }
                }
                allOk
            }
            if (success) {
                lastFileWriteTimestamp = System.currentTimeMillis()
            }
            onComplete(success)
        }
    }

    fun undoLastPatch(rootUriString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (appliedPatchesHistory.isEmpty()) {
                onComplete(false)
                return@launch
            }
            
            workspaceError = null
            repository.workspace.setRootUri(rootUriString)
            val lastPatch = appliedPatchesHistory.last()
            
            val success = withContext(Dispatchers.IO) {
                com.pocketcodeagent.data.util.PatchApplier.undoPatch(repository.workspace, lastPatch)
            }
            
            if (success) {
                appliedPatchesHistory.removeAt(appliedPatchesHistory.size - 1)
                onComplete(true)
            } else {
                workspaceError = "Failed to undo patch for file: ${lastPatch.path}"
                onComplete(false)
            }
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
}
