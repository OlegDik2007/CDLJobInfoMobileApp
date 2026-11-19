# CDL Hub — Google Play Internal Testing Release Gate

Updated 2026-11-19. **This is preparation, not authorization to publish.**

## Canonical code and package
- **Canonical native client:** `OlegDik2007/CDLJobInfoMobileApp` → `android/`.
- **Backend/server:** `OlegDik2007/Driver_portal`; its legacy `mobile/android/` copy and signed-AAB workflow are not the canonical release source. Do not upload artifacts produced from that old copy.
- **Google Play package:** `com.avidetechnologies.cdlroadhub`, matching the user's existing **CDL Road Hub** Draft app (Play Console screenshot, 2026-11-19). The publicly displayed name should become **CDL Hub**. **Upload signing certificate and existing bundle history remain unverified**. Package IDs are fixed after an artifact upload.
- **Candidate:** Android `1.0.0-rc1`, `versionCode 12`, target API 36, min API 26.
- Current development PR `#2` contains unmerged fixes; merge and finish device QA before the signed build is allowed from `main`.

## CI workflow division
- `build-android.yml`: debug APK for developers, never upload to Play.
- `build-google-play-unsigned.yml`: PR compile/release preflight for an unsigned **NON-PUBLISHABLE** AAB, plus a separately built **disposable test-key-signed AAB that is not uploaded**. It checks the package ID, build version, cleartext traffic ban, nonbackup config, GPS service type, and lack of `ACCESS_BACKGROUND_LOCATION`.
- `build-google-play-signed.yml`: trusted `main` only, manually dispatched. It checks the package string typed by the operator, required secrets, validates the upload keystore, builds the signed AAB, and verifies the signature. **It does not submit anything to Google Play.**

## Credentials and signing
**Never paste signing secrets in a chat, issue, source file, screenshot or PR.** Use GitHub Actions repository secrets on the canonical mobile repo (not just the backend repo):

| Secret | Purpose |
|---|---|
| `FIREBASE_PROJECT_ID` | Firebase client project identifier |
| `FIREBASE_APPLICATION_ID` | Firebase Android app ID |
| `FIREBASE_API_KEY` | Firebase client API key |
| `FIREBASE_GCM_SENDER_ID` | FCM sender ID |
| `CDL_PLAY_UPLOAD_KEYSTORE_BASE64` | Base64-encoded complete upload-key file |
| `CDL_PLAY_UPLOAD_STORE_PASSWORD` | Upload keystore password |
| `CDL_PLAY_UPLOAD_KEY_ALIAS` | Alias registered in upload keystore |
| `CDL_PLAY_UPLOAD_KEY_PASSWORD` | Key password |

If an upload key already exists in Play Console, **reuse it**. Creating a new key without following Play's upload-key reset process may prevent app updates. Back up any new upload key offline. Firebase **server** credentials stay exclusively in the backend.

The signed workflow maps the `CDL_PLAY_*` secrets to Gradle's existing `ANDROID_KEYSTORE_*` environment configuration and verifies the `jarsigner -verify` result and checks the AAB signer's SHA-256 certificate fingerprint against the intended upload keystore. An unsigned or debug bundle is **not** a store candidate.

## Store listing and policy gates
1. Existing Play Console application: **CDL Road Hub (Draft)**, package `com.avidetechnologies.cdlroadhub`. Verify **App integrity** and any prior AAB before choosing an upload key; do not create a second app.
2. Complete the Play Console dashboard's remaining **Set up your store listing** task. Set public title **CDL Hub**, English short/full descriptions, approved 512×512 store icon, 1024×500 feature graphic and real screenshots from a clean test phone (no private GPS, chats or notification tokens). Do not mistake the bundled launcher icon for store-listing assets.
3. Verify public URLs: `https://cdljob.info/privacy-policy`, `https://cdljob.info/delete-data`, and the account deletion option in-app. Existing public pages returned HTTP 200 during the Nov 19 review; test again from Console.
4. Review **Data Safety**, **ads**, **app access**, **target audience**, **content rating**, **account deletion**, and SDK data collection with the exact submitted build. Disclose device IDs/FCM, optional foreground and background road monitoring, optional chat/photos, and data handling accurately.
5. Declare **foreground service type: location** in Play Console. Prepare a test-device demonstration video of the explicit user activation, location permission, ongoing notification, and user-initiated stop. The app uses `FOREGROUND_SERVICE_LOCATION`; it does not declare `ACCESS_BACKGROUND_LOCATION` in this candidate.
6. Test moderation operations for chat and photos, account deletion, and legitimate rights for DOT/511 camera imagery. Do not claim unapproved third-party streams as native CDN assets.
7. Publish first to **Internal Testing**. On an actual Android 14/15/16 phone test map, live camera links, News, Road Hub, Driver Health, GPS permission denial/grant, opt-in location tracking, background/service cancellation and reopen, FCM push, photo chooser, report/block/delete and all 5 locales.
8. Do not proceed to production until internal testing is accepted and the user approves it.

## Release procedure (after prerequisites)
1. Merge reviewed PRs in order, including mobile PR #2 and the release-workflow PR.
2. In the **canonical mobile repository** use Actions → `Build signed CDL Hub Google Play AAB` → Run workflow on **main** and enter `com.avidetechnologies.cdlroadhub`.
3. Verify the `CDLHub-GooglePlay-v12-signed-AAB` artifact and signature confirmation in the action logs.
4. Upload `app-release.aab` manually to the **existing** Google Play Console app's **Internal testing** track. Do not publish to production yet.
5. Install using the Play test link, complete hardware QA and fix any Play pre-launch/test findings.
6. Final operator review of screenshots, store content, app-access instructions and release notes, then decide when to submit production.

## Remaining external blockers
- Play Console organization account and existing Draft application are visible in user-provided screenshots; the dashboard shows **10/11 app setup tasks complete**, with **Set up your store listing** remaining. This does not confirm app signing setup or existing bundle uploads.
- GitHub release keystore/Firebase secret presence cannot be confirmed by repository file reads. **Firebase must register/configure an Android client for `com.avidetechnologies.cdlroadhub`**; the existing Firebase App ID may refer to the old `info.cdljob.driverhub` package and cannot be reused blindly.
- A real-device signed-build test remains required.
- Play's in-console policy declarations, store screenshots and optional approval are not automated here.
