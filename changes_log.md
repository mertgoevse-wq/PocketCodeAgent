# PocketCodeAgent Change Log

## Date: 2026-07-03

### Initial App Creation & Build Setup
- Created the project structure for the Android MVP app: **PocketCodeAgent**.
- Set up root Gradle files (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`) matching modern Android Studio environments (JDK 21, compileSdk/targetSdk 36).
- Configured version catalog (`libs.versions.toml`) to include Jetpack Compose, Room database, Navigation Compose, OkHttp, GSON, and Kotlin Symbol Processing (KSP).
- Copied the standard Gradle Wrapper files from the existing `VRStreamMert` project.
- Configured `AndroidManifest.xml` with Internet, Access Network State, and Post Notifications permissions.
- Restored custom assets, colors, strings, and Material 3 Dark theme configurations.

### Security & Data Layer Implementation
- **Keystore Helper (`KeystoreHelper.kt`)**: Generated an AES/GCM hardware-backed key inside the Android Keystore system. Implemented local encryption and decryption helpers for secure API Key storage.
- **AI Providers Database (`ProviderEntity`, `ProviderDao`, `AppDatabase`)**: Designed a SQLite Room database schema to store provider metadata (Name, Base URL, encrypted API Key, model name, and custom headers).
- **Diagnostic Logging (`LogEntity`, `LogDao`)**: Implemented local DB logging for tracing filesystem operations, API connections, and agent states.

### File Workspace Layer (SAF Integration)
- **Workspace Manager (`WorkspaceManager.kt`)**: Implemented persistent URI directory permissions and recursive list parsing using Android's Storage Access Framework (`DocumentFile`).
- **File Reader/Writer**: Added support to search, load, read, create, and write code files using relative workspace paths (e.g. `src/App.js`).

### Networking & LLM Chat Client
- **OkHttp ApiClient (`ApiClient.kt`)**: Built an OpenAI-compatible HTTP client for testing API connection credentials and parsing EventSource SSE response streams chunk-by-chunk. Includes preset configurations for OpenRouter, Gemini, NVIDIA NIM, Groq, Mistral, and custom OpenAI endpoints.
- **Provider Presets**: Configured automatic headers, referrers, and OpenAI-compatible completions for the major preset platforms.

### Multi-Agent Workflows
- **Agent Roles (`AgentRole.kt`)**: Configured the 6 required roles: PlannerAgent, CoderAgent, ReviewerAgent, FixerAgent, PreviewAgent, and TerminalAgent.
- **Agent Prompts & Parsers (`AgentRepository.kt`)**: Configured custom system prompts for each agent. Created parsers to extract proposed file writes (wrapped in `<<<< FILE: path >>>>` tags) and recommended terminal commands (wrapped in `<<<< CMD: command >>>>` tags) for manual user confirmations.

### ViewModels & Jetpack Compose UI
- **ViewModels (`MainViewModel`, `ProviderViewModel`, `WorkspaceViewModel`, `AgentViewModel`)**: Bound all repositories, handled navigation routing, forms validation, folder picker callbacks, chat messaging state, text-diff calculations, and diagnostic log collection.
- **Compose Screens**: Created all 12 requested user interface screens.

### Compilation & Build Error Resolutions
- **Issue 1**: Gradle KSP plugin search failed in Google Maven repository.
  - *Fix*: Removed `content` matching constraints in `settings.gradle.kts` `pluginManagement` blocks so KSP resolves from Gradle Plugin Portal and Maven Central.
- **Issue 2**: Collision with multiple registered compiler extensions under the name `kotlin`.
  - *Fix*: Removed manual `kotlin.android` plugin apply statements in `app/build.gradle.kts` and root `build.gradle.kts` since `kotlin.compose` automatically applies it. Removed redundant `kotlinOptions` block.
- **Issue 3**: Room compiler failed with `unexpected jvm signature V` on KSP2 compiler.
  - *Fix*: Added `ksp.useKSP2=false` to `gradle.properties` to use the stable KSP compiler.
- **Issue 4**: Missing Android SDK path.
  - *Fix*: Generated a `local.properties` file specifying the local Android SDK path.

---

### Expansion: Real Agentic File Editing (Workflow & Safety Core)

- **FilePatch Model (`FilePatch.kt`)**:
  - Implemented the `FilePatch` model class representing structured file edit operations (create/modify/delete) with `path`, `action`, `oldText`, and `newText` properties.
  - Added the `AgentCommand` model representing terminal execution triggers with `command`, `reason`, and `requiresConfirmation` fields.
  - Added the `AgentResponse` wrapper representing the strict JSON schema to be output by Coder/Fixer agents.

- **DocumentFileWorkspace adapter (`DocumentFileWorkspace.kt`)**:
  - Abstracted Android Storage Access Framework (SAF) operations into a clean, reusable `DocumentFileWorkspace` interface and `DocumentFileWorkspaceImpl` class.
  - Implemented automatic pre-patch backups (generating `.bak` and `.deleted_bak` files inside the workspace directory) to enable full, persistent `Undo` capability.
  - Added explicit write-permission checks (`rootDoc.canWrite()`) to trigger appropriate warning banners during patch failures.

- **Diff Generator & Patch Applier (`DiffGenerator.kt`, `PatchApplier.kt`)**:
  - Moved line-by-line diff computation into a dedicated `DiffGenerator` utility.
  - Created `PatchApplier` coordinating execution of patches (creating directories and files, searching/replacing `oldText` chunks with `newText` content, and performing deletions) with error containment and backup restorations on `undo`.

- **Strict JSON Prompts & Robust Parser (`AgentRepository.kt`)**:
  - Updated the system prompts for `CODER` and `FIXER` agents to strictly require a single, un-wrapped JSON block matching the `AgentResponse` schema.
  - Built a robust JSON parser within `AgentRepository.kt` that automatically strips markdown code blocks (e.g. ` ```json ` tags) before deserializing with Gson.
  - Retained regex-based parsing fallbacks (for `<<<< FILE: path >>>>` and `<<<< CMD: command >>>>`) for flexibility if a provider returns plain conversational texts.

