# Preview Hardening Phase 6.1 — Report

## Status: ✅ BUILD SUCCESSFUL

---

## Geaenderte Dateien

| Datei | Aenderung |
|-------|-----------|
| `AGENTS.md` | Neue Sektion "Integrated System Prompt Patterns" aus Anthropic/GitHub/VSCode/Gemini-Leaks |
| `domain/preview/StaticPreviewBundler.kt` | Suspend-Funktionen mit `Dispatchers.IO`, 500KB-Limits, CSS-Link-Attribut-Preservierung |
| `ui/workbench/PreviewPanel.kt` | Console-Log-Sanitization, Workspace-Preview-Ready-Hint, File-Preview-Verbesserungen, Copy-Logs-Button |
| `ui/viewmodel/MainViewModel.kt` | `workspacePreviewReady`-State, index.html-Erkennung bei Patch-Apply |
| `ui/shell/MainShellScreen.kt` | PreviewPanel-Aufruf um `workspacePreviewReady`/`onPreviewReadyConsumed` erweitert |

---

## 1. Dispatchers.IO + Large File Limits

### Was wurde geaendert (StaticPreviewBundler.kt)

- `bundleFromWorkspace()` und `bundleHtml()` sind jetzt `suspend`-Funktionen mit `withContext(Dispatchers.IO)`
- Alle SAF-Dateileseoperationen (WorkspaceManager, DocumentFileWorkspace) laufen auf dem IO-Dispatcher
- WebView-Aufrufe (`loadDataWithBaseURL`, `loadUrl`) bleiben auf dem Main Thread (in PreviewPanel)

### Dateigroessen-Limits

- **HTML > 500 KB:** Warnung wird gesammelt, HTML wird trotzdem gebundled (nur ohne Inline-Assets)
- **CSS/JS > 500 KB:** Datei wird nicht inline gebundled, Link bleibt als `<link>`/`<script>` erhalten + Warnung

### Neue Methode

```kotlin
private fun readFileWithSizeCheck(relativePath, workspace, rootUri, rootUriString): String?
// Returns: content | "TOO_LARGE" | null
```

---

## 2. Console Log Sanitization

### Secret-Patterns (PreviewPanel.kt)

| Pattern | Ersetzung |
|---------|-----------|
| `Bearer <20+ chars>` | `Bearer [redacted]` |
| `sk-<20+ chars>` | `sk-[redacted]` |
| `nvapi-<20+ chars>` | `nvapi-[redacted]` |
| `api_key=...`, `token=...`, `secret=...` etc. | `$1=[redacted]` |

### Funktionsweise

- `sanitizeConsoleLog(raw: String): String` wird in `WebChromeClient.onConsoleMessage()` aufgerufen
- Sanitized Logs werden in `consoleLogs` (max 200) gespeichert
- Copy-Logs-Button kopiert NUR sanitized Logs
- Keine Secrets in Rohlogs speicherbar oder kopierbar
- Breites Base64-Pattern bewusst entfernt (zu viele False-Positives in Console-Debug-Ausgaben)

---

## 3. Workspace Preview Ready Hinweis

### Ablauf

1. `MainViewModel.addPendingFileAction()` erkennt index.html in `patch.path` → setzt `workspacePreviewReady = true`
2. `MainShellScreen` gibt `workspacePreviewReady` an `PreviewPanel` weiter
3. `PreviewPanel` zeigt blauen Banner: "Workspace preview ready" + Button "Jetzt laden"
4. Klick auf "Jetzt laden" → navigiert zu Workspace-Preview und laedt index.html
5. Banner wird nach Klick/Close ausgeblendet, `onPreviewReadyConsumed` setzt Flag zurueck

### Throttling

- Kein automatisches Reload ohne Nutzeraktion
- Kein Reload bei jeder kleinen State-Aenderung
- Auto-Reload auf `lastFileWriteTimestamp` bleibt nur aktiv, wenn bereits Workspace-Preview aktiv ist

---

## 4. File Preview Verbesserungen

### HTML-Erkennung

- `FileControls` prueft jetzt, ob die aktuelle Datei `.html`/`.htm`-Endung hat
- HTML-Dateien: gruener Text mit Pfad
- Nicht-HTML-Dateien: Warnfarbe + Hinweis "Nur .html/.htm-Dateien werden unterstuetzt."
- Keine Datei ausgewaehlt: "Keine HTML-Datei ausgewaehlt — oeffne eine .html Datei im Editor."

### Button-Verhalten

- "Preview current file" nur aktiv, wenn `bundleResult != null` (Datei wurde erfolgreich gebundled)

---

## 5. Fehlertexte

Alle Fehlermeldungen sind kurz, verstaendlich und ohne Secrets:

| Situation | Meldung |
|-----------|---------|
| Kein Workspace | "Kein Workspace ausgewaehlt." |
| index.html fehlt | "Keine index.html gefunden. Getestet: index.html, public/index.html, src/index.html" |
| Datei nicht lesbar | "Konnte <path> nicht lesen." |
| CSS/JS nicht lesbar | "CSS/JS nicht lesbar: <path> — Link bleibt erhalten" |
| Datei zu gross | "CSS/JS ueberschreitet 500KB-Limit: <path>" |
| WebView Fehler | "Server nicht erreichbar / SSL Fehler / Timeout — Verbindung zu langsam" |

---

## 6. Regression-Check

Erhalten geblieben:

- ✅ WebView `remember` + `DisposableEffect` mit `destroy()` im Cleanup
- ✅ Max 200 Console Logs (aelteste werden entfernt)
- ✅ Workspace / File / URL Modes mit Segmented Control
- ✅ Termux Help Commands (copyable, 5 Steps, "Alle kopieren"-Button)
- ✅ Clear WebView Cache Button
- ✅ `loadDataWithBaseURL` fuer gebundelte HTML
- ✅ Auto-Reload auf `lastFileWriteTimestamp` (throttled via `LaunchedEffect`-Key)
- ✅ Console/Warnings/Termux Bottom Panels mit AnimatedVisibility

---

## 7. Build-Ergebnis

```
BUILD SUCCESSFUL in 7s
37 actionable tasks: 8 executed, 29 up-to-date
```

---

## 8. Was bleibt limitiert?

- Kein File Picker fuer File-Mode (nicht im Scope dieser Phase)
- Kein Binary-Asset-Bundling (Bilder etc.)
- CSS/JS-Referenzen mit zusaetzlichen Attributen (media, integrity etc.) bleiben bei externen Links erhalten, werden bei Inline-Bundling aber entfernt
- `loadWorkspaceBundleAsync` nutzt Coroutine-Scope aus Composable — bei schnellem View-Wechsel koennten Callbacks auf disposed State treffen (unwahrscheinlich bei PreviewPanel, da WebView via DisposableEffect gecleant wird)
