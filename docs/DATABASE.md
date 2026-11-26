# Database: PostgreSQL on Supabase

FastAPI is the only client of the database. Android and web never connect to it,
and the app does not use Supabase Auth, PostgREST, or the Supabase JS client.

## Schema sources

| Source | Role |
|---|---|
| `backend/app/models/` | SQLAlchemy models, the source of truth for code. |
| `backend/migrations/versions/` | Alembic migrations, the **preferred** way to create and evolve a database. |
| `infra/supabase/schema.sql` | Equivalent snapshot of Alembic head (`20261121_0013`) for the Supabase SQL Editor. **Empty databases only.** |
| `infra/supabase/runtime_role.sql` | Optional least-privilege role for the running API. |

The two init paths produce the same schema. `backend/tests/test_database_schema.py`
builds a fresh database each way and fails CI if either drifts from the models.
Never apply both to the same database: `schema.sql` stops with an error if
`alembic_version` already has a row.

Known, accepted difference: both paths create a UNIQUE constraint **and** a
unique index on `users.login_id`, `users.mobile`, `registrations.registration_id`
and `sessions.token_hash` (the models declare only the index). Uniqueness is
enforced either way; the duplicate index is left in place to avoid altering the
production schema.

### Migration history

| Revision | Change |
|---|---|
| `20261021_0001` | Initial schema |
| `20261021_0002` | Drop unused `verification_challenges` |
| `20261022_0003` | `users.status` default and check |
| `20261022_0004` | `phone_verifications` (OTP challenges) |
| `20261023_0005` | Enable RLS and revoke `anon` / `authenticated` on all app tables |
| `20261023_0006` | `rate_limit_buckets` for `RATE_LIMIT_BACKEND=postgres` |
| `20261026_0007` | `users.role` (`participant`, `admin`, `super_admin`) |
| `20261103_0008` | `places` and `attendance` |
| `20261114_0009` | `rate_limit_buckets.hit_times` for the sliding window |
| `20261117_0010` | `districts`, district administrators, `yatras`, Yatra administrators, stops, and routes |
| `20261117_0011` | Protocol templates, checklist items, and document metadata |
| `20261120_0012` | Yatra counters, attendance, trackers, positions, and latest position |
| `20261121_0013` | Field integrity FKs, geofence columns, one-active-tracker index, `prune_yatra_positions` (current head) |

**Why 0005 matters:** Supabase grants the public `anon` and `authenticated` roles
full privileges on new tables in `public`, and exposes them over PostgREST with the
public anon key. A database created with `alembic upgrade head` before 0005 let
anyone holding that key read `users.pin_hash` and `sessions.token_hash`.
`schema.sql` always revoked these grants; Alembic now does too. The test
`test_upgrade_from_previous_release_revision` reproduces the exposure at 0004 and
proves 0005 removes it.

## New database

1. Create the Supabase project (region `ap-south-1` today; see Render region note in `DEPLOYMENT.md`).
2. From a machine with the repository:

   ```bash
   cd backend
   pip install -e .
   MIGRATION_DATABASE_URL='postgresql://postgres.<ref>:<password>@<pooler-host>:5432/postgres?sslmode=require' \
     alembic upgrade head
   ```

   Use the **session** pooler (port 5432) or direct connection for migrations, not
   the transaction pooler (6543).
3. Optional: apply `infra/supabase/runtime_role.sql` in the SQL Editor, set the
   role password outside the file, and point Render `DATABASE_URL` at `sahakar_api`.

Alternative when Alembic cannot reach the database: run `infra/supabase/schema.sql`
once in the SQL Editor on the **empty** database. It stamps `20261120_0012`, so
later releases use `alembic upgrade head` as normal.

## Existing database (every release)

```bash
cd backend
MIGRATION_DATABASE_URL='<owner connection string, sslmode=require>' alembic current   # inspect
MIGRATION_DATABASE_URL='<owner connection string, sslmode=require>' alembic upgrade head
```

1. Take a backup or confirm PITR coverage first (below).
2. Run `alembic current`. If it prints nothing but the tables exist, the database
   was created outside Alembic: compare it with `schema.sql`, then
   `alembic stamp <matching revision>` (for the pre-0005 snapshot that is
   `20261022_0004`) and `alembic upgrade head`.
3. Run migrations **before** deploying code that needs them. Head is
   `20261120_0012`. Login, `/me`, and admin read `users.role` (0007). Check-in
   reads `places` and `attendance` (0008). The rate limiter reads
   `rate_limit_buckets.hit_times` (0009). Rath Yatra, district, and protocol
   routes read `20261117_0010` and `20261117_0011`. Counters, QR attendance, and
   GPS read `20261120_0012`. A database still at 0009
   will fail those reads after this code is deployed. 0005 (RLS), 0006 (rate-limit table),
   0007, 0008, 0009, 0010, 0011, and 0012 are additive and must be present on production. Do not edit
   migrations that have already been applied. Re-run `runtime_role.sql` after 0012.
