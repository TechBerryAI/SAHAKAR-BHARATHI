# Production readiness report

Date: 2026-10-23. Branch: `abhishek` (base `cad3782`). Changes are uncommitted in the
working tree; nothing was pushed, merged, deployed, or run against Render, Supabase,
Firebase or Textbee.

## A. Executive summary

**Fixed.** The Android app did not compile at HEAD (half-merged localization files)
and its build was not reproducible (wrapper at the repo root, `settings.gradle.kts`
in `android/`, CRLF `gradlew`). It now builds from a clean checkout, and release
builds refuse to ship without a valid HTTPS API URL (previously they defaulted to
`https://api.invalid/`). Alembic-created databases exposed `pin_hash` and
`token_hash` to Supabase's public `anon` role; migration `0005` closes that, with tests that
reproduce the exposure and prove the fix. Concurrent duplicate registrations returned
500; they now return 409 and exactly one account is created. Both clients now handle expired or revoked
sessions, offline start-up and partial registration failures truthfully, and Android's fake
"live"/"verified"/notification controls were removed or disabled. Added shared
rate limiting (opt-in), production config fail-fast, web CSP and cache headers,
CI for web and Android, a secret scanner, and complete docs.

**Readiness: NOT READY FOR PRODUCTION.** It's code-ready for a staged beta once the owner
actions in section G are done. The deciding factors are outside the repository:
the production database's exposure state is unknown, migrations `0005`/`0006` are not applied there,
OTP/SMS delivery is deferred and unverified, and no live deployment was smoke-tested.

**Major risks.** (1) Live `pin_hash` exposure if production was built with Alembic
before 0005. (2) Registration depends on the untested Textbee OTP path. (3)
`--forwarded-allow-ips='*'` weakens per-IP throttles. (4) Render↔Supabase region
mismatch (~270 ms per query).

## Findings (initial state)

| # | Sev | Area | Finding | Status |
|---|---|---|---|---|
| F1 | P0 | DB/security | Alembic path never enabled RLS or revoked `anon`/`authenticated`; on Supabase, PIN and token hashes were readable with the public anon key (only `schema.sql` locked them down). | Fixed in code (migration 0005). **Production unverified** (G1). |
| F2 | P0 | Android | App did not compile: `HomeLanguagePacks.kt`/`RestLanguagePacks.kt` referenced enum values and fields that only exist on the unmerged `Radheshyam` branch (87 errors); the coverage test referenced APIs that don't exist either. | Fixed: dead files removed, test rewritten against real API |
| F3 | P0 | Android | Build not reproducible: wrapper at repo root but Gradle root in `android/`; root `build.gradle.kts` had no settings; `gradlew` not executable and CRLF under `core.autocrlf`. | Fixed |
| F4 | P0 | Android | Release `API_BASE_URL` silently defaulted to `https://api.invalid/`. | Fixed: release build fails on missing/http/local/placeholder URL |
| F5 | P1 | Backend | Concurrent duplicate registration → HTTP 500 (IntegrityError raised at flush, outside the handler). | Fixed + test |
| F6 | P1 | Android | 401 (expired/revoked/disabled) ignored; Home kept showing cached profile as if signed in. | Fixed: interceptor + navigation + tests |
| F7 | P1 | Web | Offline/5xx at start-up made a signed-in user look signed out. | Fixed: retry screen, token kept |
| F8 | P1 | Both | "Registration failed" shown when the account was created but auto sign-in failed (retry → duplicate error). | Fixed on both clients |
| F9 | P1 | Backend | Rate limiter in-process only. | `RATE_LIMIT_BACKEND=postgres` added (atomic, hashed keys); default unchanged for latency; documented single-instance rule |
| F10 | P1 | Backend | Production accepted unknown `ENVIRONMENT`, `sslmode=disable`, http/local CORS origins; CORS allowed credentials needlessly. | Fixed |
| F11 | P1 | Backend | Chunked bodies without `Content-Length` were not size-capped. | Fixed |
| F12 | P1 | Web | No production check of `VITE_API_BASE_URL`; no CSP with token in `localStorage`; Firebase default 1 h caching of `index.html` could serve stale shells after deploy. | Fixed |
| F13 | P1 | CI | No web CI on PRs, no Android CI, deploy job unprotected, no secret scanning. | Fixed (owner must set environment reviewers) |
| F14 | P1 | Android | Fake states: green "Verified"/check on "no records", dead torch/flip buttons, notifications switch that did nothing, PIN field collecting a PIN that went nowhere, "View pass and check in". | Fixed: neutral styling, controls removed/disabled, truthful labels |
| F15 | P2 | Android | Sign-in/registration double-tap guard depended on `Dispatchers.Main.immediate` (2 unit tests failed). | Fixed |
| F16 | P2 | Android | Instrumented tests failed on API 37 (Espresso 3.5.1). | Fixed: test-only bump to Espresso 3.7.0 / ext-junit 1.3.0 |
| F17 | P2 | Android | Lint error `MissingPermission` (guarded call lint could not see). | Fixed with explicit `SecurityException` handling, no suppression |
| F18 | P2 | DB | `schema.sql` would insert a second `alembic_version` row on an existing DB. | Fixed: refuses to run |
| F19 | P2 | Repo | `.claude/launch.json` referenced another machine's temp path. | Fixed |
| F20 | P2 | Backend | `--forwarded-allow-ips='*'` lets clients spoof `X-Forwarded-For`. | Documented (needs Render proxy ranges) |
| F21 | P3 | Repo | Git history scanned for credential patterns: only placeholders and the CI throwaway password. No rotation needed from history. Local untracked `backend/.env` contains real Supabase and Textbee credentials and is gitignored. | No action; keep it local |

