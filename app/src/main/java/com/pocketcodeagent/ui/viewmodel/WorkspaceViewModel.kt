package com.pocketcodeagent.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.ProposedFileChange
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
                onSuccess()
            }
        }
    }

    fun prepareDiff(change: ProposedFileChange) {
        viewModelScope.launch {
            val original = change.originalContent
            val modified = change.newContent
            val diff = withContext(Dispatchers.IO) {
                repository.computeDiff(original, modified)
            }
            activeDiffLines = diff
        }
    }

    fun applyProposedChange(rootUriString: String, change: ProposedFileChange, onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val fileUri = repository.createOrGetFileUriByRelativePath(rootUriString, change.relativePath)
                if (fileUri != null) {
                    repository.writeFile(fileUri.toString(), change.newContent)
                }
            }
            onComplete()
        }
    }
}
