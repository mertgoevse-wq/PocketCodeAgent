package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.AgentCommand
import com.pocketcodeagent.data.model.AgentResponse
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.data.network.ApiClient
import com.google.gson.Gson
import com.pocketcodeagent.domain.agent.AgentAction
import com.pocketcodeagent.domain.agent.AgentActionParser
import com.pocketcodeagent.domain.agent.AgentArtifact
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class AgentRepository(
    private val providerRepository: ProviderRepository,
    private val workspaceRepository: WorkspaceRepository
) {

    private fun getSystemPrompt(role: AgentRole, workspaceContext: String, agentMode: AgentMode): String {
        val basePrompt = """
            You are a subagent inside the PocketCodeAgent Android App.
            The user is running you locally on their phone without root access.
            Currently selected project files:
            $workspaceContext
            
            Always keep Android device sandbox limits in mind. No root available.
            Never request, reveal, echo, or log API keys, Authorization headers, tokens, or secrets.
            Android app projects must not create folders named example, sample, demo, playground, starter, or template.
            Do not use com.example packages.
        """.trimIndent()

        val modePrompt = when (agentMode) {
            AgentMode.DISCUSS -> """

                Current agent mode: DISCUSS.
                Do not emit pocketArtifact or pocketAction blocks.
                Do not propose file actions or shell command actions.
                Explain, plan, and ask clarifying questions when needed.
            """.trimIndent()
            AgentMode.BUILD -> """

                Current agent mode: BUILD.
                When proposing changes, use this exact artifact format:
                <pocketArtifact title="Short title">
                  <pocketAction type="file" filePath="relative/path.ext">
                    full file content
                  </pocketAction>
                  <pocketAction type="modify" filePath="relative/path.ext" oldText="exact old text if known">
                    replacement text
                  </pocketAction>
                  <pocketAction type="shell">
                    command only
                  </pocketAction>
                  <pocketAction type="preview">
                    http://127.0.0.1:5173
                  </pocketAction>
                  <pocketAction type="note">
                    short note
                  </pocketAction>
                </pocketArtifact>
                Do not merely describe file changes; put them in pocketAction blocks.
                Shell commands are only suggestions. Never claim they were executed.
                Do not suggest dangerous commands, destructive commands, sudo/su, rm -rf, curl|sh, wget|sh, or encoded PowerShell.
                All file changes must be reviewable; do not imply automatic application.
            """.trimIndent()
        }

        return when (role) {
            AgentRole.PLANNER -> """
                $basePrompt
                $modePrompt
                Your task is to analyze the user request and create a detailed checklist execution plan.
                Do not write code. Outline the files that need to be created or modified, and details on what changes to perform.
                Output your plan clearly using Markdown.
            """.trimIndent()

            AgentRole.CODER -> """
                $basePrompt
                $modePrompt
                Your task is to implement the plan by suggesting code changes.
                In BUILD mode, emit pocketArtifact/pocketAction blocks for every file, command, preview, or note action.
                In DISCUSS mode, explain the implementation approach without pocketActions.
            """.trimIndent()

            AgentRole.REVIEWER -> """
                $basePrompt
                $modePrompt
                Your task is to review the proposed code changes.
                Ensure there are no compilation errors, syntax errors, or logical bugs.
                Output your review in markdown. State clearly if you APPROVE or REJECT the changes with reasons.
            """.trimIndent()

            AgentRole.FIXER -> """
                $basePrompt
                $modePrompt
                Your task is to fix any compilation or runtime errors reported by the user.
                In BUILD mode, emit pocketArtifact/pocketAction blocks for every suggested fix.
                In DISCUSS mode, explain the likely cause and fix plan without pocketActions.
            """.trimIndent()

            AgentRole.PREVIEW -> """
                $basePrompt
                $modePrompt
                Your task is to inspect the project structure and suggest previews.
                If there is an index.html, recommend viewing it.
                If this is a Node/Vite/React project, explain that local Node execution needs Termux bridge (e.g. running 'npm run dev' on local port 5173).
                Provide clean step-by-step preview guidelines.
            """.trimIndent()

            AgentRole.TERMINAL -> """
                $basePrompt
                $modePrompt
                Your task is to recommend shell commands (e.g., git status, gradle build, npm install) that the user should execute.
                In BUILD mode, wrap each command in a pocketAction type="shell" block.
                In DISCUSS mode, explain commands in prose only.
                Never suggest destructive commands. Keep commands safe.
            """.trimIndent()
        }
    }

    fun runAgent(
        provider: Provider,
        role: AgentRole,
        agentMode: AgentMode,
        history: List<ChatMessage>,
        rootUriString: String?,
        onChunk: (String) -> Unit
    ) = flow {
        if (provider.id == 999) {
            // Simulated demo agent run
            val mockChunks = when(role) {
                AgentRole.PLANNER -> listOf(
                    "### 📋 Demo-Ausführungsplan\n\n",
                    "1. Analysiere das Projekt.\n",
                    "2. Aktualisiere `script.js` mit einer neuen Testfunktion.\n",
                    "3. Zeige die Diffs im Review-Screen an.\n\n",
                    "Klicke auf den Coder-Agenten, um diesen Plan umzusetzen! 🤖"
                )
                AgentRole.CODER -> listOf(
                    "{\n",
                    "  \"summary\": \"Ich habe script.js aktualisiert, um eine Testfunktion hinzuzufügen.\",\n",
                    "  \"patches\": [\n",
                    "    {\n",
                    "      \"path\": \"script.js\",\n",
                    "      \"action\": \"modify\",\n",
                    "      \"oldText\": \"function testDemo() {\\n    return \\\"Demo Mode Active\\\";\\n}\",\n",
                    "      \"newText\": \"function testDemo() {\\n    console.log(\\\"Demo-Aufruf erfolgreich!\\\");\\n    return \\\"Demo Mode Active (Updated)\\\";\\n}\"\n",
                    "    }\n",
                    "  ],\n",
                    "  \"commands\": [\n",
                    "    {\n",
                    "      \"command\": \"node script.js\",\n",
                    "      \"reason\": \"Führe das Skript aus, um die Ausgabe zu prüfen.\",\n",
                    "      \"requiresConfirmation\": true\n",
                    "    }\n",
                    "  ]\n",
                    "}"
                )
                else -> listOf("Erfolgreicher Demo-Lauf für Rolle: ${role.displayName}")
            }

            var fullText = ""
            for (chunk in mockChunks) {
                kotlinx.coroutines.delay(100)
                onChunk(chunk)
                fullText += chunk
            }
            
            val artifacts = artifactsFor(fullText, agentMode)
            val proposedPatches = actionsToPatches(artifacts.flatMap { it.actions })
            val proposedCommands = actionsToCommands(artifacts.flatMap { it.actions })
            val displayedMessage = displayMessageFor(fullText, artifacts)

            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = displayedMessage,
                    isAgent = true,
                    agentRole = role,
                    proposedPatches = proposedPatches,
                    proposedCommands = proposedCommands,
                    artifacts = artifacts
                )
            )
            return@flow
        }

        if (!provider.hasRequiredConfiguration()) {
            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = "Provider nicht konfiguriert. Bitte API-Key, Base URL und Modell in den Provider-Settings speichern und die Verbindung testen.",
                    isAgent = true,
                    agentRole = role
                )
            )
            return@flow
        }

        // 1. Gather workspace file list for context
        val workspaceContext = if (rootUriString != null) {
            try {
                val files = withContext(Dispatchers.IO) {
                    workspaceRepository.loadWorkspaceFiles(android.net.Uri.parse(rootUriString))
                }
                formatFilesContext(files)
            } catch (e: Exception) {
                "No workspace selected or read permission missing."
            }
        } else {
            "No workspace selected."
        }

        // 2. Prepare chat messages history
        val apiMessages = mutableListOf<Map<String, String>>()
        apiMessages.add(mapOf("role" to "system", "content" to getSystemPrompt(role, workspaceContext, agentMode)))
        
        for (msg in history) {
            val roleName = if (msg.isAgent) "assistant" else "user"
            apiMessages.add(mapOf("role" to roleName, "content" to msg.message))
        }

        providerRepository.log(role.displayName, "Executing role loop using provider: ${provider.name}")

        // 3. Make streaming call
        val result = ApiClient.chatCompletionStream(provider, apiMessages) { chunk ->
            onChunk(chunk)
        }

        if (result.isSuccess) {
            val fullText = result.getOrThrow()
            providerRepository.log(role.displayName, "Completed execution successfully.")
            
            val artifacts = artifactsFor(fullText, agentMode)
            val proposedPatches = actionsToPatches(artifacts.flatMap { it.actions })
            val proposedCommands = actionsToCommands(artifacts.flatMap { it.actions })
            val displayedMessage = displayMessageFor(fullText, artifacts)

            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = displayedMessage,
                    isAgent = true,
                    agentRole = role,
                    proposedPatches = proposedPatches,
                    proposedCommands = proposedCommands,
                    artifacts = artifacts
                )
            )
        } else {
            val exception = result.exceptionOrNull()
            val safeMessage = exception?.message?.takeIf { it.isNotBlank() }
                ?: "${provider.displayName}: Anfrage fehlgeschlagen."
            providerRepository.log(role.displayName, "Agent request failed: $safeMessage", "ERROR")
            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = safeMessage,
                    isAgent = true,
                    agentRole = role
                )
            )
        }
    }

    private fun formatFilesContext(files: List<WorkspaceFile>, indent: String = ""): String {
        val builder = StringBuilder()
        for (file in files) {
            if (file.isDirectory) {
                builder.append(indent).append("📁 ").append(file.name).append("/\n")
                builder.append(formatFilesContext(file.children, "$indent  "))
            } else {
                builder.append(indent).append("📄 ").append(file.name).append(" (${file.size} bytes)\n")
            }
        }
        return builder.toString()
    }

    private fun parseAgentResponse(text: String): AgentResponse? {
        return try {
            var jsonText = text.trim()
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.removePrefix("```json")
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.removePrefix("```")
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.removeSuffix("```")
            }
            jsonText = jsonText.trim()
            Gson().fromJson(jsonText, AgentResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun artifactsFor(text: String, agentMode: AgentMode): List<AgentArtifact> {
        val parsed = AgentActionParser.parse(text)
        return if (agentMode == AgentMode.DISCUSS) {
            val hasNonNoteActions = parsed.flatMap { it.actions }.any { it !is AgentAction.Note }
            parsed.map { artifact ->
                AgentArtifact(
                    id = artifact.id,
                    title = artifact.title,
                    actions = artifact.actions.filterIsInstance<AgentAction.Note>().ifEmpty {
                        listOf(AgentAction.Note("note-${java.util.UUID.randomUUID()}", text))
                    },
                    rawText = artifact.rawText,
                    parseWarnings = artifact.parseWarnings + if (hasNonNoteActions) {
                        listOf("DISCUSS mode ignoriert vorgeschlagene Datei-, Command- oder Preview-Actions.")
                    } else {
                        emptyList()
                    }
                )
            }
        } else {
            parsed
        }
    }

    private fun displayMessageFor(fullText: String, artifacts: List<AgentArtifact>): String {
        val noteText = artifacts
            .flatMap { it.actions }
            .filterIsInstance<AgentAction.Note>()
            .joinToString("\n\n") { it.text }
            .takeIf { it.isNotBlank() }
        return noteText ?: fullText
    }

    private fun actionsToPatches(actions: List<AgentAction>): List<FilePatch> {
        return actions.mapNotNull { action ->
            when (action) {
                is AgentAction.CreateFile -> FilePatch(
                    path = action.path,
                    action = FilePatchAction.CREATE,
                    newText = action.content,
                    additions = action.content.lines().size,
                    deletions = 0
                )
                is AgentAction.ModifyFile -> FilePatch(
                    path = action.path,
                    action = FilePatchAction.MODIFY,
                    oldText = action.oldText,
                    newText = action.newText,
                    additions = action.newText.lines().size,
                    deletions = action.oldText?.lines()?.size,
                    replaceWholeFile = action.oldText.isNullOrBlank()
                )
                is AgentAction.DeleteFile -> FilePatch(
                    path = action.path,
                    action = FilePatchAction.DELETE,
                    requiresSecondConfirmation = true,
                    additions = 0
                )
                else -> null
            }
        }
    }

    private fun actionsToCommands(actions: List<AgentAction>): List<AgentCommand> {
        return actions.filterIsInstance<AgentAction.RunCommand>()
            .filter { it.riskLevel != CommandRiskLevel.BLOCKED }
            .map { action ->
            AgentCommand(
                command = action.command,
                reason = action.reason ?: action.safeSummary,
                requiresConfirmation = true
            )
        }
    }

    private fun parseRegexFileChanges(rootUriString: String?, text: String): List<FilePatch> {
        if (rootUriString == null) return emptyList()
        val patches = mutableListOf<FilePatch>()
        val pattern = Pattern.compile("<<<<\\s*FILE:\\s*([^>]+)\\s*>>>>(.*?)<<<<\\s*END\\s*>>>>", Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            val path = matcher.group(1)?.trim().orEmpty()
            val content = matcher.group(2).orEmpty()
            if (path.isBlank()) continue
            
            val originalContent = try {
                val fileUri = workspaceRepository.getFileUriByRelativePath(rootUriString, path)
                if (fileUri != null) {
                    workspaceRepository.readFile(fileUri.toString())
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
            
            patches.add(
                FilePatch(
                    path = path,
                    action = if (originalContent.isEmpty()) FilePatchAction.CREATE else FilePatchAction.MODIFY,
                    oldText = originalContent.ifEmpty { null },
                    newText = content,
                    additions = content.lines().size,
                    deletions = originalContent.ifEmpty { null }?.lines()?.size,
                    replaceWholeFile = originalContent.isNotEmpty()
                )
            )
        }
        return patches
    }

    private fun parseRegexCommands(text: String): List<AgentCommand> {
        val commands = mutableListOf<AgentCommand>()
        val pattern = Pattern.compile("<<<<\\s*CMD:\\s*([^>]+)\\s*>>>>")
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val command = matcher.group(1)?.trim().orEmpty()
            if (command.isBlank()) continue
            commands.add(
                AgentCommand(
                    command = command,
                    reason = "Recommended by agent",
                    requiresConfirmation = true
                )
            )
        }
        return commands
    }
}
