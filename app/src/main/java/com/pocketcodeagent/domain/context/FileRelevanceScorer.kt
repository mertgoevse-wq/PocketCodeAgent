package com.pocketcodeagent.domain.context

object FileRelevanceScorer {

    // Files always ignored from context
    private val alwaysIgnorePatterns = setOf(
        "node_modules", ".git", ".gradle", "build", "dist", ".idea",
        ".vscode", ".next", "out"
    )

    private val alwaysIgnoreExtensions = setOf(
        ".apk", ".zip", ".tar", ".gz", ".jar", ".aar",
        ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".webp",
        ".mp3", ".mp4", ".wav", ".ogg",
        ".ttf", ".otf", ".woff", ".woff2",
        ".db", ".sqlite", ".sqlite3",
        ".bin", ".exe", ".dll", ".so", ".dylib"
    )

    // High-priority files by category
    private val highPriorityPaths = mapOf(
        "android" to setOf(
            "MainActivity.kt", "MainShellScreen.kt", "ChatPanel.kt",
            "MainViewModel.kt", "AgentViewModel.kt",
            "build.gradle.kts", "AndroidManifest.xml"
        ),
        "provider_api" to setOf(
            "ApiClient.kt", "ProviderRepository.kt", "ProviderViewModel.kt",
            "Provider.kt", "ProviderPreset.kt"
        ),
        "workspace_patch" to setOf(
            "WorkspaceManager.kt", "WorkspaceViewModel.kt",
            "WorkspacePatchApplier.kt", "FilePatch.kt"
        ),
        "preview" to setOf(
            "PreviewPanel.kt", "StaticPreviewBundler.kt", "PreviewTarget.kt"
        ),
        "terminal" to setOf(
            "TerminalPanel.kt", "TerminalCommand.kt", "CommandRiskScanner.kt"
        ),
        "skills_agents" to setOf(
            "AgentRegistry.kt", "AgentRolePromptBuilder.kt",
            "SkillRegistry.kt", "SkillPromptBuilder.kt", "AgentActionParser.kt"
        )
    )

    private val buildFileNames = setOf(
        "build.gradle.kts", "build.gradle", "settings.gradle.kts", "settings.gradle",
        "AndroidManifest.xml", "libs.versions.toml", "gradle.properties",
        "package.json", "Cargo.toml", "requirements.txt", "pom.xml"
    )

    fun shouldIgnoreFileName(name: String): Boolean {
        if (alwaysIgnorePatterns.any { name.equals(it, ignoreCase = true) }) return true
        if (alwaysIgnoreExtensions.any { name.endsWith(it, ignoreCase = true) }) return true
        if (ContextSanitizer.isSensitiveFileName(name)) return true
        return false
    }

    fun isBuildFile(path: String): Boolean {
        val name = path.substringAfterLast("/").substringAfterLast("\\")
        return buildFileNames.any { name.equals(it, ignoreCase = true) }
    }

    fun score(
        path: String,
        userTask: String,
        roleId: String,
        skillId: String?
    ): RelevanceResult {
        val name = path.substringAfterLast("/").substringAfterLast("\\")

        if (shouldIgnoreFileName(name)) return RelevanceResult(ignore = true)

        if (isBuildFile(path)) {
            return RelevanceResult(ignore = false, priority = 70, reason = "Build file", category = "build")
        }

        var priority = 10 // Base priority for any source file
        val reasons = mutableListOf<String>()
        val categories = mutableSetOf<String>()

        // Check against high-priority categories
        val roleCategory = roleCategory(roleId)
        val skillCategory = skillCategory(skillId)

        for ((category, names) in highPriorityPaths) {
            if (names.any { name.equals(it, ignoreCase = true) }) {
                categories.add(category)
            }
        }

        // Score: files in role/skill category get bonus
        val matchingCategories = categories.intersect(setOf(roleCategory, skillCategory))
        if (matchingCategories.isNotEmpty()) {
            priority += 60
            reasons.add("Matches role/skill: ${matchingCategories.joinToString()}")
        }

        // Detect domain/context categories
        when {
            name in highPriorityPaths["android"] ?: emptySet() -> {
                priority += 40
                reasons.add("Core Android component")
            }
            name in highPriorityPaths["provider_api"] ?: emptySet() -> {
                priority += 40
                reasons.add("Provider/API component")
            }
            name in highPriorityPaths["workspace_patch"] ?: emptySet() -> {
                priority += 40
                reasons.add("Workspace/Patch component")
            }
            name in highPriorityPaths["preview"] ?: emptySet() -> {
                priority += 40
                reasons.add("Preview component")
            }
            name in highPriorityPaths["terminal"] ?: emptySet() -> {
                priority += 40
                reasons.add("Terminal component")
            }
            name in highPriorityPaths["skills_agents"] ?: emptySet() -> {
                priority += 40
                reasons.add("Skills/Agents component")
            }
        }

        // Path depth bonus — deeper files less relevant
        val depth = path.count { it == '/' }
        if (depth <= 2) priority += 15

        // Keyword match in path vs user task
        val loweredTask = userTask.lowercase()
        if (name.lowercase().let { it in loweredTask || loweredTask.contains(it.substringBeforeLast(".")) }) {
            priority += 30
            reasons.add("Matches user task keywords")
        }

        // Language bonus
        val extension = path.substringAfterLast('.', "").lowercase()
        if (extension in setOf("kt", "kts", "java", "gradle", "xml")) priority += 10

        return RelevanceResult(
            ignore = false,
            priority = priority,
            reason = if (reasons.isEmpty()) "Project file" else reasons.joinToString("; "),
            category = if (isBuildFile(path)) "build" else (categories.firstOrNull() ?: "general")
        )
    }

    private fun roleCategory(roleId: String): String = when (roleId) {
        "android-kotlin-engineer" -> "android"
        "jetpack-compose-ui-engineer" -> "android"
        "provider-api-engineer" -> "provider_api"
        "workspace-file-engineer" -> "workspace_patch"
        "preview-webview-engineer" -> "preview"
        "terminal-termux-engineer" -> "terminal"
        "security-reviewer" -> "provider_api"
        "performance-engineer" -> "android"
        "qa-release-engineer" -> "android"
        else -> ""
    }

    private fun skillCategory(skillId: String?): String = when (skillId) {
        "build-android-feature" -> "android"
        "improve-compose-ui" -> "android"
        "fix-build-error" -> "android"
        "refactor-no-features" -> "android"
        "add-static-web-preview" -> "preview"
        "security-audit" -> "provider_api"
        "performance-audit" -> "android"
        "debug-apk" -> "android"
        "generate-termux-commands" -> "terminal"
        else -> ""
    }

    data class RelevanceResult(
        val ignore: Boolean,
        val priority: Int = 0,
        val reason: String = "",
        val category: String = "general"
    )
}
