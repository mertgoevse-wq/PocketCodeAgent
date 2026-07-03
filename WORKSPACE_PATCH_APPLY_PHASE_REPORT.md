# Workspace Patch Apply Phase Report

## Geaenderte Dateien

- `app/src/main/java/com/pocketcodeagent/data/model/FilePatch.kt`
- `app/src/main/java/com/pocketcodeagent/data/util/PatchApplier.kt`
- `app/src/main/java/com/pocketcodeagent/domain/workspace/WorkspacePatchApplier.kt`
- `app/src/main/java/com/pocketcodeagent/data/repository/AgentRepository.kt`
- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/WorkspaceViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/shell/MainShellScreen.kt`
- `app/src/main/java/com/pocketcodeagent/ui/workbench/DiffPanel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/screen/DiffReviewScreen.kt`
- `WORKSPACE_PATCH_APPLY_PHASE_REPORT.md`

## Apply Ablauf

Agent-Dateiaktionen werden weiterhin nur als `FilePatch` in `MainViewModel.pendingFileChanges` gesammelt. Es gibt kein automatisches Schreiben.

Der neue Flow ist:

1. Agent erzeugt `CreateFile`, `ModifyFile` oder `DeleteFile`.
2. Nutzer klickt `Add to Diff` oder `Review Changes`.
3. `DiffPanel` zeigt die Patches mit Status, Aktion, Dateipfad und Diff.
4. Nutzer klickt `Apply` pro Datei oder `Apply safe`.
5. Erst dann ruft die Shell `WorkspaceViewModel.applyWorkspacePatch()` oder `applyWorkspacePatches()` auf.
6. `WorkspacePatchApplier` schreibt ueber den bestehenden `DocumentFileWorkspace`, also ueber SAF oder den lokalen Demo-Workspace.

## CREATE / MODIFY / DELETE

`FilePatch` enthaelt jetzt:

- `id`
- `path`
- `action`: `CREATE`, `MODIFY`, `DELETE`
- `oldText`
- `newText`
- `status`: `PENDING`, `APPLIED`, `REJECTED`, `CONFLICT`, `FAILED`
- `source`: `AGENT`, `USER`, `IMPORT`
- `createdAt`
- `errorMessage`
- `additions`
- `deletions`
- `requiresSecondConfirmation`
- `deleteConfirmed`
- `replaceWholeFile`

CREATE:
- Schreibt nur, wenn die Datei noch nicht existiert.
- Parent-Folder werden ueber den vorhandenen SAF-Workspacepfad erstellt.
- Existiert die Datei bereits, wird `CONFLICT` gesetzt.

MODIFY:
- Datei muss existieren.
- Wenn `oldText` vorhanden ist, muss es exakt im aktuellen Dateiinhalt gefunden werden.
- Wenn `oldText` fehlt, wird nur mit gesetzter `replaceWholeFile` Policy ersetzt.
- Ohne Match oder Policy wird nichts geschrieben und der Patch wird `CONFLICT`.

DELETE:
- Wird nie automatisch angewendet.
- `DiffPanel` verlangt zuerst `Confirm delete first`.
- Erst danach kann `Apply delete` ausgefuehrt werden.
- Vor Delete wird der Inhalt in Memory gesichert, damit Undo moeglich ist.

## Conflict Detection

Konflikte entstehen bei:

- CREATE auf vorhandener Datei
- MODIFY auf fehlender Datei
- MODIFY mit nicht gefundenem `oldText`
- MODIFY ohne `oldText` und ohne Whole-File-Policy
- DELETE ohne zweite Bestaetigung
- DELETE auf fehlender oder nicht lesbarer Datei
- blockiertem oder ungueltigem Pfad

Konflikte schreiben keine Datei und werden im DiffPanel mit Status Badge und Fehlermeldung angezeigt.

## Undo

`WorkspacePatchApplier` haelt die letzte Apply-Operation in Memory:

- CREATE Undo loescht die neu erstellte Datei.
- MODIFY Undo schreibt den alten Inhalt zurueck.
- DELETE Undo erstellt die Datei wieder und schreibt den gesicherten Inhalt.

Limitierung: Die Undo-Historie ist bewusst nur fuer die letzte Apply-Operation im Speicher gehalten. Nach App-Neustart oder Prozessende ist diese Undo-Historie nicht mehr vorhanden.

## Sicherheitspruefungen

Blockiert werden:

- leere Pfade
- `../` und `.` Segmente
- absolute Pfade
- Windows-Laufwerkspfade wie `C:\`
- `content:` Ziele als Patch-Pfad
- Nullbytes
- ungueltige Zeichen wie `< > : " | ? *`
- neue Parent-Ordner namens `example`, `sample`, `demo`, `playground`, `starter`, `template`

Fehlertexte werden gekuerzt und sanitiziert. Es werden keine API-Keys, Authorization Header oder Secrets geloggt oder angezeigt.

## UI Integration

`DiffPanel` zeigt jetzt:

- Dateipfad
- Aktion Badge
- Status Badge
- Additions/Deletions
- Fehler und Konflikte
- mobilen vertikalen Diff
- `Apply`
- `Reject`
- `View full`
- `Confirm delete first`
- `Apply safe`
- `Reject all`
- `Undo`

Beim Apply setzt die Shell `AgentStatus.APPLYING`. Nach Schreiboperationen wird der FileTree neu geladen und die aktive Editor-Datei erneut gelesen.

## Bestehende Fixes

Die bestehenden Performance- und Sicherheitsfixes wurden nicht zurueckgedreht:

- Streaming-Throttling bleibt unveraendert.
- stabile LazyColumn Keys bleiben erhalten.
- WebView Lifecycle Cleanup bleibt erhalten.
- SAF-Scanlimits bleiben erhalten.
- API-Key-Masking und Error-Sanitizing bleiben erhalten.
- Shell-Kommandos werden weiterhin nicht automatisch ausgefuehrt.

## Build

Ausgefuehrt:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Ergebnis:

```text
BUILD SUCCESSFUL
```

`git diff --check` war sauber. Es bleiben nur bestehende Gradle-/Compose-Deprecation-Warnungen.
