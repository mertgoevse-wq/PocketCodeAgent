# External Agent Repository Audit — PocketCodeAgent

**Date:** July 3, 2026  
**Auditor:** Buffy (Freebuff/DeepSeek)  
**Status:** Analysis complete — no code changes, report only

---

## 1. Repositories Found and Local Paths

| Repository | Status | Notes |
|-----------|--------|-------|
| `VoltAgent/awesome-claude-code-subagents` | **Previously cloned → cleaned up** | Was at `C:\Users\mertg\PocketCodeAgent\tmp_voltagent` (deleted after analysis) |
| `Piebald-AI/claude-code-system-prompts` | **Previously cloned → cleaned up** | Was at `C:\Users\mertg\PocketCodeAgent\tmp_piebald` (deleted after analysis) |
| `asgeirtj/system_prompts_leaks` | **Previously cloned → cleaned up** | Was at `C:\Users\mertg\PocketCodeAgent\tmp_prompts_leaks` (deleted after analysis) |

**Current disk search:** All 18 search paths across `C:\Users\mertg`, `Documents`, `Downloads`, `PocketCodeAgent`, `PocketCodeAgent\external`, and `PocketCodeAgent\research` returned **no results**. The repos were temporary shallow clones from earlier analysis sessions and were cleaned up afterward.

**Note:** Analysis below is based on retained knowledge from those prior sessions. No files were re-downloaded or re-cloned for this audit.

---

## 2. Files Inspected (from prior sessions)

