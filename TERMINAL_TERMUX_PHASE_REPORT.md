# Terminal / Termux Bridge Phase Report

## Status: ✅ BUILD SUCCESSFUL in 16s

---

## Neue Dateien

- `domain/terminal/TerminalCommand.kt` — Command-Modell mit Risk/Status/Source/SafeDisplay

## Geaenderte Dateien

| Datei | Aenderung |
|-------|-----------|
| `domain/agent/CommandRiskScanner.kt` | Erweiterte BLOCKED-Patterns (dd, mkfs, API-Keys), CAUTION (npm update, git checkout ., rm/mv/cp), SAFE (node -v, npm -v, pwd) |
| `ui/workbench/TerminalPanel.kt` | Komplett-Rewrite: Command-Queue mit Risk/Status-Badges, CAUTION-Warn-Dialog, Termux-Bridge, Setup-Card, Preview-Integration |
| `ui/viewmodel/AgentViewModel.kt` | `terminalCommands: MutableList<TerminalCommand>` statt `recommendedCommands: MutableList<String>`, neue Methoden: `markCommandCopied`, `markCommandDone`, `rejectCommand`, `rejectAllCommands`, `clearCompletedCommands`, `clearRejectedCommands` |
| `ui/shell/MainShellScreen.kt` | TerminalPanel-Aufruf um `onSetPreviewUrl` erweitert |
| `ui/screen/ChatAgentScreen.kt` | `executeTerminalCommand` → `queueTerminalCommand` |
| `ui/screen/TerminalScreen.kt` | API-Migration auf `terminalCommands` / `rejectCommand` / `markCommandDone` |

---

## 1. Command Queue

### TerminalCommand Modell

```kotlin
data class TerminalCommand(
    val id: String,
    val command: String,
    val reason: String? = null,
    val riskLevel: CommandRiskLevel,
    val source: CommandSource,   // AGENT, USER, SYSTEM
    val status: CommandStatus,   // QUEUED, COPIED, MARKED_DONE, REJECTED, BLOCKED
    val createdAt: Long,
    val copiedAt: Long?,
    val notes: String?
) {
    val safeDisplay: String  // sanitized command text
}
```

Commands mit API-Keys oder Bearer-Tokens werden ueber `safeDisplay` automatisch sanitiziert.

### AgentViewModel

- `terminalCommands: SnapshotStateList<TerminalCommand>` — zentrale Command-Queue
- `queueTerminalCommand(cmd)` — Command von Agent/User zur Queue hinzufuegen
- `markCommandCopied(id)` / `markCommandDone(id)` / `rejectCommand(id)` — Einzelaction
- `rejectAllCommands()` / `clearCompletedCommands()` / `clearRejectedCommands()` — Batch-Actions

---

## 2. Risk Scanner

### BLOCKED (erweitert)
Zusaetzlich zu den bestehenden Regeln: `dd if=`, `mkfs`, Fork-Bomb-Substring-Erkennung, API-Keys/Token im Klartext (sk-*, Bearer)

### CAUTION (erweitert)
Zusaetzlich: `npm update`, `npm audit fix`, `git checkout .`, `rm` (ohne -rf), `mv`/`cp` (potentielle Ueberschreibung)

### SAFE (erweitert)
Zusaetzlich: `node -v`, `npm -v`, `pwd`

Der Scanner arbeitet case-insensitive. Unbekannte Commands werden als CAUTION klassifiziert.

---

## 3. TerminalPanel UI

### Command Cards
Pro Command: Risk-Badge (SAFE gruen / CAUTION amber / BLOCKED rot), Status-Badge, Command-Text (safedisplay), Reason, Action-Buttons

### Buttons pro Command

| Risk | Buttons |
|------|---------|
| SAFE | "Copy" + "Done" + "Reject" |
| CAUTION | "Copy (verify)" → Dialog + "Done" + "Reject" |
| BLOCKED | Nur "Blockiert — kann nicht kopiert werden" Text + "Reject" |

### Preview Integration

Bei `npm run dev` Commands: zusaetzlicher "Set Preview" Button → setzt `PreviewTarget.Url("http://127.0.0.1:5173")` via `MainViewModel.setPreviewUrl()`

### CAUTION Dialog

AlertDialog mit:
- Warnung: "Kann Dateien, Dependencies oder Build-Zustaende veraendern"
- Grund (reason) des Commands
- Command in SafeDisplay
- Buttons: "Copy anyway" / "Cancel"

### Global Buttons

- "Copy all safe" — alle SAFE Commands auf einmal kopieren
- "Clear done" — MARKED_DONE Commands entfernen
- "Clear rejected" — REJECTED Commands entfernen
- "Reject all" — alle nicht-rejected Commands rejecten
- "Setup" — Termux Setup Card toggle

---

## 4. Termux Bridge

### Detection

`PackageManager.getPackageInfo("com.termux", 0)` — ohne Caching (kein `remember` mehr), wird bei jeder Recomposition geprueft.

### Status-Anzeige

- **Termux installiert:** "Termux installiert — Commands kopieren und dort ausfuehren." + grünes CheckCircle + "Open" Button
- **Termux nicht erkannt:** "Termux nicht erkannt. Installiere Termux manuell (F-Droid / GitHub)." + Info-Icon

### Open Termux

Wenn installiert: `context.packageManager.getLaunchIntentForPackage("com.termux")` → `context.startActivity(intent)`

---

## 5. Termux Setup Card

Einklappbare Card mit 6 kopierbaren Setup-Commands:

1. `pkg update`
2. `pkg install nodejs git`
3. `termux-setup-storage`
4. `cd /sdcard/Download/DEIN_PROJEKT`
5. `npm install`
6. `npm run dev -- --host 127.0.0.1`

Buttons: "Alles kopieren" + "Set Preview URL"

Hinweis: "Fuer React/Vite/Node-Projekte laeuft der Dev-Server in Termux. PocketCodeAgent zeigt die Preview per WebView ueber http://127.0.0.1:5173."

---

## 6. ChatPanel Integration

Besteht bereits aus Phase 4/5 — RunCommand Artifact Cards zeigen:
- "Copy Command" (disabled bei BLOCKED)
- "Add to Queue" (disabled bei BLOCKED)
- BLOCKED-Text: "Blockiert: Command wird nicht kopiert oder in die Queue gelegt."

---

## 7. Sicherheit

- ✅ Commands nie automatisch ausgefuehrt
- ✅ Commands mit Secrets werden ueber `safeDisplay` sanitiziert
- ✅ BLOCKED Commands nicht kopierbar
- ✅ Kein Fake-Termux
- ✅ Kein Root
- ✅ Keine API-Keys/Secrets in Logs
- ✅ CAUTION Commands erfordern zusaetzlichen Dialog

---

## 8. Was bleibt limitiert?

- 🔲 Termux-Intent uebertraegt keine Commands automatisch (Android-Sandbox-Limit)
- 🔲 Kein Shell-Output von Termux in der App sichtbar
- 🔲 ChatAgentScreen (Legacy-Screen) verwendet noch vereinfachte Command-Darstellung
- 🔲 TerminalScreen (Legacy-Screen) ist nicht auf das neue TerminalCommand-Modell migriert worden
- 🔲 Keine Command-History-Persistierung (nur In-Memory)

---

## 9. Build

```
BUILD SUCCESSFUL in 16s
37 actionable tasks: 10 executed, 27 up-to-date
```
