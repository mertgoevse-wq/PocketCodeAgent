# Workspace Context Engine Phase Report

**Phase:** Workspace Context Engine — Intelligent project context selection for LLM prompts
**Build:** ✅ `BUILD SUCCESSFUL`
**Commit:** Pending

---

## Overview

PocketCodeAgent now builds a rich, budgeted `WorkspaceContext` before each agent run instead of sending a crude URI-based workspace summary. The context engine selects relevant project files, sanitizes secrets, applies token budgets, and includes pending changes, terminal queue, and preview status.

---

## New Files

| File | Purpose |
|------|---------|
| `domain/context/ContextBudget.kt` | Token budget constants: 60K total, 20K active file, 12K relevant files, 8K build files, max 8 relevant + 4 build files |
| `domain/context/WorkspaceContext.kt` | Data model: `WorkspaceContext` + `RelevantFileContext` with `toPromptString()` builder |
| `domain/context/ContextSanitizer.kt` | Secret redaction: Bearer tokens, sk-/nvapi- keys, api_key, token, secret, password, GitHub tokens. Blocks `.env`, `google-services.json`, `local.properties`, keystore files from context entirely |
| `domain/context/FileRelevanceScorer.kt` | Scores files by user task, role, and skill. High-priority paths organized by category (android, provider_api, workspace_patch, preview, terminal, skills_agents). Always ignores: node_modules, .git, .gradle, build, dist, .idea, binary files |
| `domain/context/WorkspaceContextBuilder.kt` | Main builder: loads file tree, scores files, reads top N relevant + build files (all on Dispatchers.IO), sanitizes content, builds summaries |

## Modified Files

| File | Change |
|------|--------|
| `AgentRolePromptBuilder.kt` | `build()` now accepts `WorkspaceContext` instead of raw string. Shows estimated char count |
| `SkillPromptBuilder.kt` | `build()` now accepts `WorkspaceContext` instead of `workspaceSummary` + `openFile`. Shows estimated char count |
| `AgentRepository.kt` | Added `WorkspaceContextBuilder`, public `buildWorkspaceContext()` method. `runAgent()` accepts optional `WorkspaceContext` with fallback to tree-only context |
| `AgentViewModel.kt` | `sendMessage()` and `runAgentRole()` now accept `pendingChanges`, `activeFileName`, `previewTarget`. Builds context before agent run. Tracks `lastContext` / `lastContextFileCount` / `lastContextWarnings` for UI |
| `ChatPanel.kt` | Added `ContextChipRow` (shows file count, est. chars, warnings) + `ContextPreviewDialog` (full details). Passes context data through |
| `MainShellScreen.kt` | Passes `pendingFileChanges`, `selectedFileName`, `activePreviewTarget` from MainViewModel to ChatPanel |

---

## How Relevance Scoring Works

`FileRelevanceScorer.score(path, userTask, roleId, skillId)` assigns a priority score to each file:

1. **Base score:** 10 for any source file
2. **Role/skill category match:** +60 if file category matches the selected role or skill
3. **High-priority paths:** +40 for known core files (MainActivity, ChatPanel, build.gradle.kts, etc.)
4. **User task keyword match:** +30 if filename appears in the user task
5. **Path depth bonus:** +15 for shallow files (depth ≤ 2)
6. **Language bonus:** +10 for Kotlin/Java/Gradle/XML files

Files matching `alwaysIgnorePatterns` (node_modules, .git, .gradle, build, dist, etc.) or binary extensions are excluded.

---

## How Budget Works

`ContextBudget` defines hard limits:
- `DEFAULT_MAX_CHARS = 60_000` — total target budget
- `ACTIVE_FILE_MAX_CHARS = 20_000` — currently open file
- `RELEVANT_FILE_MAX_CHARS = 12_000` — per relevant file
- `BUILD_FILE_MAX_CHARS = 8_000` — per build config file
- `SUMMARY_MAX_CHARS = 3_000` — file tree summary
- `MAX_RELEVANT_FILES = 8` — top relevant files
- `MAX_BUILD_FILES = 4` — top build/config files
- `LARGE_FILE_THRESHOLD_BYTES = 200_000` — files flagged as truncated
- `HUGE_FILE_THRESHOLD_BYTES = 500_000` — path/metadata only, no content

