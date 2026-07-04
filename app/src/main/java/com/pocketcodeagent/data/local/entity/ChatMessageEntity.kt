package com.pocketcodeagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: Int,
    val role: String,            // "user" or "assistant"
    val content: String,         // sanitized text
    val sender: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hasArtifacts: Boolean = false
)
