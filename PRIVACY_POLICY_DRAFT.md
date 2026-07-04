# Privacy Policy — PocketCodeAgent

*Last updated: July 4, 2026*

## 1. Overview

PocketCodeAgent ("the App") is a native Android coding workbench. The App is developed and maintained as an open-source project. This Privacy Policy explains how the App handles user data.

**Key principle: All data stays on your device.**

The App does not operate any backend servers. It does not collect analytics, telemetry, crash reports, or usage data. No data is transmitted to the developer or any third party unless you explicitly configure and use your own third-party LLM provider.

---

## 2. Data Stored Locally

The App stores the following data exclusively on your device:

| Data | Storage Location | Encrypted |
|------|-----------------|-----------|
| API Keys (user-configured) | Android Keystore (hardware-backed) | ✅ Yes |
| Provider configurations (base URL, model name) | Room Database (SQLite) | ❌ No (non-sensitive metadata) |
| Chat message history | Room Database | ❌ No |
| File change diffs / patches | Room Database | ❌ No |
| Terminal command history | Room Database | ❌ No |
| Workspace file tree cache | In-memory only | N/A |
| App preferences (theme, language) | SharedPreferences | ❌ No |
| Diagnostic logs | Room Database, auto-capped at 200 entries | ❌ No (sanitized) |

All local databases can be cleared via the Settings screen.

---

## 3. API Key Handling

- API keys are encrypted using the hardware-backed **Android Keystore** system before storage
- API keys are only used to authenticate requests to the **LLM provider you configure**
- API keys are never sent to developers, analytics services, or any third party other than your chosen provider
- API key fields in the UI are **masked by default** (password field)
- API keys are **redacted from all logs** — log sanitization patterns cover `sk-*`, `nvapi-*`, and Bearer tokens
- If you share screenshots or screen recordings, ensure API key fields are hidden

---

## 4. Provider API Requests

When you send a chat message:

1. Your message content and selected workspace context are sent to the LLM provider **you configured**
2. The request includes your API key in the `Authorization` header
3. The provider's privacy policy applies to data you send to them
4. PocketCodeAgent has no control over how the provider handles your data
5. You are responsible for reviewing your provider's terms and privacy policy

**No request ever goes to a PocketCodeAgent server** — the App has no backend infrastructure.

---

## 5. Workspace File Access

- File access uses Android's **Storage Access Framework (SAF)** — you select which directory the App can access
- The App requests **persistable URI permissions** to maintain access across sessions
- Files are read for: displaying the file tree, opening files in the editor, building workspace context for AI prompts, static HTML preview bundling, and workspace export
- Files can be modified: only after you review and accept AI-suggested changes
- No files are ever transmitted to the developer
- No files are transmitted to third parties unless you explicitly export/share them

---

## 6. Export and Sharing

- Workspace export (ZIP/Markdown) is only triggered by **explicit user action**
- Patch Markdown export is only triggered by **explicit user action**
- The Android share sheet may be used to share exported files — this is user-initiated
- No automatic cloud sync, backup, or sharing

---

## 7. Diagnostic Logs

- Logs are stored locally in the Room Database
- Logs are **sanitized** — API keys, Bearer tokens, and secrets are redacted
- Logs are capped at **200 entries** (oldest deleted first)
- Logs can be cleared from Settings
- Logs are never transmitted off-device

---

## 8. WebView Preview

- The built-in WebView preview loads HTML/CSS/JS content **from your local workspace**
- No external network requests are made by the preview unless your local code includes them
- WebView console logs are sanitized to remove any Bearer tokens or API keys

---

## 9. Third-Party Services

The App uses the following third-party libraries (all run locally):

| Library | Purpose | Data Handled |
|---------|---------|--------------|
| OkHttp | HTTP client for API requests | Provider API calls only |
| Room | Local database | Chat history, patches, logs |
| Gson | JSON parsing | API response parsing |
| Jetpack Compose | UI framework | No data handled |
| DocumentFile | SAF file access | File I/O |

These libraries do not transmit data to third parties.

---

## 10. Children's Privacy

The App is not directed at children under 13. It does not knowingly collect data from children.

---

## 11. Changes to This Policy

This policy may be updated. Changes will be reflected in the App's repository and this document.

---

## 12. Contact

This is an open-source project. For privacy questions:
- Open an issue in the project repository
- Review the source code (all data handling is transparent)

---

## 13. Disclaimer

THE APP IS PROVIDED "AS IS" WITHOUT WARRANTY. THE USER IS SOLELY RESPONSIBLE FOR:
- Configuring and securing their own API keys
- Reviewing AI-suggested code changes before applying them
- Complying with their chosen LLM provider's terms of service
- Any code executed in Termux or other terminal environments