4. Never run `alembic downgrade`, `alembic stamp` with a guessed revision, or any
   reset against production. 0005's downgrade is intentionally a no-op.

`backend/migrations/env.py` uses `MIGRATION_DATABASE_URL` when set, else
`DATABASE_URL`. Do not run Alembic locally with the repository's `backend/.env` if
its `DATABASE_URL` points at Supabase unless you intend to migrate production.

## Connections

| Use | Variable | Recommended endpoint |
|---|---|---|
| Running API (Render) | `DATABASE_URL` | Supabase transaction pooler `:6543` (prepared statements are disabled automatically) or session pooler `:5432` |
| Migrations | `MIGRATION_DATABASE_URL` | Session pooler `:5432` or direct connection, as the table owner |

- `postgres://` and `postgresql://` are rewritten to `postgresql+psycopg://`.
- Production forces `sslmode=require` unless the URL sets a stronger mode
  (`verify-ca` / `verify-full`). `disable`, `allow` and `prefer` are rejected at startup.
- Pool: 5 connections + 5 overflow per process, 30 s pool timeout, 30 min recycle,
  TCP keepalives, 10 s connect timeout. Readiness (`/health`) uses a 2 s statement timeout.

## Roles and access

- Supabase API roles (`anon`, `authenticated`): no privileges on any app table,
  RLS enabled with no policies. Verified by tests against a simulated Supabase.
- Table owner (`postgres` on Supabase): runs migrations. Owners bypass non-forced RLS.
- Runtime role `sahakar_api` (`runtime_role.sql`), matching the statements the API runs:
  - `users`: SELECT, INSERT, UPDATE. No DELETE.
  - `registrations`: SELECT, INSERT. No UPDATE, no DELETE.
  - `sessions`: SELECT, INSERT, UPDATE, DELETE (revoke plus retention).
  - `audit_events`: SELECT, INSERT. No UPDATE, no DELETE. RLS has no UPDATE or DELETE policy, so a later extra GRANT cannot rewrite history.
  - `phone_verifications`, `rate_limit_buckets`: SELECT, INSERT, UPDATE, DELETE.
  - `places`: SELECT, INSERT, UPDATE. No DELETE.
  - `attendance`: SELECT, INSERT. No UPDATE, no DELETE.
  - `districts`, `yatras`, `yatra_routes`, `protocol_templates`, `protocol_items`: SELECT, INSERT, UPDATE. No DELETE.
  - `district_administrators`, `yatra_administrators`: SELECT, INSERT, DELETE. No UPDATE.
  - `yatra_stops`: SELECT, INSERT, UPDATE, DELETE.
  - `protocol_documents`: SELECT, INSERT. No UPDATE, no DELETE.
  No CREATE on `public`, no access to `alembic_version`. Verified by
  `test_runtime_role_can_run_the_api_but_not_change_schema`.
  **Implemented in the repository. Not applied by deploy.**

`identifier_hash` on `audit_events` is SHA-256 of the canonical mobile or login id. That is pseudonymization: anyone who can guess the mobile can recompute the hash. It is not anonymization, and it is not a secret.

Apply order for a real database:

1. `MIGRATION_DATABASE_URL=… alembic upgrade head` as the table owner.
2. Run `infra/supabase/runtime_role.sql` as the owner. Re-run it after every migration that adds a table.
3. Outside the file, and without committing the value: `ALTER ROLE sahakar_api WITH LOGIN PASSWORD '<generated secret>';`
4. Set Render `DATABASE_URL` (web service and the retention cron) to the pooler URL for `sahakar_api.<project-ref>`.
5. Keep the owner URL only in `MIGRATION_DATABASE_URL` on the machine that runs Alembic. Do not put it on the web service or the cron.
- The service-role key and anon key are not needed by the backend. Do not put them in Render.

## Backups and recovery

Nothing in this repository takes or restores a backup. Classify each item as it is today:

