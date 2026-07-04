package com.pocketcodeagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val sessionId: Int,
    val title: String,
    val rawText: String,        // sanitized
    val createdAt: Long = System.currentTimeMillis()
)
