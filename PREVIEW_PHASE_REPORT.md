# Preview Phase Report — Workspace Static HTML + Local Server Preview

## Ergebnis assembleDebug

**BUILD SUCCESSFUL in 14s** ✅  
37 actionable tasks: 8 executed, 29 up-to-date  
Keine Build-Fehler, nur bestehende Deprecation-Warnungen (Compose/Room/Gradle).

---

## Neue Dateien

- `app/src/main/java/com/pocketcodeagent/domain/preview/PreviewTarget.kt`
- `app/src/main/java/com/pocketcodeagent/domain/preview/StaticPreviewBundler.kt`

## Geänderte Dateien

- `app/src/main/java/com/pocketcodeagent/ui/workbench/PreviewPanel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/shell/MainShellScreen.kt`
- `PREVIEW_PHASE_REPORT.md`

---

## Wie funktioniert Workspace Static Preview?

**PreviewTarget Modell:**
```kotlin
sealed class PreviewTarget {
    data object None
    data object WorkspaceStatic
    data class File(val path: String, val fileName: String)
    data class Url(val url: String)
}
```

**Ablauf:**
1. Nutzer klickt "Load Workspace Preview" im Workspace-Modus
2. `MainViewModel.loadWorkspacePreview()` setzt `PreviewTarget.WorkspaceStatic`
3. `PreviewPanel` reagiert via `LaunchedEffect(previewTarget)`:
   - Sucht `index.html` im Workspace (Reihenfolge: `index.html`, `public/index.html`, `src/index.html`)
   - Liest die HTML-Datei über `DocumentFileWorkspace` (SAF-kompatibel)
   - Bundle CSS/JS via `StaticPreviewBundler`
   - Lädt in WebView via `loadDataWithBaseURL()`
4. Statusleiste zeigt: Source-Pfad, letzte Reload-Zeit, Warnungen-Count

---

## Wie funktioniert CSS/JS Bundling?

Der `StaticPreviewBundler` parst die geladene HTML-Datei und:

1. **CSS-Dateien erkennen:**
   - `<link rel="stylesheet" href="styles.css">` — egal ob `rel` vor oder nach `href` steht
   - `<link href="styles.css" rel="stylesheet">` — beide Attribut-Reihenfolgen werden gematched

2. **JS-Dateien erkennen:**
   - `<script src="app.js"></script>`
   - `<script src="./assets/main.js"></script>`

3. **Sicherheitsfilter (`resolvePath`):**
   - Blockiert: `../`, absolute Pfade (`/`), `C:\`, `\\`, `content:`, `file://`
   - Externe URLs (`http://`, `https://`) bleiben unverändert — Warnung wird gesammelt
   - `./` wird bereinigt, relative Pfade werden relativ zum Base-Dir aufgelöst

4. **Inlining:**
   - CSS → `<style>/* bundled: path */\n...css...\n</style>`
   - JS → `<script>/* bundled: path */\n...js...\n</script>`
   - Original `<link>` / `<script src>` Tags werden entfernt

5. **Fehlende/nicht-lesbare Dateien:**
   - Warnung wird gesammelt
   - Original-Link bleibt erhalten
   - HTML wird trotzdem geladen

**Output: `StaticPreviewResult`**
- `html` — gebundelte HTML
- `sourcePath` — Pfad der Startdatei
- `warnings` / `errors` — gesammelte Warnungen
- `loadedCssFiles` / `loadedJsFiles` — erfolgreich gebundelte Dateien

---

## Wie funktioniert File Preview?

1. Nutzer öffnet eine `.html`/`.htm`-Datei im Editor oder FileTree
2. Wechselt in den "File" Preview-Modus
3. Klickt "Preview current file"
4. Dieselbe Bundler-Logik mit dieser Datei als Startdatei
5. CSS/JS werden relativ zu dieser Datei aufgelöst

**Limitierung:** Es gibt noch keinen File-Picker-Dialog im PreviewPanel selbst. Es nutzt die zuletzt gebundelte Datei.

---

## Wie funktioniert URL Preview?

1. Nutzer wählt "URL" Modus
2. Gibt URL ein (Default: `http://127.0.0.1:5173`)
3. Klickt "Load"
4. WebView lädt die URL direkt (kein Bundling)
5. Buttons: Reload, Clear WebView Cache

