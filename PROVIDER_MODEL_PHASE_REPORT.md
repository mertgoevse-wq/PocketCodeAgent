# Provider/Model Phase Report

## Implementierte Provider

PocketCodeAgent unterstuetzt jetzt Provider-Presets fuer:
- OpenRouter: `https://openrouter.ai/api/v1`
- Google Gemini OpenAI-compatible: `https://generativelanguage.googleapis.com/v1beta/openai`
- NVIDIA NIM: `https://integrate.api.nvidia.com/v1`
- Groq: `https://api.groq.com/openai/v1`
- Mistral: `https://api.mistral.ai/v1`
- Together: `https://api.together.xyz/v1`
- Custom OpenAI-Compatible: frei editierbare Base URL

Zu jedem Preset gibt es eine statische Modellliste fuer den Stand 03.07.2026. In den Provider-Settings kann zusaetzlich `Modelle` ausgefuehrt werden; das ruft den OpenAI-kompatiblen `GET /models` Endpoint des Providers mit dem lokalen Key ab und ergaenzt die Dropdown-Auswahl.

## Geaenderte Dateien

- `app/src/main/java/com/pocketcodeagent/data/local/AppDatabase.kt`
- `app/src/main/java/com/pocketcodeagent/data/local/entity/ProviderEntity.kt`
- `app/src/main/java/com/pocketcodeagent/data/model/Provider.kt`
- `app/src/main/java/com/pocketcodeagent/data/model/ProviderPreset.kt`
- `app/src/main/java/com/pocketcodeagent/data/network/ApiClient.kt`
- `app/src/main/java/com/pocketcodeagent/data/repository/AgentRepository.kt`
- `app/src/main/java/com/pocketcodeagent/data/repository/ProviderRepository.kt`
- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/ProviderViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/shell/MainShellScreen.kt`
- `app/src/main/java/com/pocketcodeagent/ui/screen/ProviderSetupScreen.kt`
- `app/src/main/java/com/pocketcodeagent/ui/chat/ChatPanel.kt`
- `PROVIDER_MODEL_PHASE_REPORT.md`

## API-Key Masking und Speicherung

`ProviderConfig` enthaelt keinen Klartext-Key mehr. Runtime-Provider behalten den Key nur fuer Requests/Form-State, gespeichert wird er weiterhin verschluesselt ueber `KeystoreHelper` in `ProviderEntity.encryptedApiKey`.

Die Settings zeigen API-Keys standardmaessig maskiert. Der Show/Hide-Button zeigt den Wert lokal nur im Feld und versteckt ihn automatisch nach 10 Sekunden. Es gibt keinen API-Key-Toast mehr. Fehlertexte und Logs laufen durch Sanitizing und ersetzen Bearer Tokens, Authorization-Werte, API-Keys, Tokens und Secrets.

## Test Connection

`testProviderConnection(provider)` nutzt:
- `POST /chat/completions`
- `Authorization: Bearer <key>`
- Testprompt: `Reply with OK only.`
- `temperature = 0.0`
- `max_tokens = 8`
- `stream = false`

Das Ergebnis enthaelt Providername, Modellname, HTTP-Code falls vorhanden, kurze Antwort bei Erfolg und einen sanitized Error bei Fehlern. Erfolgreiche Tests setzen den Providerstatus auf `Ready`; Fehler setzen `Error` und speichern nur den bereinigten Fehlertext.

## Streaming und Fallback

Der Chat-Client nutzt OpenAI-kompatible Chat Completions:
- `POST /chat/completions`
- `model`, `messages`, `temperature`, `max_tokens`, `stream`
- SSE mit `data: [DONE]`
- `choices[0].delta.content`
- `choices[0].message.content`
- `error.message`

Alle OkHttp-Requests laufen auf `Dispatchers.IO`; der Screenshot-Fehler `NetworkOnMainThreadException` wird dadurch nicht mehr durch den API-Client ausgeloest. Wenn Streaming fehlschlaegt und der Fehler nicht eindeutig Auth/Endpoint/Modellzugriff ist, versucht der Client automatisch Non-Streaming.

## Verbesserte Fehlerfaelle

Verbessert wurden:
- fehlender API-Key: `Provider nicht konfiguriert`
- HTTP 401: API-Key ungueltig oder abgelaufen
- HTTP 403: Konto, Modellzugriff oder Limits pruefen
- HTTP 404: Modell oder Endpoint nicht gefunden
- HTTP 429: Rate-Limit erreicht
- Timeout: Internet oder Provider pruefen
- Parserfehler: Antwortformat unerwartet

ChatPanel deaktiviert Send und Agent-Chips, wenn kein echter Provider konfiguriert ist. Demo Mode bleibt explizit ueber Provider-ID `999` erlaubt und wird nicht als echter Provider verschleiert.

## UI-Pruefung

Die Screenshots zeigten `Ready`, waehrend die Anfrage danach mit `NetworkOnMainThreadException` fehlschlug. Die ProviderModelBar zeigt jetzt nur noch `Ready`, wenn ein Provider voll konfiguriert und erfolgreich getestet ist. Ohne Key bleibt sie bei `Not configured`; waehrend Tests zeigt sie `Testing`; echte Fehler bleiben als `Error` sichtbar.

Ein echtes App-Run-Smoke-Testen auf Device/Emulator war lokal nicht moeglich, weil `adb devices` keine verbundenen Geraete gelistet hat.

## Build

Ausgefuehrt:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Ergebnis:

```text
BUILD SUCCESSFUL
```

Letzter `assembleDebug` Lauf: erfolgreich in 25s, 37 Tasks ausgefuehrt. Es bleiben nur bestehende Deprecation-Warnungen aus Compose/Room/Gradle, keine Build-Fehler.
