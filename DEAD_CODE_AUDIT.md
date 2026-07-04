# Dead Code Audit — PocketCodeAgent

## Date
July 4, 2026

## Summary
After many agent phases, the project accumulated legacy screens, duplicate models, and unused utilities. This audit identifies candidates for safe removal.

---

## Audit Table

| File / Item | Reason | References | Risk | Recommendation |
|------------|--------|-----------|------|----------------|
| **ChatAgentScreen.kt** | Legacy chat screen, replaced by ChatPanel + MainShellScreen | 0 references outside own file | None | DELETE |
| **CodeEditorScreen.kt** | Legacy editor, replaced by CodeEditorPanel | 0 references outside own file | None | DELETE |
| **DiffReviewScreen.kt** | Legacy diff, replaced by DiffPanel | 0 references outside own file | None | DELETE |
| **FileExplorerScreen.kt** | Legacy file tree, replaced by FileTreePanel | 0 references outside own file | None | DELETE |
| **LivePreviewScreen.kt** | Legacy preview, replaced by PreviewPanel | 0 references outside own file | None | DELETE |
| **TerminalScreen.kt** | Legacy terminal, replaced by TerminalPanel | 0 references outside own file | None | DELETE |
| **ProjectDashboardScreen.kt** | Legacy dashboard, replaced by MainShellScreen | 0 references outside own file | None | DELETE |
| **LogsScreen.kt** | Legacy logs, replaced by SettingsScreen | 0 references outside own file | None | DELETE |
| **AgentRole.kt** (old enum) | Replaced by AgentRegistry/RichAgentRole | 0 references to AgentRole.PLANNER or AgentRole.CODER | None | DELETE |
| **PatchApplier.kt** (data/util) | Replaced by domain/workspace/WorkspacePatchApplier | Only self-reference + FileRelevanceScorer mention | None | DELETE |
| **AgentCommand** (data class) | Still used by ChatMessage and AgentRepository | 9 references in ChatMessage, AgentRepository, FilePatch | Active | KEEP |
| **AgentResponse** (data class) | Used by AgentRepository.parseAgentResponse() | 1 reference in AgentRepository | Active | KEEP |
| **DiffGenerator** (data/util) | Used by WorkspaceViewModel | 1 reference in WorkspaceViewModel | Active | KEEP |
| **NeonPurple** alias | Legacy color alias for SlateBlue | Used only in old screens being deleted | None after screen deletion | DELETE from Color.kt |
| **ElectricTeal** alias | Legacy color alias for CalmSage | Used only in old screens being deleted | None after screen deletion | DELETE from Color.kt |
| **GlowPink** alias | Legacy color alias for WarmCopper | Used only in old screens being deleted | None after screen deletion | DELETE from Color.kt |
| **GrayBorder** alias | Legacy color alias for BorderGrey | Used only in old screens being deleted | None after screen deletion | DELETE from Color.kt |
| **values-de/strings.xml** | Duplicate of values/strings.xml (both German) | N/A | None | DELETE (base values/ serves as German fallback) |

---

## Files to KEEP (Legacy but referenced)

| File | Why Keep |
|------|----------|
| AgentCommand (in FilePatch.kt) | Used by ChatMessage, AgentRepository |
| AgentResponse (in FilePatch.kt) | Used by AgentRepository.parseAgentResponse() |
| DiffGenerator.kt | Used by WorkspaceViewModel |
| PatchApplier.kt — **DELETED** (replaced by WorkspacePatchApplier) | — |

---

## Files to KEEP (Active screens)

| File | Why Keep |
|------|----------|
| WelcomeScreen.kt | Onboarding flow |
| ProviderSetupScreen.kt | Provider configuration |
| WorkspacePickerScreen.kt | SAF folder picker |
| SettingsScreen.kt | Settings and owner security |
| ChatPanel.kt | Main chat UI |
| MainShellScreen.kt | Shell with tabs |
| All workbench panels | CodeEditorPanel, DiffPanel, FileTreePanel, PreviewPanel, TerminalPanel |
