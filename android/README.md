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

## Run on an emulator or phone

From the repository root:

```bash
node start-android.js
```

It finds the Android SDK (`ANDROID_HOME`, `local.properties`, or the default install
folder) and a JDK (`JAVA_HOME`, else Android Studio's bundled `jbr`). From WSL it also
accepts a Windows `sdk.dir` / `ANDROID_HOME` (`C:\Users\…\Android\Sdk`), uses `adb.exe`,
and builds with `gradlew.bat` via `cmd.exe` — Linux Gradle cannot use Windows `aapt.exe`.
It uses a connected device, or starts the first emulator, then forwards the device's port
8000 to this computer, builds the debug app against `http://127.0.0.1:8000/`, installs it,
and opens it. Start the API first with `node start.js`. Without it the app opens but cannot
sign in.

| Option | Effect |
|---|---|
| `--prod` | Use the live API (`https://sahakar-bharathi-api.onrender.com/`). Sign in only with a test account. |
| `--avd <name>` | Emulator to start when nothing is connected (`emulator -list-avds`). |
| `--device <serial>` | Device to use when several are connected (`adb devices`). |
| `--logs` | Stream the app's log output after opening it (Ctrl+C stops the logs, not the app). |
| `--reinstall` | Uninstall first. Needed only when an older build signed with a different debug key is installed; **clears the app's data on that device**. |

A phone needs USB debugging turned on (Settings → Developer options). The emulator keeps
running after the script finishes; run the command again after code changes.

## Build and test

```bash
cd android
./gradlew :app:testDebugUnitTest          # JVM unit tests
./gradlew :app:assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # needs a running emulator/device
```

Windows: use `gradlew.bat` with the same tasks.

WSL: `node start-android.js` from the repository root uses the Windows SDK and
`gradlew.bat` automatically. Do not run `./gradlew` against a Windows SDK — Linux
Gradle looks for `aapt`, not `aapt.exe`. A Linux SDK in WSL still uses `./gradlew`.

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
- API cold start (`data/remote/ColdStart.kt`): the host can take most of a minute to
  start after being idle. Each attempt may wait 75 s (call limit 180 s). GET/HEAD are
  retried after a network failure, timeout or 502/503/504 (backoff 2 s, 5 s); sign-in,
  registration, profile updates and logout are sent once and never retried. Any request
  pending over 4 s shows a "server is starting up" banner on every screen (English and
  Hindi text; other languages show the English base, like other untranslated strings).
- The profile photo is stored on the device only and is labelled as such; there is
  no upload API.
- Tracking, QR attendance/pass, benefits and notifications are not connected to any
  backend. Those screens say so; their controls are disabled or removed.
- Android does not edit profile details; the web app does. Both read `GET /api/v1/me`.
