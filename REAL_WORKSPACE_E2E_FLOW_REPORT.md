# Real Workspace E2E Flow Hardening — Phase Report

**Date:** July 4, 2026
**Build:** ✅ test + assembleDebug (all tests passing)
**APK:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Geänderte Dateien

| File | Change |
|------|--------|
| `app/src/test/.../AgentActionParserTest.kt` | +6 tests: E2E 3-file artifact (XML+JSON), 4 blocked-folder warnings (example/sample/demo/playground) |
| `app/src/test/.../WorkspacePathHelperTest.kt` | +10 safety tests: ../ blocking, absolute path, windows path, content URI, normal root/css/js paths |
| `app/src/main/java/.../WorkspacePathHelper.kt` | **Fixed** `sanitizeRelativePath`: leading `/` check now runs BEFORE `trim('/')` — absolute paths properly blocked |
| `app/src/main/java/.../workbench/DiffPanel.kt` | Empty state: added "Test-Prompt kopieren" button with clipboard copy + Toast feedback. E2E prompt constant |
| `app/src/main/java/.../shell/MainShellScreen.kt` | Added `SnackbarHost` + `SnackbarHostState`. Apply Safe: snackbar + FileTree reload + `workspacePreviewReady` flag. Undo: snackbar + FileTree reload + conditional preview clear. |

---

## Geprüfter E2E Flow

```
User gibt Build-Prompt ein
  → ChatPanel sendet an AgentRepository
  → Agent erzeugt pocketArtifact mit pocketAction type="file"
  → File Actions erscheinen als FilePatch in MainViewModel.pendingFileChanges
  → OpenPatchCount Badge im Diff-Tab
  → DiffPanel zeigt index.html/styles.css/app.js Diffs
  → Apply Safe schreibt echte Dateien via WorkspacePatchApplier
  → Snackbar: "Dateien geschrieben. Preview bereit."
  → Files Tab zeigt neue Dateien (FileTree reload)
  → workspacePreviewReady = true (wenn index.html betroffen)
  → Preview Tab Badge "!" erscheint
  → Preview lädt Workspace-Preview via StaticPreviewBundler
  → WebView zeigt Ergebnis
  → Undo: Snackbar "Letzte Änderung rückgängig gemacht."
  → FileTree reload, preview aktualisiert
```

---

## Neue Tests

| Kategorie | Tests | Was getestet wird |
|-----------|-------|-------------------|
| **E2E Parser** | 2 | 3 file actions (index.html, styles.css, app.js) in XML + JSON, korrekte Pfade, nicht-leerer Content |
| **Blocked Folders** | 4 | example, sample, demo, playground → Warnung im parseWarnings |
| **Path Safety** | 4 | ../, /absolut, C:\, content:// → alle geblockt, Fallback greift |
| **Normal Paths** | 6 | index.html, styles.css, app.js, src/components/Button.kt → erlaubt |

---

## Smoke-Test-Prompt

In DiffPanel (leerer State) und Settings (SmokeTestCard):
> Erstelle eine kleine statische Test-Web-App mit drei Dateien: index.html, styles.css und app.js. Dunkles Premium-Design, keine Neonfarben. Titel: PocketCodeAgent E2E Test. Button mit Klickzähler. Status-Text: Preview funktioniert. Gib alle Änderungen als pocketArtifact mit pocketAction type="file" aus. Führe nichts automatisch aus.

---

## Apply/Preview/Undo Verhalten

| Aktion | Feedback | Nebeneffekt |
|--------|----------|-------------|
| Apply Safe (alle ok) | Snackbar "Dateien geschrieben. Preview bereit." | FileTree reload, workspacePreviewReady=true (wenn index.html) |
| Apply Safe (teilweise fail) | Snackbar "Einige Dateien konnten nicht geschrieben werden." | — |
| Undo | Snackbar "Letzte Änderung rückgängig gemacht." | FileTree reload, previewReady=false (nur wenn index.html betroffen) |
| Undo (nur CSS/JS) | Snackbar | FileTree reload, previewReady bleibt erhalten ✅ |

---

## Security Audit

| Check | Status |
|-------|--------|
| Pfad-Traversal (../) blockiert | ✅ sanitizeRelativePath + WorkspacePatchApplier |
| Absolute Pfade (/etc) blockiert | ✅ Fixed: check vor trim('/') |
| Blocked Folders (example/sample/demo/playground) | ✅ warnIfRestrictedPath generiert Warnung |
| content:// / file:// URIs blockiert | ✅ sanitizeRelativePath |
| Windows-Pfade (C:\) blockiert | ✅ Regex ^[A-Za-z]: |
| Secrets in Logs/Feedbacks | ✅ Snackbars zeigen nur clean messages |

---

## Bekannte Limitierungen

- StaticPreviewBundler `resolvePath` ist `private` — nicht direkt testbar ohne Android-Dependencies (außer man extrahiert es)
- Preview Bundler selbst hat keine Unit-Tests (braucht Context + DocumentFileWorkspace)
- Remove resolved patches aus DiffPanel geschieht nicht automatisch — Nutzer muss Fenster neu öffnen

---

## Build Result

```
test:           BUILD SUCCESSFUL (93+ tests)
assembleDebug:  BUILD SUCCESSFUL
APK:            app/build/outputs/apk/debug/app-debug.apk
```
