package com.pocketcodeagent.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.Provider

class MainViewModel : ViewModel() {
    var currentScreen by mutableStateOf("welcome")
    var selectedWorkspaceUri by mutableStateOf<String?>(null)
    var selectedWorkspaceName by mutableStateOf<String?>(null)
    
    var selectedProvider by mutableStateOf<Provider?>(null)
    
    // Editor State
    var selectedFileUri by mutableStateOf<String?>(null)
    var selectedFileName by mutableStateOf<String?>(null)

    // Diff Review State
    var pendingFileChanges by mutableStateOf<List<FilePatch>>(emptyList())
    var currentDiffFileIndex by mutableStateOf(0)

    fun navigateTo(screen: String) {
        currentScreen = screen
    }

    fun selectWorkspace(uri: String, name: String) {
        selectedWorkspaceUri = uri
        selectedWorkspaceName = name
    }
}
