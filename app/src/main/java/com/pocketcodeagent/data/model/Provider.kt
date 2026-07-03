package com.pocketcodeagent.data.model

import com.pocketcodeagent.data.local.KeystoreHelper
import com.pocketcodeagent.data.local.entity.ProviderEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Provider(
    val id: Int = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val customHeaders: Map<String, String> = emptyMap(),
    val isStreamSupported: Boolean = true
) {
    fun toEntity(): ProviderEntity {
        val encryptedKey = KeystoreHelper.encrypt(apiKey)
        val headersJson = Gson().toJson(customHeaders)
        return ProviderEntity(
            id = id,
            name = name,
            baseUrl = baseUrl,
            encryptedApiKey = encryptedKey,
            modelName = modelName,
            customHeadersJson = headersJson,
            isStreamSupported = isStreamSupported
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
                baseUrl = entity.baseUrl,
                apiKey = decryptedKey,
                modelName = entity.modelName,
                customHeaders = headers,
                isStreamSupported = entity.isStreamSupported
            )
        }
    }
}
