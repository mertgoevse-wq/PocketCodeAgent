# Agent Action Parser Phase Report

## Neue Dateien

- `app/src/main/java/com/pocketcodeagent/domain/agent/AgentMode.kt`
- `app/src/main/java/com/pocketcodeagent/domain/agent/AgentRunState.kt`
- `app/src/main/java/com/pocketcodeagent/domain/agent/AgentAction.kt`
- `app/src/main/java/com/pocketcodeagent/domain/agent/AgentArtifact.kt`
- `app/src/main/java/com/pocketcodeagent/domain/agent/AgentActionParser.kt`
- `app/src/main/java/com/pocketcodeagent/domain/agent/CommandRiskScanner.kt`

## Unterstuetzte pocketArtifact/pocketAction Typen

Der Parser erkennt mehrere `<pocketArtifact>` Bloecke pro Antwort und darin:
- `type="file"`: `CreateFile`, oder `ModifyFile` wenn `oldText` vorhanden ist
- `type="modify"`: `ModifyFile`
- `type="delete"`: `DeleteFile`
- `type="shell"` / `type="command"`: `RunCommand`
- `type="preview"`: `OpenPreview`
- `type="note"`: `Note`

Unvollstaendige oder unbekannte Action-Typen crashen nicht. Sie werden als Warnung gesammelt und soweit moeglich als Note angezeigt.

## JSON-Fallback

`AgentActionParser` unterstuetzt JSON-Antworten mit:
- `summary`
- `actions`
- legacy `patches`
- legacy `commands`

Ungueltiges JSON wird nicht geworfen, sondern als Note-Artifact mit Parse-Warnung angezeigt.

## CommandRiskScanner Regeln

`CommandRiskLevel`:
- `SAFE`
- `CAUTION`
- `BLOCKED`

Blockiert werden unter anderem:
- `rm -rf`
- `sudo`
- `su`
- `chmod 777`
- `curl | sh`
- `wget | sh`
- `format`
- `del /s`
- `adb shell rm`
- `powershell -enc`
- base64-decode-and-execute Muster

`CAUTION` gilt unter anderem fuer:
- `npm install`
- `npm run build`
- `git reset`
- `git clean`
- `gradlew clean`
- `pip install`
- `pkg install`

`SAFE` gilt unter anderem fuer:
- `npm run dev`
- `npm test`
- `gradlew assembleDebug`
- `ls`
- `dir`
- `cat`
- `type`

Der Scanner arbeitet case-insensitive.

## Integration in ChatPanel

`ChatPanel` hat jetzt einen echten `Discuss` / `Build` Toggle:
- `DISCUSS`: Antworten werden als Notes angezeigt; File-, Command- und Preview-Actions werden ignoriert.
- `BUILD`: System Prompt fordert `pocketArtifact` / `pocketAction` an.

Finale Agent-Antworten rendern Artifact Cards unter der Bubble. Cards zeigen Typ-Label, Titel, Summary und bei Commands ein Risk Badge. Buttons:
- `Review Changes`
- `Add to Diff`
- `Copy Command`
- `Add to Queue`
- `Open Preview`

`BLOCKED` Commands koennen nicht kopiert oder in die Queue gelegt werden.

## Integration in DiffPanel, TerminalPanel und PreviewPanel

File Actions werden in bestehende `FilePatch` Objekte uebersetzt und ueber `MainViewModel.addPendingFileAction()` in die Diff-Review uebernommen. Es wird nichts automatisch angewendet.

RunCommand Actions werden ueber `MainViewModel.addCommandAction()` und `AgentViewModel.queueTerminalCommand()` in die Terminal Queue gelegt. Die Terminal UI wurde textlich angepasst: PocketCodeAgent behauptet nicht, Commands selbst auszufuehren.

OpenPreview Actions setzen `MainViewModel.activePreviewTarget`, wechseln in den Preview Tab und geben das Ziel an `PreviewPanel` weiter. Der WebView laedt das Ziel erst nach Nutzerbutton `Open Preview`.

## Was noch fehlt

- Keine persistierten Artifact-Historien ausserhalb der aktuellen Chat-Liste.
- Keine echten Parser-Unit-Tests; die Parserlogik wird aktuell ueber Build und UI-Pfade verifiziert.
- DeleteFile hat jetzt Review-Markierung, aber noch keine zweite Bestaetigungs-UI im DiffPanel.
- Terminal bleibt eine ehrliche Queue; keine Termux-Intent-Ausfuehrung.

## Build-Ergebnis

Ausgefuehrt:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Ergebnis:

```text
BUILD SUCCESSFUL
```

Letzter `assembleDebug` Lauf: erfolgreich in 24s, 37 Tasks ausgefuehrt. Es bleiben nur bestehende Deprecation-Warnungen aus Compose/Room/Gradle, keine Build-Fehler.
