# CDL Hub — Google Play Internal Testing Release Gate

Updated 2026-11-19. **This is preparation, not authorization to publish.**

## Canonical code and package
- **Canonical native client:** `OlegDik2007/CDLJobInfoMobileApp` → `android/`.
- **Backend/server:** `OlegDik2007/Driver_portal`; its legacy `mobile/android/` copy and signed-AAB workflow are not the canonical release source. Do not upload artifacts produced from that old copy.
- **Google Play package:** `info.cdljob.driverhub`. **Check against the existing app entry and upload signing certificate BEFORE first submission**. Package IDs are effectively permanent once published.
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
1. Verify Google Play developer account and the package name `info.cdljob.driverhub`.
2. Name **CDL Hub**, English short/full descriptions, approved 512×512 store icon, 1024×500 feature graphic and real screenshots from a clean test phone (no private GPS, chats or notification tokens). Do not mistake the bundled launcher icon for store-listing assets.
3. Verify public URLs: `https://cdljob.info/privacy-policy`, `https://cdljob.info/delete-data`, and the account deletion option in-app. Existing public pages returned HTTP 200 during the Nov 19 review; test again from Console.
4. Review **Data Safety**, **ads**, **app access**, **target audience**, **content rating**, **account deletion**, and SDK data collection with the exact submitted build. Disclose device IDs/FCM, optional foreground and background road monitoring, optional chat/photos, and data handling accurately.
5. Declare **foreground service type: location** in Play Console. Prepare a test-device demonstration video of the explicit user activation, location permission, ongoing notification, and user-initiated stop. The app uses `FOREGROUND_SERVICE_LOCATION`; it does not declare `ACCESS_BACKGROUND_LOCATION` in this candidate.
6. Test moderation operations for chat and photos, account deletion, and legitimate rights for DOT/511 camera imagery. Do not claim unapproved third-party streams as native CDN assets.
7. Publish first to **Internal Testing**. On an actual Android 14/15/16 phone test map, live camera links, News, Road Hub, Driver Health, GPS permission denial/grant, opt-in location tracking, background/service cancellation and reopen, FCM push, photo chooser, report/block/delete and all 5 locales.
8. Do not proceed to production until internal testing is accepted and the user approves it.

## Release procedure (after prerequisites)
1. Merge reviewed PRs in order, including mobile PR #2 and the release-workflow PR.
2. In the **canonical mobile repository** use Actions → `Build signed CDL Hub Google Play AAB` → Run workflow on **main** and enter `info.cdljob.driverhub`.
3. Verify the `CDLHub-GooglePlay-v12-signed-AAB` artifact and signature confirmation in the action logs.
4. Upload `app-release.aab` manually to the **existing** Google Play Console app's **Internal testing** track. Do not publish to production yet.
5. Install using the Play test link, complete hardware QA and fix any Play pre-launch/test findings.
6. Final operator review of screenshots, store content, app-access instructions and release notes, then decide when to submit production.

## Remaining external blockers
- No verified Play Console account/app or release package identity yet from connected sources.
- GitHub release keystore/Firebase secret presence cannot be confirmed by repository file reads.
- A real-device signed-build test remains required.
- Play's in-console policy declarations, store screenshots and optional approval are not automated here.
