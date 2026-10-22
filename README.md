# Sahakar Bharati

Native Android, React web, and FastAPI for the Sahakar Bharati Rath Yatra platform.

Clients never talk to the database. Registration, sign-in, and profile go through the API. Local success is not production readiness. Tracking, QR attendance, benefits, and notifications are not operational.

## Workspace

| Section | Path | Role |
|---|---|---|
| Android | [`android/`](android/) | Jetpack Compose + Material 3. Gradle module `:app`. |
| API | [`backend/`](backend/) | FastAPI + PostgreSQL. Render service root. |
| Web | [`web/`](web/) | Vite + React. Firebase Hosting. |
| Infra | [`infra/`](infra/) | Supabase schema snapshot and optional local helpers. |
| Deploy | [`render.yaml`](render.yaml) | Render Blueprint. Service name stays `sahakar-bharati-api`. |
| CI | [`.github/workflows/`](.github/workflows/) | Backend tests and Firebase Hosting deploy. |

Generated caches (`.gradle`, `node_modules`, `.venv`, `dist`) stay out of the explorer via [`.vscode/settings.json`](.vscode/settings.json). Secrets stay in gitignored `.env` and `local.properties`.

## API

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

**Render:** public URL confirmed from the dashboard as `https://sahakar-bharathi-api.onrender.com`. Root directory `backend`, build `pip install .`, start `uvicorn app.main:app --host 0.0.0.0 --port $PORT --proxy-headers --forwarded-allow-ips='*'`. Required env: `ENVIRONMENT=production`, `DATABASE_URL`. Live `/health` is not claimed from this repository. Do not rename the existing Render service.

## Android

Open the `android/` folder in Android Studio. The project root for Gradle is `android/`, not the repository root. `local.properties` can live in `android/` or the repository root; it is not committed.

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows:

```bat
cd android
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

Emulator debug builds use `http://10.0.2.2:8000/`. Release builds require `api.base.url=https://sahakar-bharathi-api.onrender.com/` in gitignored `local.properties`.

### WSL note

This checkout often lives on a Windows drive (`D:\`) while the Android SDK is a Windows install (`C:\Users\...\Android\Sdk`). Linux Gradle cannot use the Windows `aapt.exe` build-tools. Prefer Android Studio or `gradlew.bat` on Windows. If you must build from WSL, point `ANDROID_HOME` at a Linux SDK; do not overwrite a teammate’s `local.properties`.

## Web

See [`web/README.md`](web/README.md). Local development uses `VITE_API_BASE_URL=http://127.0.0.1:8000`. Production Hosting builds set `https://sahakar-bharathi-api.onrender.com` in GitHub Actions. Firebase Auth and Firestore are not the application data plane.

## Identity

Do not change these without an explicit decision:

- Application ID / namespace: `com.example.sahakarbharati`
- Android package layout: `ui/theme`, `ui/navigation`, `core/ui`, `core/copy`, `data`, and `features/*`
