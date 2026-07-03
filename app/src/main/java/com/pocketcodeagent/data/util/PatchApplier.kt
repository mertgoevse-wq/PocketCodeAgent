package com.pocketcodeagent.data.util

import com.pocketcodeagent.data.local.DocumentFileWorkspace
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction

object PatchApplier {

    sealed class PatchResult {
        object Success : PatchResult()
        data class Error(val message: String) : PatchResult()
    }

    fun applyPatch(workspace: DocumentFileWorkspace, patch: FilePatch): PatchResult {
        if (!workspace.hasWritePermission()) {
            return PatchResult.Error("Missing write permissions for workspace directory.")
        }

        try {
            // Backup the file first to support Undo
            workspace.backupFile(patch.path)

            when (patch.action) {
                FilePatchAction.CREATE -> {
                    if (workspace.exists(patch.path)) {
                        return PatchResult.Error("File already exists: ${patch.path}")
                    }
                    val success = workspace.writeFile(patch.path, patch.newText.orEmpty())
                    return if (success) PatchResult.Success else PatchResult.Error("Failed to write new file: ${patch.path}")
                }
                FilePatchAction.MODIFY -> {
                    val currentContent = workspace.readFile(patch.path)
                        ?: return PatchResult.Error("Cannot modify non-existent file: ${patch.path}")

                    val oldText = patch.oldText
                    val newText = patch.newText.orEmpty()
                    val updatedContent = if (!oldText.isNullOrEmpty() && currentContent.contains(oldText)) {
                        currentContent.replaceOnce(oldText, newText)
                    } else if (oldText.isNullOrEmpty() && patch.replaceWholeFile) {
                        newText
                    } else {
                        return PatchResult.Error("Patch conflict: oldText was not found exactly in ${patch.path}")
                    }

                    val success = workspace.writeFile(patch.path, updatedContent)
                    return if (success) PatchResult.Success else PatchResult.Error("Failed to write modifications to: ${patch.path}")
                }
                FilePatchAction.DELETE -> {
                    if (!patch.deleteConfirmed) {
                        return PatchResult.Error("Delete requires second confirmation: ${patch.path}")
                    }
                    val success = workspace.deleteFile(patch.path)
                    return if (success) PatchResult.Success else PatchResult.Error("Failed to delete file: ${patch.path}")
                }
            }
        } catch (e: Exception) {
            return PatchResult.Error("Unexpected error applying patch: ${e.message}")
        }
    }

    fun undoPatch(workspace: DocumentFileWorkspace, patch: FilePatch): Boolean {
        return try {
            workspace.restoreBackup(patch.path)
        } catch (e: Exception) {
            false
        }
    }

    private fun String.replaceOnce(target: String, replacement: String): String {
        val index = this.indexOf(target)
        if (index == -1) return this
        return this.substring(0, index) + replacement + this.substring(index + target.length)
    }
}
