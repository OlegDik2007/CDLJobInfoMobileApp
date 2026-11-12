# CDL Hub — unified Android / iOS app identity

## Recommended public naming

- **Brand:** CDL Hub
- **App Store / Google Play title:** **CDL Hub: Weather & Cameras** (27 characters; proposed store listing title, subject to store-name availability and trademark review)
- **Phone launcher label:** **CDL Hub** (intentionally short, avoids truncation)
- **Tagline:** **Truckers' Road Intelligence**
- **Description:** Official traffic cameras, road weather conditions and warnings, real-time road alerts, fuel stations, CDL jobs and driver community tools in one app
- **Do not advertise** as turn-by-turn truck GPS navigation until in-app navigation is implemented

Keep the app IDs unchanged: Android `info.cdljob.driverhub`, iOS `info.cdljob.driverhub`.

## Brand icon

Approved image source: `branding/approved-cdl-hub-icon.webp.b64` (small user-selected icon, optimized for version control).\nThe older vector `branding/cdl-hub-icon.svg` is NOT the selected brand and is not rendered in CI.

Distinctive elements:
- Navy highway backdrop
- White semi-truck
- Orange location pin
- Cyan frame and curved road

All icons are derived from the same approved source image by `branding/render_icons.py`.

To regenerate:

```sh
python3 -m pip install pillow
python3 branding/render_icons.py --platform all
```

Android CI generates a branded PNG launcher and replaces the version-controlled XML fallback in its temporary checkout. Xcode CI renders Apple-compliant opaque AppIcon PNG sizes from the same artwork. For local builds, run the renderer first.

## Release identity

- Android test build: `versionCode 10` / `0.9.0`
- iOS source release candidate: `0.9.0`, build `9`
- Platform build signing is separate: a debug Android APK is not a signed Google Play bundle, and an unsigned iOS build is not distributable through TestFlight.
- Website naming stays `CDL Hub`; no route guidance or camera-source rights are altered by the icon change.

## Other names considered (not selected)

- CDL RoadWatch — emphasizes safety, but is narrower than Jobs and Community
- TruckRadar — memorable, but may imply radar data the product does not provide
- CDL Road Center — descriptive, less distinctive

Verify store-name and trademark availability before public submission.
