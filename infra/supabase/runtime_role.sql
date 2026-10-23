-- Optional least-privilege role for the FastAPI runtime (docs/DATABASE.md).
--
-- Run AFTER `alembic upgrade head` (or schema.sql), as the role that owns the
-- tables (Supabase: postgres). Safe to re-run; re-run after every migration
-- that adds a table.
--
-- Afterwards, outside this file and without committing the value:
--   ALTER ROLE sahakar_api WITH LOGIN PASSWORD '<generated secret>';
-- Then set Render DATABASE_URL to connect as sahakar_api (on the Supabase
-- pooler the user name is sahakar_api.<project-ref>). Keep the owner
-- credential only for migrations (MIGRATION_DATABASE_URL).
--
-- The role can read and write participant data but cannot change the schema,
-- read alembic_version, or bypass RLS on tables added later.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sahakar_api') THEN
        CREATE ROLE sahakar_api NOLOGIN NOINHERIT;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO sahakar_api;
REVOKE CREATE ON SCHEMA public FROM sahakar_api;

GRANT SELECT, INSERT, UPDATE ON TABLE
    public.users,
    public.registrations,
    public.sessions,
    public.audit_events,
    public.phone_verifications,
    public.rate_limit_buckets
TO sahakar_api;
-- Undelivered OTP challenges are discarded, and stale throttle buckets pruned.
GRANT DELETE ON TABLE public.phone_verifications, public.rate_limit_buckets TO sahakar_api;

-- RLS is enabled on every table with no policies for the Supabase API roles.
-- This role is not the owner, so it needs explicit policies.
DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'users', 'registrations', 'sessions', 'audit_events', 'phone_verifications', 'rate_limit_buckets'
    ]
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_access ON public.%I', table_name);
        EXECUTE format(
            'CREATE POLICY sahakar_api_access ON public.%I FOR ALL TO sahakar_api USING (true) WITH CHECK (true)',
            table_name
        );
    END LOOP;
END
$$;
