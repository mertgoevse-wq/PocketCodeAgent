package com.pocketcodeagent.domain.skill

import com.pocketcodeagent.domain.agent.AgentMode

object SkillRegistry {

    val BUILD_ANDROID_FEATURE = Skill(
        id = "build-android-feature",
        displayName = "Build Android Feature",
        category = SkillCategory.ANDROID,
        description = "Implement a new Kotlin/Compose feature following project architecture",
        promptTemplate = """
Implement the following Android feature in PocketCodeAgent following the project's MVVM architecture:

Task: {{TASK}}

Guidelines:
- Follow domain/data/ui layer separation with ViewModels and Compose screens.
- Use existing theme colors, composable patterns, and modifier chains.
- Never create example/demo/template folders. Never use com.example packages.
- Never expose API keys or secrets.
- In BUILD mode, emit pocketArtifact/pocketAction blocks for every file change.
- Shell commands are suggestions only — never claim they were executed.
""".trimIndent(),
        recommendedRoleId = "android-kotlin-engineer",
        mode = AgentMode.BUILD
    )

    val IMPROVE_COMPOSE_UI = Skill(
        id = "improve-compose-ui",
        displayName = "Improve Compose UI",
        category = SkillCategory.ANDROID,
        description = "Enhance existing Jetpack Compose UI with better styling and interactions",
        promptTemplate = """
Improve the following Jetpack Compose UI in PocketCodeAgent:

Task: {{TASK}}

Guidelines:
- Use the project's theme: SlateBlue, CalmSage, WarmCopper, TextPrimary/Secondary, BorderGrey.
- Keep dark theme: container colors in 0xFF0E0E10, 0xFF18181C, 0xFF1E1E26 range.
- Touch targets ≥ 44dp. Scrollable where needed.
- Follow existing composable patterns: Surface, Card, Column/Row, modifier chains.
- Never create example/demo/template folders. Never use com.example packages.
- In BUILD mode, emit pocketArtifact/pocketAction blocks for all UI file changes.
""".trimIndent(),
        recommendedRoleId = "jetpack-compose-ui-engineer",
        mode = AgentMode.BUILD
    )

    val FIX_BUILD_ERROR = Skill(
        id = "fix-build-error",
        displayName = "Fix Build Error",
        category = SkillCategory.DEBUGGING,
        description = "Diagnose and fix Gradle/Kotlin compilation errors",
        promptTemplate = """
Fix the following build error in PocketCodeAgent:

Error context: {{TASK}}

Guidelines:
- Read the full error message and the file at the reported line.
- Fix only the root cause — do not refactor surrounding code.
- If the error is in a file you created, review for missing imports, type mismatches, or syntax issues.
- Run ./gradlew.bat compileDebugKotlin to verify the fix.
- In BUILD mode, emit pocketArtifact/pocketAction blocks for every fix.
- Never create example/demo/template folders. Never use com.example packages.
""".trimIndent(),
        recommendedRoleId = "qa-release-engineer",
        mode = AgentMode.BUILD
    )

    val REFACTOR_WITHOUT_CHANGES = Skill(
        id = "refactor-without-changes",
        displayName = "Refactor Without Feature Changes",
        category = SkillCategory.REFACTORING,
        description = "Improve code structure without altering behavior",
        promptTemplate = """
Refactor the following code in PocketCodeAgent without changing any behavior:

Task: {{TASK}}

Guidelines:
- Find all references to the code being refactored before making changes.
- Keep old and new implementation parallel until all call sites are migrated.
- Remove unused imports, variables, and dead code after migration.
- Do not add new features or abstractions beyond the refactoring scope.
- Run ./gradlew.bat compileDebugKotlin after changes.
- In BUILD mode, emit pocketArtifact/pocketAction blocks for every refactored file.
- Never create example/demo/template folders. Never use com.example packages.
""".trimIndent(),
        recommendedRoleId = "performance-engineer",
        mode = AgentMode.BUILD
    )

    val ADD_STATIC_WEB_PREVIEW = Skill(
        id = "add-static-web-preview",
        displayName = "Add Static Web Preview Files",
        category = SkillCategory.WEB,
        description = "Create or update HTML/CSS/JS files for WebView preview",
        promptTemplate = """
Add or update static web preview files for PocketCodeAgent's WebView:

Task: {{TASK}}

Guidelines:
- HTML files > 500KB should show a warning before bundling.
- CSS/JS files > 500KB should be linked externally, not inlined.
- Path traversal (../, absolute, content:, file://) must be blocked.
- All SAF file reads must run on Dispatchers.IO.
- In BUILD mode, emit pocketArtifact/pocketAction blocks for all file changes.
- Never include API keys, secrets, or tokens in generated files.
- Never create example/demo/template folders. Never use com.example packages.
""".trimIndent(),
        recommendedRoleId = "preview-webview-engineer",
        mode = AgentMode.BUILD
    )

