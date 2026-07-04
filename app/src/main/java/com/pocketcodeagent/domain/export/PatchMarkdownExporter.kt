package com.pocketcodeagent.domain.export

import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.domain.context.ContextSanitizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PatchMarkdownExporter {

    /**
     * Export a list of file patches as a sanitized Markdown document.
     */
    fun exportToMarkdown(patches: List<FilePatch>): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        sb.appendLine("# PocketCodeAgent — Patch Export")
        sb.appendLine()
        sb.appendLine("> Exportiert am ${dateFormat.format(Date())}")
        sb.appendLine("> ${patches.size} Änderung(en)")
        sb.appendLine()

        patches.forEach { patch ->
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("## `${patch.path}`")
            sb.appendLine()
            sb.appendLine("| Feld | Wert |")
            sb.appendLine("|------|------|")
            sb.appendLine("| Aktion | `${patch.action.name}` |")
            sb.appendLine("| Status | `${patch.status.name}` |")
            sb.appendLine("| Quelle | `${patch.source.name}` |")
            sb.appendLine("| Erstellt | ${dateFormat.format(Date(patch.createdAt))} |")
            sb.appendLine("| Zeilen hinzugefügt | ${patch.additions ?: "-"} |")
            sb.appendLine("| Zeilen entfernt | ${patch.deletions ?: "-"} |")
            sb.appendLine()

            // Error message if any
            patch.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                sb.appendLine("> ⚠️ Fehler: $error")
                sb.appendLine()
            }

            // Diff
            val oldLines = patch.oldText?.lines().orEmpty()
            val newLines = patch.newText?.lines().orEmpty()
            val hasDiff = oldLines.isNotEmpty() || newLines.isNotEmpty()

            if (hasDiff) {
                sb.appendLine("```diff")
                oldLines.forEach { line ->
                    sb.appendLine("- ${sanitizeLine(line)}")
                }
                newLines.forEach { line ->
                    sb.appendLine("+ ${sanitizeLine(line)}")
                }
                sb.appendLine("```")
            } else if (patch.newText != null) {
                sb.appendLine("```")
                sb.appendLine(sanitizeLine(patch.newText))
                sb.appendLine("```")
            }

            sb.appendLine()
        }

        return sb.toString()
    }

    private fun sanitizeLine(line: String): String {
        return ContextSanitizer.redactSummary(line)
    }
}
