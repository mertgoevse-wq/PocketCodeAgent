package com.pocketcodeagent.domain.context

import android.net.Uri
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.data.repository.WorkspaceRepository
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.terminal.TerminalCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkspaceContextBuilder(
    private val workspaceRepository: WorkspaceRepository
) {

    suspend fun buildContext(
        userTask: String,
        selectedRoleId: String,
        selectedSkillId: String?,
        activeFilePath: String?,
        pendingChanges: List<FilePatch>,
        terminalCommands: List<TerminalCommand>,
        previewTarget: PreviewTarget?
    ): WorkspaceContext = withContext(Dispatchers.IO) {
        val workspaceUri = workspaceRepository.workspace.getRootUriString()
        if (workspaceUri == null || workspaceUri.isBlank()) {
            return@withContext WorkspaceContext.empty()
        }

        val allWarnings = mutableListOf<String>()
        val workspaceName = deriveWorkspaceName(workspaceUri)

        // 1. Load file tree
        val allFiles = try {
            if (workspaceUri.startsWith("demo://")) {
                workspaceRepository.workspace.listFiles()
            } else {
                workspaceRepository.loadWorkspaceFiles(Uri.parse(workspaceUri))
            }
        } catch (e: Exception) {
            return@withContext WorkspaceContext(
                workspaceName = workspaceName,
                fileTreeSummary = "File tree unavailable: ${e.message}",
                activeFilePath = activeFilePath,
                activeFileContent = null,
                relevantFiles = emptyList(),
                buildFiles = emptyList(),
                pendingChangesSummary = "",
                terminalQueueSummary = "",
                previewSummary = "",
                warnings = listOf("Failed to load workspace files: ${e.message}"),
                estimatedChars = 0
            )
        }

        // 2. Build file tree summary
        val fileTreeSummary = buildFileTreeSummary(allFiles)

        // 3. Flatten files and score
        val flatFiles = flattenFiles(allFiles)
        val scored: List<ScoredEntry> = flatFiles
            .map { file: FileEntry ->
                val s = FileRelevanceScorer.score(
                    path = file.path,
                    userTask = userTask,
                    roleId = selectedRoleId,
                    skillId = selectedSkillId
                )
                ScoredEntry(file = file, result = s)
            }
            .filter { e: ScoredEntry -> !e.result.ignore }

        // 4. Separate build files from regular files
        val buildFileEntries: List<ScoredEntry> = scored
            .filter { e: ScoredEntry -> e.result.category == "build" }
            .sortedByDescending { e: ScoredEntry -> e.result.priority }
            .take(ContextBudget.MAX_BUILD_FILES)

        val regularFiles: List<ScoredEntry> = scored
            .filter { e: ScoredEntry -> e.result.category != "build" }
            .sortedByDescending { e: ScoredEntry -> e.result.priority }

        // 5. Read build files (limited)
        val buildFileContexts: List<RelevantFileContext> = buildFileEntries.mapNotNull { entry: ScoredEntry ->
            readFileContext(entry.file, entry.result, allWarnings)
        }

        // 6. Read active file content
        val activeFileContext = readActiveFile(activeFilePath, allWarnings)

        // 7. Determine which files are already included
        val includedPaths = mutableSetOf<String>()
        buildFileContexts.forEach { ctx: RelevantFileContext -> includedPaths.add(ctx.path) }
        if (activeFilePath != null) includedPaths.add(activeFilePath)

        // 8. Read top relevant files (excluding build files and active file)
        val relevantFiles: List<RelevantFileContext> = regularFiles
            .filter { entry: ScoredEntry -> entry.file.path !in includedPaths }
            .take(ContextBudget.MAX_RELEVANT_FILES)
            .mapNotNull { entry: ScoredEntry ->
                readFileContext(entry.file, entry.result, allWarnings)
            }

        // 9. Build summary strings
        val pendingChangesSummary = buildPendingChangesSummary(pendingChanges)
        val terminalQueueSummary = buildTerminalQueueSummary(terminalCommands)
        val previewSummary = buildPreviewSummary(previewTarget)

        // 10. Estimate total chars
        val context = WorkspaceContext(
            workspaceName = workspaceName,
            fileTreeSummary = fileTreeSummary,
            activeFilePath = activeFilePath,
            activeFileContent = activeFileContext?.content,
            relevantFiles = relevantFiles,
            buildFiles = buildFileContexts,
            pendingChangesSummary = ContextSanitizer.redactSummary(pendingChangesSummary),
            terminalQueueSummary = terminalQueueSummary,
            previewSummary = previewSummary,
            warnings = allWarnings,
            estimatedChars = 0
        )
        val estimated = context.toPromptString().length
        context.copy(estimatedChars = estimated)
    }

    private fun flattenFiles(files: List<WorkspaceFile>, parentPath: String = ""): List<FileEntry> {
        val result = mutableListOf<FileEntry>()
        for (file in files) {
            val path = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            if (file.isDirectory) {
                if (!FileRelevanceScorer.shouldIgnoreFileName(file.name)) {
                    result.addAll(flattenFiles(file.children, path))
                }
            } else {
                if (!FileRelevanceScorer.shouldIgnoreFileName(file.name)) {
                    result.add(FileEntry(path = path, uriString = file.uriString, sizeBytes = file.size))
                }
            }
        }
        return result
    }

    private fun buildFileTreeSummary(files: List<WorkspaceFile>): String {
        val builder = StringBuilder()
        appendTree(builder, files, "")
        val result = builder.toString()
        return if (result.length > ContextBudget.SUMMARY_MAX_CHARS) {
            result.take(ContextBudget.SUMMARY_MAX_CHARS) + "\n... (tree truncated)"
        } else {
            result
        }
    }

    private fun appendTree(builder: StringBuilder, files: List<WorkspaceFile>, indent: String) {
        val sorted = files.sortedBy { f -> if (f.isDirectory) 0 else 1 }
        for (file in sorted.take(80)) {
            if (FileRelevanceScorer.shouldIgnoreFileName(file.name)) continue
            if (file.isDirectory) {
                builder.append(indent).append("📁 ").append(file.name).append("/\n")
                appendTree(builder, file.children.take(30), "$indent  ")
            } else {
                val sizeStr = formatSize(file.size)
                builder.append(indent).append("📄 ").append(file.name).append(" ($sizeStr)\n")
            }
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

    private fun readFileContext(
        file: FileEntry,
        score: FileRelevanceScorer.RelevanceResult,
        warnings: MutableList<String>
    ): RelevantFileContext? {
        val isLarge = file.sizeBytes > ContextBudget.LARGE_FILE_THRESHOLD_BYTES
        val isHuge = file.sizeBytes > ContextBudget.HUGE_FILE_THRESHOLD_BYTES

        if (isHuge) {
            warnings.add("${file.path} (>500 KB) — path/metadata only, content excluded")
            return RelevantFileContext(
                path = file.path,
                language = extensionFromPath(file.path),
                reason = score.reason,
                content = null,
                sizeBytes = file.sizeBytes,
                truncated = true,
                priority = score.priority
            )
        }

        val rawContent = try {
            workspaceRepository.readFile(file.uriString)
        } catch (e: Exception) {
            warnings.add("Cannot read ${file.path}: ${e.message}")
            return null
        }

        val (sanitized, sanitizeWarnings) = ContextSanitizer.sanitize(file.path, rawContent)
        warnings.addAll(sanitizeWarnings)

        val budget = ContextBudget.RELEVANT_FILE_MAX_CHARS
        val truncated = sanitized.length > budget || isLarge

        return RelevantFileContext(
            path = file.path,
            language = extensionFromPath(file.path),
            reason = score.reason,
            content = sanitized.take(budget),
            sizeBytes = file.sizeBytes,
            truncated = truncated,
            priority = score.priority
        )
    }

    private fun readActiveFile(
        activePath: String?,
        warnings: MutableList<String>
    ): RelevantFileContext? {
        if (activePath == null) return null

        val rootUriString = workspaceRepository.workspace.getRootUriString() ?: return null
        val uriString = try {
            workspaceRepository.getFileUriByRelativePath(rootUriString, activePath)?.toString()
                ?: return null
        } catch (e: Exception) {
            warnings.add("Cannot resolve active file ${activePath}: ${e.message}")
            return null
        }

        val rawContent = try {
            workspaceRepository.readFile(uriString)
        } catch (e: Exception) {
            warnings.add("Cannot read active file ${activePath}: ${e.message}")
            return null
        }

        val (sanitized, sanWarnings) = ContextSanitizer.sanitize(activePath, rawContent)
        warnings.addAll(sanWarnings)

        return RelevantFileContext(
            path = activePath,
            language = extensionFromPath(activePath),
            reason = "Currently open file",
            content = sanitized.take(ContextBudget.ACTIVE_FILE_MAX_CHARS),
            sizeBytes = rawContent.length.toLong(),
            truncated = sanitized.length > ContextBudget.ACTIVE_FILE_MAX_CHARS,
            priority = 100
        )
    }

    private fun buildPendingChangesSummary(changes: List<FilePatch>): String {
        if (changes.isEmpty()) return ""
        val active = changes.filter {
            it.status == com.pocketcodeagent.data.model.FilePatchStatus.PENDING ||
                    it.status == com.pocketcodeagent.data.model.FilePatchStatus.CONFLICT
        }
        if (active.isEmpty()) return ""

        val builder = StringBuilder()
        val displayCount = minOf(active.size, 10)
        active.take(displayCount).forEach { patch ->
            val actionLabel = when (patch.action) {
                com.pocketcodeagent.data.model.FilePatchAction.CREATE -> "CREATE"
                com.pocketcodeagent.data.model.FilePatchAction.MODIFY -> "MODIFY"
                com.pocketcodeagent.data.model.FilePatchAction.DELETE -> "DELETE"
            }
            builder.appendLine("$actionLabel ${patch.path}")
        }
        if (active.size > displayCount) {
            builder.appendLine("... and ${active.size - displayCount} more")
        }
        return builder.toString().let { ContextSanitizer.redactSummary(it) }
    }

    private fun buildTerminalQueueSummary(commands: List<TerminalCommand>): String {
        if (commands.isEmpty()) return ""
        val active = commands.filter {
            it.status == com.pocketcodeagent.domain.terminal.CommandStatus.QUEUED ||
                    it.status == com.pocketcodeagent.domain.terminal.CommandStatus.COPIED
        }
        if (active.isEmpty()) return ""
        val builder = StringBuilder()
        val displayCount = minOf(active.size, 5)
        active.take(displayCount).forEach { cmd ->
            val riskLabel = when (cmd.riskLevel) {
                com.pocketcodeagent.domain.agent.CommandRiskLevel.SAFE -> "SAFE"
                com.pocketcodeagent.domain.agent.CommandRiskLevel.CAUTION -> "CAUTION"
                com.pocketcodeagent.domain.agent.CommandRiskLevel.BLOCKED -> "BLOCKED"
            }
            builder.appendLine("[$riskLabel] ${cmd.safeDisplay}")
        }
        if (active.size > displayCount) {
            builder.appendLine("... and ${active.size - displayCount} more")
        }
        return builder.toString()
    }

    private fun buildPreviewSummary(previewTarget: PreviewTarget?): String {
        if (previewTarget == null || previewTarget is PreviewTarget.None) return ""
        return when (previewTarget) {
            is PreviewTarget.WorkspaceStatic -> "Workspace (index.html)"
            is PreviewTarget.File -> "File: ${previewTarget.fileName}"
            is PreviewTarget.Url -> "URL: ${previewTarget.url}"
            else -> ""
        }
    }

    private fun deriveWorkspaceName(uri: String): String {
        return try {
            val parsed = Uri.parse(uri)
            parsed.lastPathSegment ?: uri
        } catch (e: Exception) {
            uri
        }
    }

    private fun extensionFromPath(path: String): String {
        return path.substringAfterLast('.', "").lowercase()
    }

    private data class FileEntry(
        val path: String,
        val uriString: String,
        val sizeBytes: Long
    )

    private data class ScoredEntry(
        val file: FileEntry,
        val result: FileRelevanceScorer.RelevanceResult
    )
}
