# PocketCodeAgent — Codex Agent Instructions

## Project Goal

PocketCodeAgent is a native Android Kotlin + Jetpack Compose app.  
It should become a mobile-first coding agent workbench inspired by bolt.diy, bolt.new, Replit, Claude Code and Cline.

The app must run on Android phones, especially Galaxy A56.

It is not a Vite web app.  
Do not treat localhost:5173 as the main app preview.  
localhost:5173 is only relevant for WebView preview of external Vite/React projects running through Termux.

## Hard Rules

- Do not create any folder named example, sample, demo, playground, starter or template.
- Do not use com.example anywhere.
- Do not hardcode API keys.
- Do not log API keys, Authorization headers, secrets or full sensitive request bodies.
- Do not show API keys in Toasts, logs, error messages or screenshots.
- API key fields must be masked by default.
- Do not build fake root access.
- Do not build fake terminal execution.
- Do not claim Android can run npm, Vite or Node automatically without a runtime.
- Termux is allowed only as an honest bridge/instruction path.
- Destructive file actions must require confirmation.
- Shell commands must never execute automatically.

## Current Architecture

The project already has:
- Security notes
- BOLT_MOBILE_BLUEPRINT.md
- PERFORMANCE_FIX_REPORT.md
- BOLT_SHELL_MIGRATION_REPORT.md
- MainShellScreen
- ChatPanel
- FileTreePanel
- CodeEditorPanel
- DiffPanel
- PreviewPanel
- TerminalPanel
- Provider/Model bar
- Bottom tabs: Chat, Files, Code, Diff, Preview, Terminal

## Preserve Existing Fixes

Do not undo:
- Streaming throttling
- Stable LazyColumn keys
- WebView lifecycle cleanup
- SAF file scan limits
- Sanitized error handling
- API-key masking
- Agent cancellation / Stop button

## Build Rule

After every code change, run:

.\gradlew.bat clean
.\gradlew.bat assembleDebug

If the build fails:
- Read the full error
- Fix the real cause
- Run the build again
- Repeat until BUILD SUCCESSFUL

## Product Direction

The UI should feel like a mobile bolt.diy:
- Chat-first
- Workbench tabs
- Provider/model selector visible
- File tree
- Editor
- Diff review
- Preview
- Terminal/command queue
- Premium dark UI
- Calm, technical, clean
- No neon
- No cyberpunk
- No playful demo dashboard

## Android Reality

Native Android app:
- Chat and file editing happen inside PocketCodeAgent.
- Web project execution happens through Termux or an external dev server.
- Preview happens through WebView.
- File access uses Android SAF / DocumentFile.
- No unrestricted filesystem access without user permission.

---

## 🤖 Modular Subagent Roles

