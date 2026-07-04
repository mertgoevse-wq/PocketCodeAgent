package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.FilePatchAction
import com.pocketcodeagent.data.model.AgentCommand
import com.pocketcodeagent.data.model.AgentResponse
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.ProviderTestStatus
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.data.network.ApiClient
import com.google.gson.Gson
import com.pocketcodeagent.domain.agent.AgentAction
import com.pocketcodeagent.domain.agent.AgentActionParser
import com.pocketcodeagent.domain.agent.AgentArtifact
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.CommandRiskLevel
import com.pocketcodeagent.domain.agent.registry.AgentRegistry
import com.pocketcodeagent.domain.agent.registry.AgentRolePromptBuilder
import com.pocketcodeagent.domain.agent.registry.RichAgentRole
import com.pocketcodeagent.domain.context.WorkspaceContext
import com.pocketcodeagent.domain.context.WorkspaceContextBuilder
import com.pocketcodeagent.domain.preview.PreviewTarget
import com.pocketcodeagent.domain.terminal.TerminalCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class AgentRepository(
    private val providerRepository: ProviderRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    private val contextBuilder = WorkspaceContextBuilder(workspaceRepository)

    suspend fun buildWorkspaceContext(
        userTask: String,
        selectedRoleId: String,
        selectedSkillId: String?,
        activeFilePath: String?,
        pendingChanges: List<FilePatch>,
        terminalCommands: List<TerminalCommand>,
        previewTarget: PreviewTarget?
    ): WorkspaceContext {
        return contextBuilder.buildContext(
            userTask = userTask,
            selectedRoleId = selectedRoleId,
            selectedSkillId = selectedSkillId,
            activeFilePath = activeFilePath,
            pendingChanges = pendingChanges,
            terminalCommands = terminalCommands,
            previewTarget = previewTarget
        )
    }

    private fun getSystemPrompt(role: RichAgentRole, context: WorkspaceContext, agentMode: AgentMode): String {
        return AgentRolePromptBuilder.build(role, agentMode, context)
    }

    /** Legacy support: maps old AgentRole enum to RichAgentRole via AgentRegistry. */
    private fun getSystemPrompt(role: AgentRole, context: WorkspaceContext, agentMode: AgentMode): String {
        return getSystemPrompt(AgentRegistry.fromLegacy(role), context, agentMode)
    }

    fun runAgent(
        provider: Provider,
        role: AgentRole,
        agentMode: AgentMode,
        history: List<ChatMessage>,
        rootUriString: String?,
        workspaceContext: WorkspaceContext? = null,
        onChunk: (String) -> Unit
    ) = flow {
        if (provider.id == 999) {
            // Simulated demo agent run
            val lastUserMsg = history.lastOrNull { !it.isAgent }?.message?.lowercase().orEmpty()
            val isWebAppBuild = agentMode == AgentMode.BUILD && lastUserMsg.containsAny(
                "web app", "html", "test", "preview", "landing page", "static", "website", "webseite"
            )

            if (isWebAppBuild) {
                val mockChunks = demoWebAppResponse()
                var fullText = ""
                for (chunk in mockChunks) {
                    kotlinx.coroutines.delay(60)
                    onChunk(chunk)
                    fullText += chunk
                }
                val artifacts = artifactsFor(fullText, agentMode)
                val proposedPatches = actionsToPatches(artifacts.flatMap { it.actions })
                val proposedCommands = actionsToCommands(artifacts.flatMap { it.actions })
                val displayedMessage = displayMessageFor(fullText, artifacts)
                emit(
                    ChatMessage(
                        sender = "${role.displayName} (Demo)",
                        message = "[Demo-Agent — Offline demo, keine echte API.]\n\n$displayedMessage",
                        isAgent = true,
                        agentRole = role,
                        proposedPatches = proposedPatches,
                        proposedCommands = proposedCommands,
                        artifacts = artifacts
                    )
                )
                return@flow
            }

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
                    sender = "${role.displayName} (Demo)",
                    message = "[Demo-Agent: Diese Antwort nutzt keine echte API.]\n\n$displayedMessage",
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

        // 1. Build or use provided workspace context
        val context = workspaceContext ?: buildFallbackContext(rootUriString)

        // 2. Prepare chat messages history
        val apiMessages = mutableListOf<Map<String, String>>()
        apiMessages.add(mapOf("role" to "system", "content" to getSystemPrompt(role, context, agentMode)))
        
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
            updateProviderStatusOnError(provider, safeMessage)
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

    private suspend fun updateProviderStatusOnError(provider: Provider, safeMessage: String) {
        if (provider.id > 0) {
            val updated = provider.copy(
                lastTestStatus = ProviderTestStatus.ERROR,
                lastErrorSanitized = safeMessage.take(200)
            )
            providerRepository.saveProvider(updated)
        }
    }

    private fun buildFallbackContext(rootUriString: String?): WorkspaceContext {
        if (rootUriString == null) return WorkspaceContext.empty()
        return try {
            val files = workspaceRepository.loadWorkspaceFiles(android.net.Uri.parse(rootUriString))
            val treeSummary = AgentRolePromptBuilder.formatFilesContext(files)
            WorkspaceContext(
                workspaceName = rootUriString.substringAfterLast("/"),
                fileTreeSummary = treeSummary,
                activeFilePath = null,
                activeFileContent = null,
                relevantFiles = emptyList(),
                buildFiles = emptyList(),
                pendingChangesSummary = "",
                terminalQueueSummary = "",
                previewSummary = "",
                warnings = emptyList(),
                estimatedChars = treeSummary.length
            )
        } catch (e: Exception) {
            WorkspaceContext(
                workspaceName = null,
                fileTreeSummary = "No workspace selected or read permission missing.",
                activeFilePath = null,
                activeFileContent = null,
                relevantFiles = emptyList(),
                buildFiles = emptyList(),
                pendingChangesSummary = "",
                terminalQueueSummary = "",
                previewSummary = "",
                warnings = listOf("Failed to load workspace: ${e.message}"),
                estimatedChars = 0
            )
        }
    }

    private fun formatFilesContext(files: List<WorkspaceFile>, indent: String = ""): String {
        return AgentRolePromptBuilder.formatFilesContext(files, indent)
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

    // ─── Demo Web App Response ────────────────────────────────────────────────

    private fun demoWebAppResponse(): List<String> {
        val html = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PocketCodeAgent Demo</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <div class="container">
    <header>
      <h1>PocketCodeAgent Demo</h1>
      <p class="subtitle">Offline Preview — Build Mode Active</p>
    </header>
    <main>
      <div class="card">
        <h2>Demo Web App</h2>
        <p>Diese Seite wurde vom Demo-Agenten erstellt.</p>
        <button id="counterBtn" class="btn">Klicks: <span id="count">0</span></button>
        <p id="status" class="status">Bereit.</p>
      </div>
    </main>
    <footer>
      <p>PocketCodeAgent · Offline Demo · Keine echte API</p>
    </footer>
  </div>
  <script src="app.js"></script>
</body>
</html>"""

        val css = """* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  background: #0e0e10;
  color: #f8f9fe;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

.container {
  max-width: 480px;
  width: 100%;
  padding: 24px;
}

header {
  text-align: center;
  margin-bottom: 32px;
}

header h1 {
  font-size: 1.6rem;
  font-weight: 700;
  color: #5e72e4;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 0.8rem;
  color: #666670;
  margin-top: 4px;
}

.card {
  background: #18181c;
  border-radius: 12px;
  padding: 28px;
  text-align: center;
  border: 1px solid #2d2d34;
}

.card h2 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 8px;
}

.card p {
  font-size: 0.85rem;
  color: #adb5bd;
  margin-bottom: 16px;
}

.btn {
  background: #5e72e4;
  color: #fff;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
  min-width: 160px;
}

.btn:hover {
  background: #4a5ecc;
}

.btn:active {
  background: #3d4fad;
  transform: scale(0.98);
}

.status {
  font-size: 0.75rem;
  color: #2dce89;
  margin-top: 12px;
  font-weight: 500;
}

footer {
  text-align: center;
  margin-top: 32px;
  font-size: 0.7rem;
  color: #444450;
}
"""

        val js = """document.addEventListener('DOMContentLoaded', function() {
  var count = 0;
  var btn = document.getElementById('counterBtn');
  var countSpan = document.getElementById('count');
  var statusEl = document.getElementById('status');

  btn.addEventListener('click', function() {
    count++;
    countSpan.textContent = count;
    if (count === 1) {
      statusEl.textContent = 'Erster Klick!';
    } else if (count === 5) {
      statusEl.textContent = 'Fuenf Klicks — Preview funktioniert!';
    } else if (count === 10) {
      statusEl.textContent = 'Zehn Klicks! Demo Mode aktiv.';
    } else {
      statusEl.textContent = 'Klicks: ' + count;
    }
  });
});
"""

        val xml = """<pocketArtifact title="Demo Web App">
<pocketAction type="file" filePath="index.html">
${html}
</pocketAction>
<pocketAction type="file" filePath="styles.css">
${css}
</pocketAction>
<pocketAction type="file" filePath="app.js">
${js}
</pocketAction>
<pocketAction type="note">
Demo-Web-App erstellt mit index.html, styles.css und app.js.

Vorschau: Wechsle zum Preview-Tab und waehle Workspace Preview.

Optional (nur als Vorschlag): Falls Termux installiert ist, kannst du einen Dev-Server starten:
  cd zum Workspace-Ordner und fuehre 'npm run dev' oder 'npx serve .' aus.
  Keine automatische Ausfuehrung — nur manuell im Terminal-Tab.
</pocketAction>
</pocketArtifact>"""

        // Return as chunks for streaming simulation
        return xml.chunked(80) + listOf("")
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
