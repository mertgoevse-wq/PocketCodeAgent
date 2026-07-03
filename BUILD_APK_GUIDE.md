# PocketCodeAgent - APK Build & Installations-Guide 🚀

Diese Anleitung erklärt Schritt-für-Schritt, wie du die **PocketCodeAgent**-App kompilierst, auf dein Android-Smartphone übertragst und dort installierst.

---

## 🏗️ 1. APK auf dem PC bauen

PocketCodeAgent wird mit modernem Gradle und JDK 17+ gebaut.

### Voraussetzungen

1. **Java Development Kit (JDK 17 oder höher)**:
   - Stelle sicher, dass `java -version` in deinem Terminal JDK 17 oder höher ausgibt.
2. **Android SDK**:
   - Die SDK-Pfade müssen in der Datei `local.properties` im Projekt-Hauptverzeichnis eingetragen sein (wird von Android Studio automatisch verwaltet).

### Build-Befehl ausführen

Öffne eine Kommandozeile (PowerShell, CMD oder Git Bash) im Projekt-Hauptordner `C:\Users\mertg\PocketCodeAgent` und führe folgenden Befehl aus:

- **In Windows PowerShell / CMD:**

    ```powershell
    .\gradlew.bat assembleDebug
    ```

* **In macOS / Linux / Git Bash:**

    ```bash
    ./gradlew assembleDebug
    ```

### APK-Ausgabe finden

Nachdem der Build mit **BUILD SUCCESSFUL** abgeschlossen wurde, findest du die installierbare APK-Datei im folgenden Verzeichnis:

```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 📲 2. Auf das Smartphone übertragen

Du kannst die APK-Datei auf verschiedene Arten auf dein Android-Handy übertragen:

1. **Über USB-Kabel (MTP)**:
   - Verbinde dein Handy per USB-Kabel mit dem PC.
   - Wähle am Handy den USB-Modus "Dateitransfer" (MTP).
   - Kopiere die Datei `app-debug.apk` in den Ordner **Download** deines Handys.
2. **Über ADB (Android Debug Bridge)**:
   - Aktiviere die **Entwickleroptionen** und das **USB-Debugging** auf deinem Handy.
   - Führe auf dem PC folgenden Befehl aus:

     ```bash
     adb install app\build\outputs\apk\debug\app-debug.apk
     ```

3. **Über Cloud / Messenger**:
   - Lade die APK auf Google Drive, OneDrive hoch oder sende sie dir selbst per E-Mail / Telegram / Discord.

---

## ⚙️ 3. Installation auf Android erlauben

Da die APK nicht aus dem Google Play Store stammt, blockiert Android die Installation standardmäßig als Sicherheitsmaßnahme.

1. Öffne die **Dateimanager-App** auf deinem Handy (z. B. "Eigene Dateien", "Files by Google").
2. Navigiere zum Ordner **Download** und tippe auf `app-debug.apk`.
3. Es erscheint ein Warnhinweis: *"Aus Sicherheitsgründen dürfen Apps aus dieser Quelle nicht installiert werden."*
4. Tippe auf **Einstellungen** im Dialog.
5. Aktiviere den Schalter **"Aus dieser Quelle zulassen"** für deinen Dateimanager.
6. Gehe zurück und tippe auf **Installieren**.

---

## 🛠️ 4. Fehlerbehebung (Troubleshooting)

### A. Fehler: "App wurde nicht installiert" oder "Paket scheint beschädigt zu sein"

* **Grund 1: Paketnamens-Konflikt**:
  - Wenn bereits eine ältere oder andere Version von PocketCodeAgent auf deinem Handy installiert ist (z. B. eine Release-Version mit einer anderen Signatur), blockiert Android das Update.
  - *Lösung*: Deinstalliere die vorhandene Version von PocketCodeAgent vollständig von deinem Handy und versuche die Installation der neuen APK erneut.
- **Grund 2: Google Play Protect Blockade**:
  - Play Protect scannt APKs und warnt bei unbekannten Signaturen.
  - *Lösung*: Tippe im Play Protect Warnfenster auf *"Trotzdem installieren"*.

### B. Gradle / Compiler-Fehler auf dem PC

* **Fehler: "SDK location not found"**:
  - *Lösung*: Erstelle eine Datei namens `local.properties` im Projekt-Hauptordner und trage dort den absoluten Pfad zu deinem Android SDK ein, z.B.:

      ```properties
      sdk.dir=C\:\\Users\\DEIN_BENUTZERNAME\\AppData\\Local\\Android\\Sdk
      ```

* **Fehler: "Unsupported class file major version" (Java-Version-Mismatch)**:
  - *Lösung*: Setze die Umgebungsvariable `JAVA_HOME` temporär im Terminal auf dein JDK 17 oder höher (z. B. die in Android Studio integrierte Runtime):

      ```powershell
      $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
      .\gradlew.bat assembleDebug
      ```
