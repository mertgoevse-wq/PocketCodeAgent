# Agent Registry Phase Report

**Phase:** Agent Registry — Native PocketCodeAgent Role System  
**Build:** ✅ `BUILD SUCCESSFUL in 23s`  
**Commit:** Pending

---

## New Files

| File | Purpose |
|------|---------|
| `domain/agent/registry/AgentRole.kt` | `RichAgentRole` data class + `RoleRiskLevel` enum (LOW/MEDIUM/HIGH) |
| `domain/agent/registry/AgentRegistry.kt` | 11 built-in roles, `fromLegacy`/`toLegacyOrPlanner` compatibility mappings |
| `domain/agent/registry/AgentRolePromptBuilder.kt` | Builds final system prompt from base rules + role instructions + mode + workspace context |

## Modified Files

| File | Change |
|------|--------|
| `AgentRepository.kt` | Uses `AgentRolePromptBuilder.build()` instead of inline `when(role)`. `formatFilesContext` delegates to builder. Old `getSystemPrompt(AgentRole, ...)` preserved as legacy wrapper. |
| `AgentViewModel.kt` | Added `selectedRegistryRole: RichAgentRole` state (defaults to Planner). Legacy overload `runAgentRole(provider, AgentRole, ...)` delegates to new method. `sendMessage` uses selected registry role. |
| `ChatPanel.kt` | Replaced 4 hardcoded AgentChip buttons with compact role selector dropdown (11 roles) + Run + Apply buttons. Radio-button selection UX. |

## Built-in Roles (11)

| # | Role | ID | Risk | Default Modes |
|---|------|----|------|---------------|
| 1 | Planner | `planner` | LOW | DISCUSS, BUILD |
| 2 | Android Kotlin Engineer | `android-kotlin-engineer` | MEDIUM | BUILD |
| 3 | Jetpack Compose UI Engineer | `jetpack-compose-ui-engineer` | LOW | BUILD |
| 4 | Provider/API Engineer | `provider-api-engineer` | HIGH | BUILD |
| 5 | Workspace File Engineer | `workspace-file-engineer` | HIGH | BUILD |
| 6 | Preview/WebView Engineer | `preview-webview-engineer` | LOW | BUILD |
| 7 | Terminal/Termux Engineer | `terminal-termux-engineer` | HIGH | BUILD |
| 8 | Security Reviewer | `security-reviewer` | HIGH | DISCUSS |
| 9 | Performance Engineer | `performance-engineer` | MEDIUM | BUILD |
| 10 | QA Release Engineer | `qa-release-engineer` | LOW | DISCUSS, BUILD |
| 11 | Documentation Writer | `documentation-writer` | LOW | DISCUSS, BUILD |

Every role follows AGENTS.md rules: no example/demo folders, no com.example, no API key exposure, no fake execution.

## How Role Prompts Are Built

`AgentRolePromptBuilder.build(role, agentMode, workspaceContext)` composes the final system prompt from:

1. **Base rules** — AGENTS.md extracted: no example folders, no com.example, no secret logging, sandbox limits, no auto-execution
2. **Role system instructions** — Per-role expertise, constraints, and output format rules
3. **Mode instructions** — DISCUSS (explain only, no pocketActions) or BUILD (pocketArtifact/pocketAction format)
4. **Workspace context** — File tree formatted with `formatFilesContext()`

## Build/Discuss Mode Interaction

- **DISCUSS mode**: Roles emit explanations and plans only. `Security Reviewer` is restricted to DISCUSS-only.
- **BUILD mode**: Roles emit `pocketArtifact/pocketAction` blocks. Roles like `Provider/API Engineer` and `Workspace File Engineer` have HIGH risk levels but can still operate in BUILD mode with their specific safety constraints.
- The `AgentModeRow` in ChatPanel lets users toggle between modes before running.

## UI Integration

- **Role selector**: Compact dropdown in ChatPanel's agent action row. Shows current role name with dropdown arrow. Tapping opens a scrollable menu of all 11 roles with display name, short description, and radio-button selection.
- **Run button**: Executes the selected role against the chat context and provider.
- **Apply button**: Preserved for diff review of patches from the last agent message.
- **Backward compatible**: Old `ChatAgentScreen` with hardcoded Plan/Code/Review/Fix buttons still works via the legacy `runAgentRole(provider, AgentRole, ...)` overload.

## Legacy Compatibility

- `AgentRegistry.fromLegacy(AgentRole)` maps old 6-role enum to new 11-role registry
- `AgentRegistry.toLegacyOrPlanner(RichAgentRole)` maps back for `currentAgentRole` UI display
- Multiple new roles map to `AgentRole.CODER` in legacy — acceptable since legacy UI only shows the agent's display name during streaming
- When the old enum is fully phased out, `toLegacyOrPlanner` can be removed

## Remaining Limitations

- Role instructions are inline strings in AgentRegistry.kt — future phases could externalize to JSON/template files
- `toLegacyOrPlanner` must be manually updated when new roles are added (falls back to PLANNER safely)
- `RichAgentRole` is named to avoid collision with existing `AgentRole` enum — rename when old enum is removed
- No role-specific temperature override in API calls yet (all use default provider temperature)
- Role selector does not show risk level or allowed modes in the dropdown (compact by design)

## Build Result

```
BUILD SUCCESSFUL in 23s
```
