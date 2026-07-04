# Smoke Test Helper — Phase Report

**Date:** July 4, 2026
**Build:** ✅ assembleDebug (BUILD SUCCESSFUL)

---

## Changed Files

| File | Change |
|------|--------|
| `app/src/main/java/com/pocketcodeagent/ui/screen/SettingsScreen.kt` | Added `SmokeTestCard` composable with 8-step checklist, test prompt display, copy-to-clipboard button, and 5 navigation chips. Added optional navigation callbacks (`onOpenChat`, `onOpenFiles`, `onOpenDiff`, `onOpenPreview`, `onOpenTerminal`). |
| `app/src/main/java/com/pocketcodeagent/ui/shell/MainShellScreen.kt` | Wired navigation callbacks in SettingsScreen call: each callback closes the owner settings sheet and switches to the appropriate tab. |

---

## Smoke Test Card Content

### 8-Step Checklist

| Step | Title | Description |
|------|-------|-------------|
| 1 | Provider prüfen | Settings öffnen, Provider auswählen, API-Key eintragen, Test-Button |
| 2 | Workspace öffnen | Oben links auf Workspace tippen, lokalen Ordner auswählen |
| 3 | Build Mode wählen | Im Chat "Build" statt "Discuss" aktivieren |
| 4 | Testprompt kopieren | Button unten nutzen, dann in Chat einfügen |
| 5 | Diff prüfen | Nach Agent-Antwort im Diff-Tab Änderungen durchsehen |
| 6 | Apply safe | Nur ungefährliche Dateien übernehmen |
| 7 | Preview öffnen | Preview-Tab prüft Workspace-HTML oder URL |
| 8 | Terminal Queue prüfen | Gepuffte Commands aus Agent-Antworten |

### Test Prompt

> Erstelle eine kleine statische Test-Web-App mit index.html, styles.css und app.js. Dunkles Premium-Design, keine Neonfarben, Button mit Klickzähler. Gib alle Änderungen als pocketArtifact mit pocketAction type=file aus.

### Buttons

- **Testprompt kopieren** — copies prompt to clipboard via `LocalClipboardManager`, shows "Kopiert!" feedback for 2 seconds
- **Chat** — closes settings, navigates to Chat tab
- **Files** — closes settings, navigates to Files tab
- **Diff** — closes settings, navigates to Diff tab
- **Preview** — closes settings, navigates to Preview tab
- **Terminal** — closes settings, navigates to Terminal tab

---

## Safety Guarantees

- **Keine automatische Ausführung** — all buttons are navigation or clipboard only
- **Kein Provider-Key anzeigen** — no API key text anywhere
- **Kein Fake-Test** — only instructions, no simulated test runs
- **Nur Hilfe/Navigation** — every button either copies text or switches tabs

---

## Known Limitations

- Smoke Test Card is accessible via Settings gear → Provider sheet → "Security & Owner Settings" button → scroll down. It's two sheets deep. A direct access route from the main shell could be added.
- The `pinInput` variable in SettingsScreen is pre-existing dead code (not from this phase).
- `LocalClipboardManager` requires API 31+ for full clipboard management, but basic `setText` works on all API levels.

---

## Build Result

```
BUILD SUCCESSFUL (assembleDebug)
APK: app/build/outputs/apk/debug/app-debug.apk
```
