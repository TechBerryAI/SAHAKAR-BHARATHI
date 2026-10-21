# Sahakar Bharati

Native Android application and Python API for the Sahakar Bharati Rath Yatra digital platform. The approved UI is Jetpack Compose and Material 3.

## Layout

- `frontend/app/` — Android frontend (Kotlin, Compose). Gradle still addresses it as `:app`.
- `backend/` — FastAPI + PostgreSQL service for registration, sessions, and profile.
- `tools/` — optional local helpers, such as regenerating density-specific logo drawables.
- `gradlew` / `gradlew.bat` — Gradle wrapper. Keep these at the repository root.

The Android frontend must never connect directly to the database. All persisted participant data goes through the API.

Local registration is not production readiness. Tracking, QR attendance, benefits, and notifications are not operational.

## Backend

See [`backend/README.md`](backend/README.md) for environment variables, migrations, Render commands, and session design.

```bash
# PostgreSQL must already be running locally. Create databases sahakar and sahakar_test.
cd backend
cp .env.example .env
python3.12 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
alembic upgrade head
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Android emulator debug builds use `http://10.0.2.2:8000/`. Release builds require HTTPS via `api.base.url` in gitignored `local.properties` after Render assigns a hostname. Do not commit that URL.

The React app in `web/` is hosted on Firebase Hosting and calls this API with `VITE_API_BASE_URL`. Registration and sign-in use the same PIN + login ID or mobile contract as Android. Firebase Auth and Firestore are no longer the application data plane.

**Render (not verified from this repository):** root directory `backend`, build `pip install .`, start `uvicorn app.main:app --host 0.0.0.0 --port $PORT --proxy-headers --forwarded-allow-ips='*'`. Required env: `ENVIRONMENT=production`, `DATABASE_URL`. See [`render.yaml`](render.yaml).

## Android build

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
