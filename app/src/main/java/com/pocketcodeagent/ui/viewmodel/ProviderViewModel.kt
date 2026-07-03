package com.pocketcodeagent.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.repository.ProviderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProviderViewModel(private val repository: ProviderRepository) : ViewModel() {

    val providers: StateFlow<List<Provider>> = repository.allProvidersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form states
    var name by mutableStateOf("")
    var baseUrl by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var modelName by mutableStateOf("")
    var customHeadersText by mutableStateOf("") // Key: Value format
    var isStreamSupported by mutableStateOf(true)

    // UI Status
    var testResult by mutableStateOf<String?>(null)
    var isTesting by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    fun clearForm() {
        name = ""
        baseUrl = ""
        apiKey = ""
        modelName = ""
        customHeadersText = ""
        isStreamSupported = true
        testResult = null
    }

    fun loadIntoForm(provider: Provider) {
        name = provider.name
        baseUrl = provider.baseUrl
        apiKey = provider.apiKey
        modelName = provider.modelName
        customHeadersText = provider.customHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        isStreamSupported = provider.isStreamSupported
        testResult = null
    }

    private fun parseHeaders(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (customHeadersText.isEmpty()) return map
        val lines = customHeadersText.split("\n")
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                map[parts[0].trim()] = parts[1].trim()
            }
        }
        return map
    }

    fun saveProvider(id: Int = 0, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            val provider = Provider(
                id = id,
                name = name,
                baseUrl = baseUrl,
                apiKey = apiKey,
                modelName = modelName,
                customHeaders = parseHeaders(),
                isStreamSupported = isStreamSupported
            )
            repository.saveProvider(provider)
            isSaving = false
            clearForm()
            onSuccess()
        }
    }

    fun deleteProvider(provider: Provider) {
        viewModelScope.launch {
            repository.deleteProvider(provider)
        }
    }

    fun testConnection() {
        val provider = Provider(
            name = name,
            baseUrl = baseUrl,
            apiKey = apiKey,
            modelName = modelName,
            customHeaders = parseHeaders(),
            isStreamSupported = isStreamSupported
        )
        viewModelScope.launch {
            isTesting = true
            testResult = "Connecting and validating..."
            val result = repository.testProviderConnection(provider)
            testResult = if (result.isSuccess) {
                "Success! Response: ${result.getOrNull()}"
            } else {
                "Failed! Error: ${result.exceptionOrNull()?.message}"
            }
            isTesting = false
        }
    }
}
