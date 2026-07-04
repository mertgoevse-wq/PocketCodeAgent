package com.pocketcodeagent.domain.context

data class RelevantFileContext(
    val path: String,
    val language: String,
    val reason: String,
    val content: String?,
    val sizeBytes: Long,
    val truncated: Boolean,
    val priority: Int
)

data class WorkspaceContext(
    val workspaceName: String?,
    val fileTreeSummary: String,
    val activeFilePath: String?,
    val activeFileContent: String?,
    val relevantFiles: List<RelevantFileContext>,
    val buildFiles: List<RelevantFileContext>,
    val pendingChangesSummary: String,
    val terminalQueueSummary: String,
    val previewSummary: String,
    val warnings: List<String>,
    val estimatedChars: Int
) {
    fun toPromptString(): String = buildString {
        appendLine("## Project: ${workspaceName ?: "No workspace"}")

        if (activeFilePath != null) {
            appendLine()
            appendLine("### Open File: $activeFilePath")
            if (activeFileContent != null) {
                appendLine("```${languageFromPath(activeFilePath)}")
                append(activeFileContent.take(ContextBudget.ACTIVE_FILE_MAX_CHARS))
                if (activeFileContent.length > ContextBudget.ACTIVE_FILE_MAX_CHARS) {
                    append("\n// ... truncated (${activeFileContent.length} total chars)")
                }
                appendLine()
                appendLine("```")
            }
        }

        if (fileTreeSummary.isNotBlank() && fileTreeSummary != "No workspace selected.") {
            appendLine()
            appendLine("### Project Structure")
            append(fileTreeSummary)
        }

        if (buildFiles.isNotEmpty()) {
            appendLine()
            appendLine("### Build Configuration")
            buildFiles.forEach { bf ->
                appendLine("-- ${bf.path} (${bf.reason}) --")
                bf.content?.let { appendLine(it.take(ContextBudget.BUILD_FILE_MAX_CHARS)) }
            }
        }

        if (relevantFiles.isNotEmpty()) {
            appendLine()
            appendLine("### Relevant Files")
            relevantFiles.sortedByDescending { it.priority }.forEach { rf ->
                appendLine("-- ${rf.path} (${rf.reason}) --")
                if (rf.truncated) appendLine("[Truncated from ${rf.sizeBytes} bytes]")
                rf.content?.let { appendLine(it.take(ContextBudget.RELEVANT_FILE_MAX_CHARS)) }
            }
        }

        if (pendingChangesSummary.isNotBlank()) {
            appendLine()
            appendLine("### Pending Changes")
            appendLine(pendingChangesSummary)
        }

        if (terminalQueueSummary.isNotBlank()) {
            appendLine()
            appendLine("### Terminal Queue")
            appendLine(terminalQueueSummary)
        }

        if (previewSummary.isNotBlank()) {
            appendLine()
            appendLine("### Preview Status")
            appendLine(previewSummary)
        }

        if (warnings.isNotEmpty()) {
            appendLine()
            appendLine("### Warnings")
            warnings.forEach { appendLine("- $it") }
        }
    }

    companion object {
        fun empty(): WorkspaceContext = WorkspaceContext(
            workspaceName = null,
            fileTreeSummary = "No workspace selected.",
            activeFilePath = null,
            activeFileContent = null,
            relevantFiles = emptyList(),
            buildFiles = emptyList(),
            pendingChangesSummary = "",
            terminalQueueSummary = "",
            previewSummary = "",
            warnings = emptyList(),
            estimatedChars = 0
        )
    }
}

private fun languageFromPath(path: String): String = when {
    path.endsWith(".kt") -> "kotlin"
    path.endsWith(".kts") -> "kotlin"
    path.endsWith(".java") -> "java"
    path.endsWith(".xml") -> "xml"
    path.endsWith(".json") -> "json"
    path.endsWith(".gradle") -> "groovy"
    path.endsWith(".properties") -> "properties"
    path.endsWith(".html") || path.endsWith(".htm") -> "html"
    path.endsWith(".css") -> "css"
    path.endsWith(".js") -> "javascript"
    path.endsWith(".ts") -> "typescript"
    path.endsWith(".md") -> "markdown"
    path.endsWith(".py") -> "python"
    path.endsWith(".sh") -> "bash"
    path.endsWith(".yaml") || path.endsWith(".yml") -> "yaml"
    else -> ""
}
