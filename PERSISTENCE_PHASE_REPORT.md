# Persistence Layer Phase Report

## Build Result
✅ `BUILD SUCCESSFUL` — `./gradlew.bat assembleDebug`

## Overview
Added Room-based persistence for chat sessions, messages, artifacts, file patches, and terminal commands. All content is sanitized before storage using the existing `ContextSanitizer.redactSummary()`. Sessions are automatically created on first message and restored on app restart.

## New Files

### Entities (`data/local/entity/`)
| File | Table | Key |
|------|-------|-----|
| `SessionEntity.kt` | `sessions` | Int auto-generate |
| `ChatMessageEntity.kt` | `chat_messages` | String UUID |
| `ArtifactEntity.kt` | `artifacts` | String UUID |
| `FilePatchEntity.kt` | `file_patches` | String UUID |
| `TerminalCommandEntity.kt` | `terminal_commands` | String UUID |

### DAOs (`data/local/dao/`)
| File | Operations |
|------|-----------|
| `SessionDao.kt` | loadLastSession, insert, update, delete, deleteAll |
| `ChatMessageDao.kt` | getMessagesForSession, insert, deleteBySession |
| `ArtifactDao.kt` | getArtifactsForSession, insert, deleteBySession |
| `FilePatchDao.kt` | getPatchesForSession, insert, updatePatchStatus, deleteBySession |
| `TerminalCommandDao.kt` | getCommandsForSession, insert, updateCommandStatus, deleteBySession |

### Repository
`SessionRepository.kt` — Full CRUD for all entity types, PreviewTarget serialization, sanitization via `ContextSanitizer`.

## Modified Files

| File | Changes |
|------|---------|
| `AppDatabase.kt` | v2 → v3, 5 new entities/DAOs, `fallbackToDestructiveMigration()` |
| `MainActivity.kt` | Inject `SessionRepository` into `MainViewModel` and `AgentViewModel` |
| `AgentViewModel.kt` | `sessionId` state, `restoreSession()` (init), `newSession()`, `ensureSession()` (suspend), saves messages/artifacts/commands on flow, persists command status changes, persists role/skill/mode changes |
| `MainViewModel.kt` | Constructor now takes `SessionRepository`, persists patches on add/update, persists workspace URI/name on select |
| `ChatPanel.kt` | "New" session button with confirmation dialog |

## Sanitization
All text persisted to Room is passed through `ContextSanitizer.redactSummary()` which redacts:
- `Authorization: Bearer ...` → `Authorization: Bearer [REDACTED]`
- `sk-...` → `sk-[REDACTED]`
- `nvapi-...` → `nvapi-[REDACTED]`
- `api_key=...` → `api_key=[REDACTED]`
- `token=...`, `secret=...`, `password=...` → `[REDACTED]`
- GitHub tokens (`ghp_*`), OpenAI keys

## Session Lifecycle
1. **Create:** First `sendMessage()` triggers `ensureSession()` inside `runAgentRole()` coroutine (suspend, sequential — no race condition)
2. **Save:** Messages saved after add to list, artifacts after agent response, terminal commands on queue
3. **Restore:** `AgentViewModel.init` calls `restoreSession()` which loads last session's messages and terminal commands
4. **Delete:** "New Session" button deletes all session data (cascading) and resets in-memory state

## Migration
- **v2 → v3:** Destructive migration (`fallbackToDestructiveMigration()`). No data preserved from v2 providers/logs tables. Acceptable for dev project.
- **Future:** Add non-destructive migration when shipping to users.

## Limitations
- **Patches not restored:** `MainViewModel.pendingFileChanges` is not populated from persisted patches on session restore. Architecture gap: `MainViewModel` doesn't know the session ID at restore time.
- **Preview target not restored:** Saved in session entity but not loaded into `MainViewModel.activePreviewTarget`.
- **Active file not restored:** `selectedFileUri`/`selectedFileName` are saved but not loaded.
- **`clearChat()` is legacy:** Now just clears in-memory state without DB cleanup. Prefer `newSession()`.

## Next Steps
- Connect `MainViewModel` to session ID for full state restoration
- Add non-destructive Room migration for v4+
- Add session list/gallery UI (multiple sessions)