    val SECURITY_AUDIT = Skill(
        id = "security-audit",
        displayName = "Security Audit",
        category = SkillCategory.SECURITY,
        description = "Audit code for vulnerabilities, secret leaks, and sandbox violations",
        promptTemplate = """
Perform a security audit on PocketCodeAgent code:

Scope: {{TASK}}

Check for:
- Hardcoded API keys, tokens, or secrets.
- Logged Authorization headers or sensitive request bodies.
- Path traversal vulnerabilities in file operations.
- Unsafe WebView settings (file access enabled, missing sanitization).
- Command injection risks in shell command suggestions.
- Missing confirmation dialogs on destructive operations.

Rules:
- Describe findings by location and risk — never copy or echo secrets.
- Follow SECURITY_NOTES.md rules.
- In DISCUSS mode, list findings in order of severity with clear remediation steps.
- Never create example/demo/template folders. Never use com.example packages.
""".trimIndent(),
        recommendedRoleId = "security-reviewer",
        mode = AgentMode.DISCUSS
    )

    val PERFORMANCE_AUDIT = Skill(
        id = "performance-audit",
        displayName = "Performance Audit",
        category = SkillCategory.PERFORMANCE,
        description = "Identify bottlenecks: main-thread I/O, unbounded lists, recomposition",
        promptTemplate = """
Perform a performance audit on PocketCodeAgent:

Scope: {{TASK}}

Check for:
- Main-thread I/O: SAF reads not on Dispatchers.IO.
- Unbounded lists: missing LazyColumn keys, no pagination.
- WebView lifecycle leaks: missing DisposableEffect cleanup.
- Excessive recompositions: unstable state keys, missing derivedStateOf.
- Streaming throttling: ensure 150ms chunks are preserved.

Rules:
- Follow PERFORMANCE_FIX_REPORT.md — do not undo existing fixes.
- In DISCUSS mode, list bottlenecks with proposed fixes.
- Never create example/demo/template folders. Never use com.example packages.
""".trimIndent(),
        recommendedRoleId = "performance-engineer",
        mode = AgentMode.DISCUSS
    )

    val PREPARE_DEBUG_APK = Skill(
        id = "prepare-debug-apk",
        displayName = "Prepare Debug APK",
        category = SkillCategory.RELEASE,
        description = "Build and verify the debug APK for testing",
        promptTemplate = """
Prepare a debug APK build for PocketCodeAgent:

Task: {{TASK}}

Steps:
- Run ./gradlew.bat clean (Windows) or ./gradlew clean.
- Run ./gradlew.bat assembleDebug.
- Verify BUILD SUCCESSFUL.
- APK is at: app/build/outputs/apk/debug/app-debug.apk.
- Check for any compilation warnings or missing resources.

Rules:
- Commands are suggestions only — never auto-execute.
- Follow BUILD_APK_GUIDE.md for build instructions.
- Never create example/demo/template folders. Never use com.example packages.
""".trimIndent(),
        recommendedRoleId = "qa-release-engineer",
        mode = AgentMode.BUILD
    )

    val GENERATE_TERMUX_COMMANDS = Skill(
        id = "generate-termux-commands",
        displayName = "Generate Termux Run Commands",
        category = SkillCategory.PREVIEW,
        description = "Generate safe Termux setup and dev server commands",
        promptTemplate = """
Generate Termux commands for PocketCodeAgent:

Project context: {{TASK}}

Guidelines:
- Commands are suggestions only — never claim they were executed.
- Every command must pass CommandRiskScanner: SAFE, CAUTION, or BLOCKED.
- Include: pkg update, pkg install nodejs git, termux-setup-storage, cd to project, npm install, npm run dev -- --host 127.0.0.1.
- For npm run dev, suggest setting Preview URL to http://127.0.0.1:5173.
- Never suggest destructive commands (rm -rf, sudo, curl|sh).
- Never include API keys or tokens in command text.
- In BUILD mode, wrap commands in pocketAction type="shell" blocks.
""".trimIndent(),
        recommendedRoleId = "terminal-termux-engineer",
        mode = AgentMode.BUILD
    )

    val ALL: List<Skill> = listOf(
        BUILD_ANDROID_FEATURE,
        IMPROVE_COMPOSE_UI,
        FIX_BUILD_ERROR,
        REFACTOR_WITHOUT_CHANGES,
        ADD_STATIC_WEB_PREVIEW,
        SECURITY_AUDIT,
        PERFORMANCE_AUDIT,
        PREPARE_DEBUG_APK,
        GENERATE_TERMUX_COMMANDS
    )

    fun findById(id: String): Skill? = ALL.firstOrNull { it.id == id }

    val default: Skill? get() = null
}
