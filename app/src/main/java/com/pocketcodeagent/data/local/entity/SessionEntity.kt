package com.pocketcodeagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val workspaceUri: String? = null,
    val workspaceName: String? = null,
    val selectedProviderId: Int? = null,
    val selectedModel: String? = null,
    val selectedRoleId: String? = null,
    val selectedSkillId: String? = null,
    val agentMode: String? = null,
    val previewTargetType: String? = null,
    val previewTargetData: String? = null,
    val activeFileName: String? = null,
    val activeFileUri: String? = null
)