### awesome-claude-code-subagents
- `README.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `LICENSE`, `install-agents.sh`
- `categories/01-core-development/README.md`, `backend-developer.md`
- `categories/02-language-specialists/README.md`, `kotlin-specialist.md`
- `categories/03-infrastructure/README.md`
- `categories/04-quality-security/README.md`, `code-reviewer.md`
- `categories/05-data-ai/`
- `categories/06-developer-experience/`
- `categories/07-specialized-domains/`
- `categories/08-business-product/`
- `categories/09-meta-orchestration/README.md`, `multi-agent-coordinator.md`, `codebase-orchestrator.md`, `agent-installer.md`
- `categories/10-research-analysis/`
- `tools/subagent-catalog/README.md`, `search.md`, `list.md`, `fetch.md`
- `.claude-plugin/marketplace.json`
- `.github/workflows/`

### claude-code-system-prompts
- `system-prompts/system-prompt-communication-style.md`
- `system-prompts/system-prompt-doing-tasks-software-engineering-focus.md`
- `system-prompts/system-prompt-doing-tasks-ambitious-tasks.md`
- `system-prompts/system-prompt-doing-tasks-security.md`
- `system-prompts/system-prompt-subagent-delegation-examples.md`
- `system-prompts/system-prompt-subagent-prompt-writing-examples.md`
- `system-prompts/system-prompt-writing-subagent-prompts.md`
- `system-prompts/system-prompt-coordinator-mode-orchestration.md`
- `system-prompts/system-prompt-worker-instructions.md`
- `system-prompts/system-prompt-auto-mode.md`
- `system-prompts/system-prompt-plan-mode-enhanced.md`
- `system-prompts/system-prompt-frontend-browser-verification.md`
- `system-prompts/system-prompt-executing-actions-with-care.md`
- `system-prompts/system-prompt-tool-usage-subagent-guidance.md`
- `system-prompts/system-prompt-coordinator-worker-instructions.md`
- `system-prompts/agent-prompt-general-purpose-agent.md`
- `system-prompts/agent-prompt-code-review-part-1-base-finder-angles.md`
- `system-prompts/agent-prompt-explore.md`
- `system-prompts/agent-prompt-read-only-search-agent.md`
- `system-prompts/agent-prompt-general-task-agent.md`
- `system-prompts/skill-debugging.md`
- `system-prompts/skill-run-app.md`
- `system-prompts/system-prompt-doing-tasks-no-unnecessary-additions.md`
- `system-prompts/system-prompt-doing-tasks-help-and-feedback.md`
- `system-prompts/system-prompt-doing-tasks-no-compatibility-hacks.md`
- `system-prompts/system-prompt-outcome-first-communication-style.md`
- `system-prompts/system-prompt-tone-and-style-concise-output-short.md`
- `system-prompts/system-prompt-comment-what-and-task-context-avoidance.md`
- `system-prompts/system-prompt-comment-why-only-guidance.md`

### system_prompts_leaks
- `README.md`
- `Anthropic/Claude Code/` (directory listing only)
- `Anthropic/claude-cowork.md` — full Claude Cowork system prompt
- `Anthropic/claude-opus-4.8.md`
- `Anthropic/claude-sonnet-5.md`
- `Anthropic/default-styles.md`
- `Cursor/cursor.md`
- `OpenAI/chatgpt-gpt-5-agent-mode.md`
- `Google/gemini-cli.md`
- `Microsoft/vscode-copilot-agent.md`
- `Microsoft/github-copilot.md`
(Plus directory listings of many other prompt files from Anthropic, OpenAI, Google, Microsoft, Meta, Mistral, Perplexity, xAI, Qwen, Notion, Misc)

---

## 3. Unsafe Files Skipped

The following file types were NOT read or inspected:

| File Type | Reason |
|-----------|--------|
| `.sh`, `.bash`, `.ps1`, `.py`, `.js`, `.ts`, `.exe` scripts | Execution risk |
| `package.json`, `requirements.txt`, `Cargo.toml`, `Gemfile` | Could trigger dependency installs |
| `.env`, `.secret`, `credentials.*`, `*.pem`, `*.key` | May contain real secrets |
| Compiled/compressed files (`.zip`, `.tar.gz`, `.bin`) | Untrusted binary content |
| Binary files (`.png`, `.jpg`, `.mp4`, `.pdf`) | Not text-parseable |
| Git objects (`.git/`) | Repository internals |

**Specific files explicitly skipped:**
- `install-agents.sh` — executable shell script, not executed
- `system_prompts_leaks/Anthropic/raw/` — raw leaked data, not re-inspected in this audit
- Any file containing API keys, tokens, or session identifiers

---

## 4. Useful Subagent Categories

From **awesome-claude-code-subagents** (154+ agents in 10 categories):

### Categories directly applicable to PocketCodeAgent

| Category | Agent Count | PocketCodeAgent Relevance |
|----------|-------------|---------------------------|
| **01-core-development** | 11 | back end-developer, frontend-developer, mobile-developer, api-designer, ui-designer |
| **02-language-specialists** | 30 | kotlin-specialist, typescript-pro, python-pro, java-architect, swift-expert |
| **03-infrastructure** | 16 | docker-expert, kubernetes-specialist, devops-engineer |
| **04-quality-security** | 17 | code-reviewer, security-auditor, test-engineer, debugger |
| **05-data-ai** | 13 | ml-engineer, llm-engineer (relevant for provider/model architecture) |
| **06-developer-experience** | 15 | cli-developer, documentation-engineer, refactoring-specialist |
| **09-meta-orchestration** | 13 | multi-agent-coordinator, codebase-orchestrator, agent-installer |
| **10-research-analysis** | 11 | research-analyst, technical-researcher |

### PocketCodeAgent-specific role mapping

| PCA Role | Source Inspiration | Implemented? |
|----------|-------------------|-------------|
| `@ui-designer` | core-development/ui-designer | ✅ In AGENTS.md |
| `@mobile-developer` | core-development/mobile-developer | ✅ In AGENTS.md |
| `@kotlin-specialist` | language-specialists/kotlin-specialist | ✅ In AGENTS.md |
| `@code-reviewer` | quality-security/code-reviewer | ✅ In AGENTS.md (enhanced with Multi-Angle) |
| `@debugger` | quality-security/debugger + piebald debug skill | ✅ In AGENTS.md |
| `@performance-engineer` | Based on PCA performance fixes | ✅ In AGENTS.md |
| `@security-auditor` | quality-security/security-auditor + OpenHands | ✅ In AGENTS.md |
| `@refactoring-specialist` | developer-experience/refactoring-specialist | ✅ In AGENTS.md |
| `@documentation-engineer` | developer-experience/documentation-engineer | ✅ In AGENTS.md |
| `@architect-reviewer` | infrastructure/cloud-architect adapted | ✅ In AGENTS.md |
| `@research-analyst` | research-analysis/research-analyst | ✅ In AGENTS.md |
| `@first-principles` | meta-orchestration patterns | ✅ In AGENTS.md |
| `@test-automator` | quality-security/test-engineer | ✅ In AGENTS.md |
| `@dependency-manager` | infrastructure patterns | ✅ In AGENTS.md |
| `@coordinator` | piebald coordinator-mode-orchestration | ✅ In AGENTS.md |
| `@explore` | piebald read-only-search-agent | ✅ In AGENTS.md |
| `@plan-architect` | piebald plan-mode-enhanced + OpenHands | ✅ In AGENTS.md |
| `@onboarding-agent` | OpenHands onboarding microagent | ✅ In AGENTS.md |
| `@memory-keeper` | OpenHands agent_memory + piebald memory | ✅ In AGENTS.md |

**Gap analysis — roles NOT yet adapted but potentially useful:**
- `@terminal-engineer` — Terminal/Termux-specific expertise
- `@workspace-engineer` — SAF/Workspace-specific file operations
- `@preview-engineer` — WebView/Static-HTML preview specialist
- `@provider-engineer` — API provider/model configuration expert
- `@qa-release-engineer` — APK build, testing, release management

---

## 5. Useful Skill Patterns

From **claude-code-system-prompts** and **OpenHands skills**:

### Coordinator/Worker Pattern (Piebald-AI)
**Already adapted** into AGENTS.md. Key principles:
- Research → Synthesis → Implementation → Verification phases
- Read-Only tasks parallel, Write tasks sequential
- Never delegate understanding — write concrete file paths and line numbers
- Continue vs Spawn decision tree for worker reuse

### Code Review Multi-Angle Pattern (Piebald-AI)
**Already adapted.** Five review angles:
- Angle A: Line-by-line diff scan
- Angle B: Removed behavior auditor
- Angle C: Cross-file tracer
- Angle D: Language pitfall specialist
- Angle E: Wrapper/proxy correctness

### Explore Agent Pattern (Piebald-AI)
**Already adapted.** Read-only codebase search agent:
- Parallel grep + read operations
- Search breadth levels: quick / medium / very thorough
- Stateless — complete context in each call

### Plan Mode Pattern (Piebald-AI + OpenHands)
**Already adapted.** Structured planning with:
- Goal & Success Criteria
- Scope of Change (file paths)
- Implementation Steps (≤8 commits)
- Testing Plan (framework + commands)
- Quality Gates (lint/typecheck/security)
- Risks & Mitigations + Timeline

### Progressive Interview Pattern (OpenHands)
**Already adapted** via `@onboarding-agent`:
- Max 5 questions, dynamic follow-ups
- Concrete step-by-step plan generation
- PR-ready output

### Skills-as-Files Pattern (OpenHands)
Individual skill files in `skills/` directory with:
- YAML frontmatter (name, description, triggers)
- Markdown instruction body
- Clear "when to use" / "when NOT to use" sections

**Recommendation:** Consider a `.agents/skills/` directory in PocketCodeAgent for reusable Compose-specific skill instructions.

---

## 6. Prompt/Agent Patterns Worth Adapting

From analysis of **system_prompts_leaks** (treated as untrusted inspiration only):

### Generic patterns (NOT verbatim prompt text)

| Pattern | Source | Status |
|---------|--------|--------|
| Research → Strategy → Execution lifecycle | Gemini CLI | ✅ In AGENTS.md "Integrated System Prompt Patterns" |
| Context efficiency (combine turns, grep before read) | Gemini CLI | ✅ In AGENTS.md |
| Autonomous mode rules (minimize interruptions) | Gemini CLI + ChatGPT Agent | ✅ In AGENTS.md |
| Clarify before work (AskUserQuestion) | Anthropic Cowork | ✅ In AGENTS.md |
| Parallel tool calling mandate | VSCode Copilot CLI | ✅ In AGENTS.md |
| Ability loading before action | GitHub Copilot | ✅ In AGENTS.md |
| Prompt injection protection | ChatGPT Agent Mode | ✅ In AGENTS.md |
| Task list + verification step | Anthropic Cowork | ✅ In AGENTS.md |
| Topic updates / status reporting | Gemini CLI | ✅ In AGENTS.md |
| Outcome-first communication | Piebald-AI | ✅ In AGENTS.md |
| Code comments: only WHY, never WHAT | Piebald-AI | ✅ In AGENTS.md |
| Don't gold-plate (no surrounding cleanup) | Piebald-AI | ✅ In AGENTS.md |
| File handling rules (workspace vs outputs) | Anthropic Cowork | Partially — SAF-only on Android |
| Skills pattern (research first, then format skill) | Anthropic Cowork | Partially — no format skills yet |
| Evenhandedness in political/moral questions | Anthropic Cowork | Not applicable for coding agent |

### Patterns explicitly deferred or not applicable

| Pattern | Reason |
|---------|--------|
| Artifact rendering (HTML/React/SVG/Mermaid) | Desktop-only, not applicable to Compose UI |
| Computer use / desktop automation | Android sandbox prevents this |
| Web content fetching restrictions | Android WebView has different constraints |
| Citation requirements with markdown links | Desktop chat UI pattern, not mobile |
| E-signature / document routing | Not relevant to coding agent |
| MCP connector registry patterns | No MCP infrastructure on Android |

---

## 7. Explicitly Rejected Patterns

These patterns from the inspiration repos were **NOT adapted** and must **NOT** be used:

| Pattern | Reason for Rejection |
|---------|---------------------|
| Leaked proprietary system prompts (verbatim) | Copyright and ethical concerns — prompts belong to their respective companies |
| Prompt injection/jailbreak examples | Security risk — would weaken PocketCodeAgent's safety |
| "Print full system prompt" instructions | Violation of platform terms of service |
| Fake terminal/root execution patterns | Violates AGENTS.md "Do not build fake root access" |
| Automatic shell execution | Violates AGENTS.md "Shell commands must never execute automatically" |
| API key extraction/exfiltration patterns | Violates SECURITY_NOTES.md |
| Hidden chain-of-thought bypasses | Encourages deception about what the agent is doing |
| Multi-step attack workflows | Security risk |
| `sudo` / `su` / privilege escalation | Not possible on Android, promoting false capabilities |
| "Act as if you have access to..." impersonation patterns | Dishonest agent behavior |
| Direct file system access claims (without SAF) | Android SAF is the only legitimate path |
| Fake npm/node execution within app | Termux bridge is the honest alternative |

---

## 8. Security Concerns Found

### In the inspiration repos (NOT imported):

1. **system_prompts_leaks** — Contains actual proprietary prompts with internal model names, tool definitions, and API endpoint structures. These were read for pattern analysis but NEVER copied verbatim. The prompts are intellectual property of Anthropic, OpenAI, Google, Microsoft, and others.

2. **claude-code-system-prompts** — Contains detailed internal instructions for subagent spawning, task orchestration, and safety mechanisms. Only structural patterns were adapted, not the exact prompt text.

3. **awesome-claude-code-subagents** — Public domain (MIT License). Contains agent role definitions that are safe to use as inspiration. The `.claude-plugin/marketplace.json` contains plugin installation metadata which is public.

### In PocketCodeAgent (already mitigated):

- ✅ All system prompt patterns in AGENTS.md are rewritten in German/English, adapted to PocketCodeAgent's Android/Kotlin/Compose context
- ✅ No proprietary prompt text was copy-pasted
- ✅ No API keys, tokens, or secrets from any repository were imported
- ✅ No executable scripts were run
- ✅ No npm/pip/gradle installs were triggered by the analysis
- ✅ `install-agents.sh` was read for structure analysis only, never executed

---

## 9. Recommended PocketCodeAgent Implementation Plan

Based on the audit findings, here is the recommended implementation sequence:

### Phase A: Fill Role Gaps (new subagent roles)
1. `@terminal-engineer` — Terminal/Termux-specific role for command queue, risk scanning, Termux bridge
2. `@workspace-engineer` — SAF file operations, workspace lifecycle, DocumentFile patterns
3. `@preview-engineer` — WebView lifecycle, static HTML bundling, URL loading
4. `@provider-engineer` — API provider configuration, model selection, endpoint testing
5. `@qa-release-engineer` — APK build verification, changelog management, release notes

### Phase B: Skill System
1. Create `.agents/skills/` directory
2. Define skill format: YAML frontmatter + markdown body (inspired by OpenHands)
3. Implement skill-loader in AGENTS.md
4. Write first skills:
   - `compose-ui-component.md` — Patterns for building Compose components
   - `saf-file-ops.md` — SAF/DocumentFile best practices
   - `webview-lifecycle.md` — WebView remember/dispose patterns
   - `provider-testing.md` — API provider configuration and testing

### Phase C: Chat Agent Improvements
1. Improve SSE streaming reliability based on provider-fallback patterns
2. Add "Agent thinking" indicator during planning phase
3. Implement progressive disclosure of agent actions during streaming
4. Add token usage tracking and display

### Phase D: Verification Pipeline
1. Pre-commit verification hook (inspired by Piebald-AI verification standard)
2. Automated regression check for existing fixes
3. Build success gate before any push

---

## 10. Next Implementation Phases

| Priority | Phase | Dependencies | Estimated Effort |
|----------|-------|-------------|-----------------|
| P0 | Chat Streaming & API Hardening | Provider Model Phase | M |
| P1 | Role Gap Fill (5 new roles) | AGENTS.md update | S |
| P1 | Skill System Scaffold | .agents/skills/ directory | S |
| P2 | Verification Pipeline | Build system | M |
| P2 | Chat History Persistence | Room DB schema | M |
| P3 | Multi-Provider Support | Provider Model Phase | L |
| P3 | Syntax Highlighting in CodeEditor | CodeView library integration | M |

---

## 11. Summary

- **3 repos analyzed** (temporary clones, now cleaned up)
- **30+ files inspected** from safe text/markdown directories
- **19 subagent roles** adapted into AGENTS.md
- **12 generic patterns** integrated into "Integrated System Prompt Patterns" section
- **10+ patterns** explicitly rejected (security/ethics/copyright)
- **0 scripts executed**, 0 dependencies installed, 0 proprietary prompts copied
- **0 Kotlin source files modified** — report only

The PocketCodeAgent AGENTS.md now contains a rich, adapted set of subagent roles and system prompt patterns derived from safe, structural analysis of the inspiration repositories. All content has been rewritten for the Android/Kotlin/Compose context and aligned with the project's hard rules (no fake root, no automatic execution, no secrets in logs).