## B. Changes implemented

| Files | Problem | Change | Verified by |
|---|---|---|---|
| `android/gradlew*`, `android/gradle/wrapper/*`, `android/build.gradle.kts` (moved from root), `.gitattributes` | F3 | Wrapper moved into the Gradle root; LF pinned for `gradlew`, CRLF for `.bat`; exec bit set | Clean builds from WSL via `./gradlew` |
| `android/app/build.gradle.kts`, `android/keystore.properties.example` | F4, signing, versioning | URL from `-P`/env/`local.properties`; `validateReleaseConfig` before `preReleaseBuild`; env/keystore-properties signing, partial config fails; version code/name overridable; debug URL override | Rejected http, localhost, `.invalid`, missing slash, unset; built signed APK with throwaway key, `apksigner verify` v2 OK |
| `core/copy/HomeLanguagePacks.kt`, `RestLanguagePacks.kt` (deleted), `AppLanguageCoverageTest.kt` | F2 | Removed unreferenced files that broke compilation; test now checks the 26 shipped languages | Compiles; 3 tests pass |
| `data/AppSession.kt`, `data/AppContainer.kt`, `data/remote/SessionExpiryInterceptor.kt`, `ui/navigation/RathYatraScreen.kt`, `features/home/HomeViewModel.kt` | F6 | Observable signed-in state; 401 on authenticated call clears only the matching token; navigation leaves protected screens; 30 s read / 60 s call timeout | Unit tests + emulator e2e |
| `data/repository/AuthRepository.kt` | F8 | Distinguishes "created but sign-in failed" | Unit test |
| `features/registration/SignInViewModel.kt`, `RegistrationViewModel.kt` | F15 | Loading set before launch | AuthViewModelTest 6/6 |
| `features/attendance/QrAttendanceScreen.kt`, `features/profile/ProfileScreen.kt` | F14 | Removed dead buttons, neutral styling, disabled PIN field and notifications switch, truthful labels, profile load error shown | Emulator UI dump shows new labels |
| `features/registration/RegistrationFormScreen.kt` | F17 | Explicit exception handling | `lintDebug` 0 errors |
| `android/gradle/libs.versions.toml` | F16 | Espresso 3.7.0, ext-junit 1.3.0 (androidTest only) | 3/3 instrumented tests on API 37 emulator |
| `android/app/src/test/.../data/SessionHandlingTest.kt` | tests | 7 tests: interceptor, stale-token race, offline cache, registration partial failure | pass |
| `backend/app/core/config.py` | F10 | Known environments only; production TLS, CORS https-only; `RATE_LIMIT_BACKEND` | `test_hardening.py` |
| `backend/app/main.py` | F10, F11 | Streaming body cap (413), `Cache-Control: no-store` + `nosniff` on `/api/*`, CORS without credentials, limiter selection, startup warnings | `test_hardening.py` |
| `backend/app/core/rate_limit.py`, `app/models/rate_limit.py`, migration `20261023_0006` | F9 | Atomic Postgres fixed-window limiter, SHA-256 keys, self-pruning | `test_shared_rate_limit.py` incl. 40 concurrent calls across 4 limiter instances → exactly 10 allowed |
| migration `20261023_0005`, `infra/supabase/schema.sql`, `infra/supabase/runtime_role.sql`, `migrations/env.py` | F1, F18 | RLS + revoke; schema.sql at head with safe stamp; optional least-privilege role; `MIGRATION_DATABASE_URL` | `test_database_schema.py` (8 tests) |
| `backend/app/services/registration.py` | F5 | IntegrityError at flush → 409 | `test_integrity.py` (passed 3 consecutive runs) |
| `backend/tests/conftest.py` | safety | Pins `MIGRATION_DATABASE_URL` to the test DB | — |
| `backend/.env.example` | docs | Every setting documented, placeholders only | — |
| `web/src/lib/api.ts`, `session.ts`, `App.tsx`, `screens/*` | F7, F8 | `restoreFailed` state + retry screen; `RegisteredButSignInFailedError` → Sign in with Login ID; profile refresh error shown; logout `keepalive` | Vitest + browser e2e |
| `web/vite.config.ts`, `web/firebase.json` | F12 | Production URL guard, CSP meta, SW cleanup, security + cache headers | `vite.config.test.ts`; built output inspected |
| `web/package.json`, lockfile, `vitest.config.ts`, `src/test/setup.ts`, `src/lib/api.test.ts`, `vite.config.test.ts`, `tsconfig.node.json` | tests | Vitest 5.0.3 (pinned, additions only in lockfile) | 24 tests |
| `.github/workflows/*`, `render.yaml` | F13 | Web CI, Android CI (+ manual signed release behind environment), backend CI covers `infra/`, secret job, deploy needs tests + `production` environment + branch check; Render declares Textbee vars | YAML parsed; **not run on GitHub** |
| `scripts/check-secrets.sh`, `.githooks/pre-commit`, `.gitignore` | secrets | Path + content scanner (prints names, not values); ignore keystores, service accounts, keys | Clean repo passes; planted Supabase URL caught |
| `.claude/launch.json` | F19 | Removed stale entry; added WSL e2e configs bound to a throwaway DB | Used for e2e |
| `README.md`, `android/README.md`, `backend/README.md`, `web/README.md`, `infra/README.md`, `docs/*.md` | docs | Rewritten to match code | — |

