package com.pocketcodeagent.domain.agent

sealed class AgentAction {
    abstract val id: String
    abstract val typeLabel: String
    abstract val userVisibleTitle: String
    abstract val safeSummary: String

    data class Note(
        override val id: String,
        val text: String
    ) : AgentAction() {
        override val typeLabel: String = "Note"
        override val userVisibleTitle: String = "Notiz"
        override val safeSummary: String = sanitizeSummary(text)
    }

    data class CreateFile(
        override val id: String,
        val path: String,
        val content: String
    ) : AgentAction() {
        override val typeLabel: String = "Create File"
        override val userVisibleTitle: String = "Datei erstellen: $path"
        override val safeSummary: String = "${content.lineCount()} Zeilen, ${content.length} Zeichen"
    }

    data class ModifyFile(
        override val id: String,
        val path: String,
        val oldText: String?,
        val newText: String
    ) : AgentAction() {
        override val typeLabel: String = "Modify File"
        override val userVisibleTitle: String = "Datei aendern: $path"
        override val safeSummary: String = if (oldText.isNullOrBlank()) {
            "Neuer Inhalt ohne oldText-Abgleich, Review erforderlich"
        } else {
            "Ersetzt ${oldText.lineCount()} Zeilen durch ${newText.lineCount()} Zeilen"
        }
    }

    data class DeleteFile(
        override val id: String,
        val path: String
    ) : AgentAction() {
        override val typeLabel: String = "Delete File"
        override val userVisibleTitle: String = "Datei loeschen: $path"
        override val safeSummary: String = "Gefaehrliche Dateiaktion, Review und spaeter zweite Bestaetigung erforderlich"
    }

    data class RunCommand(
        override val id: String,
        val command: String,
        val reason: String?,
        val riskLevel: CommandRiskLevel
    ) : AgentAction() {
        override val typeLabel: String = "Command"
        override val userVisibleTitle: String = "Command vorschlagen"
        override val safeSummary: String = sanitizeSummary(reason?.takeIf { it.isNotBlank() } ?: command)
    }

    data class OpenPreview(
        override val id: String,
        val target: String
    ) : AgentAction() {
        override val typeLabel: String = "Preview"
        override val userVisibleTitle: String = "Preview oeffnen"
        override val safeSummary: String = sanitizeSummary(target)
    }
}

private fun String.lineCount(): Int {
    if (isEmpty()) return 0
    return count { it == '\n' } + 1
}

internal fun sanitizeSummary(value: String): String {
    return value
        .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._\\-]+"), "Bearer [redacted]")
        .replace(Regex("(?i)(api[_-]?key|authorization|token|secret)\\s*[:=]\\s*[^\\s,;]+"), "$1=[redacted]")
        .replace(Regex("sk-[A-Za-z0-9]{12,}"), "[redacted]")
        .trim()
        .take(220)
}
