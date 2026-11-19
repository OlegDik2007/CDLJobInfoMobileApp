# CDL Live Road — iOS

Native Swift + SwiftUI shell for CDLJOBINFO Live Road.

## v0.9.0

- Opens https://cdljob.info/mobile-app/ in WKWebView
- Native APNs device-token registration with the CDLJOBINFO backend
- Native notification permission and alert presentation
- Notification tap opens the Alerts tab
- Native GPS permission bridge for the Live Road UI
- Requests Always location access for the optional background road-alert feature
- Uses Apple's low-power significant-change location monitoring instead of continuous GPS
- Posts significant location changes to /api/mobile-device-location
- Debug builds register APNs sandbox tokens; Release/TestFlight builds register production tokens
- Bundle ID: info.cdljob.driverhub
- iOS 16+ / iPhone only

## Apple Developer setup required

Before APNs can deliver notifications:

1. In Xcode -> Signing & Capabilities, choose the Apple Developer Team.
2. Confirm the checked-in `CDLDriverHub.entitlements` is used by both Debug and Release signing and that the Push Notifications capability is enabled for the App ID. Debug requests the development APNs environment and Release requests production; the signed entitlement must match the provisioning profile.
3. Ensure the App ID for info.cdljob.driverhub has Push Notifications enabled.
4. Create an APNs Auth Key in the Apple Developer portal.
5. Configure the backend secrets:
   - APNS_TEAM_ID
   - APNS_KEY_ID
   - APNS_PRIVATE_KEY
   - APNS_BUNDLE_ID=info.cdljob.driverhub

The backend accepts either a PEM APNs private key or a base64-encoded PEM value.

`PrivacyInfo.xcprivacy` declares the required-reason `UserDefaults` API use (`CA92.1`). It does not replace App Store Connect App Privacy labels or the location/push disclosure review.

## Background location model

The app uses startMonitoringSignificantLocationChanges rather than continuous background GPS. This is lower-power and is intended to keep the server's last road-alert location reasonably current while the driver is moving.

The user must grant Always location access for background significant-change updates.

## Run

Open CDLDriverHub.xcodeproj in Xcode, choose the CDLDriverHub target, select your Apple Developer Team and run on a physical iPhone.

## TestFlight

Product -> Archive -> Distribute App -> App Store Connect -> Upload.
