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
| `backend-ci.yml` | push / PR touching `backend/`, `infra/supabase/`, `render.yaml` | Migrations on empty Postgres 16, full pytest (incl. fresh-DB, `schema.sql` parity, Supabase role checks), ruff | No |
| `web-ci.yml` | PR, or push to any branch except `production`, touching `web/` | lint, typecheck, unit tests, production build, CSP check | No |
| `android-ci.yml` | push / PR touching `android/`; manual | unit tests, debug build, release-URL guard check, unsigned R8 release APK + AAB. Manual `signed_release` job builds signed artifacts | No (uploads artifacts only) |
| `secrets-ci.yml` | every push / PR | `scripts/check-secrets.sh` on tracked files | No |
| `web-firebase-hosting.yml` | push to `production` touching `web/`; manual on `production` | same checks as web CI, then deploy to the live channel | **Yes** |

Any failing step stops the job, so a failing build or test never deploys.

### One-time GitHub settings (owner action)

1. **Branch protection** on `production` and `development`: require PRs and the
   status checks `Backend CI / test`, `Secret scan / secrets`, `Web CI / check`,
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
| Start | `uvicorn app.main:app --host 0.0.0.0 --port $PORT` |
| Health check | `/health` (database readiness; `/health/live` is process-only and returns no secrets) |
| Python | `PYTHON_VERSION=3.12.8` (also `backend/runtime.txt`) |
| Instances / workers | Blueprint starts one process. `RATE_LIMIT_BACKEND=postgres` shares the sliding window if you add instances. `memory` is rejected in production. |

Environment variables (dashboard; never commit values):

| Variable | Required | Notes |
|---|---|---|
| `ENVIRONMENT` | yes | `production` |
| `DATABASE_URL` | yes | Supabase pooler URL, TLS. Startup fails if missing, local, or TLS disabled. |
| `CORS_ORIGINS` | yes for web | `https://sahakarbharati-e3b04.web.app,https://sahakarbharati-e3b04.firebaseapp.com`. https only in production. |
| `TEXTBEE_API_KEY` | yes | OTP SMS. Production startup fails closed when it is empty. An empty `TEXTBEE_DEVICE_ID` is valid (Textbee's default phone). |
| `TEXTBEE_DEVICE_ID` | no | Textbee sending phone. Blank uses the account default. |
| `TEXTBEE_BASE_URL` | no | Defaults to `https://api.textbee.dev/api/v1`. Production rejects a non-https URL. |
| `RATE_LIMIT_BACKEND` | yes | Production requires `postgres` (migration 0006 and 0009). `memory` is development only. |
| `CLIENT_IP_SOURCE` | yes | `cf-connecting-ip` on Render. Do not use `X-Forwarded-For` or `--forwarded-allow-ips`. |
| `LOGIN_IP_RATE_LIMIT` | no | Default 30 login attempts per IP per 15 minutes, separate from the per-identifier cap of 5. |
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
- Current Alembic head is `20261120_0012`. Do **not** run `alembic downgrade`.
  0005's downgrade is a no-op; 0008's downgrade drops `places` and `attendance`;
  0009's downgrade drops `hit_times` and the limiter will fail. 0010 and 0011 drop
  the Yatra, district, and protocol tables. 0012 drops counters, attendance,
  trackers, and positions. For a bad data
  change, restore to a scratch project and repair forward.
- Client IP throttles use `CF-Connecting-IP` (see below). The start command
  does not enable Uvicorn's `--forwarded-allow-ips`.

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

### Client IP

Checked against Render and Cloudflare's documentation on 2026-11-16.

Render's published path for a web service is:

```text
visitor → Cloudflare → Render load balancer → this process
```

The process port is not reachable from the public internet. Render forwards Cloudflare's request headers, including `CF-Ray`. Cloudflare's `CF-Connecting-IP` is the single address that connected to Cloudflare. That is the throttle, log, and request-context identity when `CLIENT_IP_SOURCE=cf-connecting-ip`.

A caller cannot choose that value. Cloudflare writes the header on the request it sends toward the origin. `X-Forwarded-For` is not used: Cloudflare appends to a value the caller already sent, so the left side of that list is caller-controlled. `X-Real-IP` is not used.

If `CF-Connecting-IP` is missing or is not one IP address, the identity is the TCP peer (Render's load balancer). The caller cannot choose that either. Every visitor then shares one bucket until the header is present again.

`CLIENT_IP_SOURCE=peer` (development and tests) ignores `CF-Connecting-IP`, `X-Forwarded-For`, and `X-Real-IP`, because a local process can be reached directly.

Do not put another CDN in front of the Render hostname. `CF-Connecting-IP` would then be that CDN, not the visitor.

`X-Forwarded-Proto` is applied only for `cf-connecting-ip`, and only when the value is exactly `http` or `https`. Cloudflare overwrites that header. It does not affect the throttle address.

Login has two independent limits: 5 attempts per identifier per 15 minutes, and 30 attempts per IP per 15 minutes. Rotating identifiers does not reset the IP limit. A wrong PIN and an unknown mobile return the same error. The database lockout (5 failures / 15 minutes per identifier) is unchanged.

OTP sending stays at 5 per mobile per hour, 30 per IP per hour, and a 60-second resend cooldown.

### Phone-number enumeration

`POST /api/v1/phone-verifications` returns **202** `{id, expires_in_seconds}`
whether or not the mobile already has an account. Both paths do the same work
(hash a code, store a challenge, send an SMS) so status, body, and timing do
not answer "is this number registered?".

- A new number receives `SAHAKAR BHARTI: OTP is ######. Do not share it.`
- A registered number receives a sign-in notice with no code. Confirming any
  code returns the same `The code is incorrect or has expired.` as a wrong code.
- Registration still returns `An account already exists for this mobile number.`
  or the email equivalent **after** a valid verification token. A missing or
  fake token is always `Verify this mobile number before registering.`,
  including when the mobile is already taken.
- Web and Android keep a Sign in action on the OTP step. The SMS is what tells
  an existing participant to use it. The 409 strings stay for the person who
  has just proved they hold the phone.

### Monitoring

What to watch, and what is not configured, is in `docs/MONITORING.md`.
`/health` and `/health/live` return only `status` and `database`.

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
