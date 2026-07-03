# Bolt Shell Migration Report — PocketCodeAgent Phase 2

## Ergebnis assembleDebug

**BUILD SUCCESSFUL in 25s** ✅  
37 actionable tasks: 8 executed, 29 up-to-date  
Nur Deprecation-Warnungen (Icons.Filled.Send → AutoMirrored, outlinedButtonBorder), keine Fehler.

---

## Welche alten Screens wurden ersetzt oder integriert?

| Alt | Status | Ersetzt durch |
|---|---|---|
| `ProjectDashboardScreen.kt` | Obsolet (nicht mehr geroutet) | `MainShellScreen` |
| `ChatAgentScreen.kt` | Bleibt als Fallback auf Disk | `ChatPanel.kt` |
| `FileExplorerScreen.kt` | Bleibt als Fallback auf Disk | `FileTreePanel.kt` |
| `CodeEditorScreen.kt` | Bleibt als Fallback auf Disk | `CodeEditorPanel.kt` |
| `DiffReviewScreen.kt` | Bleibt als Fallback auf Disk | `DiffPanel.kt` |
| `LivePreviewScreen.kt` | Bleibt als Fallback auf Disk | `PreviewPanel.kt` |
| `TerminalScreen.kt` | Bleibt als Fallback auf Disk | `TerminalPanel.kt` |
| `SettingsScreen.kt` | Integriert als `ModalBottomSheet` | `MainShellScreen` |

Die alten Screen-Dateien wurden **nicht gelöscht** — sie verbleiben als Fallback-Implementierungen. Nur das Routing in `MainActivity` wurde auf die neue Shell umgestellt.

---

## Welche neuen Shell/Panel-Dateien wurden erstellt?

### Shell-Infrastruktur

- **`ui/shell/AppTab.kt`** — Enum mit 6 Tabs: Chat, Files, Code, Diff, Preview, Terminal. Jeder Tab hat Label und Icon.
- **`ui/shell/MainShellScreen.kt`** — Zentrale Oberfläche. Enthält TopBar (mit Status-Badge), ProviderModelBar (kompakte Provider/Modell-Auswahl), WorkspaceStatusBar (Ordner + Scan-Status), MainContentArea (Tab-Switch) und BottomNavigation mit Diff-Badge.

### Chat

- **`ui/chat/ChatPanel.kt`** — Bolt-like Chat. Stabile LazyColumn-Keys, gepuffertes Streaming (150ms Throttle aus Phase 1), Send/Stop-Button, Agent-Chips (Plan/Code/Review/Fix/Apply), ChatBubble mit Diff-Review und Command-Queue.

### Workbench Panels

- **`ui/workbench/FileTreePanel.kt`** — Dateibaum via WorkspaceViewModel. Ordner-Expand/Collapse, SAF-basiert.
- **`ui/workbench/CodeEditorPanel.kt`** — Code-Ansicht mit Zeilennummern und Save-Button.
- **`ui/workbench/DiffPanel.kt`** — Inline/vertikaler Diff (mobile-first, kein Side-by-Side). Annehmen/Ablehnen/Alle annehmen Buttons.
- **`ui/workbench/PreviewPanel.kt`** — WebView mit stabilem `remember`, max. 200 Console-Log-Einträgen, URL-Bar, manuellem Reload, sauberem `DisposableEffect`.
- **`ui/workbench/TerminalPanel.kt`** — Command-Queue mit Ausführen/Ablehnen-Buttons. Termux-Hinweis. Kein Fake-Terminal.
- **`ui/workbench/PanelPlaceholder.kt`** — Wiederverwendbarer Empty-State für alle Panels.

### ViewModels erweitert

- **`MainViewModel.kt`** — Neuer State: `activeTab: AppTab`, `agentStatus: AgentStatus`, `showSettingsSheet`, `openDiff()`, `openFileInEditor()`.
- **`AgentViewModel.kt`** — Neuer State: `agentJob: Job` für Cancellation. Neue Funktion `stopAgent()` für den Stop-Button.

---

## Wie funktioniert die neue Tab-Struktur?

