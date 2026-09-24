# CDLJobInfo Mobile App

Native mobile apps for CDLJOBINFO Driver Hub.

## Apps

- **Android 0.3.2** — Kotlin + Android WebView
- **iOS 0.3.0** — Swift + SwiftUI + WKWebView
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
