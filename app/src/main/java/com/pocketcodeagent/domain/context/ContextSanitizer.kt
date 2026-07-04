package com.pocketcodeagent.domain.context

object ContextSanitizer {

    private val secretPatterns = listOf(
        // Authorization headers
        Regex("(?i)Authorization\\s*:\\s*Bearer\\s+[A-Za-z0-9._\\-]+") to "Authorization: Bearer [REDACTED]",
        Regex("(?i)Bearer\\s+[A-Za-z0-9._\\-]{20,}") to "Bearer [REDACTED]",
        // API keys
        Regex("sk-[A-Za-z0-9]{12,}") to "sk-[REDACTED]",
        Regex("nvapi-[A-Za-z0-9]{12,}") to "nvapi-[REDACTED]",
        Regex("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[A-Za-z0-9._\\-]{8,}") to "$1=[REDACTED]",
        Regex("(?i)api[_-]?key[\"']?\\s*=\\s*[\"'][A-Za-z0-9._\\-]{8,}[\"']") to "api_key=[REDACTED]",
        // Tokens
        Regex("(?i)(token|access[_-]?token)\\s*[:=]\\s*[A-Za-z0-9._\\-]{8,}") to "$1=[REDACTED]",
        // Secrets
        Regex("(?i)(secret|private[_-]?key|password)\\s*[:=]\\s*[^\\s]{4,}") to "$1=[REDACTED]",
        // GitHub tokens (ghp_, gho_, ghu_, ghs_, ghr_)
        Regex("gh[pousr]_[A-Za-z0-9]{12,}") to "gh*_[REDACTED]",
        // OpenAI keys
        Regex("sk-(proj-)?[A-Za-z0-9]{12,}") to "sk-[REDACTED]"
    )

    private val sensitiveFiles = setOf(
        ".env",
        "google-services.json",
        "local.properties",
        "keystore",
        "credentials.json",
        "secrets.properties"
    )

    fun sanitize(path: String, content: String): Pair<String, List<String>> {
        val warnings = mutableListOf<String>()
        var sanitized = content

        val fileName = path.substringAfterLast("/").substringAfterLast("\\")

        // For sensitive files, truncate to existence-only note
        if (sensitiveFiles.any { fileName.equals(it, ignoreCase = true) || fileName.contains(it) }) {
            return Pair("[Content of $fileName suppressed — sensitive file]", listOf("$fileName content not sent to LLM"))
        }

        for ((pattern, replacement) in secretPatterns) {
            if (pattern.containsMatchIn(sanitized)) {
                warnings.add("${pattern.pattern.take(30)}... found in $path — redacted")
                sanitized = pattern.replace(sanitized, replacement)
            }
        }

        return Pair(sanitized, warnings)
    }

    fun isSensitiveFileName(name: String): Boolean {
        return sensitiveFiles.any { name.equals(it, ignoreCase = true) || name.contains(it) }
    }

    fun redactSummary(summary: String): String {
        var result = summary
        for ((pattern, replacement) in secretPatterns) {
            result = pattern.replace(result, replacement)
        }
        return result
    }

    fun hasPotentialSecrets(content: String): Boolean {
        return redactSummary(content) != content
    }
}
