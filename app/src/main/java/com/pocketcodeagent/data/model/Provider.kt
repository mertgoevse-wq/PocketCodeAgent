package com.pocketcodeagent.data.model

import com.pocketcodeagent.data.local.KeystoreHelper
import com.pocketcodeagent.data.local.entity.ProviderEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class ProviderType(val displayName: String) {
    OPENROUTER("OpenRouter"),
    GEMINI_OPENAI("Google Gemini"),
    NVIDIA_NIM("NVIDIA NIM"),
    GROQ("Groq"),
    MISTRAL("Mistral"),
    TOGETHER("Together"),
    CUSTOM_OPENAI("Custom");

    companion object {
        fun fromStored(value: String?): ProviderType {
            return entries.firstOrNull { it.name == value } ?: CUSTOM_OPENAI
        }

        fun infer(name: String, baseUrl: String): ProviderType {
            val text = "${name.lowercase()} ${baseUrl.lowercase()}"
            return when {
                "openrouter" in text -> OPENROUTER
                "generativelanguage.googleapis.com" in text || "gemini" in text -> GEMINI_OPENAI
                "nvidia" in text || "integrate.api.nvidia.com" in text -> NVIDIA_NIM
                "groq" in text -> GROQ
                "mistral" in text -> MISTRAL
                "together" in text -> TOGETHER
                else -> CUSTOM_OPENAI
            }
        }
    }
}

enum class ProviderTestStatus(val label: String) {
    NOT_CONFIGURED("Not configured"),
    READY("Ready"),
    TESTING("Testing"),
    ERROR("Error");

    companion object {
        fun fromStored(value: String?): ProviderTestStatus {
            return entries.firstOrNull { it.name == value } ?: NOT_CONFIGURED
        }
    }
}

data class Provider(
    val id: Int = 0,
    val name: String,
    val providerType: ProviderType = ProviderType.infer(name, ""),
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val customHeaders: Map<String, String> = emptyMap(),
    val isStreamSupported: Boolean = true,
    val enabled: Boolean = true,
    val lastTestStatus: ProviderTestStatus = ProviderTestStatus.NOT_CONFIGURED,
    val lastErrorSanitized: String? = null
) {
    // Getter mappings for ProviderConfig specification
    val displayName: String get() = name
    val apiKeyAlias: String get() = "keystore_alias_for_provider_$id"
    val selectedModel: String get() = modelName
    val extraHeaders: Map<String, String> get() = customHeaders
    val supportsStreaming: Boolean get() = isStreamSupported
    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    fun hasRequiredConfiguration(): Boolean {
        return enabled && baseUrl.isNotBlank() && modelName.isNotBlank() && apiKey.isNotBlank()
    }

    fun toEntity(): ProviderEntity {
        val encryptedKey = KeystoreHelper.encrypt(apiKey)
        val headersJson = Gson().toJson(customHeaders)
        return ProviderEntity(
            id = id,
            name = name,
            providerType = providerType.name,
            baseUrl = baseUrl,
            encryptedApiKey = encryptedKey,
            modelName = modelName,
            customHeadersJson = headersJson,
            isStreamSupported = isStreamSupported,
            enabled = enabled,
            lastTestStatus = lastTestStatus.name,
            lastErrorSanitized = lastErrorSanitized
        )
    }

    companion object {
        fun fromEntity(entity: ProviderEntity): Provider {
            val decryptedKey = KeystoreHelper.decrypt(entity.encryptedApiKey)
            val headerType = object : TypeToken<Map<String, String>>() {}.type
            val headers: Map<String, String> = try {
                if (entity.customHeadersJson.isNotEmpty()) {
                    Gson().fromJson(entity.customHeadersJson, headerType)
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }
            return Provider(
                id = entity.id,
                name = entity.name,
                providerType = ProviderType.fromStored(entity.providerType)
                    .takeUnless { it == ProviderType.CUSTOM_OPENAI }
                    ?: ProviderType.infer(entity.name, entity.baseUrl),
                baseUrl = entity.baseUrl,
                apiKey = decryptedKey,
                modelName = entity.modelName,
                customHeaders = headers,
                isStreamSupported = entity.isStreamSupported,
                enabled = entity.enabled,
                lastTestStatus = ProviderTestStatus.fromStored(entity.lastTestStatus),
                lastErrorSanitized = entity.lastErrorSanitized
            )
        }
    }
}

data class ProviderConfig(
    val id: Int,
    val displayName: String,
    val providerType: ProviderType,
    val baseUrl: String,
    val selectedModel: String,
    val supportsStreaming: Boolean,
    val enabled: Boolean,
    val customHeaders: Map<String, String> = emptyMap(),
    val lastTestStatus: ProviderTestStatus = ProviderTestStatus.NOT_CONFIGURED,
    val lastErrorSanitized: String? = null
)

fun Provider.toConfig(): ProviderConfig {
    return ProviderConfig(
        id = id,
        displayName = displayName,
        providerType = providerType,
        baseUrl = baseUrl,
        selectedModel = selectedModel,
        supportsStreaming = supportsStreaming,
        enabled = enabled,
        customHeaders = customHeaders,
        lastTestStatus = lastTestStatus,
        lastErrorSanitized = lastErrorSanitized
    )
}
