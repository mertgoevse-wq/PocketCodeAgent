# Provider Parser E2E QA — Phase Report

**Date:** July 4, 2026
**Build:** ✅ clean + test + assembleDebug (72+ tests, all passing)
**APK:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Befund (Ursache des Problems)

Der Galaxy-A56-Test zeigte "Parserfehler - Antwortformat unerwartet" bei OpenRouter, obwohl HTTP erfolgreich war. Die Ursache: wenn der Provider eine Antwort liefert, die nicht exakt dem OpenAI-Chat-Format entspricht (z.B. kaputtes JSON, Plaintext, oder non-standard Felder), schlug die JSON-Parsing-Logik komplett fehl — selbst wenn lesbarer Text in der Antwort enthalten war.

**Fix:** `parseJsonObject` hat jetzt einen `tryExtractTextFromRaw`-Fallback, der bei fehlgeschlagenem JSON-Parsing per Regex versucht, Text aus `"content"`, `"text"`, `"message"` oder `"response"`-Feldern zu extrahieren. Als letzter Ausweg wird Plaintext ohne JSON-Klammern direkt genutzt.

---

## Geänderte Dateien

| File | Change |
|------|--------|
| `app/.../data/network/ApiClient.kt` | `parseJsonObject`: neuer `tryExtractTextFromRaw`-Fallback bei JSON-Parse-Fehlern. Extrahiert Text aus `"content"/"text"/"message"/"response"` per Regex. Plaintext-Fallback für nicht-JSON-Antworten. |
| `app/src/test/.../ProviderParserTest.kt` | **New.** 36 Unit-Tests mit Fake-JSON: OpenAI message/delta, choices.text, top-level fields, OK-Varianten, Error-Objekte, malformed JSON, Plaintext, leere/null-Felder, Long-Input. |

---

## Unterstützte Response-Formate

| Format | Beispiel | Handler |
|--------|---------|---------|
| OpenAI chat message | `choices[0].message.content` | `extractContentFromChoice` |
| Streaming delta | `choices[0].delta.content` | `extractContentFromChoice` |
| Legacy text field | `choices[0].text` | `extractContentFromChoice` |
| Top-level text | `{"text": "..."}` | `extractCompletionContent` |
| Top-level content | `{"content": "..."}` | `extractCompletionContent` |
| Top-level response | `{"response": "..."}` | `extractCompletionContent` |
| Top-level result | `{"result": "..."}` | `extractCompletionContent` |
| Nested output array | `output[0].content[0].text` | `extractFromNestedOutput` |
| Malformed JSON w/ content key | `{broken,"content":"text"}` | `tryExtractTextFromRaw` ★ NEW |
| Malformed JSON w/ text key | `{broken,"text":"value"}` | `tryExtractTextFromRaw` ★ NEW |
| Malformed JSON w/ message key | `{"message":"error"}` | `tryExtractTextFromRaw` ★ NEW |
| Plain text (no JSON) | `This is plain text` | `tryExtractTextFromRaw` ★ NEW |
| Error object | `{"error":{"message":"..."}}` | `extractCompletionContent` |

---

## Streaming-Fallback

Bereits vorhanden und unverändert:
- `chatCompletionStream` versucht zuerst Streaming via `executeStreamingChat`
- Wenn Streaming keinen Content liefert → automatischer non-streaming Retry via `executeNonStreamingChat`
- Nur ein Retry, keine Endlosschleife
- Kein Retry bei 401/403/404 (Auth-Fehler)

---

## OpenRouter-Header

Bereits vorhanden und unverändert in `buildRequest`:
- `HTTP-Referer: https://github.com/mertgoevse-wq/PocketCodeAgent`
- `X-Title: PocketCodeAgent`
- Nur gesetzt wenn nicht bereits in Custom-Headers definiert
- Nur für OpenRouter (Name oder Base-URL enthält "openrouter")

---

## Tests (36 Unit-Tests, alle bestehend)

- 28 Tests für Standard-Formate (OpenAI message, delta, text, top-level fields, error objects, OK-Varianten, malformed JSON)
- 8 Tests für `tryExtractTextFromRaw`-Fallback (broken JSON mit content/text/message/response keys, Plaintext, Garbage, Long-Input, JSON-Arrays)
- Keine echten API-Keys, keine Netzwerk-Tests

---

## Sicherheitsprüfung

| Check | Status |
|-------|--------|
| API-Keys im Log | ✅ `sanitizedText` redacted `Authorization: Bearer [redacted]` und `sk-*` / `nvapi-*` Keys |
| Secrets in Error-Messages | ✅ Errors zeigen nur sanitized Snippets (max 200-300 chars) |
| API-Key in `buildRequest` | ✅ Nur im `Authorization`-Header, nie im Log |
| Prompt Injection | ✅ Core-Rules nie überschreibbar, Nutzer-Input in separaten Messages |
| Raw Response Logging | ✅ `sanitizedText` kürzt auf 800 chars + redacted Secrets |

---

## Bekannte Limitierungen

- `tryExtractTextFromRaw` compiliert Regex-Patterns pro Aufruf (seltener Fallback-Pfad, Performance irrelevant)
- Der Fallback extrahiert nur das erste passende Text-Feld — falls mehrere existieren, wird nur das erste genutzt
- Unicode-Escapes (`\u0041`) werden im Fallback nicht dekodiert (selten, da reale Responses entweder gültiges JSON oder Plaintext sind)
- Der UI-Error-Card (ProviderSetupScreen) wurde bereits in Phase PROVIDER_SETTINGS_UX_FIX verbessert — Parserfehler erscheinen als ErrorActionCard mit "Erneut testen" / "Modelle laden" Buttons

---

## Build Result

```
clean:    BUILD SUCCESSFUL
test:     BUILD SUCCESSFUL (36 ProviderParser + alle bestehenden Tests)
assembleDebug: BUILD SUCCESSFUL
APK:      app/build/outputs/apk/debug/app-debug.apk
```