Content is truncated at read-time. File tree summary is truncated if > 3000 chars.

---

## How Sanitizing Works

`ContextSanitizer.sanitize(path, content)` runs these patterns:
- `Authorization: Bearer ...` → `Bearer [REDACTED]`
- `sk-...` / `nvapi-...` → `sk-[REDACTED]` / `nvapi-[REDACTED]`
- `api_key=...` / `token=...` / `secret=...` / `password=...` → `$1=[REDACTED]`
- GitHub tokens (`ghp_...`, `gho_...`) → `gh*_[REDACTED]`

Sensitive files (`.env`, `google-services.json`, `local.properties`, keystore, `credentials.json`, `secrets.properties`, `gradle.properties`) are entirely replaced with a `[Content suppressed]` placeholder.

Per-file warnings are collected and surfaced in the context chip + dialog.

---

## Context Flow

```
User taps "Send" / "Run"
    ↓
AgentViewModel.sendMessage()
    ↓
AgentViewModel.runAgentRole()
    ↓
agentRepository.buildWorkspaceContext(  ← Dispatchers.IO
    userTask, roleId, skillId,
    activeFilePath, pendingChanges,
    terminalCommands, previewTarget
)
    ↓
WorkspaceContextBuilder.buildContext()
    ├── Load file tree (SAF / demo)
    ├── Score all files (FileRelevanceScorer)
    ├── Read build files (top 4, sanitized)
    ├── Read active file (if open)
    ├── Read relevant files (top 8, excluded dupes)
    ├── Sanitize all content
    ├── Build summaries (pending changes, terminal queue, preview)
    └── Return WorkspaceContext
    ↓
context.toPromptString() → AgentRolePromptBuilder.build() / SkillPromptBuilder.build()
    ↓
System prompt sent to LLM
```

---

## UI Integration

### Context Chip (ChatPanel)
- Shows between message list and mode row
- Displays: 📄 file count, ~chars, ⚠ warnings count
- Tapping opens the Context Preview Dialog

### Context Preview Dialog
- Scrollable dialog showing:
  - Workspace name
  - File count, estimated chars
  - Active file
  - Build files (with reasons)
  - Relevant files (with reasons, priority)
  - Warnings (if any)
  - Pending changes summary
- "Close" button

---

## Budget Enforcement Example

For a typical Android project with 80 files:
1. File tree: ~2K chars (truncated at 3K)
2. Build files (gradle.kts, settings, AndroidManifest): 4 files × up to 8K each = up to 32K
3. Active file: up to 20K
4. Relevant files: 8 files × up to 12K each = up to 96K
5. Summaries: pending changes + terminal + preview = ~1K

Total budget target: 60K chars. Large files are flagged/truncated to stay within budget.

---

## Limitations

- Active file path currently comes from `MainViewModel.selectedFileName` (file name only, not full path). Future: resolve full relative path for better context
- File content reading for relevant files goes through SAF on the IO dispatcher — large projects may take 1-3 seconds to build context
- `WorkspaceContextBuilder` is instantiated inside `AgentRepository` — future phases could make it injectable via DI
- Skill-augmented user message replaces the last chat message content for API calls only; the user still sees their original message in chat
- Relevance scoring is keyword/name-based, not semantic — a task about "login" won't find `AuthManager.kt` unless its name or path contains the keyword
- Terminal command queue and preview target are passed from MainViewModel through ChatPanel to AgentViewModel — this creates a 3-layer chain but avoids tight coupling

---

## Build Result

```
BUILD SUCCESSFUL
```

No new compilation errors. Existing Compose/Room deprecation warnings unchanged.
