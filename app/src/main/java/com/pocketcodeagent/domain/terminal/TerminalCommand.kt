package com.pocketcodeagent.domain.terminal

import com.pocketcodeagent.domain.agent.CommandRiskLevel
import java.util.UUID

enum class CommandSource { AGENT, USER, SYSTEM }

enum class CommandStatus { QUEUED, COPIED, MARKED_DONE, REJECTED, BLOCKED }

data class TerminalCommand(
    val id: String = "cmd-${UUID.randomUUID().toString().take(8)}",
    val command: String,
    val reason: String? = null,
    val riskLevel: CommandRiskLevel = CommandRiskLevel.CAUTION,
    val source: CommandSource = CommandSource.AGENT,
    val status: CommandStatus = if (riskLevel == CommandRiskLevel.BLOCKED) CommandStatus.BLOCKED else CommandStatus.QUEUED,
    val createdAt: Long = System.currentTimeMillis(),
    val copiedAt: Long? = null,
    val notes: String? = null
) {
    val safeDisplay: String
        get() = command
            .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._\\-]+"), "Bearer [redacted]")
            .replace(Regex("(?i)(api[_-]?key|token|secret)\\s*[:=]\\s*[^\\s]+"), "$1=[redacted]")
            .replace(Regex("sk-[A-Za-z0-9]{12,}"), "sk-[redacted]")

    fun markCopied(): TerminalCommand = copy(status = CommandStatus.COPIED, copiedAt = System.currentTimeMillis())
    fun markDone(): TerminalCommand = copy(status = CommandStatus.MARKED_DONE)
    fun reject(): TerminalCommand = copy(status = CommandStatus.REJECTED)
}
