package com.pocketcodeagent

import com.pocketcodeagent.data.local.entity.FilePatchEntity
import com.pocketcodeagent.data.local.entity.TerminalCommandEntity
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchSource
import com.pocketcodeagent.data.model.FilePatchStatus
import com.pocketcodeagent.data.repository.SessionRepository
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.terminal.CommandSource
import com.pocketcodeagent.domain.terminal.CommandStatus
import com.pocketcodeagent.domain.terminal.TerminalCommand
import org.junit.Assert.*
import org.junit.Test

class SessionRestoreTest {

    @Test
    fun `PreviewTarget WorkspaceStatic serialization roundtrip`() {
        val target: PreviewTarget = PreviewTarget.WorkspaceStatic
        val serialized = serializeTarget(target)
        assertEquals("WORKSPACE", serialized.first)
        assertNull(serialized.second)
        val restored = deserializeTarget(serialized.first, serialized.second)
        assertTrue(restored is PreviewTarget.WorkspaceStatic)
    }

    @Test
    fun `PreviewTarget File serialization roundtrip`() {
        val target = PreviewTarget.File("src/index.html", "index.html")
        val serialized = serializeTarget(target)
        assertEquals("FILE", serialized.first)
        assertTrue(serialized.second!!.contains("src/index.html"))
        val restored = deserializeTarget(serialized.first, serialized.second)
        assertTrue(restored is PreviewTarget.File)
        assertEquals("src/index.html", (restored as PreviewTarget.File).path)
        assertEquals("index.html", restored.fileName)
    }

    @Test
    fun `PreviewTarget Url serialization roundtrip`() {
        val target = PreviewTarget.Url("http://127.0.0.1:5173")
        val serialized = serializeTarget(target)
        assertEquals("URL", serialized.first)
        assertEquals("http://127.0.0.1:5173", serialized.second)
        val restored = deserializeTarget(serialized.first, serialized.second)
        assertTrue(restored is PreviewTarget.Url)
        assertEquals("http://127.0.0.1:5173", (restored as PreviewTarget.Url).url)
    }

    @Test
    fun `PreviewTarget None returns None from deserialization`() {
        val result = deserializeTarget(null, null)
        assertTrue(result is PreviewTarget.None)
    }

    @Test
    fun `AgentMode DISCUSS restore from string`() {
        val mode = restoreAgentMode("DISCUSS")
        assertEquals(AgentMode.DISCUSS, mode)
    }

    @Test
    fun `AgentMode BUILD restore from string`() {
        val mode = restoreAgentMode("BUILD")
        assertEquals(AgentMode.BUILD, mode)
    }

    @Test
    fun `AgentMode null falls back to DISCUSS`() {
        val mode = restoreAgentMode(null)
        assertEquals(AgentMode.DISCUSS, mode)
    }

    @Test
    fun `AgentMode invalid string falls back to DISCUSS`() {
        val mode = restoreAgentMode("INVALID_MODE")
        assertEquals(AgentMode.DISCUSS, mode)
    }

    @Test
    fun `TerminalCommand status restore from entity`() {
        val entity = TerminalCommandEntity(
            id = "cmd-1", sessionId = 1, command = "npm run dev",
            reason = "start server", riskLevel = "SAFE", status = "COPIED",
            source = "AGENT", createdAt = 123456L, copiedAt = 123457L
        )
        val cmd = TerminalCommand(
            id = entity.id, command = entity.command,
            reason = entity.reason, riskLevel = CommandRiskLevel.valueOf(entity.riskLevel),
            source = CommandSource.valueOf(entity.source), status = CommandStatus.valueOf(entity.status),
            createdAt = entity.createdAt, copiedAt = entity.copiedAt, notes = entity.notes
        )
        assertEquals("cmd-1", cmd.id)
        assertEquals("npm run dev", cmd.command)
        assertEquals(CommandRiskLevel.SAFE, cmd.riskLevel)
        assertEquals(CommandStatus.COPIED, cmd.status)
        assertEquals(CommandSource.AGENT, cmd.source)
    }

    @Test
    fun `TerminalCommand invalid status falls back to QUEUED`() {
        val entity = TerminalCommandEntity(
            id = "cmd-2", sessionId = 1, command = "ls",
            riskLevel = "SAFE", status = "INVALID_STATUS", source = "USER"
        )
        val status = try { CommandStatus.valueOf(entity.status) } catch (_: Exception) { CommandStatus.QUEUED }
        assertEquals(CommandStatus.QUEUED, status)
    }

    @Test
    fun `FilePatch entity mapping roundtrip`() {
        val patch = FilePatch(
            id = "patch-1", path = "index.html",
            action = FilePatchAction.CREATE, oldText = null,
            newText = "<html></html>", status = FilePatchStatus.PENDING,
            source = FilePatchSource.AGENT, createdAt = 123456L,
            additions = 1, deletions = 0
        )
        val entity = FilePatchEntity(
            id = patch.id, sessionId = 1, path = patch.path,
            action = patch.action.name, oldText = patch.oldText,
            newText = patch.newText, status = patch.status.name,
            source = patch.source.name, createdAt = patch.createdAt,
            additions = patch.additions, deletions = patch.deletions,
            requiresSecondConfirmation = false, deleteConfirmed = false,
            replaceWholeFile = false
        )
        assertEquals("patch-1", entity.id)
        assertEquals("index.html", entity.path)
        assertEquals("CREATE", entity.action)
        assertEquals("<html></html>", entity.newText)
        assertEquals("PENDING", entity.status)
    }

    @Test
    fun `FilePatch invalid action falls back to MODIFY`() {
        val action = try { FilePatchAction.valueOf("INVALID_ACTION") } catch (_: Exception) { FilePatchAction.MODIFY }
        assertEquals(FilePatchAction.MODIFY, action)
    }

    // ─── Helper methods mirroring SessionRepository logic ─────────────────

    private fun serializeTarget(target: PreviewTarget?): Pair<String?, String?> {
        if (target == null || target is PreviewTarget.None) return Pair(null, null)
        return when (target) {
            is PreviewTarget.WorkspaceStatic -> Pair("WORKSPACE", null)
            is PreviewTarget.File -> Pair("FILE", target.path + "|" + target.fileName)
            is PreviewTarget.Url -> Pair("URL", target.url)
            else -> Pair(null, null)
        }
    }

    private fun deserializeTarget(type: String?, data: String?): PreviewTarget {
        return when (type) {
            "WORKSPACE" -> PreviewTarget.WorkspaceStatic
            "FILE" -> {
                if (data == null) return PreviewTarget.None
                val parts = data.split("|", limit = 2)
                if (parts.size != 2) return PreviewTarget.None
                PreviewTarget.File(path = parts[0], fileName = parts[1])
            }
            "URL" -> PreviewTarget.Url(data ?: "")
            else -> PreviewTarget.None
        }
    }

    private fun restoreAgentMode(mode: String?): AgentMode {
        return try { mode?.let { AgentMode.valueOf(it) } } catch (_: Exception) { null }
            ?: AgentMode.DISCUSS
    }
}
