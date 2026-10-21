# Sahakar Bharati backend

Python 3.12 FastAPI service for participant registration, opaque session authentication, and profile APIs. PostgreSQL is the system of record. The Android app must call this API; it must never connect to the database.

This service is not production-ready. Local registration working is not a security review, compliance claim, or launch sign-off.

## Stack

- FastAPI, Pydantic v2, Uvicorn
- SQLAlchemy 2.x with psycopg
- Alembic migrations
- Argon2id PIN hashing via pwdlib
- pytest against PostgreSQL

## Local startup

From the repository root:

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
```

Android emulator debug builds should use `http://10.0.2.2:8000/`. A physical device needs the development machine LAN address. Production builds must use HTTPS.

## Environment variables

| Variable | Purpose |
|---|---|
| `POSTGRES_HOST` / `POSTGRES_PORT` | Local PostgreSQL listener |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Database role. Password may be empty for local trust or peer auth. |
| `POSTGRES_DB` / `POSTGRES_TEST_DB` | Application and pytest databases |
| `DATABASE_URL` | Optional full SQLAlchemy URL. Leave blank to build it from the `POSTGRES_*` fields. |
| `TEST_DATABASE_URL` | Optional pytest URL. Leave blank to use `POSTGRES_TEST_DB`. |
| `SESSION_TTL_SECONDS` | Opaque session lifetime (default 7 days) |
| `LOGIN_RATE_LIMIT` / `LOGIN_RATE_WINDOW_SECONDS` | In-process login throttle |
| `REGISTER_RATE_LIMIT` / `REGISTER_RATE_WINDOW_SECONDS` | In-process registration throttle |
| `LOGIN_LOCKOUT_FAILURES` / `LOGIN_LOCKOUT_SECONDS` | Database-backed lockout after failed PINs |
| `MAX_REQUEST_BYTES` | JSON body size limit |
| `CORS_ORIGINS` | Optional comma-separated browser origins |
| `ENVIRONMENT` | `development` or `production` |

Do not commit real secrets, production credentials, or personal data.

## Authentication

Sessions are opaque bearer tokens, not JWTs.

1. `POST /api/v1/sessions` verifies the Argon2id PIN hash and inserts a `sessions` row.
2. The raw token is returned once. Only the SHA-256 hash is stored.
3. Authenticated requests send `Authorization: Bearer <token>`.
4. `DELETE /api/v1/sessions/current` sets `revoked_at`. Expired or revoked tokens are `401`.
5. The current user is loaded from the session row. Client-supplied user IDs are ignored.

PINs are 4–6 digits and are never stored or logged. Login is rate-limited and locks after repeated failures.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/v1/registrations` | Creates user + registration. Returns server `registration_id` / `login_id`. |
| `POST` | `/api/v1/sessions` | Login with login ID or mobile + PIN. |
| `DELETE` | `/api/v1/sessions/current` | Logout / revoke. |
| `GET` | `/api/v1/me` | Authenticated profile. |
| `PATCH` | `/api/v1/me` | Authenticated profile update. |
| `GET` | `/health` | Process and database ping. |

## Operations notes (not a certification)

- Deploy behind HTTPS only. Terminate TLS at a reverse proxy or load balancer.
- Keep `DATABASE_URL` and any future secrets in the host environment or a secret manager.
- Apply Alembic migrations before opening the new API version: `alembic upgrade head`.
- Back up PostgreSQL with `pg_dump` / managed snapshots and test restore on a scratch instance.
- Logs must not include PINs, tokens, Authorization headers, or raw request bodies.
- Restrict database and admin access to operators who need it. This is not an access-control audit.
- In-process rate limits do not span multiple API workers. Use a shared limiter (for example Redis) before multi-instance production.

## Outstanding

- No tracking, QR attendance, benefits, or notification APIs
- No production host, certificate, or managed database
- No photo upload
