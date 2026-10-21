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

Tests use `TEST_DATABASE_URL` or `POSTGRES_TEST_DB` only. They never fall back to `DATABASE_URL`.

Android emulator debug builds should use `http://10.0.2.2:8000/`. A physical device needs the development machine LAN address. Production builds must use HTTPS via `api.base.url` in `local.properties`. Do not invent a hostname until Render assigns one.

## Environment variables

| Variable | Purpose |
|---|---|
| `ENVIRONMENT` | `development` or `production`. Production fails fast without a remote `DATABASE_URL`. |
| `POSTGRES_HOST` / `POSTGRES_PORT` | Local PostgreSQL listener |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Database role. Password may be empty for local trust or peer auth. |
| `POSTGRES_DB` / `POSTGRES_TEST_DB` | Application and pytest databases |
| `DATABASE_URL` | Required in production. Full SQLAlchemy or libpq URL. `postgres://` and `postgresql://` are rewritten to `postgresql+psycopg://`. |
| `TEST_DATABASE_URL` | Pytest URL. Leave blank to use `POSTGRES_TEST_DB`. Must name a database containing `test`. |
| `SESSION_TTL_SECONDS` | Opaque session lifetime (default 7 days) |
| `LOGIN_RATE_LIMIT` / `LOGIN_RATE_WINDOW_SECONDS` | In-process login throttle |
| `REGISTER_RATE_LIMIT` / `REGISTER_RATE_WINDOW_SECONDS` | In-process registration throttle |
| `LOGIN_LOCKOUT_FAILURES` / `LOGIN_LOCKOUT_SECONDS` | Database-backed lockout after failed PINs |
| `MAX_REQUEST_BYTES` | JSON body size limit |
| `CORS_ORIGINS` | Comma-separated browser origins. Production must include `https://sahakarbharati-e3b04.web.app`. `*` is rejected in production. |
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
| `POST` | `/api/v1/registrations` | Creates user + registration. Returns server `registration_id` / `login_id`. |
| `POST` | `/api/v1/sessions` | Login with login ID or mobile + PIN. |
| `DELETE` | `/api/v1/sessions/current` | Logout / revoke. |
| `GET` | `/api/v1/me` | Authenticated profile. |
| `PATCH` | `/api/v1/me` | Authenticated profile update. |
| `GET` | `/health/live` | Process liveness. Always 200 if the app is running. |
| `GET` | `/health/ready` | Database readiness. 503 if `SELECT 1` fails. |
| `GET` | `/health` | Same as `/health/ready`. Use this as the Render health check. |

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

A Blueprint file lives at the repository root as [`render.yaml`](../render.yaml). Secrets are `sync: false` and must be entered in the dashboard.

### Migrations

Do not run Alembic from every web worker at startup.

1. If the Render plan has a pre-deploy command, set it to `alembic upgrade head`.
2. Otherwise open a one-off Render shell against the same service and run `alembic upgrade head` before sending traffic to a new revision.
3. If the database was created from [`supabase/schema.sql`](../supabase/schema.sql) instead of Alembic, stamp the matching revision first (`alembic stamp 20261022_0003` after applying that script, or `alembic stamp 20261021_0002` then `alembic upgrade head`), then use `upgrade head` going forward.
4. Never run `alembic downgrade`, `alembic stamp` against the wrong revision, or a database reset on production.

The FastAPI role must be able to read and write the application tables. Do not enable PostgREST access to `pin_hash` or `token_hash`. The backend does not use Supabase Auth.

## Client compatibility

- **Android** already uses these routes, snake_case fields, and `Authorization: Bearer`. Keep PIN 4–6 digits. After Render gives the service a URL, set `api.base.url=https://<service>.onrender.com/` in gitignored `local.properties` for release builds. Debug stays `http://10.0.2.2:8000/`.
- **React (`web/`)** calls these same routes with `Authorization: Bearer`. Local development uses `VITE_API_BASE_URL=http://127.0.0.1:8000`. Production Hosting builds use `https://sahakar-bharathi-api.onrender.com`. Production `CORS_ORIGINS` must include `https://sahakarbharati-e3b04.web.app` (and the `firebaseapp.com` alias). Firebase Hosting stays; Firebase Auth/Firestore are not the data plane.

## Remaining risks

- In-process rate limits are not shared across Uvicorn workers or Render instances. They do not protect a horizontally scaled deployment against distributed brute-force attacks.
- A 4–6 digit PIN is a weak authenticator. There is no OTP, PIN change, or password-reset flow.
- Chunked request bodies without `Content-Length` are not capped in-process. Enforce a body size limit at the proxy as well.
- Live Supabase connectivity, Render dashboard autodeploy, and a production smoke test are owner actions after secrets are set.

## Outstanding product work

- No tracking, QR attendance, benefits, or notification APIs
- No photo upload
- React Hosting deploy still needs the GitHub secret `FIREBASE_SERVICE_ACCOUNT_SAHAKARBHARATI_E3B04`
