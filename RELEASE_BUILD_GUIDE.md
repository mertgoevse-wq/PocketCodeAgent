# Release Build Guide — PocketCodeAgent

## Overview

This guide covers preparing PocketCodeAgent for release: generating a signing key, building a signed APK/AAB, and important security practices.

---

## Current Build Configuration

| Setting | Value |
|---------|-------|
| `applicationId` | `com.pocketcodeagent` |
| `versionCode` | `1` |
| `versionName` | `1.0` |
| `minSdk` | `26` (Android 8.0) |
| `targetSdk` | `36` (Android 16) |
| `compileSdk` | `36` |
| `namespace` | `com.pocketcodeagent` |

---

## Prerequisites

- JDK 17+
- Android SDK (API 36)
- Gradle 8.x (via wrapper)
- A secure location for your keystore file

---

## Step 1: Generate a Signing Key

Generate a Release key using `keytool` (part of JDK):

```bash
keytool -genkey -v \
  -keystore pocketcodeagent-release.keystore \
  -alias pocketcodeagent \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS
```

You will be prompted to:
- Set a **keystore password** (store securely!)
- Set a **key password** (can be same as keystore password)
- Enter certificate details (name, organization, etc.)

---

## Step 2: Secure the Keystore

**CRITICAL RULES:**

1. **NEVER commit the keystore to the Git repository**
2. **NEVER commit passwords to the repository**
3. Store the keystore in a secure location outside the project directory
4. Back up the keystore — if lost, you cannot update the app on Google Play
5. Store passwords in a password manager or secure vault

Add to `.gitignore`:
```
*.keystore
*.jks
keystore.properties
```

---

## Step 3: Configure Signing in Gradle

Create `keystore.properties` in the project root (NOT committed to Git):

```properties
storeFile=C:/secure/path/pocketcodeagent-release.keystore
storePassword=your-keystore-password
keyAlias=pocketcodeagent
keyPassword=your-key-password
```

Update `app/build.gradle.kts` to read signing config:

```kotlin
// Add inside android {} block, before defaultConfig
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = if (keystorePropertiesFile.exists()) {
    java.util.Properties().apply {
        load(keystorePropertiesFile.inputStream())
    }
} else null

signingConfigs {
    create("release") {
        if (keystoreProperties != null) {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}

buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

---

## Step 4: Build the Release APK

```bash
# Windows
./gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

Release APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Step 5: Build the Release AAB (for Google Play)

```bash
# Windows
./gradlew.bat bundleRelease

# Linux/Mac
./gradlew bundleRelease
```

AAB will be at:
```
app/build/outputs/bundle/release/app-release.aab
```

Google Play requires **AAB format** (not APK) for new apps.

---

## Step 6: ProGuard / R8 Configuration

The current release config uses `proguard-android-optimize.txt`. Create `app/proguard-rules.pro` if needed:

```proguard
# Keep Room entities
-keep class com.pocketcodeagent.data.local.entity.** { *; }

# Keep Gson serialized models
-keep class com.pocketcodeagent.data.model.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```

---

## Debug vs Release Builds

| Aspect | Debug | Release |
|--------|-------|---------|
| Signing | Debug keystore (auto-generated) | Your release keystore |
| Minification | Disabled | Enabled (R8/ProGuard) |
| APK location | `app/build/outputs/apk/debug/` | `app/build/outputs/apk/release/` |
| Google Play | Not accepted | Use AAB format |

---

## Pre-Release Checklist

- [ ] Version code bumped in `app/build.gradle.kts`
- [ ] Version name updated
- [ ] All tests pass: `./gradlew.bat test`
- [ ] Release build succeeds: `./gradlew.bat assembleRelease`
- [ ] Release AAB build succeeds: `./gradlew.bat bundleRelease`
- [ ] App tested on a real Android device (not just emulator)
- [ ] Privacy policy URL ready and publicly accessible
- [ ] Store listing content prepared
- [ ] Screenshots taken (phone + tablet, no API keys visible)
- [ ] Data safety form completed in Play Console
- [ ] Content rating questionnaire completed
- [ ] Keystore backed up securely
- [ ] No debug logging or test code in release build

---

## Common Issues

| Issue | Solution |
|-------|----------|
| `keystore.properties` not found | File must be at project root with correct property names |
| `Keystore was tampered with` | Wrong password or corrupted keystore |
| `keytool not found` | Ensure JDK `bin/` is in PATH |
| `AAB too large` | Check ProGuard rules, remove unused resources |
| `minSdk too low` | Google Play may reject if targetSdk is below requirement |
