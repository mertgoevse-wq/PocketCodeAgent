package com.pocketcodeagent.domain.agent.registry

import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.context.WorkspaceContext

object AgentRolePromptBuilder {

    private val baseRules = """
You are a subagent inside PocketCodeAgent, a native Android coding workbench running on a mobile phone without root access.

Core Rules (from AGENTS.md):
- Never create folders named example, sample, demo, playground, starter, or template.
- Never use com.example packages.
- Never hardcode, request, reveal, or log API keys, Authorization headers, tokens, or secrets.
- API key fields must be masked by default.
- Destructive file actions require confirmation.
- Shell commands are suggestions only — never claim they were executed.
- No fake root access. No fake terminal execution.
- Respect Android SAF sandbox limits for file access.
""".trimIndent()

    fun build(
        role: RichAgentRole,
        agentMode: AgentMode,
        context: WorkspaceContext
    ): String {
        val modeInstructions = when (agentMode) {
            AgentMode.DISCUSS -> buildDiscussModeInstructions()
            AgentMode.BUILD -> buildBuildModeInstructions()
        }
        val contextString = context.toPromptString()
        return buildString {
            appendLine(baseRules)
            appendLine()
            appendLine(role.systemInstructions)
            appendLine()
            appendLine(modeInstructions)
            appendLine()
            appendLine("Workspace context (${context.estimatedChars} chars):")
            appendLine(contextString)
        }
    }

    private fun buildDiscussModeInstructions(): String = """
Current agent mode: DISCUSS.
- Do NOT emit pocketArtifact or pocketAction blocks.
- Do NOT propose file changes or shell command actions.
- Explain, plan, and ask clarifying questions when needed.
- Use clear Markdown formatting with headers and bullet points.
""".trimIndent()

    private fun buildBuildModeInstructions(): String = """
Current agent mode: BUILD.
- When proposing changes, use this exact artifact format:
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
- Do not merely describe file changes; put them in pocketAction blocks.
- Shell commands are only suggestions. Never claim they were executed.
- Do not suggest dangerous commands, destructive commands, sudo/su, rm -rf, curl|sh, wget|sh, or encoded PowerShell.
- All file changes must be reviewable; do not imply automatic application.
""".trimIndent()

    fun formatFilesContext(files: List<WorkspaceFile>, indent: String = ""): String {
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
}
