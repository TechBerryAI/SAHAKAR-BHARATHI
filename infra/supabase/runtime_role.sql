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

-- Privileges match what the API actually does. Re-running this script
-- removes anything broader that an older copy granted.
GRANT SELECT, INSERT, UPDATE ON TABLE public.users TO sahakar_api;
GRANT SELECT, INSERT ON TABLE public.registrations TO sahakar_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.sessions TO sahakar_api;
GRANT SELECT, INSERT ON TABLE public.audit_events TO sahakar_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.phone_verifications TO sahakar_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.rate_limit_buckets TO sahakar_api;
GRANT SELECT, INSERT, UPDATE ON TABLE public.places TO sahakar_api;
GRANT SELECT, INSERT ON TABLE public.attendance TO sahakar_api;

REVOKE DELETE ON TABLE public.users, public.places FROM sahakar_api;
REVOKE UPDATE, DELETE ON TABLE
    public.registrations,
    public.audit_events,
    public.attendance
FROM sahakar_api;
REVOKE ALL ON TABLE public.alembic_version FROM sahakar_api;

-- RLS is enabled on every table with no policies for the Supabase API roles.
-- This role is not the owner, so it needs explicit policies. Tables that the
-- API must not change have no UPDATE or DELETE policy.
DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'users', 'sessions', 'phone_verifications', 'rate_limit_buckets', 'places'
    ]
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_access ON public.%I', table_name);
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_select ON public.%I', table_name);
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_insert ON public.%I', table_name);
        EXECUTE format(
            'CREATE POLICY sahakar_api_access ON public.%I FOR ALL TO sahakar_api USING (true) WITH CHECK (true)',
            table_name
        );
    END LOOP;

    FOREACH table_name IN ARRAY ARRAY['registrations', 'audit_events', 'attendance']
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_access ON public.%I', table_name);
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_select ON public.%I', table_name);
        EXECUTE format('DROP POLICY IF EXISTS sahakar_api_insert ON public.%I', table_name);
        EXECUTE format(
            'CREATE POLICY sahakar_api_select ON public.%I FOR SELECT TO sahakar_api USING (true)',
            table_name
        );
        EXECUTE format(
            'CREATE POLICY sahakar_api_insert ON public.%I FOR INSERT TO sahakar_api WITH CHECK (true)',
            table_name
        );
    END LOOP;
END
$$;
