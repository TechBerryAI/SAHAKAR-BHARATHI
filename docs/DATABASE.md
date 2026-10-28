# Database: PostgreSQL on Supabase

FastAPI is the only client of the database. Android and web never connect to it,
and the app does not use Supabase Auth, PostgREST, or the Supabase JS client.

## Schema sources

| Source | Role |
|---|---|
| `backend/app/models/` | SQLAlchemy models, the source of truth for code. |
| `backend/migrations/versions/` | Alembic migrations, the **preferred** way to create and evolve a database. |
| `infra/supabase/schema.sql` | Equivalent snapshot of Alembic head (`20261026_0007`) for the Supabase SQL Editor. **Empty databases only.** |
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
once in the SQL Editor on the **empty** database. It stamps `20261026_0007`, so
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
   `20261026_0007`. Login, `/me`, and admin read `users.role`; a database still
   at 0006 or earlier will 500 after this code is deployed. 0005 (RLS) and 0006
   (rate-limit table) are additive and must also be present on production.
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
- Runtime role `sahakar_api` (optional, `runtime_role.sql`): SELECT/INSERT/UPDATE on
  app tables, DELETE only on `phone_verifications` and `rate_limit_buckets`, explicit
  RLS policies, no CREATE on `public`, no access to `alembic_version`. Verified by
  `test_runtime_role_can_run_the_api_but_not_change_schema`. Re-run the script
  after any migration that adds a table.
- The service-role key and anon key are not needed by the backend. Do not put them in Render.

## Backups and recovery

These are Supabase dashboard settings; the repository cannot verify them.

- **Daily backups**: included on Pro and above (retention depends on plan). Free
  projects have no downloadable backups; take your own with `pg_dump` before
  each release.
- **Point-in-time recovery (PITR)**: paid add-on. Enable it before launch if you
  need recovery to an arbitrary moment.
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

## Housekeeping (not automated yet)

`sessions` (expired/revoked), `phone_verifications` and `audit_events` grow without
bound. Plan a periodic job, for example
`DELETE FROM sessions WHERE expires_at < now() - interval '30 days'`.
`rate_limit_buckets` prunes itself.
