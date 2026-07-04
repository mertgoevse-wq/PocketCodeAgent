# Mobile UX Polish Phase — Bolt-like Final Pass

## Build Result
✅ `BUILD SUCCESSFUL` — `./gradlew.bat assembleDebug`

## Overview
Focused polish pass to unify the design system, fix theme inconsistencies, and improve information architecture — all without new architecture or regression.

## Changed Files

### New: `ui/components/PcaComponents.kt`
Shared reusable design-system composables:
- **`PcaStatusBadge`** — unified status badge with dot + label + optional count. Used consistently with `alpha = 0.13f`.
- **`PcaRiskBadge`** — risk level pill (SAFE/CAUTION/BLOCKED). Used consistently with `alpha = 0.12f`.
- **`PcaSectionHeader`** — consistent panel header with optional subtitle, icon, and action button.
- **`PcaWarningBanner`** — dismissible warning banner with WarmCopper styling.
- **`PcaCard`** — unified dark premium card surface (`Color(0xFF15151A)`, 8dp radius).
- **`PcaTouchButton`** — button with minimum 48dp touch target for accessibility.
- **`PcaTouchIconButton`** — icon button with minimum 48dp touch target.
- **`PcaEmptyState`** — delegates to existing `PanelPlaceholder` (no duplication).

### Modified: `SettingsScreen.kt`
- Replaced neon colors (`ElectricTeal`, `GlowPink`, `NeonPurple`) with theme colors (`CalmSage`, `WarmCopper`, `SlateBlue`)
- Replaced `Color.White`/`Color.Gray` with `TextPrimary`/`TextSecondary`
- Unified backgrounds to `Color(0xFF0E0E10)` and card surfaces to `Color(0xFF15151A)`
- Removed unused imports (`DeepSlateBackground`, `BorderGrey`)

### Modified: `MainShellScreen.kt`
**ShellBottomNav** now supports three badge types:
- **DIFF tab:** WarmCopper badge showing pending change count (existing, unchanged)
- **TERMINAL tab:** CalmSage badge showing queued command count (new)
- **PREVIEW tab:** SlateBlue "!" indicator when workspace preview is ready (new)

Added imports: `CommandStatus` for clean queue counting.

## Design System Improvements

| Aspect | Before | After |
|--------|--------|-------|
| Theme consistency | Mixed neon + theme colors in SettingsScreen | All screens use `SlateBlue`/`CalmSage`/`WarmCopper` |
| Text colors | Hardcoded `Color.White`/`Color.Gray` | Consistent `TextPrimary`/`TextSecondary` |
| Background colors | Multiple hardcoded backgrounds per screen | Unified `0xFF0E0E10` (container bg), `0xFF15151A` (card bg) |
| Tab badges | Only DIFF had a badge | TERMINAL (queue count) + PREVIEW (ready indicator) |
| Touch targets | Some buttons < 48dp | `PcaTouchButton`/`PcaTouchIconButton` enforce ≥48dp |
| Shared components | Each panel had its own inline badges | `PcaComponents.kt` provides reusable building blocks |

## Accessibility
- `PcaTouchButton`: minimum 48dp height and width
- `PcaTouchIconButton`: minimum 48dp height and width
- Bottom nav items: default Material 3 sizing (≥48dp touch area)
- Text remains readable on Galaxy A56 (12-13sp body, 10-11sp secondary)

## Limitations
- New `PcaComponents` are created but not yet integrated into existing panels (FileTreePanel, DiffPanel, TerminalPanel, etc.). They serve as reference components for future migration.
- SettingsScreen still uses `TopAppBar` directly instead of the new `PcaSectionHeader`.
- Terminal panel empty state uses its own inline implementation rather than `PanelPlaceholder`.

## Next Steps
- Integrate `PcaStatusBadge`/`PcaRiskBadge` into TerminalPanel, DiffPanel, and ChatPanel
- Migrate SettingsScreen to use `PcaSectionHeader` and `PcaCard`
- Add animation polish (page transitions, tab switching)
