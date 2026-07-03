# PocketCodeAgent - End-to-End Testplan (Galaxy A56) 🧪

Dieser Testplan deckt alle kritischen Kernbereiche von **PocketCodeAgent** ab, um eine reibungslose Ausführung auf dem Samsung Galaxy A56 ohne Root-Zugriff zu garantieren.

---

## 📱 Testumgebung
*   **Gerät:** Samsung Galaxy A56
*   **Betriebssystem:** Android 15 / 16 (One UI)
*   **Berechtigungen:** Speicherzugriff (SAF), Benachrichtigungen, Internet

---

## 🔍 Test-Szenarien

### 1. Start, Onboarding & Demo-Modus
*   **Testschritte:**
    1. Starte die App nach der Neuinstallation.
    2. Verifiziere, dass die deutsche Onboarding-Anleitung korrekt gerendert wird.
    3. Tippe auf **"Demo-Modus starten (Ohne Key) 🧪"**.
*   **Erwartetes Ergebnis:** Die App initialisiert den virtuellen In-Memory-Speicher (`demo://workspace`), weist den Provider-ID `999` zu und wechselt direkt zum 8-Kachel-Dashboard. Es gibt keine Abstürze.

### 2. Dashboard-Statusanzeigen & Navigation
*   **Testschritte:**
    1. Überprüfe die Dashboard-Karten im Hauptbildschirm.
    2. Prüfe, ob "Demo Workspace" und "Demo Provider (Simuliert)" korrekt als aktiv angezeigt werden.
    3. Navigiere über die Bottom Navigation zwischen den Tabs (Dashboard, Agent, Files, Preview, Terminal, Settings).
*   **Erwartetes Ergebnis:** Alle Status-Karten werden flüssig gerendert. Der Wechsel zwischen den Tabs erfolgt ohne Verzögerung oder Hänger.

### 3. Dateibaum, Suche & Lösch-Sicherheit
*   **Testschritte:**
    1. Tippe auf den **Files**-Tab.
    2. Filtere die Dateien über das Suchfeld nach "script".
    3. Tippe auf das Mülleimer-Symbol neben einer Datei.
    4. Klicke im Bestätigungsdialog auf "Abbrechen", danach erneut und auf "Löschen".
*   **Erwartetes Ergebnis:** 
    - Die Suche filtert die Dateiliste in Echtzeit.
    - Dateityp-Icons werden passend zur Endung angezeigt (HTML, JS, CSS, JSON).
    - Ein Löschvorgang wird erst nach Bestätigung ausgeführt und aktualisiert die Liste sofort.

### 4. Code-Editor (Undo & Line Numbers)
*   **Testschritte:**
    1. Tippe im Explorer auf `index.html`.
    2. Deaktiviere und aktiviere die Zeilennummern über das Zahlen-Symbol in der Toolbar.
    3. Ändere Text im Editor und klicke auf das **Undo**-Symbol.
    4. Tippe auf das Kopieren-Symbol, um den gesamten Code in die Zwischenablage zu legen.
    5. Klicke auf **Speichern** (Diskettensymbol).
*   **Erwartetes Ergebnis:**
    - Der Editor rendert Text in einer sauberen Monospace-Schriftart.
    - Zeilennummern blenden sich fehlerfrei aus/ein.
    - Undo setzt Textänderungen zeichengenau zurück. Speichern aktualisiert den Datei-Timestamp.

### 5. Live Preview (Static & Server)
*   **Testschritte:**
    1. Wechsel zum **Preview**-Tab.
    2. Wähle unter "Static Web" eine HTML-Datei aus und überprüfe die Webview-Rendering-Ausgabe.
    3. Wechsel zum "Logs"-Subtab und prüfe, ob JavaScript-Ausgaben aufgefangen wurden.
    4. Wechsel zum "Termux Hilfe"-Tab und kopiere einen Befehl.
*   **Erwartetes Ergebnis:**
    - WebView rendert HTML und JavaScript fehlerfrei.
    - Konsolen-Ausgaben werden farblich hervorgehoben (Errors = Rot, Warnings = Orange).
    - Kopieren-Buttons kopieren die Termux-Befehle fehlerfrei in das Android-Clipboard.

### 6. Agenten-Planer & Diff-Review (Apply All)
*   **Testschritte:**
    1. Wechsel zum **Agent**-Tab.
    2. Schreibe eine Nachricht (z. B. "Ändere den Titel in index.html").
    3. Klicke in der Aktionsleiste auf **"Code"**.
    4. Sobald der Patch generiert wurde, klicke auf **"Apply"** oder die Diffs-Schaltfläche.
    5. Betrachte die Zeilendifferenzen (Grün für hinzugefügt, Rot für entfernt).
    6. Klicke oben rechts auf das Doppel-Häkchen-Symbol (**Apply All**).
*   **Erwartetes Ergebnis:**
    - Der Agent streamt den Text flüssig.
    - Diffs werden zeilenbasiert und gut lesbar auf dem Galaxy A56-Display dargestellt.
    - "Apply All" schreibt alle Änderungen in den Workspace und leitet zurück zum Chat.
