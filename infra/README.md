# Infra

Supporting files that are not part of a running app. See `docs/DATABASE.md`.

| Path | Purpose |
|---|---|
| [`supabase/schema.sql`](supabase/schema.sql) | Snapshot of Alembic head `20261026_0007` for an **empty** Supabase database. Refuses to run on a database that already has `alembic_version`. Prefer `alembic upgrade head`. |
| [`supabase/runtime_role.sql`](supabase/runtime_role.sql) | Optional least-privilege `sahakar_api` role for the running API. |
| [`tools/render_sahakar_logo.py`](tools/render_sahakar_logo.py) | Regenerates Android density-specific logo drawables under `android/app/src/main/res`. |
