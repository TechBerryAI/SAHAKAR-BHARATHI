# Sahakar Bharati

Participant platform for the Sahakar Bharati Rath Yatra: a native Android app, a
React web app / PWA, and a FastAPI backend on PostgreSQL (Supabase).

Status and evidence: [`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md).
Registration, sign-in, profile and sessions work end to end. Tracking, QR
attendance, benefits and notifications have no backend and are shown as unavailable.

## Architecture

```
Android (Compose, Retrofit) ─┐
                             ├─ HTTPS, Authorization: Bearer <opaque token> ─▶ FastAPI (Render) ─▶ PostgreSQL (Supabase)
Web / PWA (React, Vite) ─────┘                                                     │
                                                                                    └─▶ Textbee (OTP SMS)
```

- Clients never talk to the database. Supabase is used only as managed PostgreSQL;
  Supabase Auth, PostgREST and Firebase Auth/Firestore are not used.
- Sessions are opaque bearer tokens (SHA-256 stored server-side), PINs are Argon2id.
- Web is static on Firebase Hosting; the backend is a single Render web service.

## Repository

| Path | Contents |
|---|---|
| [`android/`](android/) | Gradle root with wrapper, module `:app` (Kotlin, Jetpack Compose) |
| [`backend/`](backend/) | FastAPI app, Alembic migrations, pytest suite (Render root dir) |
| [`web/`](web/) | Vite + React + TypeScript app, PWA, Firebase Hosting config |
| [`infra/supabase/`](infra/supabase/) | `schema.sql` snapshot, optional `runtime_role.sql` |
| [`docs/`](docs/) | Database, deployment, readiness report |
| [`scripts/`](scripts/) | Launchers used by `make start` and `make android`, plus [`check-secrets.sh`](scripts/check-secrets.sh) |
| [`render.yaml`](render.yaml) | Render Blueprint (service `sahakar-bharati-api`) |
| [`.github/workflows/`](.github/workflows/) | Backend, web, Android CI; Firebase Hosting deploy |

## Commands

Run every command below from the repository root. `make` prints this list again.

You need Python 3.12, Node 22+, and PostgreSQL. The Android app also needs JDK 17+ and Android SDK 37.

### Once, before the first start

Create the local databases when `backend/.env` will use this machine's PostgreSQL. A hosted `DATABASE_URL` does not need these databases or Docker.

```bash
createuser sahakar
createdb -O sahakar sahakar
createdb -O sahakar sahakar_test
```

Copy the env files. Edit them only when the local database user, password, or host differs. Keep production credentials out of these files.

```bash
cp backend/.env.example backend/.env
cp web/.env.example web/.env.local
```

Then install dependencies and apply migrations:

```bash
make install
make migrate
make hooks
```

`make hooks` is optional. It makes git run the secret check before each commit.

### Which command to run

| When | Command | What you get |
|---|---|---|
| You want the API and the web app | `make start` | Migrations, API at http://127.0.0.1:8000, web app at http://localhost:5174. Ctrl+C stops both. |
| You want the Android app on an emulator or USB phone | `make android` | Starts the local API when it is down, builds the debug app, installs it, and opens it. On WSL this uses the Windows SDK. |
| The phone should use the live API | `make android-prod` | Same install and open, against https://sahakar-bharathi-api.onrender.com/. Sign in only with a test account. |
| You need the Android app's logs | `make android-logs` | Opens the app, then streams its logs until Ctrl+C. |
| You need one particular emulator | `make android ANDROID_ARGS="--avd <name>"` | Starts that emulator when nothing is connected. Names come from `emulator -list-avds`. |
| Several phones are connected | `make android ANDROID_ARGS="--device <serial>"` | Uses that device. Serials come from `adb devices`. |
| Install fails because of an old debug signature | `make android ANDROID_ARGS="--reinstall"` | Uninstalls first. This clears the app's data on that device. |
| The web app is already running and you are working on the API | `make api` | API only, with reload, on port 8000. |
| The API is already running and you are working on the web app | `make web` | Web app only, on http://localhost:5174. |
| You pulled new database migrations | `make migrate` | Applies Alembic migrations to the database in `backend/.env`. |
| You want to know the whole project still works | `make test` | Backend, web, Android unit tests, and script tests. Stops at the first failure. Needs a PostgreSQL database whose name contains `test`. |
| You changed the backend | `make test-backend` | Backend pytest. One group: `make test-backend ARGS="-k attendance"`. |
| You changed the web app | `make test-web` | Web Vitest suite. |
| You changed the Android app | `make test-android` | Android JVM unit tests on this computer. |
| You changed files in `scripts/` | `make test-scripts` | Node tests for those scripts. |
| You want the style and type checks | `make lint` | Backend ruff, web ESLint, and the web typecheck. `make lint-backend` and `make lint-web` run one side. |
| You want to look for secrets in tracked files | `make secrets` | Prints the file and the rule name. It does not print the secret. |

Two terminals cover a normal day: `make start` in one, `make android` in the other. Run `make test` before you commit.

## Configuration

Every variable, with placeholders only:

- Backend: [`backend/.env.example`](backend/.env.example) and [`backend/README.md`](backend/README.md#environment-variables)
- Web: [`web/.env.example`](web/.env.example) (`VITE_API_BASE_URL`)
- Android: [`android/README.md`](android/README.md) (`SAHAKAR_API_BASE_URL`, signing variables) and [`android/keystore.properties.example`](android/keystore.properties.example)
- Deploy secrets (Render, GitHub): [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)

Secrets live only in gitignored files (`backend/.env`, `web/.env.local`,
`local.properties`, `keystore.properties`) or in the Render / GitHub dashboards.

## Further reading

- [`docs/DATABASE.md`](docs/DATABASE.md): init, migrations, roles, Supabase settings, backups
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md): CI/CD, branch flow, Render, Firebase, Android release, rollback
- [`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md): findings, changes, test evidence, launch checklist

## Identity

Do not change without an explicit decision: application ID / namespace
`com.example.sahakarbharati`; Render service `sahakar-bharati-api`; Firebase project
`sahakarbharati-e3b04`; Android package layout `ui/theme`, `ui/navigation`,
`core/ui`, `core/copy`, `data`, `features/*`.
