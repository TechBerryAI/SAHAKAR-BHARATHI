# Android

Jetpack Compose + Material 3 client. The Gradle root is **this folder** (`android/`);
the wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) lives here. The application
module is `:app`. Application ID / namespace: `com.example.sahakarbharati`.

## Requirements

| Tool | Version |
|---|---|
| JDK | 17+ (CI uses Temurin 21; JDK 25 also verified) |
| Gradle | 9.6.0 via the wrapper |
| Android Gradle Plugin | 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01 |
| SDK | compileSdk / targetSdk 37, minSdk 26 |

Android Studio: open the `android/` folder. Command line: set `ANDROID_HOME` or
`sdk.dir` in `android/local.properties` (gitignored).

## Build and test

```bash
cd android
./gradlew :app:testDebugUnitTest          # JVM unit tests
./gradlew :app:assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # needs a running emulator/device
```

Windows: use `gradlew.bat` with the same tasks.

WSL note: Linux Gradle cannot use a Windows SDK's `build-tools` (it looks for Linux
`aapt`). Use Android Studio / `gradlew.bat` on Windows, or a Linux SDK in WSL.

## API base URL

| Build | Source (first match wins) | Default |
|---|---|---|
| debug | `-Psahakar.debugApiBaseUrl`, `SAHAKAR_DEBUG_API_BASE_URL`, `debug.api.base.url` in `local.properties` | `http://10.0.2.2:8000/` (emulator → host) |
| release | `-Psahakar.apiBaseUrl`, `SAHAKAR_API_BASE_URL`, `api.base.url` in `local.properties` | **none: the build fails** |

Release builds run `validateReleaseConfig` before anything is compiled and fail if
the URL is missing, not `https://`, a local address (`localhost`, `127.*`,
`10.0.2.2`), a placeholder host (`*.invalid`, `*.example`, `*.test`), lacks the
trailing `/`, or has a query/fragment. A release APK can no longer silently point
at `https://api.invalid/`.

Production value: `https://sahakar-bharathi-api.onrender.com/`.

Debug cleartext is allowed only to `10.0.2.2`, `localhost` and `127.0.0.1`
(`src/debug/res/xml/network_security_config.xml`); release allows no cleartext.
For a physical phone, use `adb reverse tcp:8000 tcp:8000` with
`-Psahakar.debugApiBaseUrl=http://127.0.0.1:8000/`.

## Release build and signing

Unsigned (verification only):

```bash
./gradlew :app:assembleRelease :app:bundleRelease -Psahakar.apiBaseUrl=https://sahakar-bharathi-api.onrender.com/
# app/build/outputs/apk/release/app-release-unsigned.apk
# app/build/outputs/bundle/release/app-release.aab
```

Signed: provide **all four** values (a partial set fails the build), either as
environment variables or in gitignored `android/keystore.properties`
(template: `keystore.properties.example`):

| Env var | keystore.properties |
|---|---|
| `SAHAKAR_KEYSTORE_FILE` | `storeFile` |
| `SAHAKAR_KEYSTORE_PASSWORD` | `storePassword` |
| `SAHAKAR_KEY_ALIAS` | `keyAlias` |
| `SAHAKAR_KEY_PASSWORD` | `keyPassword` |

```bash
SAHAKAR_API_BASE_URL=https://sahakar-bharathi-api.onrender.com/ \
SAHAKAR_VERSION_CODE=2 SAHAKAR_VERSION_NAME=1.0.1 \
SAHAKAR_KEYSTORE_FILE=/secure/path/upload.jks SAHAKAR_KEYSTORE_PASSWORD=… SAHAKAR_KEY_ALIAS=… SAHAKAR_KEY_PASSWORD=… \
./gradlew :app:bundleRelease
# app/build/outputs/bundle/release/app-release.aab (signed)
```

Create an upload key once (store it offline, never in the repo):

```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

Version: `SAHAKAR_VERSION_CODE` / `-Psahakar.versionCode` (default 1) and
`SAHAKAR_VERSION_NAME` / `-Psahakar.versionName` (default 1.0). Every Play upload
needs a higher version code. CI's manual signed job uses the workflow run number.

R8 is enabled for release with optimisation scoped to `androidx`, `kotlin` and
`kotlinx`; app classes (including the Gson DTOs) are not renamed.

## Behaviour notes

- Session token and cached profile live in `EncryptedSharedPreferences`, excluded
  from cloud backup and device transfer.
- Any 401 on an authenticated call (expired, revoked, account disabled) clears the
  stored session and returns to Sign in (`SessionExpiryInterceptor`).
- Logout clears local state first, then asks the server to revoke the session.
- The profile photo is stored on the device only and is labelled as such; there is
  no upload API.
- Tracking, QR attendance/pass, benefits and notifications are not connected to any
  backend. Those screens say so; their controls are disabled or removed.
- Android does not edit profile details; the web app does. Both read `GET /api/v1/me`.
