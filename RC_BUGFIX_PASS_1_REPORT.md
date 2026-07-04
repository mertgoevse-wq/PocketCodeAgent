# RC Bugfix Pass 1 Report

## Geaenderte Dateien

- `app/src/main/java/com/pocketcodeagent/data/repository/SessionRepository.kt`
- `app/src/main/java/com/pocketcodeagent/domain/context/ContextSanitizer.kt`
- `app/src/main/java/com/pocketcodeagent/domain/workspace/WorkspacePathHelper.kt`
- `app/src/main/java/com/pocketcodeagent/ui/shell/MainShellScreen.kt`
- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/WorkspaceViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/workbench/CodeEditorPanel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/workbench/FileTreePanel.kt`
- `app/src/test/java/com/pocketcodeagent/ContextSanitizerTest.kt`
- `app/src/test/java/com/pocketcodeagent/WorkspacePathHelperTest.kt`
- `RC_BUGFIX_PASS_1_REPORT.md`

## Session Restore Fixes

- `MainViewModel` restored jetzt beim Start die letzte Session aus `SessionRepository`.
- `MainShellScreen` synchronisiert zusaetzlich mit `AgentViewModel.sessionId`, sobald der Agent seine Session geladen hat.
- Wiederhergestellt werden:
  - `pendingFileChanges` aus Room
  - `activePreviewTarget`
  - `selectedWorkspaceUri` / `selectedWorkspaceName`
  - `selectedFileUri` / `selectedFileName`
- Wenn ein Workspace vorhanden ist, geht die App sanft in die Shell und zeigt einen kleinen Restore-Hinweis.
- Fehler beim Restore werden generisch angezeigt, ohne Secrets oder rohe Exception-Texte.

## Async Conflict Check

- `WorkspaceViewModel.checkForConflicts()` blockiert nicht mehr synchron.
- Die neue Callback-Version liest SAF-Dateien auf `Dispatchers.IO`.
- `CodeEditorPanel` deaktiviert Save/Patch waehrend der Konfliktpruefung und zeigt den vorhandenen Loading-State.
- Save und Diff-Erstellung laufen erst nach erfolgreichem async Check weiter.

## Relative Path Fix

- Neuer pure Kotlin Helper: `WorkspacePathHelper`.
- Relative Pfade werden aus dem geladenen `WorkspaceFile`-Baum per URI-Match abgeleitet.
- `content://`, `file://`, absolute Pfade und Traversal werden nicht als Patchpfad verwendet.
- Fallback-Reihenfolge:
  1. `WorkspaceViewModel.openFileRelativePath`
  2. FileTree/WorkspaceFile-Match
  3. Dateiname
  4. `unknown`
- Der HTML-Preview-Button nutzt jetzt ebenfalls den relativen Workspace-Pfad statt der `content://` URI.

## Secret Detection Before Share

- `ContextSanitizer.hasPotentialSecrets()` erkennt redaktierbare Secrets ohne sie auszugeben.
- Vor File Share im CodeEditor:
  - Bei moeglichen Secrets erscheint ein Dialog.
  - Optionen: `Share sanitized copy`, `Share anyway`, `Cancel`.
  - Default-Aktion im Dialog ist die sanitized copy.
- Dateien >200 KB zeigen vor dem Teilen eine Warnung.
- Dateien >500 KB bleiben fuer Editor-Share blockiert.
- Raw Share passiert nur nach expliziter Nutzerentscheidung.

## ZIP Export UI

- `FileTreePanel` hat jetzt einen kompakten `Export ZIP` Button.
- `MainShellScreen` nutzt `ActivityResultContracts.CreateDocument("application/zip")`.
- `WorkspaceViewModel.exportWorkspaceZip()` ruft den bestehenden `WorkspaceExporter` auf `Dispatchers.IO` auf.
- Exportstatus und Fortschritt werden im Files-Panel angezeigt.
- Bestehende Excludes aus `WorkspaceExporter` bleiben erhalten:
  - `.env`
  - `google-services.json`
  - `local.properties`
  - Keystore/JKS
  - `secrets.properties`
  - `credentials.json`
  - Build-/Cache-/VCS-Ordner

## Tests

Ergaenzt:

- `ContextSanitizerTest`: `hasPotentialSecrets detects secrets without exposing them`
- `WorkspacePathHelperTest`: nested URI to relative path, content URI fallback

Keine neuen Testdependencies.

## Bekannte Rest-Limitierungen

- ZIP Import bleibt absichtlich TODO.
- Room Migration bleibt fuer diese RC-Phase destruktiv.
- Compose/UI/SAF sind weiterhin nicht instrumentiert getestet.
- `StaticPreviewBundler.kt` hat eine bestehende Compiler-Warnung: `Condition is always 'true'`.
- Bestehende Compose/Room Deprecation-Warnungen bleiben unveraendert.

## Build Ergebnis

Ausgefuehrt:

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Ergebnis:

```text
clean: BUILD SUCCESSFUL
test: BUILD SUCCESSFUL
assembleDebug: BUILD SUCCESSFUL
```

`git diff --check` war sauber. Es gab nur Windows-CRLF-Hinweise.