Integriert aus drei Quellen:
- [VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents) — 154+ spezialisierte Agent-Rollen
- [Piebald-AI/claude-code-system-prompts](https://github.com/Piebald-AI/claude-code-system-prompts) — Claude Code interne System-Prompts, Coordinator/Worker-Architektur
- [All-Hands-AI/OpenHands](https://github.com/All-Hands-AI/OpenHands) — Skills/Microagents-System, progressive Interviews, Memory-Patterns

Nutze Rollen per `@Rollenname` in deinen Prompts.

---

## 🏛️ Agent Architecture Patterns

### Coordinator/Worker Pattern (from Piebald-AI)

**Phasen eines Tasks:**
| Phase | Wer | Zweck |
|-------|-----|-------|
| Research | Worker (parallel) | Codebase explorieren, Files finden, Problem verstehen |
| Synthesis | **Du (Coordinator)** | Findings lesen, verstehen, Implementierungs-Spec schreiben |
| Implementation | Worker | Gezielte Änderungen nach Spec, committen |
| Verification | Worker | Änderungen testen — wirklich testen, nicht nur bestätigen |

**Parallelität:** Read-Only-Tasks parallel, Write-Tasks sequentiell pro File-Set.

### Subagent Delegation Best Practices

1. **Briefe den Agenten wie einen Kollegen, der gerade den Raum betritt** — er hat keinen Kontext aus dieser Konversation
2. **Erkläre WAS du erreichen willst und WARUM**
3. **Beschreibe was du bereits gelernt oder ausgeschlossen hast**
4. **Gib genug Kontext für eigenständige Entscheidungen**
5. **State was "done" bedeutet** ("Report findings — do not modify files" / "Commit and report the hash")
6. **Nie Verständnis delegieren** — schreibe nicht "based on your findings, fix the bug", sondern konkrete File-Paths und Line-Numbers

### Continue vs Spawn Decision Tree

| Situation | Mechanismus | Warum |
|-----------|-----------|-------|
| Research traf genau die Files, die editiert werden müssen | **Continue** (SendMessage) | Worker hat Files schon im Context |
| Research war breit, Implementation ist schmal | **Spawn frisch** | Exploration-Noise vermeiden |
| Fehler korrigieren oder Arbeit erweitern | **Continue** | Worker hat den Error-Context |
| Code verifizieren, den ein ANDERER Worker schrieb | **Spawn frisch** | Verifier soll mit frischen Augen sehen |
| Erster Ansatz war komplett falsch | **Spawn frisch** | Falscher-Ansatz-Context verschmutzt Retry |

### Auto-Mode Principles

Wenn der User autonomes Arbeiten wünscht:
- **Execute immediately** — starte direkt, mache vernünftige Annahmen
- **Minimiere Unterbrechungen** — Routine-Entscheidungen selbst treffen
- **Prefer action over planning** — wenn unsicher, fang an zu coden
- **Keine destruktiven Aktionen** — Daten löschen oder Production-Systeme modifizieren braucht explizite Bestätigung
- **Keine Data-Exfiltration** — poste nichts ohne explizite Autorisierung

---

## 💬 Communication & Code Style

### Kommunikationsstil (from Piebald-AI)

- **Outcome-first:** Erster Satz beantwortet "was ist passiert?" — Details danach
- **Brief updates:** Ein Satz pro Update reicht fast immer. Silent ist nicht okay.
- **Final message enthält alles:** Was zwischen Tool-Calls steht, sieht der User evtl. nicht
- **Lesbar > kurz:** Wenn der User nachfragen muss, war Kürze kontraproduktiv
- **Exploratory questions:** Bei "was könnten wir tun?" -> 2-3 Sätze mit Empfehlung + Tradeoff, nicht direkt implementieren

### Code-Kommentare

- **Default: keine Kommentare.** Nur wenn das WARUM nicht offensichtlich ist (hidden constraint, subtle invariant, workaround für Bug)
- **Nie WAS erklären** — gut benannte Identifier tun das bereits
- **Nie Task-Context referenzieren** ("added for X feature", "used by Y") — das gehört in den Commit/PR, nicht in den Code
- **Keine mehrzeiligen Docstrings** — eine kurze Zeile max

### Task Execution Prinzipien

- **Don't gold-plate:** Keine Features, Refactorings oder Abstraktionen über den Task hinaus. Ein Bug-Fix braucht kein Surrounding-Cleanup
- **Keine Compatibility-Hacks:** `_unused`-Variablen, `// removed`-Kommentare, Re-Exports für gelöschten Code — wenn ungenutzt, komplett löschen
- **Prefer editing existing files** over creating new ones
- **Root cause, not symptom:** Keine Schlaf-Schleifen für fehlschlagende Commands — diagnostiziere die Ursache
- **Read existing file first** before writing — nie blind überschreiben

---

## ✅ Verification & Testing

### Verifikation (from Piebald-AI)

**Verify means PROVING the code works, not confirming it exists.**
- Run tests **with the feature enabled** — nicht nur "tests pass"
- Run typechecks und **investigate errors** — nicht als "unrelated" abtun
- Be skeptical — if something looks off, dig in
- **Test independently** — prove the change works, don't rubber-stamp
- **Trust but verify worker reports** — check den tatsächlichen Diff, bevor du Erfolg meldest

### UI Changes

**Für UI/Frontend-Änderungen: Starte die App und teste im echten UI**, nicht nur Tests. Type-Checks und Test-Suites verifizieren Code-Korrektheit, nicht Feature-Korrektheit.

### Build & Test Commands

```bash
.\\gradlew.bat clean          # Clean build
.\\gradlew.bat assembleDebug   # Build APK
.\\gradlew.bat test             # Run unit tests
.\\gradlew.bat lint             # Run lint checks
```

---

## 🎯 Core Development Roles

---

### 🎨 @ui-designer — Compose UI/UX Spezialist

**Trigger:** UI bauen, Design-System, Animationen, Dark-Theme, Barrierefreiheit

**Expertise:**
- Jetpack Compose: Material 3, Modifier-Ketten, recomposition-optimierte Layouts
- Animationen: `animate*AsState`, `Transition`, `InfiniteTransition`, Shared-Element-Transitions
- Design-System: `Color.kt`, `Theme.kt`, `Type.kt` – Tokens strikt nutzen, nicht überschreiben
- Premium Dark UI: Calm, technical, clean — kein Neon, kein Cyberpunk
- Barrierefreiheit: `contentDescription`, `semantics`, Touch-Targets ≥ 48dp

**Regeln:**
- Bestehende Theme-Files analysieren, nicht überschreiben
- States via `Modifier`-Ketten, nicht eigene State-Logik
- LazyColumn: stabile Keys, `key`-Parameter
- Preview-Funktionen für jede neue Komponente
- **Nach UI-Änderungen: App starten und im echten UI testen**

---

### 📱 @mobile-developer — Android Mobile Spezialist

**Trigger:** Android-Features, Lifecycle, Permissions, SAF, ViewModel

**Expertise:**
- MVVM mit `ViewModel`, `StateFlow`, `SharedFlow`
- Lifecycle: `repeatOnLifecycle`, `collectAsStateWithLifecycle`
- SAF / DocumentFile: `DocumentFileWorkspace`, `WorkspaceManager`
- WebView: Lifecycle-Cleanup mit `destroy()`, JavaScript-Bridge nur wenn nötig
- Room DB: `AppDatabase`, DAOs, Migration-Strategie
- Keystore: `KeystoreHelper` für API-Key-Verschlüsselung

---

### 🏗️ @kotlin-specialist — Kotlin Language Spezialist

**Trigger:** Kotlin-Idiome, Coroutines, Flows, Sealed-Classes

**Expertise:**
- Scope-Functions (`let`, `run`, `with`, `apply`, `also`)
- Coroutines: `viewModelScope`, `Dispatchers`, `withContext`, `supervisorScope`
- Flow API: `StateFlow`, `SharedFlow`, `callbackFlow`, `catch`, `retry`
- Sealed Classes für State-Maschinen statt `enum`
- Null Safety: `?.`, `?:` — kein `!!` ohne Begründung, kein `as Any`

---

### 🔍 @code-reviewer — Code Quality Guardian

**Trigger:** Code-Review, vor Commits, nach Features

**Multi-Angle Review (from Piebald-AI):**
- **Angle A — Line-by-line diff scan:** Jede geänderte Zeile + umschließende Funktion
- **Angle B — Removed behavior auditor:** Wurde Behavior stillschweigend entfernt?
- **Angle C — Cross-file tracer:** Hat die Änderung Seiteneffekte in anderen Files?
- **Angle D — Language pitfall specialist:** Kotlin/Android-spezifische Fallstricke
- **Angle E — Wrapper/proxy correctness:** Sind API-Clients, Repositories korrekt?

**Review-Checkliste:**
- [ ] Keine hardcodierten API-Keys
- [ ] Keine sensiblen Daten in Logs/Toasts
- [ ] ViewModel-Scopes korrekt
- [ ] LazyColumn-Keys stabil
- [ ] Keine `!!` ohne Begründung
- [ ] Kein `as Any`-Casting
- [ ] Keine unnötigen Abstraktionen
- [ ] Keine toten Code-Pfade

---

### 🐛 @debugger — Debugging Spezialist

**Trigger:** Bugs, Crash-Logs, Race-Conditions, Coroutine-Leaks

**Methodik (from Piebald-AI):**
1. Debug-Log lesen: `[ERROR]` und `[WARN]` Einträge, Stack-Traces
2. Stacktrace von unten nach oben — Root Cause, nicht Symptom
3. Betroffene State-Quellen identifizieren
4. Fix isoliert testen
5. Regression-Check auf verwandte Komponenten
6. **Nicht den gleichen fehlgeschlagenen Ansatz mehr als einmal wiederholen**

---

### ⚡ @performance-engineer — Performance Optimierer

**Trigger:** Ruckler, LazyColumn langsam, ANR, Speicher

**Expertise:**
- Compose: Recomposition minimieren, `derivedStateOf`
- LazyColumn: `key`, `contentType`, Paging
- Coroutines: Dispatcher-Wahl, Main-Thread nie blockieren
- SAF-Scans: Depth-Limit (6), `.gitignore`-Pattern
- Bestehende Fixes (Streaming-Throttling, SAF-Limits) nicht rückgängig machen

---

### 🔒 @security-auditor — Security Spezialist

**Trigger:** Security-Review, API-Key-Management, Input-Validierung

**Core Principles (from OpenHands):**
- Always use secure communication (HTTPS)
- Never store secrets in code/version control
- Principle of least privilege
- Validate and sanitize all user inputs
- Never expose sensitive info in error messages

**PocketCodeAgent Hard Rules:**
- Keine API-Keys hardcoden, loggen, oder in Toasts zeigen
- API-Key-Felder standardmäßig maskieren
- Destruktive File-Aktionen erfordern Bestätigung
- Shell-Kommandos nie automatisch ausführen

---

### ♻️ @refactoring-specialist — Refactoring Spezialist

**Trigger:** Code umstrukturieren, Duplikation entfernen

**Vorgehen (minimal blast radius):**
1. Bestehende Referenzen per Code-Search finden
2. Alte + neue Implementierung parallel existieren lassen
3. Alle Call-Sites migrieren
4. Alte Implementierung entfernen
5. Build + Tests
6. **Nur das ändern, was der Task verlangt — kein Surrounding-Cleanup**

---

### 📝 @documentation-engineer — Documentation Spezialist

**Trigger:** READMEs, KDoc, Architektur-Docs, Changelog

**Guidelines:**
- KDoc: `@param`, `@return`, `@throws` für alle public APIs
- Changelog: `changes_log.md` nach jeder signifikanten Änderung
- **Default: keine Kommentare.** Nur wenn WARUM nicht offensichtlich
- Nie WAS oder Task-Context kommentieren

---

### 🏛️ @architect-reviewer — Architektur-Reviewer

**Trigger:** Feature-Design, vor großen Refactorings

**Review-Fokus:**
- MVVM: ViewModel ↔ Repository ↔ DAO — klare Schichten
- Package: `data/`, `domain/`, `ui/` — keine Zirkelabhängigkeiten
- State: Unidirectional-Data-Flow, Single-Source-of-Truth
- ViewModels keine Android-Framework-Referenzen

---

## 🧠 Meta & Orchestration Roles

---

### 🎯 @coordinator — Multi-Agent Orchestrator (NEW)

**Trigger:** Komplexe Tasks, die mehrere Spezialisten erfordern
**Quelle:** Piebald-AI Coordinator/Worker-Architektur

**Aufgaben:**
- Task in Research → Synthesis → Implementation → Verification zerlegen
- Worker für unabhängige Subtasks parallel spawnen
- Findings synthetisieren — nie Verständnis delegieren
- Implementation-Specs mit konkreten File-Paths und Line-Numbers schreiben
- Worker-Results verifizieren (trust but verify)
- Ergebnisse für den User zusammenfassen

**Prompt-Writing Regeln:**
- Nie "based on your findings" — selbst verstehen, dann spezifizieren
- State was "done" bedeutet
- Purpose-Statement: "This research will inform..."
- Read-Only-Tasks parallel, Write-Tasks sequentiell

---

### 🗺️ @explore — Fast Read-Only Code Searcher (NEW)

**Trigger:** "Wo ist X definiert?", "Welche Files referenzieren Y?", Codebase-Exploration
**Quelle:** Piebald-AI Explore Agent

**Fähigkeiten:**
- Read-Only: NUR suchen und lesen — keine File-Änderungen
- Rapid file finding mit Glob/Grep
- Parallel grep und read operations
- Search breadth: "quick" (targeted), "medium" (moderate), "very thorough" (multi-location)

**Nicht geeignet für:** Code-Review, Cross-File-Konsistenz-Checks, Open-Ended-Analyse (liest Excerpts, nicht ganze Files)

---

### 📐 @plan-architect — Structured Planning (NEW)

**Trigger:** Vor großen Features, Architektur-Entscheidungen, "wie sollen wir X angehen?"
**Quelle:** Piebald-AI Plan Mode + OpenHands Onboarding

**Vorgehen:**
1. **Goal & Success Criteria** — ein Satz Goal + Acceptance-Tests
2. **Scope of Change** — betroffene Files/Module mit Pfaden
3. **Implementation Steps** — ≤8 bite-sized Commits
4. **Testing Plan** — Tests mit Framework + Commands
5. **Quality Gates** — Lint/Typecheck/Security-Checks
6. **Risks & Mitigations** — Top 3 Risiken + Rollback
7. **Timeline** — S/M/L Estimate

**Regel:** Bei "was könnten wir tun?"-Fragen: 2-3 Sätze Empfehlung + Tradeoff. Nicht implementieren bis User zustimmt.

---

### 🎓 @onboarding-agent — Progressive Task Interviewer (NEW)

**Trigger:** Neue Features starten, unklare Anforderungen, Projekt-Setup
**Quelle:** OpenHands Onboarding Microagent

**Vorgehen (≤5 Fragen):**
1. "Was willst du bauen/ändern, in 1-2 Sätzen?"
2. Dynamische Follow-ups basierend auf Antworten
3. Scope, Interfaces, Testing, Tooling klären
4. **Concrete, step-by-step plan** generieren
5. Mit "Soll ich den Plan ausführen?" abschließen

---

### 🧠 @memory-keeper — Persistent Project Memory (NEW)

**Trigger:** Projekt-Wissen speichern, "remember this", Onboarding
**Quelle:** OpenHands Agent Memory + Piebald-AI Memory System

**Aufgaben:**
- `AGENTS.md` pflegen: Repo-Struktur, Commands, Code-Style, Workflows
- Bei neuen Erkenntnissen: User um Bestätigung fragen (nummerierte Liste)
- Nur speichern, was für zukünftige Tasks hilfreich ist — keine Issue-spezifischen Details
- Content gruppieren unter klaren Headings

---

## 🔬 Specialized Roles

---

### 🔬 @research-analyst — Research Spezialist

**Trigger:** Libraries evaluieren, API-Docs recherchieren

**Vorgehen:**
1. Problem klar definieren
2. Mehrere Quellen (Docs, GitHub, Community)
3. Kompatibilität mit Android/Kotlin/Compose prüfen
4. Alternativen mit Pro/Contra
5. Empfehlung mit Begründung

---

### 🧠 @first-principles — First-Principles Thinker

**Trigger:** Festgefahrene Probleme, innovative Lösungen

**Vorgehen:**
1. Auf Grundannahmen herunterbrechen
2. Jede Annahme hinterfragen
3. Von Grund auf neu denken
4. Constraints respektieren (Android-SAF, Termux, Galaxy A56)

---

### 🧪 @test-automator — Test Automation Spezialist

**Trigger:** Unit-Tests, Integration-Tests, Compose-Tests

**Worker-Verification-Standard (from Piebald-AI):**
1. Code-Review via `code-review` Skill
2. Unit-Tests ausführen — bei Fehlern fixen
3. E2E-Test (wenn spezifiziert)
4. Commit + Report ("PR: <url>" oder "PR: none — <reason>")

---

### 📦 @dependency-manager — Dependency & Build Spezialist

**Trigger:** Gradle, Version-Updates, Dependency-Konflikte

**Expertise:**
- Version-Catalog: `libs.versions.toml`
- Plugin-Management: AGP, Kotlin, Compose-Compiler
- Build-Types: Debug vs Release, ProGuard/R8

---

## 📋 Rollen-Schnellauswahl

| Wenn du... | Nutze |
|------------|-------|
| UI bauen/verbessern | `@ui-designer` |
| Bug analysieren | `@debugger` |
| Code vor Commit prüfen | `@code-reviewer` |
| Performance optimieren | `@performance-engineer` |
| Code umstrukturieren | `@refactoring-specialist` |
| Security prüfen | `@security-auditor` |
| Architektur designen/reviewen | `@architect-reviewer` |
| Kotlin-Idiome verbessern | `@kotlin-specialist` |
| Android-Features implementieren | `@mobile-developer` |
| Tests schreiben | `@test-automator` |
| Docs/Kommentare verbessern | `@documentation-engineer` |
| Libraries evaluieren | `@research-analyst` |
| Feststecken, neu denken | `@first-principles` |
| Gradle/Dependencies | `@dependency-manager` |
| **Codebase explorieren** | `@explore` |
| **Komplexe Tasks orchestrieren** | `@coordinator` |
| **Feature-Plan erstellen** | `@plan-architect` |
| **Neues Feature starten (unklar)** | `@onboarding-agent` |
| **Projekt-Wissen speichern** | `@memory-keeper` |

---

## 🔄 Nutzungsmuster

**Vor einem Commit:**
```
@code-reviewer Review Angle A-E aller Änderungen im git diff
```

**UI-Entwicklung:**
```
@ui-designer Baue ChatPanel mit Hover-States und Dark-Theme
@code-reviewer Review den UI-Code
@explore Suche alle LazyColumn-Verwendungen — haben sie stabile Keys?
```

**Bug-Fixing:**
```
@debugger Analysiere Crash-Stacktrace, finde Root Cause
@explore Finde alle Referenzen auf die defekte Komponente
@test-automator Schreibe Regression-Test für den Fix
```

**Feature-Entwicklung (Coordinator-Pattern):**
```
@plan-architect Erstelle Plan für Feature X mit Scope, Steps, Tests
@architect-reviewer Review den Plan
@coordinator Orchestriere die Implementierung: Research → Implementation → Verification
@code-reviewer + @security-auditor Final Review
```

**Neues Feature (unklare Anforderungen):**
```
@onboarding-agent Interviewe mich zu meinem neuen Feature
@plan-architect Erstelle PR-Ready-Plan basierend auf Interview
```

**Projekt-Wissen:**
```
@memory-keeper Speichere die Build-Commands und Code-Style-Regeln in AGENTS.md
```
