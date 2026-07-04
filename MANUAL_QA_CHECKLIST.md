# Manual QA Checklist — PocketCodeAgent

**Target device:** Samsung Galaxy A56  
**Target build:** `assembleDebug` APK  
**Date:** July 2026

---

## 1. App Start

- [ ] App startet ohne Crash (Kaltstart)
- [ ] App Resume nach Background (Warmstart)
- [ ] Kein ANR-Dialog beim Start
- [ ] Keine weißen/blauen Blitzer beim Start

## 2. Onboarding

- [ ] WelcomeScreen zeigt "PocketCodeAgent" Titel
- [ ] "Workspace öffnen" Button sichtbar
- [ ] WorkspacePicker öffnet sich
- [ ] Workspace kann über SAF ausgewählt werden
- [ ] Nach Auswahl: MainShellScreen erscheint
- [ ] Workspace-Name in TopBar sichtbar

## 3. Provider Setup

- [ ] Settings Sheet öffnet sich über Settings-Icon
- [ ] Provider-Typ auswählbar: OpenRouter, NVIDIA NIM, Google Gemini
- [ ] API-Key Feld maskiert (Passwort-Stil)
- [ ] Base URL editierbar
- [ ] Modell-Name editierbar
- [ ] "Speichern" Button funktioniert
- [ ] "Test Connection" Button sendet Test-Request
- [ ] Provider-Status-Pill zeigt READY / NOT_CONFIGURED / TESTING

## 4. OpenRouter Test

- [ ] Provider "OpenRouter" auswählen
- [ ] API-Key eingeben
- [ ] Base URL: `https://openrouter.ai/api/v1`
- [ ] Modell: passendes Modell auswählen
- [ ] "Test Connection" → READY (grün)
- [ ] Chat-Nachricht senden → Antwort kommt

## 5. NVIDIA NIM Test

- [ ] Provider "NVIDIA NIM" auswählen
- [ ] API-Key eingeben
- [ ] Base URL: NVIDIA NIM Endpoint
- [ ] "Test Connection" → READY oder Fehler angezeigt
- [ ] Chat funktioniert oder klarer Fehler

## 6. Gemini Test

- [ ] Provider "Google Gemini" auswählen
- [ ] API-Key eingeben
- [ ] Base URL: Gemini API Endpoint
- [ ] "Test Connection" → READY oder Fehler
- [ ] Chat funktioniert oder klarer Fehler

## 7. Provider Fehler

- [ ] 401 Unauthorized → ERROR Status-Pill, keine rohe Antwort im UI
- [ ] 403 Forbidden → ERROR, keine Keys im Log
- [ ] 404 Not Found → ERROR, verständliche Meldung
- [ ] 429 Rate Limit → ERROR, Retry-Logik?
- [ ] API-Key nicht in Logs / Toasts / Error-Messages sichtbar

## 8. Chat — Discuss Mode

- [ ] Discuss Mode ausgewählt
- [ ] Nachricht eingegeben, Send gedrückt
- [ ] Agent antwortet mit Text
- [ ] Keine pocketAction-Blöcke im Output (oder als Note)
- [ ] Streaming sichtbar (Text erscheint Wort für Wort)
- [ ] Stop-Button stoppt den Stream
- [ ] "[Vom Benutzer gestoppt]" erscheint

## 9. Chat — Build Mode

- [ ] Build Mode ausgewählt
- [ ] Nachricht mit Build-Aufgabe gesendet
- [ ] Agent antwortet mit pocketArtifact/pocketAction
- [ ] Artifact Cards erscheinen im Chat
- [ ] Create File / Modify File / Delete / RunCommand Cards korrekt
- [ ] "Add to Diff" Button auf File-Cards funktioniert
- [ ] "Copy Command" / "Add to Queue" auf Command-Cards funktioniert

## 10. Role Selector

- [ ] Role-Dropdown in ChatPanel sichtbar
- [ ] Alle 11 Rollen wählbar
- [ ] Planner als Default
- [ ] Rolle wechseln → Run startet mit neuer Rolle
- [ ] Role-Anzeige während Streaming

## 11. Skill Selector

- [ ] Skill-Dropdown sichtbar
- [ ] Alle 9 Skills wählbar
- [ ] Skill-Auswahl setzt Role + Mode
- [ ] Skill-Name im Chip sichtbar
- [ ] "→ Role" Hinweis korrekt

## 12. Agent Action Cards

- [ ] Artifact-Titel korrekt
- [ ] Create File Card: Dateiname, Zeilenzahl, "Add to Diff"
- [ ] Modify File Card: Dateiname, old/new, "Add to Diff"
- [ ] Delete File Card: Dateiname, "Review Delete"
- [ ] Run Command Card: Befehl, Risk-Badge, "Copy" / "Add to Queue"
- [ ] BLOCKED Command: nicht kopierbar
- [ ] Open Preview Card: "Open Preview"

## 13. Diff — Add / Apply / Reject

- [ ] Agent erzeugt Patches
- [ ] "Add to Diff" → Diff Tab mit Patch sichtbar
- [ ] Diff-Tab zeigt Pfad, Aktion, Status, Diff-Lines
- [ ] "Apply" schreibt Datei
- [ ] "Reject" lehnt Patch ab
- [ ] "Apply safe" wendet alle PENDING-Patches an
- [ ] Status-Badges (PENDING → APPLIED / REJECTED / CONFLICT)

## 14. Undo Last Apply

- [ ] Nach Apply: "Undo" Button sichtbar
- [ ] "Undo" macht letzte Apply-Operation rückgängig
- [ ] Datei wieder im Originalzustand
- [ ] Nach Undo: erneutes Undo zeigt "Keine Apply-Operation"

