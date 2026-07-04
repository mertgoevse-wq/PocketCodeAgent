# Demo Provider Separation Report

## Problem

Die App zeigte teils echten OpenRouter-Provider mit "Ready", später wieder "Demo Mode". Das automatische Auswählen des ersten Providers in `MainShellScreen` überschrieb die Nutzerauswahl. Es gab keine klare visuelle Trennung zwischen Demo und echtem Provider.

## Geänderte Dateien

| Datei | Änderung |
|-------|----------|
| `ui/shell/MainShellScreen.kt` | Auto-Switching entfernt, Demo-Badge in ProviderModelBar |
| `data/repository/AgentRepository.kt` | Demo-Antworten mit "[Demo-Agent]" Prefix |
| `ui/viewmodel/MainViewModel.kt` | `isDemoMode` Property |

## Demo/Real Provider Logik

### Demo Mode (id=999)
- ProviderName: "Demo Mode"
- Status-Pill: "Offline demo" (SlateBlue)
- Kein API-Key nötig
- Kein Provider-Test nötig
- Agent-Antworten: `"[Demo-Agent: Diese Antwort nutzt keine echte API.]\n\n<content>"`
- Sender-Name: "Rollenname (Demo)"

### Echter Provider
- ProviderName: Nutzerdefiniert (z.B. "OpenRouter")
- Status-Pill: "Not configured" / "Ready" / "Testing" / "Error"
- API-Key erforderlich
- Provider-Test erforderlich vor "Ready"
- Kein Demo-Fallback bei Fehlern

## Auto-Switching Fix

**Vorher:**
```kotlin
if (selected != null && latest exists) update
else if (providers.size == 1) auto-select first  // BUG!
```

**Nachher:**
```kotlin
if (selected != null && latest exists) update
else if (selected.id == 999) keep demo  // preserve demo selection
else selectedProvider = null  // deleted provider → explicitly null
```

Kein automatisches Springen mehr auf den ersten Provider. Gelöschte Provider führen zu `selectedProvider = null` (kein stiller Fallback).

## Persistenz

`SessionEntity.selectedProviderId` wird beim Session-Create gespeichert. Beim Restore wird der letzte Provider wiederhergestellt, sofern er noch existiert.

## UI-Verhalten

| Zustand | ProviderModelBar | ChatPanel |
|---------|-----------------|-----------|
| Demo Mode aktiv | "Offline demo" (SlateBlue) | Antworten mit "[Demo-Agent]" Prefix |
| Real Provider Ready | "Ready" (CalmSage/grün) | Normale Antworten |
| Real Provider Error | "Error" (WarmCopper/rot) | Fehlermeldung |
| Kein Provider | "Not configured" (grau) | Send deaktiviert |

## Bekannte Limitierungen

- Demo-Provider-Objekt wird in MainActivity.kt on-the-fly erstellt (nicht in Room gespeichert)
- Wenn echter Provider gelöscht wird, bleibt selectedProvider = null (nicht automatisch Demo)
- Session-Restore mit gelöschtem Provider setzt selectedProvider auf null
- Kein expliziter "Zu Demo wechseln"-Button in der Chat-Ansicht (nur über WelcomeScreen)

## Build/Test Ergebnis

```
clean:        BUILD SUCCESSFUL
test:         BUILD SUCCESSFUL
assembleDebug: BUILD SUCCESSFUL
```

Alle bestehenden Tests grün. Keine neuen Compiler-Fehler.