- **Supabase daily backups and PITR**: required before production, **not configured** by this repository. Supabase stores the backups. The application team must turn them on in the Supabase dashboard and confirm the plan. Daily backups exist on Pro and above; retention follows that plan. Free projects have no downloadable backups. PITR is a paid add-on and is the requirement for a point-in-time restore. This repository cannot see whether either is enabled.
- **Recovery objectives**: not set by this repository and not measured. With only a daily backup, the recovery point is up to 24 hours of lost writes. PITR is what reduces that to the plan's window (minutes, on the plans that sell it). The recovery time is however long the Supabase restore plus the checklist below takes; record the actual time on the first drill. Do not treat either number as already met.
- **`pg_dump` / `pg_restore` into a scratch database**: documented below, not automated, and not run by CI. Use it when the Supabase plan has no PITR.
- **`sahakar_api` in `infra/supabase/runtime_role.sql`**: documented, not applied by deploy or by Alembic.
- **`alembic downgrade`**: not the rollback path. 0005's downgrade is a no-op. 0008's downgrade drops `places` and `attendance`. 0009's downgrade drops `hit_times`. Roll the app back in Render and repair the database forward, or restore a scratch copy.

- Manual logical backup (owner credential, session pooler or direct):

  ```bash
  pg_dump "$MIGRATION_DATABASE_URL" --format=custom --no-owner --file sahakar-$(date +%F).dump
  ```

- Restore drill (into a **new** project or a scratch database, never over production):

  ```bash
  pg_restore --no-owner --dbname "$SCRATCH_DATABASE_URL" sahakar-YYYY-MM-DD.dump
  cd backend && MIGRATION_DATABASE_URL="$SCRATCH_DATABASE_URL" alembic current
  ```

- Supabase restores replace the whole project database. After a restore, run
  `alembic current`, confirm RLS is enabled (`select relname, relrowsecurity from pg_class where relnamespace = 'public'::regnamespace and relkind='r'`),
  and re-run `runtime_role.sql` if used.

### Restore drill

Do this on a scratch project every quarter, and again before the first production launch. It is not automated.

1. Note the Supabase backup or PITR time you intend to restore. If the plan has neither, use a `pg_dump` taken that day.
2. Restore into a new project or scratch database. Do not restore over production.
3. `MIGRATION_DATABASE_URL=… alembic current` prints `20261120_0012`.
4. Confirm RLS is on for every application table and `anon` cannot `SELECT` from `users`.
5. Re-run `runtime_role.sql` if that role is used, then sign in once against the scratch API and confirm a revoked session stays revoked.
6. Discard the scratch project. Record the date and who ran the drill.

## Retention

`python -m app.core.retention` runs the cleanup once. It is idempotent. A second run deletes nothing that the first run left in place. Active sessions and challenges younger than the windows below are kept. `audit_events` is not touched. Failures log the exception type only, not the database URL.

Two ways it runs:

- The PostgreSQL rate limiter calls it about once every 200 checks, in its own transaction. A cleanup error is logged and does not fail the request.
- `render.yaml` defines a daily cron, `sahakar-bharati-retention`, at 02:15 UTC: `python -m app.core.retention`. Its `DATABASE_URL` is the runtime role. That cron is in the blueprint and is not enabled until the Render account creates it and the operator sets `DATABASE_URL`.

```sql
DELETE FROM phone_verifications WHERE created_at < now() - interval '1 day';
DELETE FROM sessions
WHERE expires_at < now() - interval '30 days'
   OR (revoked_at IS NOT NULL AND revoked_at < now() - interval '30 days');
DELETE FROM rate_limit_buckets WHERE window_start < now() - interval '1 day';
```

| Data | Retention | Why |
|---|---|---|
| `phone_verifications` | 1 day after creation | The code expires in minutes and the registration token in 15 minutes. A day is enough to investigate a bad send. |
| `sessions` | 30 days after expiry or revocation | Lets an operator see a recent logout or disable. Live sessions are kept. |
| `rate_limit_buckets` | 1 day after the last write | Active keys refresh `window_start` on each check. |
| `audit_events` | Not deleted by the application | Accountability record. Recommended operator archive after 365 days. **Not configured.** Do not `DELETE FROM audit_events` as part of a routine job. |
| `yatra_positions` | 30 days (`POSITION_RETENTION_DAYS`), except the row still referenced by `yatra_latest_positions` | Older GPS history. Attendance, the latest position, and audit rows are kept. |

`sahakar_api` can delete the three session tables above. It cannot update or delete `audit_events`, and it cannot update or delete `attendance`, `yatra_attendance`, `yatra_positions`, or `registrations`. GPS history is pruned only by the retention cron through `public.prune_yatra_positions(integer, integer)` (SECURITY DEFINER; `EXECUTE` granted to `sahakar_api`; revoked from `PUBLIC`/`anon`/`authenticated`). The in-request limiter never deletes `yatra_positions`.
If the runtime role was created from an older copy of `runtime_role.sql`, re-run
the script after 0013 so session cleanup and `prune_yatra_positions` are allowed.
Until then, cleanup failures are logged and the throttle check itself still commits.
