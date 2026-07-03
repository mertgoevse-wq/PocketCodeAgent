package com.pocketcodeagent.data.model

enum class AgentRole(val displayName: String, val roleDescription: String) {
    PLANNER(
        "PlannerAgent",
        "Analyzes the user's task request, explores dependencies, and compiles a detailed step-by-step checklist execution plan."
    ),
    CODER(
        "CoderAgent",
        "Responsible for writing, updating, or deleting project source code files based on the step-by-step plan."
    ),
    REVIEWER(
        "ReviewerAgent",
        "Inspects suggested code changes, checks syntax, verifies formatting, and ensures the implementation contains no logical bugs."
    ),
    FIXER(
        "FixerAgent",
        "Analyzes error logs, diagnostic output, or compile errors to repair and fix code bugs."
    ),
    PREVIEW(
        "PreviewAgent",
        "Triggers live previews of web pages, compiles and updates WebView environments, or interfaces with Termux local servers."
    ),
    TERMINAL(
        "TerminalAgent",
        "Suggests shell commands for compilation, testing, or Git actions, requiring user permission before execution."
    )
}
