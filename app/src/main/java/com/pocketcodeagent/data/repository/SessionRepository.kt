package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.local.AppDatabase
import com.pocketcodeagent.data.local.entity.ArtifactEntity
import com.pocketcodeagent.data.local.entity.ChatMessageEntity
import com.pocketcodeagent.data.local.entity.FilePatchEntity
import com.pocketcodeagent.data.local.entity.SessionEntity
import com.pocketcodeagent.data.local.entity.TerminalCommandEntity
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchSource
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.domain.agent.AgentArtifact
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.context.ContextSanitizer
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.terminal.CommandSource
import com.pocketcodeagent.domain.terminal.CommandStatus
import com.pocketcodeagent.domain.terminal.TerminalCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SessionRepository(private val database: AppDatabase) {
    private val sessionDao = database.sessionDao()
    private val chatMessageDao = database.chatMessageDao()
    private val artifactDao = database.artifactDao()
    private val filePatchDao = database.filePatchDao()
    private val terminalCommandDao = database.terminalCommandDao()

    // ─── Session CRUD ────────────────────────────────────────────────────────

    suspend fun createSession(
        title: String = "",
        workspaceUri: String? = null,
        workspaceName: String? = null,
        providerId: Int? = null,
        model: String? = null,
        roleId: String? = null,
        skillId: String? = null,
        mode: String? = null,
        previewTarget: PreviewTarget? = null,
        activeFileName: String? = null,
        activeFileUri: String? = null
    ): SessionEntity = withContext(Dispatchers.IO) {
        val (ptType, ptData) = serializePreviewTarget(previewTarget)
        val entity = SessionEntity(
            id = 0,
            title = sanitize(title),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            workspaceUri = workspaceUri,
            workspaceName = workspaceName,
            selectedProviderId = providerId,
            selectedModel = model?.takeIf { it.isNotBlank() },
            selectedRoleId = roleId,
            selectedSkillId = skillId,
            agentMode = mode,
            previewTargetType = ptType,
            previewTargetData = ptData,
            activeFileName = activeFileName,
            activeFileUri = activeFileUri
        )
        val newId = sessionDao.insertSession(entity).toInt()
        entity.copy(id = newId)
    }

    suspend fun loadLastSession(): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.loadLastSession()
    }

    suspend fun loadSession(sessionId: Int): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: Int, title: String) = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(sessionId)?.let {
            sessionDao.updateSession(it.copy(title = sanitize(title), updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateSessionState(
        sessionId: Int,
        providerId: Int? = null,
        model: String? = null,
        roleId: String? = null,
        skillId: String? = null,
        mode: String? = null,
        previewTarget: PreviewTarget? = null,
        workspaceUri: String? = null,
        workspaceName: String? = null,
        activeFileName: String? = null,
        activeFileUri: String? = null
    ) = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(sessionId)?.let { session ->
            val (ptType, ptData) = serializePreviewTarget(previewTarget)
            sessionDao.updateSession(
                session.copy(
                    selectedProviderId = providerId ?: session.selectedProviderId,
                    selectedModel = model ?: session.selectedModel,
                    selectedRoleId = roleId ?: session.selectedRoleId,
                    selectedSkillId = skillId ?: session.selectedSkillId,
                    agentMode = mode ?: session.agentMode,
                    previewTargetType = ptType ?: session.previewTargetType,
                    previewTargetData = ptData ?: session.previewTargetData,
                    workspaceUri = workspaceUri ?: session.workspaceUri,
                    workspaceName = workspaceName ?: session.workspaceName,
                    activeFileName = activeFileName ?: session.activeFileName,
                    activeFileUri = activeFileUri ?: session.activeFileUri,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteSession(sessionId: Int) = withContext(Dispatchers.IO) {
        chatMessageDao.deleteMessagesForSession(sessionId)
        artifactDao.deleteArtifactsForSession(sessionId)
        filePatchDao.deletePatchesForSession(sessionId)
        terminalCommandDao.deleteCommandsForSession(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    // ─── Messages ────────────────────────────────────────────────────────────

    suspend fun saveMessage(message: ChatMessage, sessionId: Int) = withContext(Dispatchers.IO) {
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                sessionId = sessionId,
                role = if (message.isAgent) "assistant" else "user",
                content = sanitize(message.message),
                sender = message.sender.take(50),
                timestamp = message.timestamp,
                hasArtifacts = message.artifacts.isNotEmpty()
            )
        )
    }

    suspend fun loadMessages(sessionId: Int): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatMessageDao.getMessagesForSession(sessionId)
    }

    // ─── Artifacts ───────────────────────────────────────────────────────────

    suspend fun saveArtifact(artifact: AgentArtifact, sessionId: Int) = withContext(Dispatchers.IO) {
        artifactDao.insertArtifact(
            ArtifactEntity(
                id = artifact.id,
                sessionId = sessionId,
                title = sanitize(artifact.title),
                rawText = sanitize(artifact.rawText),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadArtifacts(sessionId: Int): List<ArtifactEntity> = withContext(Dispatchers.IO) {
        artifactDao.getArtifactsForSession(sessionId)
    }

    // ─── File Patches ────────────────────────────────────────────────────────

    suspend fun savePendingPatch(patch: FilePatch, sessionId: Int) = withContext(Dispatchers.IO) {
        filePatchDao.insertPatch(
            FilePatchEntity(
                id = patch.id,
                sessionId = sessionId,
                path = patch.path,
                action = patch.action.name,
                oldText = patch.oldText?.let { sanitize(it) },
                newText = patch.newText?.let { sanitize(it) },
                status = patch.status.name,
                source = patch.source.name,
                createdAt = patch.createdAt,
                errorMessage = patch.errorMessage,
                additions = patch.additions,
                deletions = patch.deletions,
                requiresSecondConfirmation = patch.requiresSecondConfirmation,
                deleteConfirmed = patch.deleteConfirmed,
                replaceWholeFile = patch.replaceWholeFile
            )
        )
    }

    suspend fun updatePatchStatus(patchId: String, status: FilePatchStatus, errorMessage: String? = null) =
        withContext(Dispatchers.IO) {
            filePatchDao.updatePatchStatus(patchId, status.name, errorMessage)
        }

    suspend fun loadPendingPatches(sessionId: Int): List<FilePatch> = withContext(Dispatchers.IO) {
        filePatchDao.getPatchesForSession(sessionId).map { entity ->
            FilePatch(
                id = entity.id,
                path = entity.path,
                action = try { FilePatchAction.valueOf(entity.action) } catch (_: Exception) { FilePatchAction.MODIFY },
                oldText = entity.oldText,
                newText = entity.newText,
                status = try { FilePatchStatus.valueOf(entity.status) } catch (_: Exception) { FilePatchStatus.PENDING },
                source = try { FilePatchSource.valueOf(entity.source) } catch (_: Exception) { FilePatchSource.AGENT },
                createdAt = entity.createdAt,
                errorMessage = entity.errorMessage,
                additions = entity.additions,
                deletions = entity.deletions,
                requiresSecondConfirmation = entity.requiresSecondConfirmation,
                deleteConfirmed = entity.deleteConfirmed,
                replaceWholeFile = entity.replaceWholeFile
            )
        }
    }

    // ─── Terminal Commands ───────────────────────────────────────────────────

    suspend fun saveTerminalCommand(cmd: TerminalCommand, sessionId: Int) = withContext(Dispatchers.IO) {
        terminalCommandDao.insertCommand(
            TerminalCommandEntity(
                id = cmd.id,
                sessionId = sessionId,
                command = sanitize(cmd.command),
                reason = cmd.reason?.let { sanitize(it) },
                riskLevel = cmd.riskLevel.name,
                status = cmd.status.name,
                source = cmd.source.name,
                createdAt = cmd.createdAt,
                copiedAt = cmd.copiedAt,
                notes = cmd.notes
            )
        )
    }

    suspend fun updateCommandStatus(commandId: String, status: CommandStatus) = withContext(Dispatchers.IO) {
        terminalCommandDao.updateCommandStatus(commandId, status.name)
    }

    suspend fun loadTerminalCommands(sessionId: Int): List<TerminalCommand> = withContext(Dispatchers.IO) {
        terminalCommandDao.getCommandsForSession(sessionId).map { entity ->
            TerminalCommand(
                id = entity.id,
                command = entity.command,
                reason = entity.reason,
                riskLevel = try { CommandRiskLevel.valueOf(entity.riskLevel) } catch (_: Exception) { CommandRiskLevel.CAUTION },
                source = try { CommandSource.valueOf(entity.source) } catch (_: Exception) { CommandSource.AGENT },
                status = try { CommandStatus.valueOf(entity.status) } catch (_: Exception) { CommandStatus.QUEUED },
                createdAt = entity.createdAt,
                copiedAt = entity.copiedAt,
                notes = entity.notes
            )
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun sanitize(text: String): String = ContextSanitizer.redactSummary(text)

    private fun serializePreviewTarget(target: PreviewTarget?): Pair<String?, String?> {
        if (target == null || target is PreviewTarget.None) return Pair(null, null)
        return when (target) {
            is PreviewTarget.WorkspaceStatic -> Pair("WORKSPACE", null)
            is PreviewTarget.File -> {
                val json = JSONObject().apply {
                    put("path", target.path)
                    put("fileName", target.fileName)
                }
                Pair("FILE", json.toString())
            }
            is PreviewTarget.Url -> Pair("URL", target.url)
            else -> Pair(null, null)
        }
    }

    fun deserializePreviewTarget(type: String?, data: String?): PreviewTarget {
        return when (type) {
            "WORKSPACE" -> PreviewTarget.WorkspaceStatic
            "FILE" -> {
                try {
                    val json = data?.let { JSONObject(it) } ?: return PreviewTarget.None
                    PreviewTarget.File(
                        path = json.optString("path", ""),
                        fileName = json.optString("fileName", "")
                    )
                } catch (_: Exception) { PreviewTarget.None }
            }
            "URL" -> PreviewTarget.Url(data ?: "")
            else -> PreviewTarget.None
        }
    }

    fun restoreAgentMode(mode: String?): AgentMode {
        return try { mode?.let { AgentMode.valueOf(it) } } catch (_: Exception) { null }
            ?: AgentMode.DISCUSS
    }
}
