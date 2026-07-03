package com.pocketcodeagent.domain.workspace

import com.pocketcodeagent.data.local.DocumentFileWorkspace
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.FilePatchStatus
import java.util.UUID

data class PatchApplyResult(
    val patchId: String,
    val path: String,
    val success: Boolean,
    val status: FilePatchStatus,
    val message: String,
    val backupId: String? = null
)

data class UndoResult(
    val success: Boolean,
    val restoredFiles: List<String>,
    val message: String
)

class WorkspacePatchApplier(
    private val workspace: DocumentFileWorkspace
) {
    private data class BackupEntry(
        val backupId: String,
        val patchId: String,
        val path: String,
        val action: FilePatchAction,
        val oldContent: String?
    )

    private var lastApplyBackups: List<BackupEntry> = emptyList()

    suspend fun applyPatch(patch: FilePatch): PatchApplyResult {
        val result = applyPatchInternal(patch, keepUndoBatch = true)
        return result.first
    }

    suspend fun applyPatches(patches: List<FilePatch>): List<PatchApplyResult> {
        val batchBackups = mutableListOf<BackupEntry>()
        val results = patches.map { patch ->
            val (result, backup) = applyPatchInternal(patch, keepUndoBatch = false)
            if (backup != null) batchBackups.add(backup)
            result
        }
        if (batchBackups.isNotEmpty()) {
            lastApplyBackups = batchBackups
        }
        return results
    }

    suspend fun undoLastApply(): UndoResult {
        if (lastApplyBackups.isEmpty()) {
            return UndoResult(false, emptyList(), "Keine Apply-Operation zum Rueckgaengigmachen vorhanden.")
        }

        val restored = mutableListOf<String>()
        var failedPath: String? = null

        lastApplyBackups.asReversed().forEach { backup ->
            val success = when (backup.action) {
                FilePatchAction.CREATE -> {
                    if (workspace.exists(backup.path)) workspace.deleteFile(backup.path) else true
                }
                FilePatchAction.MODIFY -> {
                    backup.oldContent?.let { workspace.writeFile(backup.path, it) } ?: false
                }
                FilePatchAction.DELETE -> {
                    backup.oldContent?.let { workspace.writeFile(backup.path, it) } ?: false
                }
            }

            if (success) {
                restored.add(backup.path)
            } else if (failedPath == null) {
                failedPath = backup.path
            }
        }

        return if (failedPath == null) {
            lastApplyBackups = emptyList()
            UndoResult(true, restored, "Letzte Apply-Operation wurde rueckgaengig gemacht.")
        } else {
            UndoResult(false, restored, "Undo fehlgeschlagen fuer ${failedPath.orEmpty()}.")
        }
    }

    private fun applyPatchInternal(
        patch: FilePatch,
        keepUndoBatch: Boolean
    ): Pair<PatchApplyResult, BackupEntry?> {
        if (!workspace.hasWritePermission()) {
            return patch.failure(FilePatchStatus.FAILED, "Workspace hat keine Schreibberechtigung.") to null
        }

        val normalizedPath = normalizeWorkspacePath(patch.path)
            ?: return patch.failure(FilePatchStatus.FAILED, "Ungueltiger oder blockierter Workspace-Pfad.") to null

        val result = when (patch.action) {
            FilePatchAction.CREATE -> applyCreate(patch, normalizedPath)
            FilePatchAction.MODIFY -> applyModify(patch, normalizedPath)
            FilePatchAction.DELETE -> applyDelete(patch, normalizedPath)
        }

        if (keepUndoBatch && result.second != null) {
            lastApplyBackups = listOf(result.second!!)
        }
        return result
    }

    private fun applyCreate(patch: FilePatch, path: String): Pair<PatchApplyResult, BackupEntry?> {
        if (workspace.exists(path)) {
            return patch.failure(FilePatchStatus.CONFLICT, "Datei existiert bereits: $path") to null
        }

        val content = patch.newText ?: ""
        val success = workspace.writeFile(path, content)
        if (!success) {
            return patch.failure(FilePatchStatus.FAILED, "Datei konnte nicht erstellt werden: $path") to null
        }

        val backup = BackupEntry(newBackupId(), patch.id, path, FilePatchAction.CREATE, oldContent = null)
        return patch.success(path, "Datei erstellt.", backup.backupId) to backup
    }

    private fun applyModify(patch: FilePatch, path: String): Pair<PatchApplyResult, BackupEntry?> {
        if (!workspace.exists(path)) {
            return patch.failure(FilePatchStatus.CONFLICT, "Datei existiert nicht: $path") to null
        }

        val current = workspace.readFile(path)
            ?: return patch.failure(FilePatchStatus.FAILED, "Datei konnte nicht gelesen werden: $path") to null
        val newText = patch.newText ?: ""
        val oldText = patch.oldText

        val updated = when {
            !oldText.isNullOrEmpty() -> {
                if (!current.contains(oldText)) {
                    return patch.failure(FilePatchStatus.CONFLICT, "oldText wurde in der aktuellen Datei nicht exakt gefunden.") to null
                }
                current.replaceFirst(oldText, newText)
            }
            patch.replaceWholeFile -> newText
            else -> {
                return patch.failure(FilePatchStatus.CONFLICT, "Whole-file Replace ist nicht bestaetigt.") to null
            }
        }

        val backup = BackupEntry(newBackupId(), patch.id, path, FilePatchAction.MODIFY, current)
        val success = workspace.writeFile(path, updated)
        if (!success) {
            return patch.failure(FilePatchStatus.FAILED, "Aenderung konnte nicht geschrieben werden: $path") to null
        }
        return patch.success(path, "Datei geaendert.", backup.backupId) to backup
    }

    private fun applyDelete(patch: FilePatch, path: String): Pair<PatchApplyResult, BackupEntry?> {
        if (!patch.deleteConfirmed) {
            return patch.failure(FilePatchStatus.CONFLICT, "Delete braucht eine zweite Bestaetigung.") to null
        }
        if (!workspace.exists(path)) {
            return patch.failure(FilePatchStatus.CONFLICT, "Datei existiert nicht: $path") to null
        }

        val current = workspace.readFile(path)
            ?: return patch.failure(FilePatchStatus.FAILED, "Datei konnte vor Delete nicht gesichert werden: $path") to null
        val backup = BackupEntry(newBackupId(), patch.id, path, FilePatchAction.DELETE, current)
        val success = workspace.deleteFile(path)
        if (!success) {
            return patch.failure(FilePatchStatus.FAILED, "Datei konnte nicht geloescht werden: $path") to null
        }
        return patch.success(path, "Datei geloescht. Undo kann sie wiederherstellen.", backup.backupId) to backup
    }

    private fun normalizeWorkspacePath(path: String): String? {
        val trimmed = path.trim().replace('\\', '/')
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("/") || trimmed.startsWith("content:")) return null
        if (Regex("^[A-Za-z]:").containsMatchIn(trimmed)) return null
        if (trimmed.contains('\u0000')) return null

        val parts = trimmed.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        if (parts.any { it == "." || it == ".." }) return null
        if (parts.any { containsInvalidName(it) }) return null

        val blockedFolders = setOf("example", "sample", "demo", "playground", "starter", "template")
        if (parts.dropLast(1).any { it.lowercase() in blockedFolders }) return null

        return parts.joinToString("/")
    }

    private fun containsInvalidName(name: String): Boolean {
        val invalidCharacters = charArrayOf('<', '>', ':', '"', '|', '?', '*')
        return invalidCharacters.any { name.contains(it) }
    }

    private fun FilePatch.success(path: String, message: String, backupId: String): PatchApplyResult {
        return PatchApplyResult(id, path, true, FilePatchStatus.APPLIED, message, backupId)
    }

    private fun FilePatch.failure(status: FilePatchStatus, message: String): PatchApplyResult {
        return PatchApplyResult(id, path, false, status, sanitizeMessage(message))
    }

    private fun sanitizeMessage(message: String): String {
        return message
            .replace(Regex("(?i)bearer\\s+[a-z0-9._\\-]+"), "Bearer [redacted]")
            .replace(Regex("(?i)(api[_-]?key|token|secret)\\s*[:=]\\s*\\S+"), "$1=[redacted]")
            .take(240)
    }

    private fun newBackupId(): String = "backup-${UUID.randomUUID()}"
}
