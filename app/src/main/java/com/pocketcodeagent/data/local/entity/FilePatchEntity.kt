package com.pocketcodeagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_patches")
data class FilePatchEntity(
    @PrimaryKey val id: String,
    val sessionId: Int,
    val path: String,
    val action: String,          // CREATE / MODIFY / DELETE
    val oldText: String? = null, // sanitized
    val newText: String? = null, // sanitized
    val status: String = "PENDING",
    val source: String = "AGENT",
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val requiresSecondConfirmation: Boolean = false,
    val deleteConfirmed: Boolean = false,
    val replaceWholeFile: Boolean = false
)
