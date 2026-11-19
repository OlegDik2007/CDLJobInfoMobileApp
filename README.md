# CDL Hub Mobile App

Native mobile apps for CDL Hub.

## Apps

- **Android 1.0.0-rc1 (versionCode 12)** — Kotlin + Android WebView; Google Play Internal Testing candidate only
- **iOS 0.9.0** — Swift + SwiftUI + WKWebView
- Shared live UI: https://cdljob.info/mobile-app/
- Shared backend/APIs: https://cdljob.info

## Main features

- GPS-based road alerts
- Live road cameras
- Weather and road conditions
- Fuel information
- Manual location / interstate selection
- Background road-alert location updates
- Push notifications
  - Android: Firebase Cloud Messaging (FCM)
  - iOS: Apple Push Notification service (APNs)
- Jobs and company reviews as secondary features

## Google Play release

- Canonical signed Android AAB release work is in this repository, **not** the backend repo's legacy mobile copy.
- For signing, Google Play policy declarations, testers and QA, see [Internal Testing Release Gate](docs/GOOGLE-PLAY-INTERNAL-TESTING.md).
- The GitHub workflows distinguish developer debug APK, unsigned release verification and manually triggered signed AAB. A successful source build does not mean the app is published.

## Repository structure

- `android/` — Android application source
- `ios/CDLDriverHub/` — iOS Xcode project
- `.github/workflows/` — Android and iOS CI builds

Backend road data, Neon storage, alert matching, cameras, weather/fuel APIs and FCM/APNs dispatch remain in **Driver_portal**. This repository is now the canonical mobile-client repository.

## Background alert model

The server receives the driver's latest permitted location and checks nearby crashes, closures and truck restrictions.

Example:

> 🚨 Crash 4 mi ahead — I-80

Android uses a foreground location service while road monitoring is active. iOS uses Apple's lower-power significant-location-change monitoring when the user grants Always Location access.

## Website / mobile synchronization (2026-11-17)

- Android and iOS load the current `https://cdljob.info/mobile-app/` UI, which includes the internal News feed under More → News and supports five UI languages.
- Road incidents, DOT/511 cameras, NWS weather coverage, winter observations, GPS nearby feeds, fuel data, chat, jobs and reviews are supplied by the shared Driver_portal API. Website backend ingestion and freshness updates take effect without copying server code into native projects.
- Android 0.9.1 points the native News menu at `/mobile-app/#newsSec` and Driver Health at `/mobile-app/#healthSec`, instead of opening the desktop-oriented web pages. Background API user-agent strings now match the Android build.
- iOS uses the same in-app News and Health destinations through its shared mobile interface. The native project now also declares APNs signing entitlements and a privacy manifest.
- Android now provides a visible native toolbar with News and overflow destinations, plus five-language native labels. Its opted-in foreground location service uses START_NOT_STICKY to avoid Android recreating the service in the background after process death; reopening the app can resume monitoring. New APKs are required for native changes; web UI updates generally do not require reinstalling either client. Production signing, push activation, rights review and real-device QA remain prerequisites for store releases.
