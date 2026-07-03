# PocketCodeAgent Development Runbook

Dieses Projekt ist eine **native Android-App**, die in Kotlin und Jetpack Compose geschrieben wurde. Es ist **keine** Web-App oder ein React-Frontend für sich selbst.

---

## 🌐 Localhost & Preview-Kontext

> [!IMPORTANT]
> Der Port `localhost:5173` bzw. `http://127.0.0.1:5173` ist ausschließlich für **externe Web-/Vite-Projekte** relevant, die der Coding-Agent für dich im gewählten Workspace erstellt oder bearbeitet. 
> Die Android-App selbst läuft als nativer APK-Prozess auf dem Smartphone/Emulator und wird **nicht** über einen Web-Server gehostet.

---

## 💻 Entwicklung in Android Studio

1. Öffne **Android Studio**.
2. Wähle **Open** und wähle das Stammverzeichnis: `C:\Users\mertg\PocketCodeAgent`.
3. Warte, bis der Gradle Sync abgeschlossen ist.
4. Klicke auf den grünen **Run**-Pfeil (oder drücke `Shift + F10`), um die App auf einem angeschlossenen USB-Gerät oder Emulator zu starten.

---

## 🛠️ Build-Befehle über die Kommandozeile (CLI)

Führe diese Befehle im Stammverzeichnis `C:\Users\mertg\PocketCodeAgent` aus:

### 1. Projekt bereinigen
```cmd
.\gradlew.bat clean
```

### 2. Debug-APK bauen
```cmd
.\gradlew.bat assembleDebug
```
*Die erzeugte APK-Datei liegt nach dem erfolgreichen Build unter:*
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Auf einem angeschlossenen Android-Gerät installieren
Stelle sicher, dass ein Handy über USB mit aktiviertem **USB-Debugging** angeschlossen ist, und führe aus:
```cmd
.\gradlew.bat installDebug
```

---

## 📱 Emulator & F-Droid Integration (Termux)
- Für vollen Funktionsumfang (Vite/Node-Previews) installiere **Termux** auf dem Zielgerät.
- Starte den Vite-Server in Termux (`npm run dev -- --host 127.0.0.1`) und die App verbindet sich über das interne Live-Preview-Panel automatisch mit `http://127.0.0.1:5173`.
