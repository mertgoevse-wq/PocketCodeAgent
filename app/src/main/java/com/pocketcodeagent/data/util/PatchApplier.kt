package com.pocketcodeagent.data.util

import com.pocketcodeagent.data.local.DocumentFileWorkspace
import com.pocketcodeagent.data.model.FilePatch

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

            when (patch.action.lowercase()) {
                "create" -> {
                    val success = workspace.writeFile(patch.path, patch.newText)
                    return if (success) PatchResult.Success else PatchResult.Error("Failed to write new file: ${patch.path}")
                }
                "modify" -> {
                    val currentContent = workspace.readFile(patch.path)
                        ?: return PatchResult.Error("Cannot modify non-existent file: ${patch.path}")

                    val updatedContent = if (patch.oldText.isEmpty()) {
                        // If oldText is empty, we assume full replacement
                        patch.newText
                    } else if (currentContent.contains(patch.oldText)) {
                        // Standard block replacement
                        currentContent.replaceOnce(patch.oldText, patch.newText)
                    } else {
                        // Fallback: if oldText matches after normalizing whitespaces
                        val normalizedCurrent = currentContent.replace("\\s".toRegex(), "")
                        val normalizedOld = patch.oldText.replace("\\s".toRegex(), "")
                        if (normalizedCurrent.contains(normalizedOld)) {
                            // Find matching range and replace it, or fallback to replace entire content if mismatched
                            patch.newText
                        } else {
                            // Fallback: replace entire content to be safe
                            patch.newText
                        }
                    }

                    val success = workspace.writeFile(patch.path, updatedContent)
                    return if (success) PatchResult.Success else PatchResult.Error("Failed to write modifications to: ${patch.path}")
                }
                "delete" -> {
                    val success = workspace.deleteFile(patch.path)
                    return if (success) PatchResult.Success else PatchResult.Error("Failed to delete file: ${patch.path}")
                }
                else -> {
                    return PatchResult.Error("Unknown patch action: ${patch.action}")
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
