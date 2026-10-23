# Sahakar Bharati backend

Python 3.12 FastAPI service for participant registration, opaque session authentication, and profile APIs. PostgreSQL is the system of record. The Android app must call this API; it must never connect to the database.

This service can be deployed on Render against Supabase PostgreSQL. A successful local test run is not a live production smoke test, a compliance claim, or a GitHub-to-Render autodeploy guarantee.

## Stack

- FastAPI, Pydantic v2, Uvicorn
- SQLAlchemy 2.x with psycopg
- Alembic migrations
- Argon2id PIN hashing via pwdlib
- pytest against PostgreSQL

## Local startup

Install PostgreSQL on the host, then create the application and test databases:

```bash
createuser sahakar
createdb -O sahakar sahakar
createdb -O sahakar sahakar_test
```

From the repository root:

```bash
cd backend
cp .env.example .env
# Edit .env if the local Postgres user, password, host, or database names differ.
python3.12 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
alembic upgrade head
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Run tests against the local test database:

```bash
cd backend
pytest
ruff check
```

Tests use `TEST_DATABASE_URL` or `POSTGRES_TEST_DB` only. They never fall back to `DATABASE_URL`, and they pin `MIGRATION_DATABASE_URL` to the test database. The database name must contain `test`.

`tests/test_database_schema.py` creates throwaway databases on the same server to check migrations from scratch, upgrades from the previous release, `infra/supabase/schema.sql` parity, the Supabase `anon`/`authenticated` lockout and the optional runtime role. It needs a role that can create databases and roles (CI's Postgres user can); otherwise those tests skip.

Measure per-endpoint latency and database round-trips against the local test database. The script injects network delay through a local proxy; it sends no traffic to Render or Supabase and no SMS:

```bash
cd backend
python scripts/latency_bench.py --rtt-ms 0
python scripts/latency_bench.py --rtt-ms 270
```

`tests/test_query_budget.py` fails when an endpoint exceeds its SQL statement budget or runs SQL after its commit.

Android emulator debug builds use `http://10.0.2.2:8000/` by default. Release builds take the HTTPS URL from `SAHAKAR_API_BASE_URL` / `-Psahakar.apiBaseUrl` / `api.base.url` and fail without it (see `android/README.md`).

## Environment variables

| Variable | Purpose |
|---|---|
| `ENVIRONMENT` | `development`, `test` or `production`. Anything else fails at startup. Production fails fast without a remote TLS `DATABASE_URL` and rejects non-https CORS origins. |
| `POSTGRES_HOST` / `POSTGRES_PORT` | Local PostgreSQL listener |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Database role. Password may be empty for local trust or peer auth. |
| `POSTGRES_DB` / `POSTGRES_TEST_DB` | Application and pytest databases |
| `DATABASE_URL` | Required in production. Full SQLAlchemy or libpq URL. `postgres://` and `postgresql://` are rewritten to `postgresql+psycopg://`. |
| `TEST_DATABASE_URL` | Pytest URL. Leave blank to use `POSTGRES_TEST_DB`. Must name a database containing `test`. |
| `MIGRATION_DATABASE_URL` | Optional owner credential used only by Alembic, so the API can run as a least-privilege role. |
| `SESSION_TTL_SECONDS` | Opaque session lifetime (default 7 days) |
| `LOGIN_RATE_LIMIT` / `LOGIN_RATE_WINDOW_SECONDS` | Login throttle per client IP + identifier |
| `REGISTER_RATE_LIMIT` / `REGISTER_RATE_WINDOW_SECONDS` | Registration throttle per client IP |
| `RATE_LIMIT_BACKEND` | `memory` (default, single instance only) or `postgres` (shared atomic counters, needs migration 0006) |
| `LOGIN_LOCKOUT_FAILURES` / `LOGIN_LOCKOUT_SECONDS` | Database-backed lockout after failed PINs |
| `MAX_REQUEST_BYTES` | JSON body size limit |
| `CORS_ORIGINS` | Comma-separated browser origins. Production must include `https://sahakarbharati-e3b04.web.app`; `*`, `http://` and local origins are rejected in production. Credentials are not allowed (bearer tokens only). |
| `TEXTBEE_API_KEY` / `TEXTBEE_DEVICE_ID` / `TEXTBEE_BASE_URL` | OTP SMS gateway. Registration cannot complete without a key. |
| `OTP_*` | OTP lifetimes, attempts and throttles (unchanged; see `.env.example`). |
| `APP_HOST` / `APP_PORT` | Local documentation only. They do not bind the production server. |

Do not commit real secrets, production credentials, or personal data. The backend does not need the Supabase service-role key to connect to PostgreSQL.

## Authentication

Sessions are opaque bearer tokens, not JWTs.

1. `POST /api/v1/sessions` verifies the Argon2id PIN hash and inserts a `sessions` row.
2. The raw token is returned once. Only the SHA-256 hash is stored.
3. Authenticated requests send `Authorization: Bearer <token>`.
4. `DELETE /api/v1/sessions/current` sets `revoked_at`. Expired or revoked tokens are `401`.
5. The current user is loaded from the session row. Client-supplied user IDs are ignored.
6. Users with `status` other than `active` cannot log in or use an existing session.

