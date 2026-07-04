# Provider Settings UX Fix — Phase Report

**Date:** July 4, 2026
**Build:** ✅ clean + test + assembleDebug (BUILD SUCCESSFUL)

---

## Changed Files

| File | Change |
|------|--------|
| `app/src/main/java/com/pocketcodeagent/ui/screen/ProviderSetupScreen.kt` | Major rewrite — loading states, disabled reason texts, error action card, context-aware Weiter button |
| `app/src/main/java/com/pocketcodeagent/ui/viewmodel/ProviderViewModel.kt` | Added `isLoadingProvidersFlow` to prevent brief 0-count during Room DB load |

---

## UX Improvements

### 1. Provider Loading State

**Before:** Provider list briefly showed "Konfigurierte Provider (0)" while Room DB loaded.

**After:**
- `isLoadingProvidersFlow` starts as `true` at ViewModel init
- Show `CircularProgressIndicator` + "Provider werden geladen..." during load
- First emission from Room DB flips it to `false`
- Empty state only shown when truly empty: "Noch kein Provider konfiguriert."

### 2. Save Button Disabled Reason

When Save is disabled, a small reason text appears below the button:
- **API-Key fehlt** — when apiKey is blank
- **Modell fehlt** — when modelName is blank
- **Base URL fehlt** — when baseUrl is blank
- **Name fehlt** — when name is blank
- **Wird gespeichert...** — during save operation
- **Test laeuft...** — during connection test

### 3. Test Button Disabled Reason

Test button shows reason when disabled:
- **Test laeuft...** — during test
- **Base URL fehlt**
- **API-Key fehlt**
- **Modell fehlt**

### 4. Modelle Button Disabled Reason

Modelle button shows reason when disabled:
- **Wird geladen...** — during model list fetch
- **Base URL fehlt**
- **API-Key noetig, um Modelle zu laden.**

### 5. Error Action Card

**Before:** Long red error text with no actionable next step.

**After:** `ErrorActionCard` composable:
- Short error text (truncated to 120 chars, max 3 lines)
- Two action buttons: "Erneut testen" + "Modelle laden"
- Dark red background card, clean layout

### 6. Weiter Button Context-Aware Text

| Active Provider State | Button Text | Color | Warning |
|----------------------|-------------|-------|---------|
| Ready | "Weiter" | CalmSage | — |
| Not configured | "Trotzdem weiter" | SlateBlue | — |
| Error | "Trotzdem weiter" | SlateBlue | "Chat mit diesem Provider wird vermutlich fehlschlagen." |
| None selected | "Demo-Modus starten" | SlateBlue (outlined) | — |

### 7. Security Settings Access

"Security & Owner Settings" button preserved at the bottom of the provider list.

---

## Internal Changes

### ProviderViewModel.isLoadingProvidersFlow

```kotlin
private val _isLoadingProviders = MutableStateFlow(true)
val isLoadingProvidersFlow: StateFlow<Boolean> = _isLoadingProviders

val providers: StateFlow<List<Provider>> = repository.allProvidersFlow
    .onEach { _isLoadingProviders.value = false }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- `onEach` ensures no double-subscription (unlike the `.also { collect }` pattern)
- Starts `true`, flips to `false` on first DB emission
- No dead code (`onCompletion` never fires with `stateIn`)

---

## Known Limitations

- Provider list uses `WhileSubscribed(5000)` — if the composable pauses for >5s, the flow resets and `isLoadingProviders` stays `false` (already emitted). This is acceptable.
- The deprecated `Icons.Filled.ArrowBack` and `outlinedButtonBorder` warnings are pre-existing and cosmetic.
- Custom provider with empty base URL is included in the preset dropdown — users must fill in fields manually.

---

## Build Result

```
BUILD SUCCESSFUL (clean, test, assembleDebug)
APK: app/build/outputs/apk/debug/app-debug.apk
```
