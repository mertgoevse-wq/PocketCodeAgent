package com.pocketcodeagent.domain.skill

import com.pocketcodeagent.domain.agent.AgentMode
import com.pocketcodeagent.domain.agent.registry.RichAgentRole

object SkillPromptBuilder {

    private val skillRules = """
You are using a skill template inside PocketCodeAgent, a native Android coding workbench.

Core Rules (from AGENTS.md):
- Never create folders named example, sample, demo, playground, starter, or template.
- Never use com.example packages.
- Never hardcode, request, reveal, or log API keys, Authorization headers, tokens, or secrets.
- Destructive file actions require confirmation.
- Shell commands are suggestions only — never claim they were executed.
- No fake root access. No fake terminal execution.
- Respect Android SAF sandbox limits for file access.
""".trimIndent()

    fun build(
        skill: Skill,
        userTask: String,
        workspaceSummary: String,
        openFile: String?,
        role: RichAgentRole,
        agentMode: AgentMode
    ): String {
        val taskWithSkill = skill.promptTemplate.replace("{{TASK}}", userTask)

        val modeInstructions = when (agentMode) {
            AgentMode.DISCUSS -> buildDiscussInstructions()
            AgentMode.BUILD -> buildBuildInstructions()
        }

        return buildString {
            appendLine(skillRules)
            appendLine()
            appendLine("### Skill: ${skill.displayName}")
            appendLine(skill.description)
            appendLine()
            appendLine(taskWithSkill)
            appendLine()
            appendLine("### Agent Role: ${role.displayName}")
            appendLine(role.shortDescription)
            appendLine()
            appendLine(modeInstructions)
            appendLine()
            appendLine("### Workspace Context")
            appendLine(workspaceSummary)
            if (openFile != null) {
                appendLine()
                appendLine("Currently open file: $openFile")
            }
        }
    }

    private fun buildDiscussInstructions(): String = """
Mode: DISCUSS
- Explain and plan only. Do not emit pocketArtifact or pocketAction blocks.
- Use clear Markdown formatting.
""".trimIndent()

    private fun buildBuildInstructions(): String = """
Mode: BUILD
- Emit changes using pocketArtifact/pocketAction blocks:
  <pocketArtifact title="Short title">
    <pocketAction type="file" filePath="relative/path.ext">content</pocketAction>
    <pocketAction type="modify" filePath="relative/path.ext" oldText="old">replacement</pocketAction>
    <pocketAction type="shell">command only</pocketAction>
    <pocketAction type="preview">http://127.0.0.1:5173</pocketAction>
    <pocketAction type="note">short note</pocketAction>
  </pocketArtifact>
- Shell commands are suggestions only. Never claim they were executed.
- All file changes must be reviewable; do not imply automatic application.
""".trimIndent()
}
