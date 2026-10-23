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
| [`scripts/check-secrets.sh`](scripts/check-secrets.sh) | Secret scanner used by CI and the optional pre-commit hook |
| [`render.yaml`](render.yaml) | Render Blueprint (service `sahakar-bharati-api`) |
| [`.github/workflows/`](.github/workflows/) | Backend, web, Android CI; Firebase Hosting deploy |

## Quick start

**Backend** (Python 3.12, local PostgreSQL with databases `sahakar` and `sahakar_test`):

```bash
cd backend
cp .env.example .env          # local values only; never point this at production by accident
python3.12 -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
alembic upgrade head
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
pytest && ruff check
```

**Web** (Node 22+):

```bash
cd web
cp .env.example .env.local    # VITE_API_BASE_URL=http://127.0.0.1:8000
npm ci && npm run dev
npm run lint && npm run typecheck && npm test
```

**Android** (JDK 17+, Android SDK 37):

```bash
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug        # gradlew.bat on Windows
./gradlew :app:assembleRelease -Psahakar.apiBaseUrl=https://sahakar-bharathi-api.onrender.com/
```

Optional: block accidental secret commits locally with `git config core.hooksPath .githooks`.

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
