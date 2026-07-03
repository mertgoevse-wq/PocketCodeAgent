package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.FilePatch
import com.pocketcodeagent.data.model.AgentCommand
import com.pocketcodeagent.data.model.AgentResponse
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.data.network.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.flow
import java.util.regex.Pattern

class AgentRepository(
    private val providerRepository: ProviderRepository,
    private val workspaceRepository: WorkspaceRepository
) {

    private fun getSystemPrompt(role: AgentRole, workspaceContext: String): String {
        val basePrompt = """
            You are a subagent inside the PocketCodeAgent Android App.
            The user is running you locally on their phone without root access.
            Currently selected project files:
            $workspaceContext
            
            Always keep Android device sandbox limits in mind. No root available.
        """.trimIndent()

        return when (role) {
            AgentRole.PLANNER -> """
                $basePrompt
                Your task is to analyze the user request and create a detailed checklist execution plan.
                Do not write code. Outline the files that need to be created or modified, and details on what changes to perform.
                Output your plan clearly using Markdown.
            """.trimIndent()

            AgentRole.CODER -> """
                $basePrompt
                Your task is to implement the plan by suggesting code changes.
                You MUST respond STRICTLY with a single JSON object matching the format below.
                DO NOT wrap it in HTML/Markdown text other than optional ```json ... ``` blocks.
                DO NOT write conversational explanations outside the JSON. All comments must go into the "summary" field.
                
                Strict JSON Output Format:
                {
                  "summary": "Detailed summary of changes and explanations.",
                  "patches": [
                    {
                      "path": "src/App.tsx",
                      "action": "create|modify|delete",
                      "oldText": "if action is modify, the exact old text block to replace. If create, leave empty.",
                      "newText": "the new text to write or replace with."
                    }
                  ],
                  "commands": [
                    {
                      "command": "npm install",
                      "reason": "Explain why this command is needed",
                      "requiresConfirmation": true
                    }
                  ]
                }
            """.trimIndent()

            AgentRole.REVIEWER -> """
                $basePrompt
                Your task is to review the proposed code changes.
                Ensure there are no compilation errors, syntax errors, or logical bugs.
                Output your review in markdown. State clearly if you APPROVE or REJECT the changes with reasons.
            """.trimIndent()

            AgentRole.FIXER -> """
                $basePrompt
                Your task is to fix any compilation or runtime errors reported by the user.
                You MUST respond STRICTLY with a single JSON object matching the format below.
                DO NOT wrap it in HTML/Markdown text other than optional ```json ... ``` blocks.
                DO NOT write conversational explanations outside the JSON. All comments must go into the "summary" field.
                
                Strict JSON Output Format:
                {
                  "summary": "Detailed summary of the fixes applied.",
                  "patches": [
                    {
                      "path": "src/App.tsx",
                      "action": "create|modify|delete",
                      "oldText": "exact old text block to replace",
                      "newText": "the corrected text"
                    }
                  ],
                  "commands": []
                }
            """.trimIndent()

            AgentRole.PREVIEW -> """
                $basePrompt
                Your task is to inspect the project structure and suggest previews.
                If there is an index.html, recommend viewing it.
                If this is a Node/Vite/React project, explain that local Node execution needs Termux bridge (e.g. running 'npm run dev' on local port 5173).
                Provide clean step-by-step preview guidelines.
            """.trimIndent()

            AgentRole.TERMINAL -> """
                $basePrompt
                Your task is to recommend shell commands (e.g., git status, gradle build, npm install) that the user should execute.
                Wrap each recommended command in:
                <<<< CMD: command_string >>>>
                
                Never suggest destructive commands. Keep commands safe.
            """.trimIndent()
        }
    }

    fun runAgent(
        provider: Provider,
        role: AgentRole,
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
            
            val proposedPatches = mutableListOf<FilePatch>()
            val proposedCommands = mutableListOf<AgentCommand>()
            var displayedMessage = fullText

            if (role == AgentRole.CODER || role == AgentRole.FIXER) {
                val parsed = parseAgentResponse(fullText)
                if (parsed != null) {
                    proposedPatches.addAll(parsed.patches)
                    proposedCommands.addAll(parsed.commands)
                    displayedMessage = parsed.summary
                }
            } else {
                val parsedCmds = parseRegexCommands(fullText)
                proposedCommands.addAll(parsedCmds)
            }

            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = displayedMessage,
                    isAgent = true,
                    agentRole = role,
                    proposedPatches = proposedPatches,
                    proposedCommands = proposedCommands
                )
            )
            return@flow
        }

        // 1. Gather workspace file list for context
        val workspaceContext = if (rootUriString != null) {
            try {
                val files = workspaceRepository.loadWorkspaceFiles(android.net.Uri.parse(rootUriString))
                formatFilesContext(files)
            } catch (e: Exception) {
                "No workspace selected or read permission missing."
            }
        } else {
            "No workspace selected."
        }

        // 2. Prepare chat messages history
        val apiMessages = mutableListOf<Map<String, String>>()
        apiMessages.add(mapOf("role" to "system", "content" to getSystemPrompt(role, workspaceContext)))
        
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
            
            val proposedPatches = mutableListOf<FilePatch>()
            val proposedCommands = mutableListOf<AgentCommand>()
            var displayedMessage = fullText

            if (role == AgentRole.CODER || role == AgentRole.FIXER) {
                val parsed = parseAgentResponse(fullText)
                if (parsed != null) {
                    proposedPatches.addAll(parsed.patches)
                    proposedCommands.addAll(parsed.commands)
                    displayedMessage = parsed.summary
                } else {
                    providerRepository.log(role.displayName, "JSON parsing failed. Attempting regex fallback.", "WARNING")
                    // Fallback to parse old style if LLM did not respect JSON
                    val parsedChanges = parseRegexFileChanges(rootUriString, fullText)
                    proposedPatches.addAll(parsedChanges)
                    val parsedCmds = parseRegexCommands(fullText)
                    proposedCommands.addAll(parsedCmds)
                }
            } else {
                // If it is another agent role, parse any recommended commands
                val parsedCmds = parseRegexCommands(fullText)
                proposedCommands.addAll(parsedCmds)
            }

            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = displayedMessage,
                    isAgent = true,
                    agentRole = role,
                    proposedPatches = proposedPatches,
                    proposedCommands = proposedCommands
                )
            )
        } else {
            val exception = result.exceptionOrNull()
            providerRepository.log(role.displayName, "Error executing agent: ${exception?.message}", "ERROR")
            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = "Error executing ${role.displayName}: ${exception?.message}",
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

    private fun parseRegexFileChanges(rootUriString: String?, text: String): List<FilePatch> {
        if (rootUriString == null) return emptyList()
        val patches = mutableListOf<FilePatch>()
        val pattern = Pattern.compile("<<<<\\s*FILE:\\s*([^>]+)\\s*>>>>(.*?)<<<<\\s*END\\s*>>>>", Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            val path = matcher.group(1).trim()
            val content = matcher.group(2)
            
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
                    action = if (originalContent.isEmpty()) "create" else "modify",
                    oldText = originalContent,
                    newText = content
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
            commands.add(
                AgentCommand(
                    command = matcher.group(1).trim(),
                    reason = "Recommended by agent",
                    requiresConfirmation = true
                )
            )
        }
        return commands
    }
}
