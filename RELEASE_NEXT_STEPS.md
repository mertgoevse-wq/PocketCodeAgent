# PocketCodeAgent - Release-Fahrplan 🚀

Dieses Dokument beschreibt die notwendigen Schritte, um PocketCodeAgent aus dem aktuellen Entwicklungsstadium (Debug-APK) in eine signierte Release-Version für den Google Play Store oder die manuelle Verteilung zu überführen.

---

## 🔑 Schritt 1: Keystore für Release-Signatur erstellen

Um eine Release-APK oder ein Android App Bundle (`.aab`) zu bauen, benötigst du einen Signaturschlüssel (Keystore).

1. Öffne ein Terminal und generiere einen Keystore mit folgendem Befehl:
   ```bash
   keytool -genkey -v -keystore pocketcode-release.keystore -alias pocketcode-key -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Sichere die generierte Datei `pocketcode-release.keystore` gut und merke dir das Passwort!

---

## ⚙️ Schritt 2: Signatur in Gradle konfigurieren

Trage den Keystore in der Datei `app/build.gradle.kts` ein. Erstelle dazu am besten eine Datei `keystore.properties` im Projekt-Hauptordner (nicht in Git einchecken!):

```properties
storeFile=../pocketcode-release.keystore
storePassword=DEIN_STORE_PASSWORT
keyAlias=pocketcode-key
keyPassword=DEIN_KEY_PASSWORT
```

Passe die `signingConfigs` in `app/build.gradle.kts` wie folgt an:

```kotlin
android {
    ...
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = java.util.Properties()
                properties.load(keystorePropertiesFile.inputStream())
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 📦 Schritt 3: Release-Builds generieren

Führe den passenden Build-Befehl aus:

*   **Release APK bauen:**
    ```powershell
    .\gradlew.bat assembleRelease
    ```
    *Ausgabepfad:* `app/build/outputs/apk/release/app-release.apk`
*   **Android App Bundle (AAB für Google Play Store) bauen:**
    ```powershell
    .\gradlew.bat bundleRelease
    ```
    *Ausgabepfad:* `app/build/outputs/bundle/release/app-release.aab`

---

## 🌍 Schritt 4: Verteilung & Google Play Store

1. **Google Play Console einrichten:**
   - Melde dich bei der Play Console mit einem Entwicklerkonto an.
   - Erstelle eine neue App namens **PocketCodeAgent**.
2. **Rechtliches & Setup:**
   - Hinterlege eine Datenschutzerklärung (Privacy Policy).
   - Fülle den Fragebogen zur Altersfreigabe und den App-Inhalten aus.
3. **Produktionstrack starten:**
   - Erstelle einen neuen Release-Zweig in der Play Console.
   - Lade die `.aab` (App Bundle) hoch.
   - Warte auf die Freigabe durch Google (dauert meist 1 bis 7 Tage).
