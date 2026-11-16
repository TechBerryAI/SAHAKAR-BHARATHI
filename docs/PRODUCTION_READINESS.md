# Production readiness

Status date: 2026-11-16. Branch: `abhishek`.

This file separates what the repository implements from what an operator still has to do on Render, Supabase, Cloudflare, Textbee, GitHub, and Play. Writing it down does not mean those external steps have been performed. Nothing in the latest documentation pass deployed the service, opened the Supabase dashboard, or sent a real SMS.

Earlier audits (2026-10-23 and 2026-10-27) closed with "not ready" because of problems that the repository has since fixed: the in-process rate limiter, unrestricted proxy-header trust, the OTP flow, and retention. Those sentences are not the current status. Dated test counts from those machines are snapshots, not the size of the suite today.

## Repository / implementation status

Phase 1 is complete in this repository. Production operation is not. Launch still depends on the operator checklist below.

| Area | Repository status | Production action |
|---|---|---|
| OTP | Implemented. 6-digit code, Argon2id hash, expiry, attempt cap, resend cooldown, phone and IP limits, single-use confirm, verification token stored only as a hash. A registered number and a new number both get HTTP 202 with the same JSON shape. SMS failure deletes the challenge. | Send a real Textbee message and register from web and Android. HTTP 202 means the provider accepted the message, not that the handset received it. |
| Authentication | Implemented. Mobile or login id plus PIN, Argon2id, dummy verification for an unknown user, the same error either way, identifier limit and a separate IP limit. | Sign in against the deployed API. |
| Sessions | Implemented. Random opaque bearer, SHA-256 hash stored, expiry, revocation, disabled users lose their sessions. | Confirm logout and a disabled account on the deployed API. |
| Authorization | Implemented on the API. A participant cannot call admin routes. An admin cannot grant `super_admin`. | Confirm with a real participant and admin token after deploy. |
| Rate limiting | Implemented. Sliding window in PostgreSQL (`hit_times`, migration `20261114_0009`), hashed keys, fail closed. `render.yaml` sets `RATE_LIMIT_BACKEND=postgres`. The code default `memory` is for one local process and production startup rejects it. | Confirm the running service still has `postgres`, and that the database is at `20261114_0009`. |
| Trusted proxy / client IP | Implemented. Production requires `CLIENT_IP_SOURCE=cf-connecting-ip`. Identity is Cloudflare's `CF-Connecting-IP`. `X-Forwarded-For` and `X-Real-IP` are not read. A missing or malformed value falls back to the TCP peer. Local `peer` ignores every forwarding header. The start command is `uvicorn app.main:app --host 0.0.0.0 --port $PORT` with no `--proxy-headers` and no `--forwarded-allow-ips`. | From outside Cloudflare, confirm a spoofed `X-Forwarded-For` does not change the throttle identity. Do not put another CDN in front of the Render hostname. |
| Runtime DB role | Defined in `infra/supabase/runtime_role.sql`. Not applied by Alembic or by Render. | Run it as the table owner after `alembic upgrade head`, set the password outside git, and point web and cron `DATABASE_URL` at `sahakar_api`. |
| RLS | Implemented in migration `20261023_0005`. Supabase `anon` / `authenticated` have no policies on app tables. | On the live database, `has_table_privilege('anon','public.users','SELECT')` must be false, and `alembic current` must be `20261114_0009`. |
| Retention | Implemented. `python -m app.core.retention` deletes old phone-verification rows, expired or long-revoked sessions, and idle rate-limit buckets. It does not delete `audit_events`. The limiter also calls it about once per 200 checks. `render.yaml` defines cron `sahakar-bharati-retention` at 02:15 UTC. | Create that cron in the Render account and set its `DATABASE_URL` to the runtime role. The blueprint entry is not an enabled job until then. |
| Audit | Implemented. Security and admin events are appended. Rows store `identifier_hash` (SHA-256 of the identifier: a pseudonym, not anonymization). The runtime role cannot update or delete `audit_events`. | Archive after 365 days is an operator choice. It is not automated. |
| Backups / PITR | Documented in `docs/DATABASE.md` only. | Turn on Supabase daily backups and PITR, then run the restore drill and record the time it took. |
| Monitoring | Documented in `docs/MONITORING.md` only. The API logs a request id, client IP, and a sanitized `CF-Ray`. It does not page anyone. | Connect the probes and alert destinations in that file. |
| Textbee | Integrated behind `SmsGateway`. Credentials come from the environment. The API does not call Textbee from domain code. | Set `TEXTBEE_API_KEY` (and `TEXTBEE_DEVICE_ID` if not using the account default) on Render and confirm a handset. |
| Web | Implemented. Bearer token in `localStorage` (`sahakar_access_token`). CSP, `nosniff`, frame denial, and HSTS are in `web/firebase.json`. API responses use `Cache-Control: no-store`. Logout and HTTP 401 clear the token. `localStorage` is not an HttpOnly cookie. | Deploy Hosting and repeat sign-in, logout, and a 401 against production. |
| Android | Implemented. Session in `EncryptedSharedPreferences`, excluded from backup and device transfer. Logout and HTTP 401 clear it. Release builds refuse a missing or non-HTTPS API URL. Application id is still `com.example.sahakarbharati`. | Run Android CI (`testDebugUnitTest`, release assemble). Play will reject `com.example.*` on a new listing; choose a real id before the first upload. |
| Event check-in | Implemented (`places`, `attendance`) on the API, web, and Android. | Confirm a booth check-in on the deployed API. |
| Not in Phase 1 | Live chariot tracking, a QR pass, benefits, and notifications have no API and are shown as unavailable. | Out of scope until a product spec exists. |
| CI | Workflows exist for backend, web, Android, secret scan, and Firebase deploy. | They have not been executed from this documentation pass. Confirm a green run on the release commit, and configure GitHub environments and reviewers. |

