package com.pocketcodeagent.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.ProviderPresets
import com.pocketcodeagent.data.model.ProviderTestStatus
import com.pocketcodeagent.data.model.ProviderType
import com.pocketcodeagent.data.repository.ProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProviderViewModel(private val repository: ProviderRepository) : ViewModel() {

    private val _isLoadingProviders = MutableStateFlow(true)
    val isLoadingProvidersFlow: StateFlow<Boolean> = _isLoadingProviders

    val providers: StateFlow<List<Provider>> = repository.allProvidersFlow
        .onEach { _isLoadingProviders.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var providerType by mutableStateOf(ProviderType.OPENROUTER)
    var name by mutableStateOf("")
    var baseUrl by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var modelName by mutableStateOf("")
    var customHeadersText by mutableStateOf("")
    var isStreamSupported by mutableStateOf(true)
    var lastTestStatus by mutableStateOf(ProviderTestStatus.NOT_CONFIGURED)
    var lastErrorSanitized by mutableStateOf<String?>(null)

    var testResult by mutableStateOf<String?>(null)
    var modelListResult by mutableStateOf<String?>(null)
    var fetchedModelOptions by mutableStateOf<List<String>>(emptyList())
    var isTesting by mutableStateOf(false)
    var testingProviderId by mutableStateOf<Int?>(null)
    var isSaving by mutableStateOf(false)
    var isLoadingModels by mutableStateOf(false)

    init {
        applyPreset(ProviderType.OPENROUTER)
    }

    fun clearForm() {
        apiKey = ""
        customHeadersText = ""
        lastTestStatus = ProviderTestStatus.NOT_CONFIGURED
        lastErrorSanitized = null
        testResult = null
        modelListResult = null
        fetchedModelOptions = emptyList()
        applyPreset(ProviderType.OPENROUTER)
    }

    fun applyPreset(type: ProviderType) {
        val preset = ProviderPresets.forType(type)
        providerType = type
        name = preset.displayName
        baseUrl = preset.baseUrl
        modelName = preset.defaultModel
        apiKey = ""
        isStreamSupported = preset.supportsStreaming
        lastTestStatus = ProviderTestStatus.NOT_CONFIGURED
        lastErrorSanitized = null
        testResult = null
        modelListResult = null
        fetchedModelOptions = emptyList()
    }

    fun loadIntoForm(provider: Provider) {
        providerType = provider.providerType
        name = provider.name
        baseUrl = provider.baseUrl
        apiKey = provider.apiKey
        modelName = provider.modelName
        customHeadersText = provider.customHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        isStreamSupported = provider.isStreamSupported
        lastTestStatus = provider.lastTestStatus
        lastErrorSanitized = provider.lastErrorSanitized
        testResult = provider.lastErrorSanitized
        modelListResult = null
        fetchedModelOptions = emptyList()
    }

    fun currentModelOptions(): List<String> {
        return (listOf(modelName).filter { it.isNotBlank() } +
            fetchedModelOptions +
            ProviderPresets.modelOptionsFor(providerType, modelName))
            .distinct()
    }

    private fun parseHeaders(): Map<String, String> {
        if (customHeadersText.isBlank()) return emptyMap()
        return customHeadersText.lines()
            .mapNotNull { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun buildProvider(id: Int = 0): Provider {
        return Provider(
            id = id,
            name = name.ifBlank { ProviderPresets.forType(providerType).displayName },
            providerType = providerType,
            baseUrl = baseUrl.trim(),
            apiKey = apiKey,
            modelName = modelName.trim(),
            customHeaders = parseHeaders(),
            isStreamSupported = isStreamSupported,
            enabled = true,
            lastTestStatus = lastTestStatus,
            lastErrorSanitized = lastErrorSanitized
        )
    }

    fun saveProvider(id: Int = 0, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            repository.saveProvider(buildProvider(id))
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

    fun updateProviderModel(provider: Provider, selectedModel: String, onUpdated: (Provider) -> Unit) {
        viewModelScope.launch {
            val updated = provider.copy(
                modelName = selectedModel,
                lastTestStatus = ProviderTestStatus.NOT_CONFIGURED,
                lastErrorSanitized = null
            )
            repository.saveProvider(updated)
            onUpdated(updated)
        }
    }

    fun testConnection(id: Int = 0) {
        val provider = buildProvider(id)
        viewModelScope.launch {
            isTesting = true
            testingProviderId = id.takeIf { it > 0 }
            testResult = "Testing..."
            val result = if (id > 0) {
                repository.testAndPersistProviderConnection(provider)
            } else {
                repository.testProviderConnection(provider)
            }
            lastTestStatus = if (result.success) ProviderTestStatus.READY else ProviderTestStatus.ERROR
            lastErrorSanitized = result.sanitizedError
            testResult = if (result.success) {
                "${result.providerName} / ${result.modelName}: Ready (${result.answer ?: "OK"})"
            } else {
                result.sanitizedError ?: "${result.providerName}: Error"
            }
            isTesting = false
            testingProviderId = null
        }
    }

    fun testSavedProvider(provider: Provider, onUpdated: (Provider) -> Unit) {
        viewModelScope.launch {
            isTesting = true
            testingProviderId = provider.id
            val result = repository.testAndPersistProviderConnection(provider)
            val updated = provider.copy(
                lastTestStatus = if (result.success) ProviderTestStatus.READY else ProviderTestStatus.ERROR,
                lastErrorSanitized = result.sanitizedError
            )
            onUpdated(updated)
            testResult = if (result.success) {
                "${result.providerName} / ${result.modelName}: Ready (${result.answer ?: "OK"})"
            } else {
                result.sanitizedError ?: "${result.providerName}: Error"
            }
            isTesting = false
            testingProviderId = null
        }
    }

    fun refreshModelList(id: Int = 0) {
        val provider = buildProvider(id)
        viewModelScope.launch {
            isLoadingModels = true
            modelListResult = "Loading models..."
            val result = repository.listProviderModels(provider)
            result.onSuccess { models ->
                fetchedModelOptions = models.take(160)
                modelListResult = "Loaded ${models.size} models"
            }.onFailure { error ->
                modelListResult = error.message ?: "Model list failed"
            }
            isLoadingModels = false
        }
    }
}
