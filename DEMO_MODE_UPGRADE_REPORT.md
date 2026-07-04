# Demo Mode Upgrade — Report

## Phase
Demo Mode Upgrade

## Date
2026-07-04

## Build Status
✅ assembleDebug — BUILD SUCCESSFUL  
✅ tests — all green (including 6 new demo tests)

---

## Summary
Demo Mode (Provider ID 999) now generates rich, offline responses. When the user types a web-app-related prompt in BUILD mode, the demo agent returns a proper `pocketArtifact` with three file actions (index.html, styles.css, app.js) plus a note with preview/Termux hints. The existing role-based mock responses (Planner plan, Coder JSON patch) remain for non-web-app prompts.

Demo Mode remains strictly offline — no API calls, no API key required, clearly labeled as "Demo" throughout the UI.

---

## Changed Files

### 1. `AgentRepository.kt`
- **Smart demo response**: Detects web-app keywords in the last user message when `agentMode == BUILD`
  - Keywords: `web app`, `html`, `test`, `preview`, `landing page`, `static`, `website`, `webseite`
- **`demoWebAppResponse()`**: Returns XML `pocketArtifact` with:
  - `index.html` — dark theme, button with click counter, PocketCodeAgent branding
  - `styles.css` — premium dark design (#0e0e10, #5e72e4 SlateBlue accent)
  - `app.js` — click counter with status messages at 1/5/10 clicks
  - `note` — preview hint + optional `npm run dev`/`npx serve .` suggestion (no auto-execution)
- **`String.containsAny()`**: Helper extension for keyword matching (case-insensitive)
- **Message label**: `[Demo-Agent — Offline demo, keine echte API.]` prefix on demo responses
- **Sender label**: `${role.displayName} (Demo)` — clearly marked
- **Fallback**: Non-web-app or non-BUILD prompts use existing role-based responses unchanged

### 2. `AgentActionParserTest.kt`
- **6 new unit tests** for demo web app XML:
  - `demo web app XML produces three file actions` — verifies 1 artifact, 3 CreateFile actions, correct paths
  - `demo web app index html contains expected content` — verifies HTML structure
  - `demo web app styles css contains dark theme` — verifies CSS colors
  - `demo web app js contains click counter logic` — verifies JS event listener
  - `demo web app contains note with preview hint` — verifies preview mention
  - `demo web app response has no blocked path warnings` — verifies clean paths
- Companion object with `demoWebAppXml()` that mirrors the real `AgentRepository.demoWebAppResponse()`

---

## Demo Build Flow

```
User types: "Erstelle eine Web-App" in BUILD mode with Demo Provider (ID 999)
    ↓
AgentRepository.runAgent() detects provider.id == 999
    ↓
Checks: agentMode == BUILD && lastUserMsg contains web-app keywords → true
    ↓
demoWebAppResponse() returns XML with 3 file actions + note
    ↓
Streams chunks with 60ms delays (simulates API streaming)
    ↓
AgentActionParser.parse() extracts 1 AgentArtifact with 3 CreateFile actions
    ↓
ChatMessage emitted with (Demo) label + offline banner
    ↓
User sees artifact card with "Review Changes" button → opens DiffPanel
    ↓
DiffPanel shows index.html, styles.css, app.js → Apply writes to workspace
    ↓
Preview tab → Workspace Preview shows the rendered web app
```

---

## Provider Separation
- **Demo (ID 999)**: No API key required, never makes HTTP calls, status "Offline demo"
- **Real Provider**: Requires API key, makes real API calls, demo mock never used
- No mixing — the `provider.id == 999` check happens before any real API logic

---

## Security
- No API keys involved in demo mode
- No network calls for demo responses
- All demo content is hardcoded — no injection possible
- Demo workspace uses virtual `demo://` URIs in `DocumentFileWorkspace`

---

## Known Limitations
1. The demo XML content is duplicated between `AgentRepository.demoWebAppResponse()` and test companion for pure unit testing. If the demo content changes, tests must be updated manually.
2. The streaming bubble shows the role name (e.g., "Coder") rather than "Demo Agent" — the "Demo" badge in `ChatPanel` and the message prefix provide sufficient offline labeling.
3. Demo responses only trigger when the provider is explicitly Demo Mode — no automatic fallback if a real provider fails.

---

## Test Results
- **6 new tests** in `AgentActionParserTest`: all pass
- **All existing tests**: no regressions
- **Build**: clean + test + assembleDebug — all green

---

## APK
- `app/build/outputs/apk/debug/app-debug.apk`
