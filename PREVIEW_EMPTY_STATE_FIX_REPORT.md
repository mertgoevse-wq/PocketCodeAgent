# Preview Empty State Fix Report

## Problem

Preview Tab zeigte große weiße leere Fläche im WebView bei `PreviewTarget.None`. Für Nutzer unklar, ob Preview geladen wurde, leer ist, oder ein Fehler vorliegt.

## Geänderte Dateien

- `app/src/main/java/com/pocketcodeagent/ui/workbench/PreviewPanel.kt` (rewrite)

## Neue Empty States

### PreviewTarget.None
- **Kein leerer weißer WebView** mehr
- Stattdessen: Dark Empty State mit Titel "Keine Preview geladen"
- Subtitle: "Lade eine Workspace-Preview, eine HTML-Datei oder eine lokale Server-URL."
- Buttons:
  - "Load Workspace Preview" (nur wenn Workspace geöffnet)
  - "Load URL 127.0.0.1:5173"

### Loading State
- Dunkler Loading State während Bundling (isBundling = true)
- CircularProgressIndicator + "Preview wird gebaut..."
- **Kein weißer Flicker** beim Laden

### Error State
- Wenn BundleResult errors enthält (z.B. "Keine index.html gefunden"):
  - Dunkler Error-State mit Icon
  - Fehlertexte in Cards angezeigt
  - Retry + Load URL Buttons

### Workspace Controls
- Wenn keine index.html gefunden: zeigt alle getesteten Pfade an (index.html, public/index.html, src/index.html)
- Wenn index.html existiert: "Load Workspace Preview" Button

## WebView Background Handling

- `setBackgroundColor(0xFF0E0E10)` → dunkler Hintergrund vor HTML-Load
- `onPageFinished` → `setBackgroundColor(0)` um HTML-eigenen Hintergrund zu erlauben
- Keine weiße Fläche mehr während des Ladens

## URL Error UX

Bei Server nicht erreichbar:
- Error Card mit "Server nicht erreichbar"
- Buttons: Retry | Set URL 5173 | Termux Hilfe
- Termux Hilfe öffnet das Bottom-Panel mit Commands

## Build Ergebnis

```
assembleDebug: BUILD SUCCESSFUL
```

Keine neuen Compiler-Fehler.