PINs are 4–6 digits and are never stored or logged. Login identifiers are canonicalized so `9876543210` and `+919876543210` share the same lockout bucket.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET`, `HEAD` | `/` | Service status JSON. No database access. |
| `POST` | `/api/v1/registrations` | Creates user + registration. Returns server `registration_id` / `login_id`. |
| `POST` | `/api/v1/sessions` | Login with login ID or mobile + PIN. |
| `DELETE` | `/api/v1/sessions/current` | Logout / revoke. |
| `GET` | `/api/v1/me` | Authenticated profile. |
| `PATCH` | `/api/v1/me` | Authenticated profile update. |
| `GET` | `/health/live` | Process liveness. Always 200 if the app is running. |
| `GET` | `/health/ready` | Database readiness. 503 if `SELECT 1` fails. |
| `GET` | `/health` | Same as `/health/ready`. Use this as the Render health check. |

All `/api/*` responses carry `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`. Bodies over `MAX_REQUEST_BYTES` are rejected with 413, including chunked bodies without `Content-Length`.

OpenAPI (`/docs`, `/redoc`, `/openapi.json`) is disabled when `ENVIRONMENT=production`.

## Render

Confirm the GitHub repository is connected in the Render dashboard. Automatic deploys from `production` or `development` are dashboard settings; this repository cannot verify them.

**Root directory:** `backend`

**Build command:** `pip install .`

**Start command:**

```bash
uvicorn app.main:app --host 0.0.0.0 --port $PORT --proxy-headers --forwarded-allow-ips='*'
```

**Required environment variables**

- `ENVIRONMENT=production`
- `DATABASE_URL` — Supabase PostgreSQL URI. Session mode (port 5432) or transaction pooler (port 6543) are both accepted. Transaction pooler disables prepared statements.

**Optional:** `CORS_ORIGINS`, `SESSION_TTL_SECONDS`, and the rate-limit / lockout knobs above.

**Region:** run the service in the region closest to the Supabase project. Every SQL statement is a network round-trip, and a login needs about seven. On 2026-10-22 production `/health` (five round-trips) took about 1.4 s longer than `/health/live`, roughly 270 ms per round-trip to the `ap-south-1` database. Render cannot move an existing service between regions; create a new service in the target region and switch clients over.

A Blueprint file lives at the repository root as [`render.yaml`](../render.yaml). Secrets are `sync: false` and must be entered in the dashboard.

### Migrations

Full procedure: [`docs/DATABASE.md`](../docs/DATABASE.md). Do not run Alembic from every web worker at startup.

1. If the Render plan has a pre-deploy command, set it to `alembic upgrade head`.
2. Otherwise run `MIGRATION_DATABASE_URL=… alembic upgrade head` from a trusted machine or a one-off Render shell before sending traffic to a new revision.
3. `infra/supabase/schema.sql` is only for an empty database; it stamps `20261023_0006` itself and refuses to run if `alembic_version` already has a row.
4. Never run `alembic downgrade`, `alembic stamp` against the wrong revision, or a database reset on production.

Migration `20261023_0005` enables RLS and revokes the Supabase `anon`/`authenticated` roles on every app table. Databases created with Alembic before it exposed `pin_hash` and `token_hash` to the public anon key; apply it promptly. The backend does not use Supabase Auth.

## Client compatibility

- **Android** uses these routes, snake_case fields, and `Authorization: Bearer`. Keep PIN 4–6 digits. Release builds require `SAHAKAR_API_BASE_URL=https://sahakar-bharathi-api.onrender.com/` (or `api.base.url` in gitignored `local.properties`). Debug defaults to `http://10.0.2.2:8000/`.
- **React (`web/`)** calls these same routes with `Authorization: Bearer`. Local development uses `VITE_API_BASE_URL=http://127.0.0.1:8000`. Production Hosting builds use `https://sahakar-bharathi-api.onrender.com`. Production `CORS_ORIGINS` must include `https://sahakarbharati-e3b04.web.app` (and the `firebaseapp.com` alias). Firebase Hosting stays; Firebase Auth/Firestore are not the data plane.

## Remaining risks

- With `RATE_LIMIT_BACKEND=memory` (default) throttles are per process: run one instance with one worker, or switch to `postgres` before scaling out.
- `--forwarded-allow-ips='*'` lets a client spoof `X-Forwarded-For`, which weakens per-IP throttles. The per-identifier database lockout still applies.
- A 4–6 digit PIN is a weak authenticator. There is no PIN change or PIN-reset flow.
- Expired sessions, OTP challenges and audit rows are not pruned automatically.
- Live Supabase connectivity, Render dashboard autodeploy, and a production smoke test are owner actions after secrets are set.

## Outstanding product work

- No tracking, QR attendance, benefits, or notification APIs
- No photo upload
- React Hosting deploy needs the GitHub secret `FIREBASE_SERVICE_ACCOUNT_SAHAKARBHARATI_E3B04` (owner to confirm it exists)