```
MainActivity (Onboarding Router)
  ├── "welcome"  → WelcomeScreen
  ├── "providers"→ ProviderSetupScreen
  ├── "workspace"→ WorkspacePickerScreen
  └── "shell"    → MainShellScreen
                     ├── TopBar (Workspace-Name + Status-Badge)
                     ├── ProviderModelBar (Provider-Chip + Modell-Chip + Status)
                     ├── WorkspaceStatusBar (Ordner-Picker + File-Count + Reload)
                     ├── MainContentArea (wechselt per Tab)
                     │    ├── CHAT    → ChatPanel
                     │    ├── FILES   → FileTreePanel
                     │    ├── CODE    → CodeEditorPanel
                     │    ├── DIFF    → DiffPanel (mit Badge bei pending changes)
                     │    ├── PREVIEW → PreviewPanel
                     │    └── TERMINAL→ TerminalPanel
                     └── BottomNavigationBar (immer sichtbar, 6 Tabs)
```

**Settings** sind als `ModalBottomSheet` (Provider-Setup) in der Shell integriert, erreichbar über das Settings-Icon in der TopBar oder die Provider/Modell-Chips.

---

## Welche Performance-Fixes wurden erhalten?

✅ **Chat Streaming Throttling (150ms)** — `AgentViewModel.runAgentRole()` nutzt weiterhin den gepufferten Update-Mechanismus  
✅ **Stabile LazyColumn-Keys** — `ChatPanel` verwendet `key = { it.id }` für alle Nachrichten  
✅ **Einzige Streaming-Bubble** — `key = "streaming-bubble"` verhindert Flackern  
✅ **WebView DisposableEffect** — `PreviewPanel` zerstört den WebView sauber in `onDispose`  
✅ **Max 200 Console-Log-Einträge** — Automatisches Trimmen des Log-Buffers  
✅ **Workspace Scan Limits** — WorkspaceManager (aus Phase 1) bleibt unberührt  
✅ **sanitizeThrowable / Null-Fehler** — AgentRepository-Fix aus Phase 1 bleibt erhalten  
✅ **SSE Fallback Parser** — ApiClient-Fix aus Phase 1 bleibt erhalten  
✅ **Stop Button** — `AgentViewModel.stopAgent()` cancelt den laufenden Coroutine-Job  

---

## Was funktioniert schon?

- ✅ Vollständige 3-Schritt-Onboarding-Flow (Welcome → Provider → Workspace → Shell)
- ✅ 6-Tab-Navigation mit BottomBar und Diff-Badge
- ✅ TopBar mit Workspace-Name und Status-Badge (Idle/Planning/Streaming/Error)
- ✅ Kompakte ProviderModelBar für schnellen Modell-/Provider-Wechsel
- ✅ WorkspaceStatusBar mit Reload
- ✅ Settings als ModalBottomSheet (kein tiefer Umweg)
- ✅ Chat mit Send/Stop-Button, Agent-Chips, Streaming-Bubble
- ✅ FileTree mit Ordner-Expand/Collapse und Datei-Klick → Code-Tab
- ✅ CodeEditor mit Zeilennummern, Laden und Speichern
- ✅ DiffPanel mit inline vertikalem Diff (mobile-first)
- ✅ PreviewPanel mit WebView, URL-Bar, Console-Log, Reload
- ✅ TerminalPanel mit ehrlicher Termux-Brücke (keine Fake-Ausführung)
- ✅ Agent-Abbruch per Stop-Button

---

## Was ist noch Stub/Placeholder?

- 🔲 **CodeEditor** — Nur Read-only-Anzeige mit Zeilennummern. Kein Syntax-Highlighting, kein Autocomplete.
- 🔲 **DiffPanel** — Zeigt Diff-Vorschau, aber `applyPatch()` ist noch nicht an WorkspaceViewModel.applyPatch() gebunden (nur State-Manipulation).
- 🔲 **FileTree** — Zeigt nur die erste Ebene + eine Ebene Kinder. Kein vollständig lazies Deep-Expand.
- 🔲 **PreviewPanel** — Kein SAF-basiertes Static-HTML-Loading aus dem Workspace. Nur URL-basiert.
- 🔲 **TerminalPanel** — Nur Queue + Termux-Hinweis. Keine Termux-Intent-Integration.
- 🔲 **Build/Discuss Toggle** — Im ChatPanel vorbereitet strukturell, aber noch kein Mode-Switch implementiert.
- 🔲 **Theme/Appearance Settings** — Nur Provider-Setup im BottomSheet. Kein Light/Dark-Toggle.
