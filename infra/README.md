# Infra

Supporting files that are not part of a running app.

| Path | Purpose |
|---|---|
| [`supabase/schema.sql`](supabase/schema.sql) | Optional snapshot if the database was created outside Alembic. Prefer `backend` migrations going forward. |
| [`tools/render_sahakar_logo.py`](tools/render_sahakar_logo.py) | Regenerates Android density-specific logo drawables under `android/app/src/main/res`. |
