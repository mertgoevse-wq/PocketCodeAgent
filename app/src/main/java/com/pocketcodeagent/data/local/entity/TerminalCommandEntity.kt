package com.pocketcodeagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "terminal_commands")
data class TerminalCommandEntity(
    @PrimaryKey val id: String,
    val sessionId: Int,
    val command: String,         // sanitized
    val reason: String? = null,  // sanitized
    val riskLevel: String = "CAUTION",
    val status: String = "QUEUED",
    val source: String = "AGENT",
    val createdAt: Long = System.currentTimeMillis(),
    val copiedAt: Long? = null,
    val notes: String? = null
)
