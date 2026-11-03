# CDL Hub — unified Android / iOS app identity

## Recommended public naming

- **Brand:** CDL Hub
- **App Store / Google Play title:** **CDL Hub: Trucking Companion** (27 characters; subject to store-name availability and trademark review)
- **Phone launcher label:** **CDL Hub** (intentionally short, avoids truncation)
- **Tagline:** **Truckers' Road Intelligence**
- **Description:** Real-time road alerts, official traffic cameras, weather, fuel stations, jobs and driver community tools in one app
- **Do not advertise** as turn-by-turn truck GPS navigation until in-app navigation is implemented

Keep the app IDs unchanged: Android `info.cdljob.driverhub`, iOS `info.cdljob.driverhub`.

## Brand icon

Authoritative source: `branding/cdl-hub-icon.svg`.

Distinctive elements:
- Navy highway backdrop
- White semi-truck
- Orange location pin
- Cyan frame and curved road

All icons are derived from the same vector source by `branding/render_icons.py`.

To regenerate:

```sh
python3 -m pip install cairosvg pillow
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
