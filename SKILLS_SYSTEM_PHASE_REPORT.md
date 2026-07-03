# Skills System Phase Report

**Phase:** Skills System — Lightweight workflow templates for PocketCodeAgent  
**Build:** ✅ `BUILD SUCCESSFUL in 22s`  
**Commit:** Pending

---

## New Files

| File | Purpose |
|------|---------|
| `domain/skill/Skill.kt` | `Skill` data class + `SkillCategory` enum (8 categories) |
| `domain/skill/SkillRegistry.kt` | 9 built-in skills with prompt templates |
| `domain/skill/SkillPromptBuilder.kt` | Builds user-facing prompt from skill template + user task + workspace + role + mode |

## Modified Files

| File | Change |
|------|--------|
| `AgentViewModel.kt` | Added `selectedSkill` state, `applySkill()` method (sets role/mode), skill-aware `sendMessage()` using `SkillPromptBuilder` |
| `ChatPanel.kt` | Added `SkillSelectorRow` — compact dropdown between mode row and action row, shows skill chip + "→ Role" hint |

## Built-in Skills (9)

| # | Skill | Category | Role | Mode |
|---|-------|----------|------|------|
| 1 | Build Android Feature | ANDROID | Android Kotlin Engineer | BUILD |
| 2 | Improve Compose UI | ANDROID | Jetpack Compose UI Engineer | BUILD |
| 3 | Fix Build Error | DEBUGGING | QA Release Engineer | BUILD |
| 4 | Refactor Without Feature Changes | REFACTORING | Performance Engineer | BUILD |
| 5 | Add Static Web Preview Files | WEB | Preview/WebView Engineer | BUILD |
| 6 | Security Audit | SECURITY | Security Reviewer | DISCUSS |
| 7 | Performance Audit | PERFORMANCE | Performance Engineer | DISCUSS |
| 8 | Prepare Debug APK | RELEASE | QA Release Engineer | BUILD |
| 9 | Generate Termux Run Commands | PREVIEW | Terminal/Termux Engineer | BUILD |

Each skill has a `promptTemplate` with `{{TASK}}` placeholder for user input, a `recommendedRoleId` that maps to an AgentRegistry role, and a default `mode`.

## How Skills Combine with Roles

When a user selects a skill:

1. **`applySkill(skill)`** is called → sets `selectedSkill`, looks up `skill.recommendedRoleId` in `AgentRegistry` and sets `selectedRegistryRole` to that role, sets `agentMode` to the skill's default mode.
2. **`sendMessage()`** calls `SkillPromptBuilder.build()` with the skill, user task, workspace URI, open file (null), selected role, and current mode.
3. **`SkillPromptBuilder`** composes: skill rules (from AGENTS.md) → skill display name + description → skill prompt template with `{{TASK}}` replaced → role context → mode instructions → workspace context.
4. The resulting prompt is sent as the user message to the LLM. The system prompt still comes from the selected role via `AgentRolePromptBuilder`.
5. Tapping the same skill again deselects it (toggle).

## UI Integration

- **Skill selector row**: Compact row between mode row (Discuss/Build) and action row (role selector + Run + Apply). Shows "Skill:" label + dropdown chip.
- **Dropdown**: 9 skills + "None" option. Each shows display name, category, and mode. Radio-button selection indicator.
- **When skill selected**: Chip turns blue, shows "→ Role name" hint on the right.
- **No giant UI changes**: MainShell untouched, existing tabs unchanged, role selector preserved.

## Safety Rules

- Prompt templates are developer-authored literals — no user-controlled content in template structure.
- `{{TASK}}` replacement is simple string substitution — no injection risk since user input becomes the task description, not code.
- AGENTS.md rules are embedded in every skill prompt: no example/demo folders, no com.example, no API key exposure, no auto-execution.
- Skills that recommend DISCUSS mode (Security Audit, Performance Audit) default to explanation-only.

## Limitations

- `SkillPromptBuilder` duplicates base rules text with `AgentRolePromptBuilder` — future refactoring could share a common constant.
- Skill mode can be overridden by user via AgentModeRow after selection — this is intentional UX flexibility but may cause conflicting instructions.
- `applySkill()` silently does nothing if `recommendedRoleId` doesn't match any registry role — all 9 current IDs match.
- No workspace file tree or open file is passed to `SkillPromptBuilder.build()` — `openFile` is always `null`, `workspaceSummary` is just the URI string.

## Build Result

```
BUILD SUCCESSFUL in 22s
```
