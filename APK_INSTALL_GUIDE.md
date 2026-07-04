# PocketCodeAgent — APK Installation Guide

## APK Path
```
app\build\outputs\apk\debug\app-debug.apk
```
- **Size:** ~20.8 MB
- **Build type:** Debug (unsigned)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target:** Samsung Galaxy A56 (Android 14+)

---

## Installation per ADB

### Prerequisites
- Android device with USB debugging enabled
- ADB installed (Android SDK Platform Tools)
- Device connected via USB

### Command
```bash
.\gradlew.bat installDebug
```

Or manually:
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The `-r` flag replaces an existing installation without data loss.

---

## Manual Installation (without ADB)

1. Copy `app-debug.apk` to your phone (USB cable, cloud, or messaging app)
2. On the phone, open **Settings → Security → Unknown sources** or **Install unknown apps**
3. Grant permission to your file manager or browser
4. Navigate to the APK file and tap to install
5. If warned about "unsafe app": confirm installation (this is a debug build, not from Google Play)

> ⚠️ The debug APK is **unsigned**. Android will show a security warning. This is normal for dev builds.

---

## First Launch

1. Open **PocketCodeAgent**
2. Tap "Get Started" on the welcome screen
3. On Provider Setup, add your API provider:
   - **OpenRouter** (recommended): `https://openrouter.ai/api/v1` + your API key
   - **Google Gemini**: `https://generativelanguage.googleapis.com/v1beta/openai/` + your API key
   - **NVIDIA NIM**: `https://integrate.api.nvidia.com/v1` + your API key
   - **Custom**: Any OpenAI-compatible endpoint
4. Tap **Test** to verify the connection
5. Tap **Weiter** → pick a workspace folder
6. Enter the main shell

---

## Troubleshooting

### "App not installed"
- **Cause:** Conflicting package signature or insufficient storage
- **Fix:** Uninstall any previous PocketCodeAgent, free up ≥100 MB storage

### "App crashes on launch"
- **Cause:** Android version < 8.0 (Oreo) or missing WebView
- **Fix:** Update to Android 8.0+. Install/update Android System WebView from Play Store

### "WebView shows blank page"
- **Cause:** WebView version too old
- **Fix:** Update Android System WebView (Play Store → search "Android System WebView")

### "No internet" / Provider connection failed
- **Cause:** Missing INTERNET permission or no network
- **Fix:** The app requests INTERNET permission automatically. Check Wi-Fi/mobile data. Verify the API endpoint URL is correct.

### Provider Error 401 (Unauthorized)
- **Cause:** Invalid or expired API key
- **Fix:** Check your API key in Provider Settings. Re-enter and test. Keys are encrypted in Android Keystore.

### Provider Error 403 (Forbidden)
- **Cause:** API key lacks required permissions or model access
- **Fix:** Check your API provider's dashboard for model access and billing status.

### Provider Error 404 (Not Found)
- **Cause:** Wrong Base URL or model name
- **Fix:** Verify the Base URL format (must include `/v1` for OpenAI-compatible endpoints). Check model name spelling.

### Provider Error 429 (Rate Limited)
- **Cause:** Too many requests
- **Fix:** Wait and retry. Check your API provider's rate limits. Consider upgrading your API plan.

### "Workspace Permission verloren" / Files not showing
- **Cause:** SAF persistent URI permission revoked (Android may clear after reboot or app update)
- **Fix:** Re-select the workspace folder. The app uses Android Storage Access Framework (SAF) — if the permission is lost, re-pick the folder.

### "Termux nicht erkannt"
- **Cause:** Termux not installed or not detected
- **Fix:** Install Termux from F-Droid or GitHub (NOT Play Store — the Play Store version is outdated). PocketCodeAgent does NOT execute commands automatically; it provides copyable command references for manual execution in Termux.

### "API Key visible in logs"
- **Cause:** This should NOT happen — all API keys are masked in logs, Toasts, and error messages
- **Fix:** If you see an API key in plain text, it's a bug. Report it.
