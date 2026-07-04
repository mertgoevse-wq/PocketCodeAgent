# Store Readiness Phase Report — PocketCodeAgent

## Date
July 4, 2026

## Status
**Not yet published.** Documentation prepared. Real device testing and Play Console setup remain.

---

## Summary

Prepared all documentation required for Google Play Store publishing:
- Store listing draft (name, descriptions, features, disclaimers)
- Privacy policy (GDPR-friendly, transparent about local-only data)
- Permissions audit (3 normal permissions, SAF-based file access)
- Release build guide (signing, AAB, ProGuard)
- Comprehensive review of store readiness gaps

---

## Created Documents

| Document | Purpose | Status |
|----------|---------|--------|
| `STORE_LISTING_DRAFT.md` | App name, short/long description, features, target audience, disclaimers | ✅ Ready for review |
| `PRIVACY_POLICY_DRAFT.md` | Data handling, encryption, provider requests, logs, third-party libraries | ✅ Ready for review |
| `RELEASE_BUILD_GUIDE.md` | Signing key generation, Gradle config, APK/AAB build, ProGuard | ✅ Ready |
| `PERMISSIONS_AUDIT.md` | Manifest analysis, justification, data safety mapping | ✅ Complete |

---

## App Metadata Summary

| Field | Value |
|-------|-------|
| App Name | PocketCodeAgent — Mobile AI Coding Workbench |
| Package | `com.pocketcodeagent` |
| Version | `1.0` (versionCode 1) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 16) |
| Compile SDK | 36 |
| APK Size (debug) | ~20.8 MB |

---

## Permissions: 3 Normal, 0 Dangerous

| Permission | Type | Justification |
|-----------|------|---------------|
| `INTERNET` | Normal | LLM provider API calls |
| `ACCESS_NETWORK_STATE` | Normal | Connectivity check |
| `POST_NOTIFICATIONS` | Normal | Agent run status (Android 13+) |

**No broad storage** — SAF-based scoped access only ✅

---

## App Icon

Launcher icon replaced in THEME_ICON_PHASE: custom code-bracket motif (`<>`) with SlateBlue/CalmSage colors on #0E0E10 background. ✅

---

## Play Store Requirements Check

| Requirement | Status | Notes |
|-------------|--------|-------|
| targetSdk ≥ 35 | ✅ targetSdk 36 | Exceeds August 2025 requirement |
| Privacy policy URL | ⚠️ Not yet hosted | Draft exists — needs public URL |
| Data safety section | ⚠️ Not yet filled | Draft prepared in PERMISSIONS_AUDIT.md |
| Content rating | ⚠️ Not yet done | Questionnaire in Play Console |
| AI content policy | ⚠️ Needs review | App generates code — must comply with AI-Generated Content policy |
| App content labeling | ⚠️ Not yet done | No restricted content identified |
| AAB format | ⚠️ Not yet built | APK tested; AAB requires release signing |
| Screenshots | ❌ Not taken | Requires emulator or real device |
| Feature graphic | ❌ Not created | 1024x500 banner for Play Store |
| Developer account | ❌ Not registered | $25 one-time fee |

---

## Pre-Publishing To-Do List

### Critical (Block Publishing)
- [ ] Register Google Play Developer account ($25 one-time fee)
- [ ] Host privacy policy at a public URL (GitHub Pages, Firebase Hosting, or similar)
- [ ] Generate release signing key (see RELEASE_BUILD_GUIDE.md)
- [ ] Build and test release AAB on a real Android device
- [ ] Complete Play Console Data safety form
- [ ] Complete Play Console Content rating questionnaire
- [ ] Create app screenshots (phone 16:9 + tablet 16:9, no API keys visible)
- [ ] Create feature graphic (1024x500)

### Important (Before Publishing)
- [ ] Verify code generation output doesn't violate AI-Generated Content policy
- [ ] Test on real Android 8.0 device (minSdk 26 edge case)
- [ ] Test on Android 16 device (targetSdk 36)
- [ ] Add ProGuard rules for Room, Gson, OkHttp (see RELEASE_BUILD_GUIDE.md)
- [ ] Bump versionCode and versionName before first release
- [ ] Update store listing text with feedback from beta testers
- [ ] Run `./gradlew.bat lint` and fix critical warnings

### Nice-to-Have
- [ ] Add app screenshots with Ivory theme
- [ ] Create German-language store listing
- [ ] Add changelog for first release
- [ ] Set up Play Console internal testing track
- [ ] Add in-app "Rate this app" prompt (after user has used it a few times)

---

## Sensitive Feature Disclosure

PocketCodeAgent has features that require careful Play Store disclosure:

| Feature | Disclosure |
|---------|-----------|
| Code generation (AI) | ✅ Documented in listing — user reviews all changes |
| File modification | ✅ SAF-based, user confirms before apply |
| Terminal commands | ✅ Copy-only, never auto-executed |
| API key storage | ✅ Encrypted in Keystore, masked in UI |
| WebView preview | ✅ Local files only, console sanitized |
| Export/share | ✅ User-initiated only |

---

## Build Result

| Step | Result |
|------|--------|
| `./gradlew.bat clean` | ✅ |
| `./gradlew.bat test` | ✅ All passing |
| `./gradlew.bat assembleDebug` | ✅ |
| APK path | `app/build/outputs/apk/debug/app-debug.apk` |
| APK size | ~20.8 MB |

---

## Open Questions

1. **Privacy policy hosting:** GitHub Pages (free), Firebase Hosting, or custom domain?
2. **Monetization:** Free, paid, or open-source donation model?
3. **Beta testing:** Closed alpha, open beta, or straight to production?
4. **Release timing:** Wait for more features (string i18n, theme-aware colors) or ship as-is?
5. **Google Play AI policy:** Should we add an explicit in-app notice that AI-generated code must be reviewed?

---

## Next Steps

1. **Immediate:** Host privacy policy at a public URL
2. **Short-term:** Generate signing key, build release AAB
3. **Short-term:** Take screenshots on real device or emulator
4. **Medium-term:** Register Play Developer account, create listing
5. **Long-term:** Beta testing → Production release