## 15. FileTree — großer Workspace

- [ ] Workspace mit >50 Dateien öffnen
- [ ] FileTree lädt ohne Verzögerung
- [ ] Ordner expandierbar/collapsible
- [ ] Datei-Klick öffnet Code Editor
- [ ] Scrollen flüssig
- [ ] Kein ANR beim Laden

## 16. Code Editor — Save / Revert / Patch

- [ ] Datei aus FileTree öffnen
- [ ] Text bearbeiten → Unsaved-Status
- [ ] Save → Datei gespeichert (über SAF)
- [ ] Revert → Änderungen verworfen
- [ ] Copy → Inhalt in Zwischenablage
- [ ] Diff-Button: Patch erstellt, Diff-Tab offen
- [ ] Find-Funktion: Suchen in Datei
- [ ] Zeilennummern Toggle
- [ ] Line Count / Char Count Statusleiste
- [ ] >200 KB Datei: Read-only mit "Edit trotzdem"
- [ ] >500 KB Datei: "Datei zu groß" Meldung

## 17. Workspace Static Preview

- [ ] Workspace mit index.html öffnen
- [ ] Preview Tab → "Load Workspace Preview"
- [ ] HTML wird in WebView gerendert
- [ ] CSS wird gebundled (inline)
- [ ] JS wird gebundled
- [ ] Fehlende CSS/JS → Warnung im Panel, kein Crash

## 18. File Preview

- [ ] .html Datei im Editor öffnen
- [ ] Preview "File" Modus auswählen
- [ ] "Preview current file" laden
- [ ] Datei in WebView sichtbar
- [ ] Nicht-HTML-Datei: Hinweis "Nur .html/.htm"

## 19. URL Preview

- [ ] Preview "URL" Modus
- [ ] `http://127.0.0.1:5173` eingeben
- [ ] Load → WebView versucht zu laden
- [ ] Server nicht erreichbar → Fehler-Banner
- [ ] Clear WebView Cache funktioniert

## 20. Terminal Command Queue

- [ ] Agent schlägt Command vor
- [ ] "Add to Queue" → Terminal Tab öffnen
- [ ] Command Card mit Risk-Badge sichtbar
- [ ] "Copy" kopiert SAFE Command
- [ ] "Done" markiert Command als erledigt
- [ ] "Reject" lehnt Command ab
- [ ] "Clear done" / "Clear rejected"

## 21. CAUTION Dialog

- [ ] CAUTION Command klicken
- [ ] Dialog: "Kann Dateien veraendern", Grund, SafeDisplay
- [ ] "Copy anyway" kopiert
- [ ] "Cancel" nicht kopiert

## 22. BLOCKED Command

- [ ] BLOCKED Command: "Blockiert" Text
- [ ] Kein Copy / Add to Queue möglich
- [ ] "Reject" funktioniert

## 23. Termux Integration

- [ ] Termux installiert: "Termux installiert" + Open-Button
- [ ] Termux nicht installiert: "Nicht erkannt" + Info
- [ ] "Open" Button startet Termux (wenn installiert)
- [ ] Termux Help Panel: 5 Commands kopierbar

## 24. API-Key Masking

- [ ] API-Key im Settings-Feld als Punkte/Circles
- [ ] Key nicht in Logs sichtbar (recentLogs)
- [ ] Key nicht in Error-Messages / Toasts
- [ ] Key nicht in Console Logs (Preview)

## 25. Console Log Sanitization

- [ ] WebView Console mit API-Key Ausgabe
- [ ] `Bearer ...` → `Bearer [redacted]` in Console-Panel
- [ ] `sk-...` → `sk-[redacted]`
- [ ] Keine Roh-Secrets kopierbar

## 26. APK Install Galaxy A56

- [ ] APK über USB / adb installieren
- [ ] App startet ohne Crash
- [ ] Alle Tabs navigierbar
- [ ] SAF-Workspace-Dialog funktioniert
- [ ] WebView rendert Preview
- [ ] Keyboard öffnet/schließt korrekt

## 27. Offline / No Internet

- [ ] WLAN aus, Mobile Data aus
- [ ] App startet
- [ ] Chat-Send → Fehlermeldung (kein Crash)
- [ ] Provider-Status-Pill bleibt NOT_CONFIGURED
- [ ] Keine Endlosschleife beim Streamen

## 28. Rotation / Background / Resume

- [ ] App im Landscape-Modus
- [ ] Alle Tabs navigierbar
- [ ] Editor behält Inhalt bei Rotation
- [ ] App in Background (Home-Button)
- [ ] Nach Resume: gleicher Tab aktiv
- [ ] Streaming wird nicht fortgesetzt (gestoppt oder gecancelled)
- [ ] Keine doppelten Nachrichten nach Resume

---

## Summary

| Section | Count | Passed |
|---------|-------|--------|
| 1-3: Start/Onboarding/Provider | 17 | |
| 4-7: Provider Tests/Errors | 13 | |
| 8-9: Chat Modes | 12 | |
| 10-12: Role/Skill/Action Cards | 24 | |
| 13-14: Diff/Undo | 13 | |
| 15: FileTree | 6 | |
| 16: Code Editor | 14 | |
| 17-19: Preview | 12 | |
| 20-22: Terminal | 14 | |
| 23-25: Termux/API-Key/Console | 14 | |
| 26: Galaxy A56 Install | 7 | |
| 27: Offline | 6 | |
| 28: Rotation/Resume | 8 | |
| **Total** | **160** | |

---

**Tester:** _________________  
**Datum:** _________________  
**Build:** `assembleDebug`  
**Gerät:** Samsung Galaxy A56