OTP code (`services/phone_verification.py`, `services/sms.py`, OTP routes and schemas) is untouched.

## C. Test and build results

Environment: Windows 11 + WSL Ubuntu 24.04. Backend tests ran against a
throwaway PostgreSQL 16.15 cluster in WSL (`127.0.0.1:55432`). Android built with
Temurin JDK 25 and the local Android SDK (through a symlinked Linux view of the
Windows SDK). Emulator: `Medium_Phone_API_37.0`.

| Command | Result |
|---|---|
| `pytest` (backend, before changes) | 77 passed |
| `pytest` (backend, final) | **111 passed**, 0 skipped |
| `ruff check` | All checks passed |
| `npm run lint` / `npm run typecheck` | pass |
| `npm test` (Vitest) | **24 passed** |
| `VITE_API_BASE_URL=https://sahakar-bharathi-api.onrender.com npm run build` | pass; CSP present; SW has only the navigation route |
| `VITE_API_BASE_URL=http://127.0.0.1:8000 vite build` / unset | fails as intended |
| `./gradlew :app:compileDebugKotlin` (before) | **failed, 87 errors** |
| `./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease -Psahakar.apiBaseUrl=…` (final) | BUILD SUCCESSFUL; **23/23** unit tests; lint 0 errors, 42 warnings |
| `./gradlew :app:assembleRelease` without URL | fails at `validateReleaseConfig` as intended |
| `am instrument` on API 37 emulator | **OK (3 tests)** |
| `bash scripts/check-secrets.sh` | ok (248 files) |
| GitHub Actions workflows | **not run** (no push) |

Artifacts (local, gitignored): `android/app/build/outputs/apk/debug/app-debug.apk`,
`android/app/build/outputs/apk/release/app-release-unsigned.apk` (5.9 MB),
`android/app/build/outputs/bundle/release/app-release.aab` (unsigned, 8.0 MB).
The production URL is in the release dex; `10.0.2.2` and `api.invalid` are absent.

