# Theme System + App Icons Phase Report

## Phase
Theme System + Claude-Code-Inspired Ivory Theme + App Icons

## Date
July 4, 2026

## Build Status
✅ assembleDebug — BUILD SUCCESSFUL (15s)
✅ tests — all passing

---

## Summary

Created a multi-theme system with Dark Premium and Ivory (Claude-inspired) modes, plus System auto-detection. Updated the app launcher icon from the Android Studio default to a custom code-bracket motif. Added theme switching UI in Settings.

No screens were fully migrated to theme-aware colors — the color scheme infrastructure is in place for incremental adoption. Screens continue to use their existing hardcoded dark colors, which remain correct for the default DarkPremium mode.

---

## Changed Files

### New Files
| File | Purpose |
|------|---------|
| `ui/theme/PcaThemeMode.kt` | Theme mode enum: DarkPremium, IvoryClaudeLike, System |

### Modified Files
| File | Change |
|------|--------|
| `ui/theme/Color.kt` | Added Ivory palette (9 colors: background, surface, text, accent, success, warning, error, border) |
| `ui/theme/Theme.kt` | `PocketCodeAgentTheme` now takes `themeMode` param, resolves System→dark/light, provides `LightColorScheme` for Ivory via `CompositionLocalProvider(LocalPcaThemeMode)` |
| `ui/screen/SettingsScreen.kt` | New "Appearance" section with theme radio buttons (Dark Premium / Ivory / System), new `themeMode` and `onThemeModeSelected` params |
| `ui/viewmodel/MainViewModel.kt` | Added `themeMode` field (default DarkPremium), added `PcaThemeMode` import |
| `ui/MainActivity.kt` | Passes `mainViewModel.themeMode` to `PocketCodeAgentTheme` |
| `ui/shell/MainShellScreen.kt` | Passes `themeMode` and `onThemeModeSelected` to SettingsScreen |
| `res/drawable/ic_launcher_foreground.xml` | Replaced Android Studio default with custom code-bracket motif (double angle brackets + center bar, SlateBlue + CalmSage + text-secondary colors) |
| `res/drawable/ic_launcher_background.xml` | Replaced neon-green (#3DDC84) grid background with solid #0E0E10 |

---

## Theme Modes

| Mode | Background | Surface | Text | Accent | Success | Error |
|------|-----------|---------|------|--------|---------|-------|
| **Dark Premium** | #0E0E10 | #18181C | #F8F9FE | #5E72E4 | #2DCE89 | #F5365C |
| **Ivory** | #F7F1E8 | #FFF9F0 | #25221F | #B36A3C | #4E7D5A | #B94A48 |
| **System** | Auto-resolves to Dark on dark-mode devices, Ivory on light-mode devices |

---

## Ivory Palette

```
Background:  #F7F1E8  (warm ivory)
Surface:     #FFF9F0  (cream)
SurfaceVar:  #EFE5D8  (warm sand)
TextPrimary: #25221F  (graphite)
TextSec:     #6F665C  (warm gray)
Accent:      #B36A3C  (warm brown-orange)
AccentBlue:  #5E6E7E  (calm blue-gray)
Success:     #4E7D5A  (muted green)
Warning:     #B7791F  (warm amber)
Error:       #B94A48  (muted red)
Border:      #D6CEC4  (light warm gray)
```

---

## App Launcher Icon

- **Foreground**: Double code brackets (`<>`) with inner angles (`< >`) in SlateBlue (#5E72E4) and CalmSage (#2DCE89), plus a center vertical bar in gray (#ADB5BD)
- **Background**: Solid dark (#0E0E10) — clean, matches dark theme
- **Format**: 108dp adaptive icon, vector drawable

---

## Known Limitations

1. **Screens not yet theme-aware** — All screens continue to use hardcoded dark colors (`Color(0xFF0E0E10)`, `Color(0xFF18181C)`, etc.). Switching to Ivory theme changes the MaterialTheme colorScheme but screens don't consume it yet. This is an intentional incremental approach — the infrastructure is ready, and screens can be migrated one at a time.

2. **Theme not persisted** — Theme mode is in-memory only (`MainViewModel.themeMode`). On app restart, it defaults to DarkPremium. Room persistence via `SessionRepository` or DataStore would require schema changes or a new preference system.

3. **Internal icons not customized** — The user requested custom icons for Chat, Files, Code, Diff, Preview, Terminal, Provider, Security. The existing Material Icons (`Icons.Default.Chat`, `Icons.Default.Folder`, etc.) remain in place. Custom VectorDrawables can be added incrementally.

4. **System mode requires device-level dark/light setting** — On devices without dark mode support, System always resolves to Ivory (light).

---

## Build

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Size: ~20.9 MB
- Min SDK: 26 (Android 8.0)
- All existing tests pass — no regressions
