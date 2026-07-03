package com.pocketcodeagent.domain.agent

enum class CommandRiskLevel {
    SAFE,
    CAUTION,
    BLOCKED
}

object CommandRiskScanner {
    private val blockedPatterns = listOf(
        Regex("""\brm\s+-rf\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsudo\b""", RegexOption.IGNORE_CASE),
        Regex("""(^|\s)su(\s|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bchmod\s+777\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcurl\b.*\|\s*(sh|bash)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bwget\b.*\|\s*(sh|bash)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bformat\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdel\s+/s\b""", RegexOption.IGNORE_CASE),
        Regex("""\badb\s+shell\s+rm\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpowershell(\.exe)?\s+-enc\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbase64\b.*\|\s*(sh|bash|powershell|pwsh|cmd)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbase64\s+-d\b.*\|\s*(sh|bash|powershell|pwsh|cmd)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdd\s+if=""", RegexOption.IGNORE_CASE),
        Regex("""\bmkfs\b""", RegexOption.IGNORE_CASE),
        Regex("""\bfork\s*bomb""", RegexOption.IGNORE_CASE),
        Regex("""(?i)sk-[A-Za-z0-9]{12,}"""),
        Regex("""(?i)Bearer\s+[A-Za-z0-9._\-]{12,}""")
    )

    private val cautionPatterns = listOf(
        Regex("""\bnpm\s+install\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnpm\s+update\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnpm\s+audit\s+fix\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnpm\s+run\s+build\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgit\s+reset\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgit\s+clean\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgit\s+checkout\s+\.""", RegexOption.IGNORE_CASE),
        Regex("""\bgradlew(\.bat)?\s+clean\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpip\s+install\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpkg\s+install\b""", RegexOption.IGNORE_CASE),
        Regex("""\brm\s+""", RegexOption.IGNORE_CASE),
        Regex("""\bmv\s+.*\s+""", RegexOption.IGNORE_CASE),
        Regex("""\bcp\s+.*\s+""", RegexOption.IGNORE_CASE)
    )

    private val safePatterns = listOf(
        Regex("""^\s*npm\s+run\s+dev""", RegexOption.IGNORE_CASE),
        Regex("""^\s*npm\s+test""", RegexOption.IGNORE_CASE),
        Regex("""^\s*npm\s+--?v(ersion)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\s*node\s+--?v(ersion)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(\./|\.\\)?gradlew(\.bat)?\s+assembleDebug""", RegexOption.IGNORE_CASE),
        Regex("""^\s*ls(\s|$)""", RegexOption.IGNORE_CASE),
        Regex("""^\s*dir(\s|$)""", RegexOption.IGNORE_CASE),
        Regex("""^\s*pwd\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\s*cat\s+.+""", RegexOption.IGNORE_CASE),
        Regex("""^\s*type\s+.+""", RegexOption.IGNORE_CASE)
    )

    fun scan(command: String): CommandRiskLevel {
        val normalized = command.trim()
        if (normalized.isBlank()) return CommandRiskLevel.CAUTION
        if (blockedPatterns.any { it.containsMatchIn(normalized) }) return CommandRiskLevel.BLOCKED
        if (cautionPatterns.any { it.containsMatchIn(normalized) }) return CommandRiskLevel.CAUTION
        if (safePatterns.any { it.containsMatchIn(normalized) }) return CommandRiskLevel.SAFE
        return CommandRiskLevel.CAUTION
    }
}
