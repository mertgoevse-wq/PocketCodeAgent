# PocketCodeAgent

A mobile coding agent app for Android, optimized for smartphones without root. Inspired by Replit, bolt.new, bolt.diy, Claude Code, and Cline. PocketCodeAgent empowers you to run AI-driven software engineering workflows directly from your phone.

## Key Features

- **Secure API Key Management**: Store API keys for various AI models locally, encrypted using the Android Keystore system.
- **Provider System**: Supports OpenAI-compatible APIs, including:
  - OpenRouter
  - Google Gemini / Google AI Studio
  - NVIDIA NIM
  - Groq
  - Together
  - Mistral
  - Custom OpenAI-Compatible Endpoints
- **Multi-Agent Architecture**:
  1. **PlannerAgent**: Analyzes user requests and outlines an execution plan.
  2. **CoderAgent**: Suggests, creates, and modifies code files.
  3. **ReviewerAgent**: Performs code syntax and logic reviews.
  4. **FixerAgent**: diagnoses and resolves compilation or runtime errors.
  5. **PreviewAgent**: Starts or updates the local web rendering environment.
  6. **TerminalAgent**: Recommends shell commands for the user to review and execute.
- **Local Workspace Picker**: Safely select project directories using Android's Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) with persistent URI permissions.
- **File Explorer & Editor**: View and modify project files with a built-in code editor.
- **Diff Reviews**: Compare proposed changes side-by-side (diff view) before accepting file modifications.
- **Live Preview**: Render HTML/CSS/JS projects in an embedded WebView. Supports a Termux-Bridge for Vite/React local server previews (`http://127.0.0.1:5173`).
- **Terminal UI**: Execute terminal commands safely. Commands are never run without explicit user confirmation.

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository Pattern
- **Concurreny/Async**: Kotlin Coroutines + Flow
- **Persistence**: Room Database or Preferences DataStore for configurations and metadata
- **Security**: Android Keystore API for encrypting API keys
- **Networking**: OkHttp / Ktor Client
- **File Access**: DocumentFile (Android Storage Access Framework)
- **Live Preview**: Android System WebView

## Setup & Build Instructions

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or JDK 21 (Bundled JetBrains Runtime is recommended)
- Android SDK (minSdk 26, targetSdk 34)

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/mertgoevse-wq/PocketCodeAgent.git
   cd PocketCodeAgent
   ```
2. Open the project in Android Studio.
3. Build the project using the Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run on a connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```
