# Language Settings & Localization Phase Report

## Phase
Language Settings and Localization

## Date
July 4, 2026

## Build Status
✅ assembleDebug — BUILD SUCCESSFUL  
✅ tests — all passing

---

## Summary

Created a language settings system with System/German/English modes. Added 55 translatable string resources across three locales (values/strings.xml as German base, values-de/strings.xml, values-en/strings.xml). Language choice is persisted via SharedPreferences and applied at app startup via `Configuration.setLocale()`. Language change requires app restart.

Screens still use hardcoded strings — the string resources provide the metadata and translations for incremental migration.

---

## Changed Files

### New Files
| File | Purpose |
|------|---------|
| `domain/language/LanguageMode.kt` | Enum: System (null tag), German ("de"), English ("en") |
| `res/values-de/strings.xml` | German strings (55 entries, same as base) |
| `res/values-en/strings.xml` | English translations (55 entries) |

### Modified Files
| File | Change |
|------|--------|
| `res/values/strings.xml` | Expanded from 14 to 55 strings covering all core texts |
| `ui/MainActivity.kt` | `applyStoredLocale()` reads SharedPreferences and applies via `Configuration.setLocale()`. Restores languageMode in ViewModel. `saveLanguageMode()` companion for persistence. |
| `ui/viewmodel/MainViewModel.kt` | Added `languageMode` field (LanguageMode.System default) |
| `ui/screen/SettingsScreen.kt` | New "Sprache" section with System/Deutsch/English radio buttons. Imported `LanguageMode`. Restart hint displayed. |
| `ui/shell/MainShellScreen.kt` | Passes `languageMode` and `onLanguageModeSelected` to SettingsScreen. Persists to SharedPreferences on selection. |

---

## Supported Languages

| Language | Locale Tag | String Resources |
|----------|-----------|------------------|
| System (auto) | null | Follows device setting |
| Deutsch | "de" | `values/strings.xml` + `values-de/strings.xml` |
| English | "en" | `values-en/strings.xml` |

---

## Translated Core Areas (55 strings)

| Category | Strings |
|----------|---------|
| Welcome | app_name, welcome_title, welcome_subtitle, welcome_description, get_started, start_demo_mode |
| Provider | provider_settings, add_provider, configured_providers, no_provider_configured, api_key, api_key_missing, base_url, model, test_connection, save, cancel, delete, continue |
| Workspace | select_workspace, no_workspace |
| Tabs | tab_chat, tab_files, tab_code, tab_diff, tab_preview, tab_terminal |
| Chat | demo_mode, demo_badge, demo_offline, discuss, build, skill, role, run, stop, apply, new_session, more, send |
| Files | files_empty, no_file_open |
| Diff | no_pending_changes |
| Preview | no_preview_loaded, load_workspace_preview |
| Terminal | no_commands_suggested |
| Settings | settings, language_section, language_system_desc, language_german_desc, language_english_desc, language_restart_hint |
| Emergency | emergency_stop |

---

## Runtime / Restart Behavior

- **Language selection** in Settings → SharedPreferences → applied next startup
- **App restart required** for full effect (documented in UI hint)
- **On startup**: `MainActivity.applyStoredLocale()` reads SharedPreferences, sets `Locale.setDefault()`, and updates `resources.configuration`
- **ViewModel restore**: MainActivity reads saved language tag and restores `mainViewModel.languageMode`

---

## Still Hardcoded Strings

All screens (ChatPanel, MainShellScreen, ProviderSetupScreen, DiffPanel, FileTreePanel, etc.) still use hardcoded German/English strings directly in Kotlin. The string resources exist and can be consumed incrementally via `stringResource(R.string.xxx)` or `context.getString(R.string.xxx)`.

Recommended migration order:
1. Bottom navigation tabs (already defined: `tab_chat`, `tab_files`, etc.)
2. Empty states (already defined: `no_pending_changes`, `no_file_open`, etc.)
3. Settings section headers
4. Chat action labels (Discuss, Build, Run, Stop)

---

## Known Limitations

1. **Screens not consuming string resources** — Hardcoded strings remain in all Composable files. Infrastructure exists for incremental migration.
2. **App restart required** for language change to take full effect. Runtime locale switching without `AppCompatDelegate` is complex on Android.
3. **values/strings.xml and values-de/strings.xml are identical** — The base `values/` serves as the German fallback. `values-de/` is redundant but harmless.
4. **No unit tests for LanguageMode** — Serialization test and default-is-System test could be added.
5. **Only 55 strings** — Complete app translation would require ~200+ strings covering all dialogs, error messages, and workbench panels.

---

## Build

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- All tests pass — no regressions
