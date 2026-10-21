# Sahakar Bharati

Native Android application for the Sahakar Bharati Rath Yatra digital platform. The approved UI is Jetpack Compose and Material 3.

## Layout

- `frontend/app/` — Android frontend (Kotlin, Compose). This is the only application module. Gradle still addresses it as `:app`.
- `backend/` — reserved for a future server. No backend is implemented yet.
- `tools/` — optional local helpers, such as regenerating density-specific logo drawables.
- `gradlew` / `gradlew.bat` — Gradle wrapper. Keep these at the repository root.

The Android frontend must never connect directly to a production database. A future server will own database access.

## Build

From the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows:

```bat
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

`local.properties` is machine-specific and is not committed. Android Studio writes `sdk.dir` there. Do not check that file in.

### WSL note

This checkout often lives on a Windows drive (`D:\`) while the Android SDK is a Windows install (`C:\Users\...\Android\Sdk`). Linux Gradle cannot use the Windows `aapt.exe` build-tools. Prefer Android Studio or `gradlew.bat` on Windows for local builds. If you must build from WSL, point `ANDROID_HOME` at a Linux SDK; do not overwrite a teammate’s `local.properties`.

## Application identity

These values are intentional and should not change without an explicit decision:

- application ID / namespace: `com.example.sahakarbharati`
- frontend package layout: `ui/theme`, `ui/navigation`, `core/ui`, `core/copy`, `data`, and `features/*`
