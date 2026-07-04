# Provider Parser Robustness Fix Report

## Ursache des Parserfehlers

OpenRouter mit `deepseek/deepseek-v4-flash` auf Galaxy A56 lieferte gelegentlich Antworten, deren JSON-Format leicht vom standard OpenAI-kompatiblen Schema abwich. Der ApiClient warf `"Parserfehler - Antwortformat unerwartet"` obwohl HTTP 200 zurückkam und gültiges JSON vorlag -- nur mit einem anderen Pfad zum Textinhalt (z.B. `delta` statt `message`, oder `output[]` statt `choices[]`).

## Geänderte Dateien

- `app/src/main/java/com/pocketcodeagent/data/network/ApiClient.kt`
- `app/src/main/java/com/pocketcodeagent/data/repository/AgentRepository.kt`

## Unterstützte Response-Formate

### Non-Streaming (extractCompletionContent)

| Format | JSON-Pfad | Beschreibung |
|--------|-----------|--------------|
| A | `choices[0].message.content` | OpenAI-kompatibel |
| B | `choices[0].delta.content` | Delta-artig (auch non-streaming) |
| C | `text` | Text direkt auf Top-Level |
| D | `output[0].content[0].text` | Output-Array mit Content-Blöcken |
| D-alt | `output[0].parts[0].text` | Output mit parts |
| E | `error.message` | Error-Objekt (wirft Exception) |
| F | `content` / `response` / `result` | Generische Top-Level Fallbacks |

### Streaming (extractStreamingContent)

Zusätzlich zu `choices[0].delta.content` / `choices[0].message.content`:
- `text` auf Top-Level (non-standard streaming)
- `content` auf Top-Level

### Choice-Level (extractContentFromChoice)

Innerhalb eines Choice-Objekts:
- `delta.content` / `message.content` / `text`
- `output[0].content[0].text` (geblockte Output-Arrays)
- `content[0].text` / `content[0].value` (Content-Blöcke direkt im Choice)

## Test Connection Änderungen

- **OK-Akzeptanz** erweitert: `"OK"`, `"ok"`, `"OK."`, `"Okay"`, und alles was `"ok"` enthält (case-insensitive)
- Antwortlänge ≤10 Zeichen + OK-Match → Anzeige als `"OK"` (clean)
- Längere Antwort, die kein OK enthält → `"Ready (<Antworttext>)"` -- Status bleibt **Ready**, kein Parserfehler
- Wenn Text überhaupt extrahiert werden konnte → `success = true` (nicht nur bei OK)

## Streaming Fallback Änderungen

- `extractStreamingContent()`: behandelt non-standard Streaming-Formate (top-level `text`/`content`)
- `executeStreamingChat()`:
  - Wenn SSE-Streaming keinen Inhalt liefert, aber raw JSON Buffer vorhanden → Non-Streaming-Parse als Fallback
  - Wenn beides keinen Content findet → sanitized Fehlermeldung mit raw Response-Schema (ohne Secrets)
  - Fallback in `chatCompletionStream()`: wenn Streaming fehlschlägt (und kein Auth/Not-Found-Fehler) → Non-Streaming-Retry

## OpenRouter-spezifische Änderungen

OpenRouter-Header waren bereits in `buildRequest()` vorhanden:
- `HTTP-Referer: https://github.com/mertgoevse-wq/PocketCodeAgent`
- `X-Title: PocketCodeAgent`

Beide werden nur gesetzt, wenn Providername/Base-URL `openrouter`/`openrouter.ai` enthalten und Custom-Headers diese Keys nicht bereits überschreiben. Keine Secrets in Headern.

## Provider-Status Logik

- **`updateProviderStatusOnError()`** in `AgentRepository` setzt den Provider-Status auf `ERROR`, wenn Chat/Agent-Lauf fehlschlägt
- Provider wird **nicht** gelöscht -- nur `lastTestStatus` und `lastErrorSanitized` aktualisiert
- UI kann weiterhin "Test" auslösen, um den Status zurück auf `Ready` zu setzen

## Fehlermeldungen (alle sanitized)

| Szenario | Fehlertext |
|----------|-----------|
| Unbekanntes JSON-Format (HTTP 200) | `"Antwortformat unerwartet. HTTP war erfolgreich, Parser konnte keinen Text finden. Schema: <sanitized JSON>"` |
| Kein gültiges JSON | `"Parserfehler - Antwort war kein gültiges JSON. Raw: <sanitized>"` |
| Streaming leer | `"Streaming lieferte keinen Inhalt. Raw: <sanitized>"` |

Alle `sanitizedText()`-Aufrufe redacten API-Keys, Bearer-Tokens und andere Secrets aus Fehlermeldungen.

## Model Validation

- Model-ID aus GET `/models` wird exakt gespeichert (Dropdown wählt die ID)
- Kein Client-seitiges Blockieren von manuell eingegebenen Modellnamen
- HTTP 404 Meldung: `"HTTP 404 - Modell oder Endpoint nicht gefunden."`

## Build/Test Ergebnis

```
clean:        BUILD SUCCESSFUL
test:         BUILD SUCCESSFUL
assembleDebug: BUILD SUCCESSFUL
```

Alle bestehenden Tests (AgentActionParserTest, CommandRiskScannerTest, ContextSanitizerTest) weiterhin grün.
Keine neuen Compiler-Fehler.

## APK

| Metric | Value |
|--------|-------|
| Pfad | `app/build/outputs/apk/debug/app-debug.apk` |
| Build-Ergebnis | BUILD SUCCESSFUL |
