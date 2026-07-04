package com.pocketcodeagent

import com.pocketcodeagent.domain.context.ContextSanitizer
import org.junit.Assert.*
import org.junit.Test

class ContextSanitizerTest {

    @Test
    fun `Authorization Bearer is redacted`() {
        val (sanitized, warnings) = ContextSanitizer.sanitize(
            "config.txt",
            "Authorization: Bearer abcdefghijklmnopqrstuvwxyz123456"
        )
        assertTrue(sanitized.contains("[REDACTED]"))
        assertFalse(sanitized.contains("abcdefghijklmnopqrstuvwxyz123456"))
        assertTrue(warnings.isNotEmpty())
    }

    @Test
    fun `API_KEY is redacted`() {
        val (sanitized, warnings) = ContextSanitizer.sanitize(
            "config.txt",
            "api_key=sk-abcdefghijklmnopqrstuvwxyz123456"
        )
        assertTrue(sanitized.contains("[REDACTED]"))
        assertFalse(sanitized.contains("sk-abcdefghijklmnopqrstuvwxyz123456"))
        assertTrue(warnings.isNotEmpty())
    }

    @Test
    fun `token value is redacted`() {
        val (sanitized, _) = ContextSanitizer.sanitize(
            "config.txt",
            "token=ghp_abcdefghijklmnopqrstuvwxyz"
        )
        assertTrue(sanitized.contains("[REDACTED]"))
        assertFalse(sanitized.contains("ghp_abcdefghijklmnopqrstuvwxyz"))
    }

    @Test
    fun `secret is redacted`() {
        val (sanitized, _) = ContextSanitizer.sanitize(
            "app.properties",
            "secret=my_super_secret_key_12345"
        )
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `env file content is suppressed entirely`() {
        val (sanitized, warnings) = ContextSanitizer.sanitize(
            ".env",
            "DATABASE_URL=postgres://localhost\nAPI_KEY=sk-abc123\nSECRET=topsecret"
        )
        assertTrue(sanitized.contains("suppressed"))
        assertFalse(sanitized.contains("DATABASE_URL"))
        assertFalse(sanitized.contains("API_KEY"))
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains(".env"))
    }

    @Test
    fun `normal code remains readable`() {
        val normalCode = """
            fun main() {
                val x = 42
                println("Hello World")
            }
        """.trimIndent()
        val (sanitized, warnings) = ContextSanitizer.sanitize("Main.kt", normalCode)
        assertEquals(normalCode, sanitized)
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `sensitive file name detection works`() {
        assertTrue(ContextSanitizer.isSensitiveFileName(".env"))
        assertTrue(ContextSanitizer.isSensitiveFileName("google-services.json"))
        assertTrue(ContextSanitizer.isSensitiveFileName("local.properties"))
        assertFalse(ContextSanitizer.isSensitiveFileName("build.gradle.kts"))
        assertFalse(ContextSanitizer.isSensitiveFileName("MainActivity.kt"))
    }

    @Test
    fun `redactSummary redacts secrets`() {
        val summary = "Saved file with API key: sk-abcdefghijklmnopqrstuvwxyz123456"
        val redacted = ContextSanitizer.redactSummary(summary)
        assertTrue(redacted.contains("[REDACTED]"))
        assertFalse(redacted.contains("sk-abcdefghijklmnopqrstuvwxyz123456"))
    }

    @Test
    fun `GitHub token is redacted`() {
        val (sanitized, _) = ContextSanitizer.sanitize(
            "config.txt",
            "GITHUB_TOKEN=ghp_abcdefghijklmnopqrstuvwxyz123456"
        )
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `hasPotentialSecrets detects secrets without exposing them`() {
        assertTrue(ContextSanitizer.hasPotentialSecrets("api_key=sk-abcdefghijklmnopqrstuvwxyz123456"))
        assertFalse(ContextSanitizer.hasPotentialSecrets("fun main() = println(\"ok\")"))
    }
}
