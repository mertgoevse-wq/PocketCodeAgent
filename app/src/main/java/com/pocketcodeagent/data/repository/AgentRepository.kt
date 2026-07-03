package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.model.AgentRole
import com.pocketcodeagent.data.model.ChatMessage
import com.pocketcodeagent.data.model.ProposedFileChange
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.network.ApiClient
import kotlinx.coroutines.flow.flow
import java.util.regex.Pattern

class AgentRepository(
    private val providerRepository: ProviderRepository,
    private val workspaceRepository: WorkspaceRepository
) {

    private fun getSystemPrompt(role: AgentRole, workspaceContext: String): String {
        val basePrompt = """
            You are ${role.displayName}, an elite AI coding agent in the PocketCodeAgent ecosystem on Android.
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
                If you want to create or overwrite a file, wrap the entire file content exactly like this:
                <<<< FILE: relative/path/to/file.ext >>>>
                file content here
                <<<< END >>>>
                
                You can specify multiple file blocks in one response. Do not use placeholders. Provide complete implementations.
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
                Suggest corrective code edits using the same format:
                <<<< FILE: relative/path/to/file.ext >>>>
                corrected content
                <<<< END >>>>
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
            
            // 4. Parse any special payloads (files, commands) from assistant's output
            val proposedChanges = parseProposedFileChanges(rootUriString, fullText)
            val proposedCommands = parseProposedCommands(fullText)

            emit(
                ChatMessage(
                    sender = role.displayName,
                    message = fullText,
                    isAgent = true,
                    agentRole = role,
                    proposedChanges = proposedChanges,
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

    private fun parseProposedFileChanges(rootUriString: String?, text: String): List<ProposedFileChange> {
        if (rootUriString == null) return emptyList()
        val changes = mutableListOf<ProposedFileChange>()
        
        // Match <<<< FILE: relative_path >>>> content <<<< END >>>>
        val pattern = Pattern.compile("<<<<\\s*FILE:\\s*([^>]+)\\s*>>>>(.*?)<<<<\\s*END\\s*>>>>", Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            val path = matcher.group(1).trim()
            val content = matcher.group(2) // Content of the file
            
            // Get original content if file exists
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
            
            changes.add(
                ProposedFileChange(
                    relativePath = path,
                    originalContent = originalContent,
                    newContent = content
                )
            )
        }
        return changes
    }

    private fun parseProposedCommands(text: String): List<String> {
        val commands = mutableListOf<String>()
        val pattern = Pattern.compile("<<<<\\s*CMD:\\s*([^>]+)\\s*>>>>")
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            commands.add(matcher.group(1).trim())
        }
        return commands
    }
}
