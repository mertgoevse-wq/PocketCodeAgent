# Store Listing Draft — PocketCodeAgent

> **Note:** This is a draft. Before publishing, verify against current Google Play Store policies at [play.google.com/developer-content-policy](https://play.google.com/developer-content-policy/).

---

## App Name
**PocketCodeAgent** — Mobile AI Coding Workbench

---

## Short Description (max 80 characters)
AI-powered coding assistant for Android. Edit code, preview, and manage projects.

---

## Full Description

PocketCodeAgent is a native Android coding workbench that brings AI-assisted development to your phone. Chat with AI agents to plan, edit, and preview code projects — all running locally on your device.

**How it works:**
- Connect your own LLM provider (OpenAI-compatible API) or use the built-in Demo Mode
- Chat with specialized AI agents (Planner, Kotlin Engineer, UI Engineer, Reviewer, and more)
- Browse your project files via Android's Storage Access Framework
- Edit code with syntax-aware editor and line numbers
- Review AI-suggested file changes before applying them
- Preview HTML/CSS/JS projects in a built-in WebView
- Use Termux (optional) to run dev servers and preview localhost projects

**Key Features:**
- 🤖 14 specialized AI agent roles (Planner, Android Engineer, UI Engineer, Terminal Engineer, Security Reviewer, etc.)
- 💬 Chat-first interface with Discuss and Build modes
- 📁 SAF-based file tree with full read/write access to project folders
- ✏️ Monospace code editor with line numbers and diff review
- 👁️ Built-in WebView preview for static HTML and localhost projects
- 🖥️ Termux bridge for terminal commands (copy-only, no auto-execution)
- 🎨 Premium dark UI with optional Ivory light theme
- 🌐 Multi-language support (German, English, System)
- 🔒 API keys encrypted in Android Keystore — never sent to our servers

**Privacy First:**
- No backend servers — all data stays on your device
- API keys encrypted with hardware-backed Android Keystore
- File access via Android SAF — no broad storage permissions
- Logs sanitized — no secrets ever logged
- Export data only by explicit user action

---

## Feature List

| Feature | Description |
|---------|-------------|
| AI Chat Agents | 14 specialized roles with Discuss/Build modes |
| Provider Configuration | Connect OpenAI-compatible APIs + Demo Mode |
| File Explorer | SAF-based tree with create, open, delete |
| Code Editor | Monospace editor, line numbers, syntax awareness |
| Diff Review | Per-file accept/reject with backup |
| WebView Preview | Static bundling + localhost URL support |
| Terminal Bridge | Command suggestions for Termux, risk scanner |
| Workspace Export | Export files and patches as Markdown/ZIP |
| Theme System | Dark Premium + Ivory light theme |
| Language Support | German, English, System default |
| Session Persistence | Room DB with chat history, patches, commands |
| Security | Encrypted keys, sanitized logs, confirmation dialogs |

---

## Target Audience

- Android developers who want to code on-the-go
- Students learning to code with AI assistance
- Hobbyists building web projects (HTML/CSS/JS) on mobile
- Termux users who want an AI-enhanced terminal workflow
- Anyone curious about AI-assisted development on Android

---

## Privacy Notice

- PocketCodeAgent stores all data locally on your device
- API keys are encrypted in the Android Keystore and never leave the device except when making API calls to the provider you configured
- No analytics, no telemetry, no tracking
- File access requires explicit user permission via Android SAF
- Terminal commands are suggestions only — never auto-executed

---

## Known Limitations

- Requires Android 8.0 (API 26) or higher
- Real provider requires user-supplied API key (OpenAI, Anthropic, or compatible)
- Web project execution (npm, Vite) requires Termux installed separately
- No root access — file operations limited to SAF-selected directories
- Preview limited to static HTML/CSS/JS bundling and localhost URL passthrough
- Large file trees (>500 files) may load slower due to SAF constraints

---

## No Root Required

PocketCodeAgent uses Android's Storage Access Framework (SAF) for secure, scoped file access. Root access is never required and never attempted.

---

## Termux (Optional)

For running dev servers (npm, Vite, Node), install Termux separately. PocketCodeAgent only provides command suggestions — it never auto-executes shell commands. All commands must be manually copied to Termux by the user.

---

## Disclaimer

PocketCodeAgent is a tool. The user is responsible for:
- All code generated or modified using the app
- All terminal commands executed (manually, in Termux)
- Compliance with third-party API provider terms
- Reviewing all AI-suggested file changes before applying
