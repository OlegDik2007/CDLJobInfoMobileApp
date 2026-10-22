# CDL Hub Mobile — Production Readiness (2026-10-22)

## Release candidate and source of truth

- Mobile code: `OlegDik2007/CDLJobInfoMobileApp`.
- Website and API code: `OlegDik2007/Driver_portal`, live at `https://cdljob.info`.
- Current release candidate: Android 0.7.0 (`versionCode 8`), iOS 0.7.0 (`CFBundleVersion 7`).
- Both apps render `/mobile-app/` and share live road/camera/weather/fuel/chat APIs.
- Five languages in mobile web UI: en, uk, ru, es, uz. Road names, original third-party camera footage, official source content, and map base labels are not machine-translated.
- Camera catalog, road events, winter alerts, images, and video are subject to source-specific permissions. Newly discovered permission-gated sources must remain disabled until authorized.

## Features verified in code / automated builds

| Feature | Android | iOS | Backend |
|---|---|---|---|
| GPS/manual road location | WebView + Kotlin permission | WKWebView + CoreLocation | API |
| Camera Center, search and safe source links | Shared web app | Shared web app | API |
| Feed, alerts and weather | Shared web app | Shared web app | API |
| Map (unchanged by recent localization work) | MapLibre inside WebView | MapLibre inside WKWebView | Weather/map API |
| Live chat / 24h photos | Native chooser + WebView | WKWebView picker | Neon + VPS |
| Locale selection | Web UI + Kotlin language bridge | Web UI + Swift language bridge | Web UI |
| Background road monitoring | User-initiated foreground service with explicit start/stop | Opt-in significant-location-change monitoring | Last-known-location API |
| Remote push | Firebase FCM plumbing | APNs plumbing | Requires secrets and provider activation |
| Local background road alerts | Kotlin local check while service runs | No equivalent guarantee | Feed API |
| Chat reporting/block | Shared web UI | Shared web UI | Report + moderation APIs |

## Engineering work completed

- Synced Android's native GPS, chat camera/gallery chooser, local road alerts, Firebase client, localized notifications, and WebView behavior with the web repository.
- Synced iOS language/GPS bridges; added camera/photo permission text in all five languages; declared background location and remote-notification modes.
- Added Android opt-in consent before persistent location monitoring, a Stop control, and registration enable/disable propagation.
- Changed iOS to distinguish foreground GPS requests from opt-in background alert monitoring; stopping disables APNs registration in the backend.
- Added public privacy disclosures for background location, temporary chat images, device tokens and retention.
- Added 24-hour chat pruning and stale mobile GPS cleanup with a protected scheduled endpoint.
- Added chat user blocking, abuse reporting and authenticated moderation endpoint; chat image service now strips EXIF and GPS metadata on upload and supports authenticated deletion.

## **Not cleared for public App Store / Google Play submission yet**

1. **Store signing:** configure Android upload keystore and GitHub Actions secrets; configure Apple Developer Team, signing certificate, provisioning, bundle ID, and APNs capability. An unsigned iOS device archive and Android debug build are not store distributions.
2. **Push provider activation:** configure FCM service account on the backend and Firebase client config at build time, plus APNs signing credentials and entitlements. Do end-to-end real-device push tests while the app is backgrounded.
3. **Identity and abuse protection:** mobile telemetry and anonymous chat APIs use client-provided device IDs. Before scaling to untrusted public users, add installation-scoped authenticated tokens, IP/device rate limits and abuse controls; do not treat anonymous IDs as verified identities.
4. **Chat moderation operations:** appoint moderators to review incoming reports promptly, document escalation/removal standards, and verify objectionable text and image filtering. Report/block features alone do not demonstrate an operational moderation process.
5. **Real-device QA:** test 5 locales on both platforms, Wi-Fi/cellular/offline, location denied/granted, foreground-background-reopened, force-stop behavior, camera picker, video playback, chat photo deletion, expired alerts, GPS state fallback, iOS APNs production environment, Android target SDK, accessibility and battery usage.
6. **Google Play review:** background location / foreground service declarations as applicable, prominent in-app location disclosure and demo video, Data safety form, privacy-policy link, screenshots, content rating and app access.
7. **App Store review:** Apple Developer enrollment, App Privacy labels, background location purpose, age rating, screenshots, reviewer notes, UGC moderation and ensure the app adds adequate native functionality beyond a repackaged website. See Apple App Review Guidelines 1.2 and 4.2.
8. **Data rights/licensing:** verify each DOT/511 camera license, link vs image/proxy redistribution, image retention and attribution before promoting new states to the public.
9. **Distribution and rollback:** staged closed testing, internal TestFlight/Google Play tracks, monitored crash reports, and rollback playbook for web/API because mobile WebViews fetch live releases.

## Build and release workflows

- `.github/workflows/build-android.yml`: builds debug APK for testing.
- `.github/workflows/build-ios.yml`: builds simulator app without signing.
- `.github/workflows/archive-ios.yml`: validates unsigned iOS device Release archive.
- `.github/workflows/release-android.yml`: manually builds signed Google Play AAB and release APK **only if** upload keystore and secrets are present. Uses protected `production` environment.
- No signed store package is committed, and signing material must never be added to Git.

### Android signing secrets

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_STORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`

### Android Firebase build secrets

- `FIREBASE_PROJECT_ID`
- `FIREBASE_APPLICATION_ID`
- `FIREBASE_API_KEY`
- `FIREBASE_GCM_SENDER_ID`

### Production backend secrets

- `CRON_SECRET` to authorize scheduled retention.
- `CHAT_MODERATION_SECRET` to restrict moderation.
- `FIREBASE_SERVICE_ACCOUNT_JSON` for actual FCM sending (not configured as of audit).
- APNs credentials/configuration for actual iOS push (not configured as of audit).
- `BANNER_UPLOAD_ENDPOINT` and `BANNER_UPLOAD_SECRET` protect VPS chat photo storage.

### Validation

Run with real devices and document results before changing status to `release approved`. Automated successful compilation does not validate live push, GPS permissions, source licensing, or App Store acceptance.