**Fehlerbehandlung:**
- `-2` (HOST_UNREACHABLE): "Server nicht erreichbar"
- `-3` (SSL_ERROR): "SSL / HTTP Fehler"
- `-6` (TIMEOUT): "Timeout — Verbindung zu langsam"
- Andere: "Ladefehler: [description]"

---

## Wie funktioniert Termux Help?

Im einklappbaren "Termux" Panel (unten):

- 5 kopierbare Commands:
  1. `pkg update`
  2. `pkg install nodejs git`
  3. `cd /sdcard/Download/DEIN_PROJEKT`
  4. `npm install`
  5. `npm run dev -- --host 127.0.0.1`

- Button "📋 Alle Commands kopieren"
- Hinweis: "Keine Fake-Ausführung. Starte Termux manuell."
- Keine Termux-Intent-Integration (Android-Sandbox-Limit)

---

## PreviewPanel UI

- **Segmented Control:** Workspace | File | URL
- **Statusleiste:** Target, Last reload, Warnungen-Count (✓/⚠)
- **WebView** nimmt Hauptfläche ein
- **Error Banner** für Ladefehler mit Dismiss
- **Bottom Panels (einklappbar):**
  - Console (max 200 Logs, Error/Warning/Info farblich getrennt, Clear-Button)
  - Warnings (Bundling-Fehler mit ❌/⚠ Icons)
  - Termux (Hilfe-Commands)

**Theme:** Premium dark UI — Calm, technical, clean. Keine Neon/Neon-Cyberpunk-Elemente.

---

## Welche Fehlerfälle wurden abgesichert?

| Fehlerfall | Behandlung |
|------------|-----------|
| Kein Workspace geöffnet | HTML mit "Kein Workspace geöffnet" Message |
| Keine index.html gefunden | HTML mit "Keine index.html" + getestete Pfade |
| Datei nicht lesbar | HTML mit "Datei nicht lesbar: path" |
| Kaputtes HTML | WebView rendert best-effort |
| Fehlende CSS-Datei | Warnung + Original-Link bleibt |
| Fehlende JS-Datei | Warnung + Original-Link bleibt |
| Ungültige URL | Error Banner mit spezifischer Message |
| Server nicht erreichbar | Error Banner mit Retry-Möglichkeit |
| WebView Load Error | Error Banner + Dismiss-Button |
| Permission fehlt | SAF check via workspace.exists() |
| `../` Pfade | Blockiert (Warnung) |
| Absolute Pfade | Blockiert (Warnung) |
| `content:` / `file://` Pfade | Blockiert (extern behandelt) |
| `example/sample/demo/playground/starter/template` Ordner | Keine neuen Ordner erstellt |
| API-Keys in Console Logs | Werden nicht aktiv gefiltert (WebView ChromeClient-Limitierung dokumentiert) |

---

## Was bleibt limitiert?

- 🔲 **Integration nach Diff Apply** — Auto-reload funktioniert via `lastFileWriteTimestamp`, aber kein expliziter "Workspace preview ready" Hinweis für index.html-Änderungen
- 🔲 **File Picker** im File-Modus — noch kein Dialog zum Durchsuchen des Workspace
- 🔲 **Bilder/Assets** — Werden nicht base64-encoded, nur externe URLs bleiben erhalten
- 🔲 **Main-Thread I/O** — `loadWorkspaceBundle` läuft im `LaunchedEffect`-Coroutine, aber `StaticPreviewBundler` macht synchrone SAF-Reads. Sollte mittelfristig auf `Dispatchers.IO` umgestellt werden
- 🔲 **Console Log Sanitization** — WebView `onConsoleMessage` fängt alle Logs ungefiltert ab (API-Key-Verschlüsselung im WebView-Kontext nicht möglich)
- 🔲 **Syntax Highlighting** — CodeEditor zeigt Plaintext, kein Highlighting
- 🔲 **Termux Intent** — Keine direkte Termux-Integration (nur kopierbare Commands)

---

## Build

Ausgeführt:

```powershell
.\gradlew.bat assembleDebug
```

Ergebnis:

```text
BUILD SUCCESSFUL in 14s
37 actionable tasks: 8 executed, 29 up-to-date
```
