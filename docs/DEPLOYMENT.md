# Deployment, CI/CD and releases

GitHub is the source of truth. Nothing in this repository deploys the backend;
Render pulls from GitHub. Only the Firebase Hosting workflow deploys automatically.

## Branch flow

```
feature branch ──PR──▶ development ──PR──▶ production
     │                    │                    │
     CI: backend, web,    CI again             CI again; web deploys to Firebase
     android, secrets                          (environment "production" approval)
```

| Workflow | Trigger | What it does | Deploys? |
|---|---|---|---|
| `backend-ci.yml` | push / PR touching `backend/`, `infra/supabase/`, `render.yaml` | Migrations on empty Postgres 16, full pytest (incl. fresh-DB, `schema.sql` parity, Supabase role checks), ruff, secret scan | No |
| `web-ci.yml` | PR, or push to any branch except `production`, touching `web/` | lint, typecheck, unit tests, production build, CSP check | No |
| `android-ci.yml` | push / PR touching `android/`; manual | unit tests, debug build, release-URL guard check, unsigned R8 release APK + AAB. Manual `signed_release` job builds signed artifacts | No (uploads artifacts only) |
| `web-firebase-hosting.yml` | push to `production` touching `web/`; manual on `production` | same checks as web CI, then deploy to the live channel | **Yes** |

Any failing step stops the job, so a failing build or test never deploys.

### One-time GitHub settings (owner action)

1. **Branch protection** on `production` and `development`: require PRs and the
   status checks `Backend CI / test`, `Backend CI / secrets`, `Web CI / check`,
   `Android CI / build`.
2. **Environments**: create `production` (required reviewers) and
   `android-release` (required reviewers). Until reviewers are set, GitHub creates
   them unprotected on first use.
3. **Secrets**
   - `FIREBASE_SERVICE_ACCOUNT_SAHAKARBHARATI_E3B04` (environment `production` or repo).
   - `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
     `ANDROID_KEY_PASSWORD` (environment `android-release`).
4. Optional: enable GitHub secret scanning / push protection in addition to
   `scripts/check-secrets.sh`.

## Backend on Render

Blueprint: `render.yaml` (service `sahakar-bharati-api`, do not rename).

| Setting | Value |
|---|---|
| Root directory | `backend` |
| Build | `pip install .` |
| Start | `uvicorn app.main:app --host 0.0.0.0 --port $PORT --proxy-headers --forwarded-allow-ips='*'` |
| Health check | `/health` (database readiness; `/health/live` is process-only) |
| Python | `PYTHON_VERSION=3.12.8` (also `backend/runtime.txt`) |
| Instances / workers | **1 / 1** while `RATE_LIMIT_BACKEND=memory` |

Environment variables (dashboard; never commit values):

| Variable | Required | Notes |
|---|---|---|
| `ENVIRONMENT` | yes | `production` |
| `DATABASE_URL` | yes | Supabase pooler URL, TLS. Startup fails if missing, local, or TLS disabled. |
| `CORS_ORIGINS` | yes for web | `https://sahakarbharati-e3b04.web.app,https://sahakarbharati-e3b04.firebaseapp.com`. https only in production. |
| `TEXTBEE_API_KEY` | for registration | OTP SMS. Without it registration cannot complete (startup logs a warning). |
| `TEXTBEE_DEVICE_ID` | optional | Textbee sending phone. |
| `RATE_LIMIT_BACKEND` | no | `memory` (default) or `postgres` (required before >1 instance). Needs migration 0006. |
| `SESSION_TTL_SECONDS`, `LOGIN_*`, `REGISTER_*`, `OTP_*`, `MAX_REQUEST_BYTES` | no | See `backend/.env.example`. |

Not needed and must not be set: Supabase anon key, service-role key, JWT secret.

### Deploy a backend release

1. Merge to `production` with green CI.
2. Back up or confirm PITR (`DATABASE.md`).
3. Run migrations against production from a trusted machine or a Render shell:
   `MIGRATION_DATABASE_URL=… alembic upgrade head` (add a Render pre-deploy command
   `alembic upgrade head` if your plan supports it).
4. Deploy (auto-deploy if enabled in the dashboard, otherwise "Manual Deploy").
5. Smoke test:
   ```bash
   curl -fsS https://sahakar-bharathi-api.onrender.com/health/live
   curl -fsS https://sahakar-bharathi-api.onrender.com/health
   curl -s -o /dev/null -w '%{http_code}\n' https://sahakar-bharathi-api.onrender.com/api/v1/me   # expect 401
   ```
   then sign in with a known test account from the web app and Android.

### Roll back

- Render dashboard → service → **Events / Deploys** → "Rollback" to the previous deploy.
- Migrations 0005/0006 are additive and compatible with the previous release;
  do **not** downgrade the database. For a bad data change, restore to a scratch
  project and repair forward.

### Cold starts

On 2026-10-27 the live API took 46–60 s to answer its first request after being idle
(Render suspends idle free-tier services). Both clients now cope: long per-attempt
timeouts, automatic retry of GET requests only, and a "server is starting up" banner
after 4 s (see `android/README.md`, `web/README.md`). This hides the wait but does not
remove it. To remove it, use a paid Render instance type, which does not spin down.

### Region

Measured on 2026-10-22: ~270 ms per database round-trip between Render (likely
Oregon) and Supabase `ap-south-1`. A login takes ~7 round-trips. Render cannot move
a service between regions: create a new service in Singapore from the same
Blueprint, verify it, then switch clients (Android `SAHAKAR_API_BASE_URL`, web
`VITE_API_BASE_URL`, Render `CORS_ORIGINS` unchanged).

### Client IP and per-IP throttles

Uvicorn trusts `X-Forwarded-For` from any proxy (`--forwarded-allow-ips='*'`), so
per-IP throttles can be bypassed by a client that sends its own header. The
per-identifier database lockout (5 failures / 15 min) still applies across all
instances. Narrowing trusted proxy IPs requires Render's proxy ranges; tracked as
a remaining risk.

## Web on Firebase Hosting

- Project `sahakarbharati-e3b04`, public dir `web/dist`, SPA rewrite to `/index.html`.
- `VITE_API_BASE_URL` is fixed at build time. Production builds fail unless it is
  an `https://` origin that is not local or a placeholder.
- Headers (`web/firebase.json`): `no-cache` on everything (so `index.html`,
  `sw.js` and deep links revalidate after a deploy), `immutable` on hashed
  `/assets/**`, plus `X-Frame-Options`, `nosniff`, `Referrer-Policy`,
  `Permissions-Policy`, HSTS. A Content-Security-Policy `<meta>` is injected at build.
- Service worker: precaches the app shell only; API traffic is never cached. New
  deploys activate on the next load (`autoUpdate`); old precaches are cleaned up.
- Manual deploy (owner, if CI is unavailable):
  ```bash
  cd web
  npm ci && npm test
  VITE_API_BASE_URL=https://sahakar-bharathi-api.onrender.com npm run build
  npx firebase-tools deploy --only hosting --project sahakarbharati-e3b04
  ```
- Roll back: Firebase console → Hosting → release history → "Rollback".

## Android releases

See `android/README.md` for commands. Summary:

1. Bump `SAHAKAR_VERSION_CODE` / `SAHAKAR_VERSION_NAME` (CI uses the run number as the code).
2. Run **Android CI → Run workflow → signed_release** on `production`; approve the
   `android-release` environment; download the AAB.
3. Upload the AAB to Google Play (internal testing track first). Play App Signing is recommended.

Never commit keystores or passwords. Back up the upload keystore offline; losing it
blocks updates unless Play App Signing is enabled.