End-to-end (local API + throwaway DB, seeded test participant, no SMS):

| Flow | Web (built PWA, 375×812) | Android (emulator) |
|---|---|---|
| Wrong PIN → error, no session | ✅ | ✅ |
| Sign in → server profile shown | ✅ | ✅ |
| Profile update persisted (DB row + audit) | ✅ | n/a (Android has no edit UI) |
| Update by other client visible after refetch | — | ✅ ("Anita Rao") |
| Server-side revoke → back to signed out | ✅ | ✅ |
| Logout revokes server session (old token → 401) | ✅ | ✅ (0 active sessions) |
| API down → truthful error | ✅ retry screen, token kept | ✅ "server could not be reached" |
| SW caches no API responses | ✅ (10 shell entries) | — |

Not tested: physical phone, registration UI with real OTP, iOS Safari install,
slow-network throttling, font scaling and rotation on device, accessibility audit.

## D. Production configuration

| Variable | Where | Verified here |
|---|---|---|
| `ENVIRONMENT=production` | Render | Blueprint only |
| `DATABASE_URL` | Render (secret) | No (not accessed) |
| `MIGRATION_DATABASE_URL` | Operator machine / Render shell, not stored | No |
| `CORS_ORIGINS` | `render.yaml` | Value validated by tests |
| `TEXTBEE_API_KEY`, `TEXTBEE_DEVICE_ID` | Render (secret) | No |
| `RATE_LIMIT_BACKEND` | `render.yaml` (`memory`) | Yes (tests) |
| `VITE_API_BASE_URL` | Web CI / deploy workflow env | Yes (build guard) |
| `FIREBASE_SERVICE_ACCOUNT_SAHAKARBHARATI_E3B04` | GitHub secret | No |
| `SAHAKAR_API_BASE_URL` | Android CI env / release machine | Yes (build guard) |
| `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | GitHub environment `android-release` | Signing path verified with a throwaway key only |

## E. Feature readiness matrix

| Feature | Backend | Web | Android | Status |
|---|---|---|---|---|
| Sign-in (login ID or mobile + PIN) | ✅ | ✅ | ✅ | **Verified** (local) |
| Session restore / expiry / revoke / logout | ✅ | ✅ | ✅ | **Verified** (local) |
| Profile view | ✅ | ✅ | ✅ | **Verified** |
| Profile edit | ✅ | ✅ | not offered | Partially verified (web only) |
| Registration | ✅ (tests with fake SMS) | partial | partial | **Blocked on OTP** (deferred) |
| OTP / SMS (Textbee) | unchanged | unchanged | unchanged | **Deferred, unverified** |
| Profile photo | none | none | device-only, labelled | Truthful local feature |
| Chariot tracking | none | none | "Route unavailable" | Unavailable (no spec) |
| QR attendance / pass | none | none | "not connected" | Unavailable (no spec) |
| Benefits, notifications | none | none | "Opening soon" / disabled | Unavailable (no spec) |
| Help / SOS | — | — | static help text (112) | Static content |
| Languages | — | 26 | 26 | Unit-tested coverage |
| Database init + migration | ✅ | — | — | Verified on throwaway DB; **production not migrated** |
| PWA install shell / update | — | ✅ | — | Verified locally; offline use not offered |
| Deploy (Render, Firebase, GitHub Actions) | — | — | — | **Not verified live** |

## F. Deployment instructions

Follow `docs/DEPLOYMENT.md`. Order for the first release with these changes:

1. Configure GitHub branch protection, environments (`production`, `android-release`) with reviewers, and secrets.
2. Back up the Supabase database (or confirm PITR).
3. Run the exposure check (G1), then `MIGRATION_DATABASE_URL=… alembic upgrade head`.
4. Merge to `development`, then to `production`, with green CI.
5. Deploy the backend on Render; run the smoke tests in `DEPLOYMENT.md`.
6. Web deploys automatically after approval of the `production` environment.
7. Android: run the manual signed release workflow, then upload the AAB to Play internal testing.

## G. Remaining blockers

| # | Sev | Blocker | Owner action | How to verify |
|---|---|---|---|---|
| G1 | P0 | Production may expose `pin_hash`/`token_hash` via the anon key (if built with Alembic before 0005). | In the Supabase SQL Editor run `select has_table_privilege('anon','public.users','SELECT');`. If `true`, apply 0005 **immediately** and treat PIN hashes as exposed (consider forcing PIN resets once a reset flow exists). | Query returns `false` for every app table; `alembic current` = `20261023_0006` |
| G2 | P0 | OTP/SMS deferred: registration end-to-end with real SMS never verified. | Set Textbee secrets; test on a real number in staging; document delivery rates. | A new participant registers from web and Android |
| G3 | P1 | No live smoke test of Render, Firebase or CI. | Push branch, open PR, watch all workflows; deploy; run smoke tests. | Green checks; `/health` 200; sign-in from both clients |
| G4 | P1 | GitHub environments/branch protection not configured (repo cannot set them). | Configure as in `DEPLOYMENT.md`. | Deploy waits for approval |
| G5 | P1 | Upload keystore and Play listing do not exist in the repo. | Create and store the upload key offline; set CI secrets; enable Play App Signing. | Signed AAB accepted by Play internal testing |
| G6 | P1 | Proxy-header trust (`*`) weakens per-IP limits. | Obtain Render proxy ranges or set a trusted client-IP header; restrict `--forwarded-allow-ips`. | Spoofed `X-Forwarded-For` no longer changes the throttle key |
| G7 | P2 | Render region vs Supabase `ap-south-1`. | Create the service in Singapore and switch over. | `/health` latency drops |
| G8 | P2 | Product decisions missing for tracking, QR attendance, benefits, notifications (data source, attendance rules, eligibility). | Product owner to specify. | Specs exist; features built against them |
| G9 | P2 | Physical-device, accessibility, slow-network and font-scale testing not done. | Manual QA pass on low-end Android and mobile Chrome/Safari. | QA sign-off |
| G10 | P3 | Session/OTP/audit tables never pruned. | Schedule cleanup (`DATABASE.md`). | Table sizes stable |
| G11 | P3 | Android profile edit absent (web has it). | Product decision whether Android needs it. | — |
| G12 | P3 | Unused `firebase` dependency and `src/lib/firebase.ts` remain in web. | Remove when confirmed unneeded. | Bundle unchanged (not imported) |

## H. Final recommendation

**NOT READY FOR PRODUCTION.**

The repository-side work is done and backed by evidence: 111 backend tests on real
PostgreSQL (including migrations from scratch, an upgrade from the previous release, `schema.sql` parity,
Supabase role lockdown and concurrency), 24 web tests, 23 Android unit tests plus 3
instrumented tests on an API 37 emulator, lint-clean builds of all three components,
guarded release configuration, and the core flows exercised end to end on both clients.
What stops a production launch is unverified outside the repository: G1 (a possible
live exposure of credential hashes), G2 (registration depends on an untested SMS
path) and G3/G4 (no live deployment or CI run). Once G1–G4 are closed and a staging
smoke test passes, the system is ready for a **staged/beta release**. A full
production launch should also close G5 and G6.

## Launch checklist

- [ ] G1 exposure check done; production at `20261023_0006`
- [ ] Backup / PITR confirmed and a restore drill performed
- [ ] Render env vars set (section D); `/health` 200 after deploy
- [ ] CORS origins match the live web domains
- [ ] Textbee configured; registration verified with a real phone (G2)
- [ ] All four CI workflows green on the release commit
- [ ] Branch protection + environment reviewers configured
- [ ] Web deployed; hard-refresh and installed-PWA update both pick up the new build
- [ ] Signed AAB built by CI; tested on a physical device; uploaded to internal testing
- [ ] Sign-in, profile update, logout verified on web and Android against production
- [ ] Rollback paths rehearsed (Render rollback, Firebase release history)
- [ ] Support / help-desk contact confirmed for the in-app help text

## Notes on this session

- A throwaway PostgreSQL 16 server package was downloaded from the Ubuntu archive
  (`apt-get download`, no install) into WSL home for testing; its data was local and
  is stopped. A throwaway signing key was generated and deleted; the APK signed with it was deleted.
- On the `Medium_Phone_API_37.0` emulator, the debug app was updated in place, its test
  package reinstalled, and its local app data cleared (`pm clear`) for a clean e2e run.
  That removed only that dev install's local data; there was no server-side effect.