- **UI & ViewModels Integration**:
  - Updated `ChatMessage.kt` to reference lists of `FilePatch` and `AgentCommand` objects.
  - Exposed `DocumentFileWorkspace` operations and error states through `WorkspaceRepository.kt` and `WorkspaceViewModel.kt`. Exposed `applyPatch`, `prepareDiff`, and `undoLastPatch` methods.
  - Modified `ChatAgentScreen.kt` to pass lists of `FilePatch` objects to the reviewer flow, and display recommended commands alongside their descriptive reasons.
  - Rewrote `DiffReviewScreen.kt` to render patch action types, apply patches via `WorkspaceViewModel`, and display write-permission error banners dynamically.
  - Integrated `MainActivity.kt` and `MainViewModel.kt` to route file patches and filter accepted/rejected entries in the state stack.

---

### Expansion: Live Preview & Dev Server Integration

- **Vite & React Project Detection (`LivePreviewScreen.kt`)**:
  - Implemented dynamic detection of project configurations by scanning workspace folders for `package.json` and checking for Vite or React definitions.
  - When detected, the preview panel automatically scales to local dev server layout and defaults the WebView loading address to `http://127.0.0.1:5173`.

- **Segmented tab layout (`LivePreviewScreen.kt`)**:
  - Replaced the simple preview display with a structured, 3-tab user interface:
    1. 👁️ **Preview**: Displays the WebView renderer area with project-type alert banners.
    2. 💻 **Console Logs**: Lists color-coded javascript console logs (`console.log`, `console.error`, `console.warn`) captured directly from the page.
    3. ⚙️ **Termux Bridge**: Shows Termux commands and offers quick actions to copy commands and launch the Termux app via android intents.

- **WebChromeClient Console Interceptor (`LivePreviewScreen.kt`)**:
  - Overrode WebView's default `WebChromeClient.onConsoleMessage` callback to capture and map internal browser log lines to a reactive list.

- **Auto-Reload Bus (`WorkspaceViewModel.kt` & `LivePreviewScreen.kt`)**:
  - Linked the workspace file-write timestamp state variable to a `LaunchedEffect` loop inside the WebView parent, allowing automatic page reloads whenever code is modified.
