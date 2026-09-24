# CDL Driver Hub Android

Native Kotlin shell for CDLJOBINFO Live Road.

Current version: 0.3.2

Features:
- GPS-based nearby alerts, cameras, road weather, and fuel data
- Manual state/interstate selection
- Foreground location tracking while the road-alert mode is active
- Firebase Cloud Messaging road alerts when Firebase and backend push are configured
- Local road-alert polling only as a fallback when remote push is unavailable
- Jobs and company reviews as secondary options

The native Android shell loads https://cdljob.info/mobile-app/ so the live UI can improve without reinstalling the app.

Background model:
1. The foreground location service updates the driver location.
2. The backend checks nearby road events/restrictions and can deliver FCM alerts.
3. If remote push is not ready, the app falls back to one local checker.