## Production configuration in the repository

| Setting | Where it is set | What this repository can say |
|---|---|---|
| `ENVIRONMENT=production` | `render.yaml` | Declared. Not proof the live service matches. |
| `RATE_LIMIT_BACKEND=postgres` | `render.yaml` | Declared. Local default in `backend/.env.example` is `memory` and must stay local. |
| `CLIENT_IP_SOURCE=cf-connecting-ip` | `render.yaml` | Declared. |
| `DATABASE_URL`, `TEXTBEE_API_KEY`, `TEXTBEE_DEVICE_ID` | Render, `sync: false` | Not in git. Not verified here. |
| `CORS_ORIGINS` | `render.yaml` | The two Firebase Hosting origins. |
| Retention cron | `render.yaml` | Defined. Not enabled until the Render account creates it. |
| `sahakar_api` | `infra/supabase/runtime_role.sql` | Defined. Not applied to production by deploy. |

## Operator checklist

These items are not done by this repository:

- [ ] Exposure check: `anon` cannot `SELECT` from app tables; `alembic current` is `20261114_0009`
- [ ] `runtime_role.sql` applied; web and cron `DATABASE_URL` use `sahakar_api`; owner URL only in `MIGRATION_DATABASE_URL`
- [ ] Supabase backups and PITR on; restore drill recorded
- [ ] Render cron `sahakar-bharati-retention` created
- [ ] Monitoring alerts from `docs/MONITORING.md` connected to a person
- [ ] Textbee secrets set; one real registration completed on web and Android
- [ ] Spoofed `X-Forwarded-For` checked on the live Cloudflare → Render path
- [ ] CI green on the release commit; GitHub `production` and `android-release` environments have reviewers
- [ ] Web deployed; sign-in, profile update, and logout checked against production
- [ ] Android CI and a signed release checked; Play application id chosen before the first upload
- [ ] `/health` returns 200 after deploy; rollback (Render rollback, Firebase release history) rehearsed

## Historical findings

The table records defects found on 2026-10-23 and what the repository did about them. "Fixed" means the code or config in this tree. It does not mean production was rechecked.

| # | Sev | Finding | Current repository status |
|---|---|---|---|
| F1 | P0 | Alembic never enabled RLS; on Supabase the anon key could read PIN and token hashes. | Fixed in migration `20261023_0005`. Production database still has to be checked (checklist). |
| F2–F4, F6, F14–F17 | P0–P2 | Android did not compile, the wrapper was in the wrong place, release URL defaulted to `https://api.invalid/`, 401 left the user looking signed in, dead controls, test flakes, lint. | Fixed in the Android tree. CI on GitHub is still an operator check. |
| F5 | P1 | Concurrent duplicate registration returned HTTP 500. | Fixed. Unique violation maps to 409. |
| F7, F8, F12 | P1 | Web treated startup failure as signed out, hid a created account when auto sign-in failed, had no CSP and cached `index.html`. | Fixed. Token stays in `localStorage`; see the table above. |
| F9 | P1 | Rate limiter was in-process only. | Fixed. Production config is PostgreSQL sliding window. Local default remains `memory` and production rejects it. |
| F10, F11 | P1 | Production accepted bad environment, TLS, and CORS settings; chunked bodies were uncapped. | Fixed. |
| F13 | P1 | No web or Android CI, unprotected deploy, no secret scan. | Workflows are in `.github/workflows`. They were not run for this documentation pass. |
| F18 | P2 | `schema.sql` could insert a second `alembic_version` row. | Fixed. It refuses to run when a row exists. Head stamp is `20261114_0009`. |
| F20 | P2 | `--forwarded-allow-ips='*'` trusted caller-supplied `X-Forwarded-For`. | Fixed. That flag is not in the start command. Production uses `CF-Connecting-IP`. |
| F21 | P3 | Git history had no live credentials. Local `backend/.env` is gitignored. | Unchanged. Do not commit or archive that file. |

Local checks on 2026-11-16 (this machine, not GitHub, not Render): backend pytest 166 passed, web 76 tests plus lint, typecheck, and a production build, secret scan ok (307 files). Android unit tests and the release build were not run here: the Windows SDK provides `aapt.exe` and Linux Gradle cannot use it. `.github/workflows/android-ci.yml` is the run that can pass.
