# Permissions Audit — PocketCodeAgent

## Date
July 4, 2026

## Source
`app/src/main/AndroidManifest.xml`

---

## Declared Permissions

| Permission | Required? | Justification | Risk Level |
|-----------|-----------|---------------|------------|
| `INTERNET` | ✅ Yes | API calls to user-configured LLM providers (OkHttp) | Low |
| `ACCESS_NETWORK_STATE` | ✅ Yes | Check network connectivity before API calls | Low |
| `POST_NOTIFICATIONS` | ✅ Yes | Show notifications for agent run status (Android 13+) | Low |

---

## Not Declared (Intentionally Avoided)

| Permission | Why Not Declared |
|-----------|-----------------|
| `MANAGE_EXTERNAL_STORAGE` | File access uses **SAF** (Storage Access Framework) with scoped, user-granted directories — no broad storage needed |
| `READ_EXTERNAL_STORAGE` | Replaced by SAF DocumentFile pattern |
| `WRITE_EXTERNAL_STORAGE` | Replaced by SAF DocumentFile pattern |
| `READ_PHONE_STATE` | Not needed |
| `ACCESS_FINE_LOCATION` | Not needed |
| `CAMERA` | Not needed |
| `RECORD_AUDIO` | Not needed |
| `READ_CONTACTS` | Not needed |

---

## Permission Categories (for Data Safety)

| Category | Data Type | Collected | Shared | Purpose |
|----------|-----------|-----------|--------|---------|
| Personal Info | API keys (user-provided) | Yes (local, encrypted) | Yes (sent to user-configured provider) | App functionality |
| Personal Info | Provider configurations | Yes (local) | No | App functionality |
| Messages | Chat history | Yes (local) | No | App functionality |
| Files | Workspace files (user-selected) | Yes (local, SAF) | No | App functionality |

---

## Runtime Permission Handling

| Permission | Runtime Request? | Notes |
|-----------|-----------------|-------|
| `INTERNET` | No (normal permission) | Granted at install |
| `ACCESS_NETWORK_STATE` | No (normal permission) | Granted at install |
| `POST_NOTIFICATIONS` | Yes (Android 13+) | Requested when showing agent notifications |
| SAF Directory Access | Yes (via Intent) | User selects directory via system picker |

---

## Google Play Permissions Declaration

For the Google Play Console "App content > Sensitive app permissions" section:

- **No sensitive permissions declared** — INTERNET and ACCESS_NETWORK_STATE are normal permissions and don't require special declaration
- **POST_NOTIFICATIONS** is a normal permission on Android 13+
- **SAF** does not appear as a manifest permission — it's an intent-based access pattern

---

## Recommendations

1. ✅ Keep SAF-based file access — avoid `MANAGE_EXTERNAL_STORAGE` entirely
2. ✅ `POST_NOTIFICATIONS` is justified for agent run status
3. ✅ No location, camera, microphone, or contacts access — privacy-friendly
4. ⚠️ If Termux integration is ever automated (not just copy-paste), review permission needs

---

## AndroidManifest Quick Reference

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Total: 3 permissions, all justified, none dangerous beyond POST_NOTIFICATIONS.**
