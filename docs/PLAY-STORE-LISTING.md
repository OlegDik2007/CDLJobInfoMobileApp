# CDL Hub — Google Play English listing draft

Prepared 2026-11-19 for the **Internal Testing** candidate. Review against actual shipping features and Google Play Console before use.

## Main store listing

**App name (7/30 chars)**: CDL Hub

**Short description (under 80 chars)**: Road alerts, traffic cameras, weather and essential tools for truck drivers.

**Full description**

CDL Hub helps truck drivers stay informed about conditions on roads across the United States.

Explore available road alerts, closures, construction, weather conditions and truck restrictions on an interactive map. Browse supported DOT/511 traffic-camera images and official camera links, and check road conditions when planning a trip.

**Features**
- Road alerts, incidents, closures and truck restrictions from participating sources
- Camera locations, available snapshots and links to supported official streams
- Road weather and winter-driving information
- Nearby road information with optional GPS
- Optional background monitoring for relevant nearby hazards, with an on/off control
- Trucking resources, jobs and company reviews where available
- Driver news and optional community chat

You can explore much of the app without creating an account or sharing your location. GPS access is optional. Background road monitoring requires separate activation and device permission and may stop if Android ends its foreground service.

Coverage, refresh intervals, data availability and camera access vary by state and provider. Road information can be delayed. CDL Hub is an independent platform, not a government agency. Follow official road signs, avoid interacting with your phone while driving and comply with local driving laws.

For support or privacy questions, contact contact@cdljob.info.

## Suggested internal-test release notes

Initial CDL Hub testing candidate: refreshed native navigation for News and Road Hub, updated Android background-monitoring behavior, and access to current map, weather, camera and trucking information.

## Store setup fields to review

- **Support contact:** contact@cdljob.info
- **Website:** https://cdljob.info
- **Privacy policy:** https://cdljob.info/privacy-policy
- **Data deletion:** https://cdljob.info/delete-data
- **App access:** Core road map is public. Optional account/chat features may require an account; supply review credentials for restricted flows if requested.
- **Ads declaration:** audit actual displayed ad formats and third-party SDK behavior in the release; do not declare no ads without checking website content.
- **Target audience:** trucking professionals/adults; finalize age choices in Console.
- **Foreground service location demonstration:** a clean test device and real notification are needed; avoid disclosing the tester's home address or sensitive route details in screenshots/video.

## Screenshot / graphic requirements

- Use the **previously approved CDL Hub branding**; do not substitute a different icon.
- 512×512 app listing icon and 1024×500 feature graphic for Google Play.
- Real screenshots of Map, alerts, camera directory, weather and news taken from the **signed Internal Testing build**, with personal data and precise coordinates redacted.
- Verify readability and permission prompts on Android 14/15/16.

This file is copy for review, not a statement that the Google Play listing is already submitted.
