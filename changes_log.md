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
- **Compose Screens**: Created all 12 requested user interface screens:
  1. `WelcomeScreen` (Onboarding description)
  2. `ProviderSetupScreen` (Preset configs, API forms, and connection test terminal)
  3. `WorkspacePickerScreen` (SAF folder selection)
  4. `ProjectDashboardScreen` (Core navigation center)
  5. `ChatAgentScreen` (AI chat stream and proposed changes/command triggers)
  6. `FileExplorerScreen` (Expandable file tree hierarchy with creation dialog)
  7. `CodeEditorScreen` (Monospaced local source editor)
  8. `DiffReviewScreen` (Line-by-line green/red code diff reviews)
  9. `TerminalScreen` (Bridge console details and recommended command approvals)
  10. `LivePreviewScreen` (Embedded WebView running static index.html or local Dev server URL)
  11. `SettingsScreen` (Reset options and security descriptions)
  12. `LogsScreen` (Color-coded diagnostic log output console)

### Compilation & Build Error Resolutions
- **Issue 1**: Gradle KSP plugin search failed in Google Maven repository.
  - *Fix*: Removed `content` matching constraints in `settings.gradle.kts` `pluginManagement` blocks so KSP resolves from Gradle Plugin Portal and Maven Central.
- **Issue 2**: Collision with multiple registered compiler extensions under the name `kotlin`.
  - *Fix*: Removed manual `kotlin.android` plugin apply statements in `app/build.gradle.kts` and root `build.gradle.kts` since `kotlin.compose` automatically applies it. Removed redundant `kotlinOptions` block.
- **Issue 3**: Room compiler failed with `unexpected jvm signature V` on KSP2 compiler.
  - *Fix*: Added `ksp.useKSP2=false` to `gradle.properties` to use the stable KSP compiler.
- **Issue 4**: Missing Android SDK path.
  - *Fix*: Generated a `local.properties` file specifying the local Android SDK path.
