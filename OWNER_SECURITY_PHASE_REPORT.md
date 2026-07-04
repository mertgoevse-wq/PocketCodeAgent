# Owner Security Phase Report

## Ziel

PocketCodeAgent hat einen transparenten lokalen Owner-/Admin-Schutz erhalten.
**Keine Backdoor, kein Remote-Zugriff, kein Master-Key im Code.**

## Geänderte Dateien

| Datei | Änderung |
|-------|----------|
| `domain/security/OwnerAuthState.kt` | Neu: Sealed class für Auth-Zustände |
| `domain/security/EmergencyStopState.kt` | Neu: Enum für Emergency-Stop-Level |
| `domain/security/OwnerSecurityManager.kt` | Neu: Core Security Manager |
| `ui/screen/SettingsScreen.kt` | Erweitert: Owner-Security-Sektion, PIN/Biometrie-Dialoge, Emergency-Stop |
| `ui/shell/MainShellScreen.kt` | Emergency-Stop-Banner, SettingsSheet-Aufruf angepasst |
| `ui/viewmodel/AgentViewModel.kt` | Emergency-Stop-Check vor API-Calls |
| `ui/viewmodel/MainViewModel.kt` | OwnerSecurityManager-Halter |
| `ui/MainActivity.kt` | OwnerSecurityManager-Initialisierung, onPause-Hook |

## Owner Auth

### Zustände (OwnerAuthState)

| Zustand | Bedeutung |
|---------|-----------|
| `NotConfigured` | Keine PIN/Biometrie eingerichtet |
| `Locked` | Owner gesperrt, PIN/Biometrie nötig |
| `Unlocked` | Owner entsperrt, 5-Minuten-Timer läuft |
| `Failed` | PIN falsch (mit Restversuchs-Zähler) |
| `TemporarilyBlocked` | 5 Fehlversuche → 1 Minute Sperre |

### Authentifizierung

- **BiometricPrompt**: Android Standard-Biometrie (Fingerprint, Face Unlock)
- **PIN-Fallback**: Lokale 4-8-stellige PIN
  - Gespeichert als salted SHA-256 Hash
  - Hash + Salt werden via KeystoreHelper (AES/GCM) verschlüsselt in SharedPreferences gespeichert
  - PIN-Vergleich in konstanter Zeit indirekt über Hash-Vergleich
- **Auto-Lock nach 5 Minuten** Inaktivität
- **Lock bei App-Wechsel** (onPause → lock)

### Geschützte Aktionen (Owner-Unlock erforderlich)

- API-Key anzeigen
- Provider löschen
- Export Workspace ZIP
- Share file mit Secrets
- Copy CAUTION command
- Delete/Apply destructive patch
- Clear session
- Emergency Switch ändern

Geschützt via UI-Logik: Diese Aktionen sind nur ausführbar, wenn `authState == Unlocked`.

## Emergency Stop

### Stufen

| Level | Deaktiviert |
|-------|-------------|
| `NORMAL` | Nichts (alles aktiv) |
| `API_CALLS_DISABLED` | Echte API-Requests (Demo-Mode weiter möglich) |
| `TERMINAL_DISABLED` | Terminal-Zugriff / Command Queue |
| `EXPORTS_DISABLED` | Export/Share |
| `ALL_DISABLED` | Alle sensiblen Aktionen |

### UI

- Rotes Emergency-Stop-Banner in der Shell-Top-Bar wenn aktiv
- Emergency-Stop-Dialog in Settings mit allen 5 Levels
- Änderung des Emergency-Stops erfordert Owner-Unlock (PIN/Biometrie)

## Sicherheitsentscheidungen

| Entscheidung | Begründung |
|-------------|-----------|
| Kein hardcoded Passwort | Keine Backdoor-Möglichkeit |
| PIN als Salted SHA-256 Hash | Selbst bei DB-Zugriff keine PIN-Recovery |
| KeystoreHelper-Verschlüsselung | Hardware-backed AES/GCM für Hash+Salt |
| Kein Logging von PIN/Auth | Keine auth-relevanten Daten in Logs |
| BiometricPrompt Standard-API | Keine Rohdaten-Speicherung biometrischer Daten |
| Auto-Lock 5 Min + onPause | Zeitlich und kontextuell begrenzter Unlock |
| 5 Fehlversuche = 1 Min Sperre | Brute-Force-Schutz |
| Emergency-Stop in SharedPreferences | Persistiert über App-Neustarts |

## Limitierungen

- Protected Actions sind derzeit via UI-State geschützt; Backend-Code (Room DAOs, Repositories) prüft nicht auf Owner-Status (implizite UI-Gates)
- BiometricPrompt benötigt FragmentActivity Context (in Compose über `LocalContext.current` gelöst)
- PIN-Reset löscht alle PIN-Daten ohne Bestätigung der alten PIN (nur im entsperrten Zustand möglich)
- Kein separater Admin-User — Owner = Gerätebesitzer
- Emergency-Stop blockiert auf AgentViewModel-Ebene; Provider-Test könnte trotzdem ausgeführt werden (bewusstes Design: Test = kein Agent-Chat)
- Compose/UI/SAF nicht instrumentiert getestet

## Build/Test Ergebnis

```
clean:        BUILD SUCCESSFUL
test:         BUILD SUCCESSFUL
assembleDebug: BUILD SUCCESSFUL
```

Alle bestehenden Tests grün. Keine neuen Compiler-Fehler.

## APK

| Metric | Value |
|--------|-------|
| Pfad | `app/build/outputs/apk/debug/app-debug.apk` |
