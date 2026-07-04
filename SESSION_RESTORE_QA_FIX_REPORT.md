# Persistent Session Restore QA + Fix — Phase Report

**Date:** July 4, 2026
**Build:** ✅ test + assembleDebug (114+ tests passing)
**APK:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Restore Matrix

| State | Saved | Restored | Handler |
|-------|-------|----------|---------|
| Chat messages | ✅ ChatMessageEntity | ✅ | AgentViewModel.restoreSession() |
| Artifacts | ✅ ArtifactEntity | ⚠️ Saved but not re-attached to messages | SessionRepository.loadArtifacts() (data in DB, silent) |
| Pending patches | ✅ FilePatchEntity | ✅ | MainViewModel.loadPendingPatches() |
| Terminal queue | ✅ TerminalCommandEntity | ✅ | AgentViewModel.restoreSession() |
| Role | ✅ selectedRoleId | ✅ | AgentViewModel.restoreSession() |
| Skill | ✅ selectedSkillId | ✅ | AgentViewModel.restoreSession() |
| Mode | ✅ agentMode | ✅ | AgentViewModel.restoreSession() + SessionRepository.restoreAgentMode() |
| **Provider** | ✅ selectedProviderId | ✅ **NOW FIXED** | MainViewModel.pendingRestoreProviderId → MainShellScreen |
| Model | ✅ selectedModel | ⚠️ Provider's modelName used; session.selectedModel not applied independently |
| Preview target | ✅ previewTargetType/Data | ✅ | MainViewModel.applyRestoredSession() |
| Workspace URI | ✅ workspaceUri | ✅ | MainViewModel.applyRestoredSession() |
| Active file | ✅ activeFileName/Uri | ✅ | MainViewModel.applyRestoredSession() |

---

## Geänderte Dateien

| File | Change |
|------|--------|
| `app/.../viewmodel/MainViewModel.kt` | Added `pendingRestoreProviderId` field; set in `applyRestoredSession()` when session has provider but none selected |
| `app/.../shell/MainShellScreen.kt` | Extended `LaunchedEffect(providers)` to restore provider from `pendingRestoreProviderId` when providers list loads |
| `app/src/test/.../SessionRestoreTest.kt` | **New.** 12 tests: PreviewTarget serialization (4), AgentMode restore (4), TerminalCommand entity mapping (2), FilePatch entity mapping (2) |

---

## Gefixte States

- **Provider-Restore (#8)**: Wenn eine Session einen `selectedProviderId` hat, wird der Provider nach dem Laden der Provider-Liste automatisch wiederhergestellt. `pendingRestoreProviderId` dient als Warteschlange bis die Provider-Liste verfügbar ist.
- **Fallback zum Demo-Mode**: Wenn der gespeicherte Provider nicht mehr existiert (gelöscht), wird auf Demo-Mode zurückgefallen (bestehende Logik, unverändert).

---

## Sanitization

- Alle persistierten Texte (Messages, Artifacts, Patches, Commands) durchlaufen `ContextSanitizer.redactSummary()`
- SessionEntity speichert nur `selectedProviderId` (FK), nie den vollständigen Provider mit API-Key
- API-Keys sind ausschließlich verschlüsselt in der Provider-Tabelle (KeystoreHelper)
- Keine Authorization-Header oder Tokens in der Session-Tabelle

---

## Migration

- AppDatabase Version: 3 (unverändert)
- Kein Schema-Change: alle benötigten Spalten existieren bereits in SessionEntity
- `fallbackToDestructiveMigration()` als Sicherheitsnetz

---

## Tests (12, alle bestehend)

| # | Test | Kategorie |
|---|------|-----------|
| 1 | PreviewTarget WorkspaceStatic serialization | Serialization |
| 2 | PreviewTarget File serialization roundtrip | Serialization |
| 3 | PreviewTarget Url serialization roundtrip | Serialization |
| 4 | PreviewTarget None deserialization | Serialization |
| 5 | AgentMode DISCUSS restore from string | AgentMode |
| 6 | AgentMode BUILD restore from string | AgentMode |
| 7 | AgentMode null → DISCUSS fallback | AgentMode |
| 8 | AgentMode invalid → DISCUSS fallback | AgentMode |
| 9 | TerminalCommand status restore from entity | Entity mapping |
| 10 | TerminalCommand invalid status → QUEUED fallback | Entity mapping |
| 11 | FilePatch entity mapping roundtrip | Entity mapping |
| 12 | FilePatch invalid action → MODIFY fallback | Entity mapping |

---

## Bekannte Limitierungen

- **Artifacts**: Werden gespeichert aber beim Restore nicht wieder an ChatMessages angehängt (Daten sind in der DB, nur UI-Restore fehlt)
- **SelectedModel**: Session speichert das gewählte Modell separat, aber beim Provider-Restore wird das Provider-Objekt mit seinem eigenen `modelName` geladen — ein abweichendes Session-Modell wird überschrieben
- **"Session teilweise wiederhergestellt"**: Keine granulare Partial-Restore-Notice — nur ein binäres "wiederhergestellt" bei Pending-Patches/Preview/Active-File
- **Kein "Workspace neu öffnen"-Button**: Nicht implementiert (nur der bestehende Folder-Picker)

---

## Build Result

```
test:            BUILD SUCCESSFUL (114+ tests)
assembleDebug:   BUILD SUCCESSFUL
APK:             app/build/outputs/apk/debug/app-debug.apk
```
