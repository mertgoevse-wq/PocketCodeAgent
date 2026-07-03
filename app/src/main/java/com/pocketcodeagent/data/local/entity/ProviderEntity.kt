package com.pocketcodeagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val providerType: String = "CUSTOM_OPENAI",
    val baseUrl: String,
    val encryptedApiKey: String,
    val modelName: String,
    val customHeadersJson: String = "",
    val isStreamSupported: Boolean = true,
    val enabled: Boolean = true,
    val lastTestStatus: String? = null,
    val lastErrorSanitized: String? = null
)
