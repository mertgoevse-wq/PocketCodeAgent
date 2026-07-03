# Bolt.diy Mobile Blueprint: PocketCodeAgent Migration

Dieses Dokument beschreibt die technische Architektur- und Migrationsplanung für die Weiterentwicklung von PocketCodeAgent (PCA) zu einer nativen, mobilen "bolt.diy"-ähnlichen Workbench-App für Android.

## 1. Bolt.diy Komponenten & Android-Compose-Pendants

Um das nahtlose, integrierte Gefühl von bolt.diy auf kleinen Smartphone-Displays (statt Desktop) zu erreichen, müssen wir das UI stark modulieren und auf Gesten/BottomSheets setzen.

| bolt.diy Konzept | Native Android Compose Entsprechung |
| :--- | :--- |
| **Main Chat Interface** | `ModalNavigationDrawer` als Basis, primärer Chat in der Hauptansicht mit flüssigem `LazyColumn` und interaktiven Code-Blöcken. |
| **Provider / Model Selector** | `ExposedDropdownMenu` / `BottomSheet` oberhalb des Chat-Eingabefeldes, ähnlich wie in der Bolt Web-UI. |
| **API Key Settings** | Separater Tab im `ModalDrawer` oder als überlagertes `FullScreenDialog`, um Keys schnell einzufügen. |
| **Workbench / Panels** | Mobile Ansicht muss splitten: Swipe nach links oder ein Tab-System (Chat vs. Code). Auf dem Tablet: `TwoPaneLayout`. |
| **File Tree** | `ModalBottomSheet` (Swipe up von unten) oder als Side-Drawer (Swipe from right). |
| **Editor Panel** | Syntax-hervorgehobenes `TextField` (oder CodeView-Bibliothek), das den gesamten Bildschirm einnimmt, wenn eine Datei geöffnet wird. |
| **Diff View** | Side-by-Side ist mobil kaum möglich. Stattdessen: Inline-Diff (Rot/Grün hinterlegt) oder vertikales Diffing. |
| **Preview (Dev Server)** | `WebView` (AndroidX Webkit). Hinweis: Lokaler Dev-Server läuft im Hintergrund via Termux, WebView verbindet sich auf `localhost:PORT`. |
| **Terminal / Logs** | `BottomSheet` Console-Log-View. Bei Termux-Bedarf Intent-Übergabe oder Termux-Tasker/API-Anbindung für Headless-Befehle. |

## 2. Zu refactorierende / ersetzende PCA-Dateien

Aktuelle Dateien im Ordner `ui/screen`, die überarbeitet, kombiniert oder gelöscht werden müssen:

- **`ChatAgentScreen.kt`**: Wird das Herzstück (`ui/chat/MainChatScreen.kt`). Muss stark umgebaut werden, um Markdown, Artifacts (Bolt-like) und Inline-Actions parsen zu können.
- **`ProviderSetupScreen.kt` & `SettingsScreen.kt`**: Werden konsolidiert in `ui/settings/SettingsScreen.kt` (mit Fokus auf unaufdringliche, aber sichere Key-Verwaltung).
- **`FileExplorerScreen.kt` & `CodeEditorScreen.kt` & `DiffReviewScreen.kt`**: Werden Teil des neuen `ui/workbench`-Packages. Statt starrer Full-Screens werden sie als Panels/BottomSheets über den Chat gelegt.
- **`LivePreviewScreen.kt` & `TerminalScreen.kt` & `LogsScreen.kt`**: Werden ebenfalls in das `workbench`-Ökosystem integriert, um den "Build/Discuss Mode"-Wechsel (wie in Bolt) als Toggle-Button in der TopBar zu realisieren.
- **`ProjectDashboardScreen.kt` & `WorkspacePickerScreen.kt` & `WelcomeScreen.kt`**: Werden zur neuen `ui/shell/WorkspaceManagerScreen.kt` zusammengefasst.

## 3. Neue Zielstruktur (Package-Architektur)

Die Code-Basis wird aufgeräumt und fachlich in Domains getrennt:

```text
com.pocketcodeagent
├── ui/
│   ├── shell/           # MainActivity, Navigation, ModalDrawers, Themes
│   ├── components/      # Wiederverwendbare Compose-UI (Buttons, Dialogs, Markdown-Renderer)
│   ├── workbench/       # FileTree, Editor, Inline-Diff, WebView-Preview, Terminal-Logs
│   ├── chat/            # MainChatScreen, Message-Bubbles, Action-Blocks
│   └── settings/        # Provider/Model Selector, API-Key Masking & Storage
├── domain/
│   ├── agent/           # LLM Action Parser, Bolt-Artifacts (XML/Markdown Blocks extrahieren)
│   ├── workspace/       # Lokale Datei-IO, Projekt-State, File-Watching
│   └── provider/        # Model-Abstraktion (OpenRouter, Gemini, OpenAI)
└── data/
    ├── network/         # ApiClient (Retrofit/Ktor), Streaming-Requests, SSE (Server-Sent-Events)
    └── local/           # Room Database, Keystore für API-Keys
```

## 4. MVP-Priorität & Roadmap

- **Phase 1: Performance & Stabilität**
  Streamlining des `ApiClient.kt` (SSE-Streaming für Chat-Bubbles wie bei Bolt) und Keystore-Absicherung (bereits in Arbeit).
- **Phase 2: Bolt-like Mobile Shell**
  Zusammenführung der starren Screens in einen Fluid-Layout-Ansatz: "Chat" (Primary) vs. "Workbench" (Secondary, via Swipe oder Tab-Toggle).
- **Phase 3: Provider/Model Selector**
  Schneller Wechsel von Modellen direkt im Chat-Header (ohne Umweg über tiefe Settings-Menüs).
- **Phase 4: Agent Action Parser (Das Herzstück)**
  Der LLM-Output muss live geparst werden. Bolt-ähnliche Artefakte (`<boltArtifact>`, `<boltAction>`) im Stream erkennen und UI-Blöcke ("Creating files...", "Running command...") im Chat anzeigen.
- **Phase 5: Workbench Files / Editor / Diff**
  Integration der generierten Dateien in den File-Tree und Editor. Umsetzung der Inline-Diff-View vor dem Speichern der Änderungen.
- **Phase 6: Preview / Terminal (Termux Integration)**
  Anbindung an Termux via API für echte Shell-Befehle (`npm run dev`) und Darstellung des WebViews.
- **Phase 7: APK QA & Export**
  Deploy/Export-Optionen (z.B. ZIP-Export des Workspaces oder Teilen an GitHub).

## 5. Android Limitierungen & Termux Realitäts-Check

PocketCodeAgent läuft im JVM/ART-Sandkasten von Android. Es gelten strenge Grenzen ohne Root:

- **Keine native Node.js/Vite Laufzeit:** Wir können *nicht* einfach `npm start` oder einen Vite-Server im Hintergrund der nativen App ausführen (da Android keine C/C++ Node-Binary im App-Kontext bereithält).
- **Keine Fake-Features:** Wir werden keine "Pseudo-Ladebalken" bauen.
- **Lösung = Termux:** 
  Um echte Web-Projekte auszuführen, delegiert PocketCodeAgent Terminal-Kommandos (über Termux Intents oder die Termux:Tasker API) an Termux. Termux installiert und führt Node, npm, python aus.
  - *Chat & Code Editierung:* Native Android App (PocketCodeAgent).
  - *Dev-Server & Build:* Termux läuft im Hintergrund.
  - *Preview:* Die native App bindet via WebView den Port (z.B. `http://localhost:5173`) aus Termux ein.
- **Dateisystem (SAF):** PocketCodeAgent und Termux müssen sich ein Verzeichnis teilen (z.B. über Storage Access Framework `DocumentFile` oder ein geteiltes externes Verzeichnis in `Downloads/PocketCodeAgent`). Dateizugriffe müssen performant und berechtigungs-sicher gebaut werden.
