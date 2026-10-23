-- Sahakar Bharati participant schema for Supabase (PostgreSQL).
-- Matches Alembic head 20261023_0006 (verification_challenges is not created).
-- backend/tests/test_database_schema.py fails if this file drifts from the models.
--
-- USE ONLY ON AN EMPTY DATABASE. The preferred path is `alembic upgrade head`
-- (see docs/DATABASE.md). This file is an alternative for the Supabase SQL
-- Editor when Alembic cannot reach the database. Apply it with:
--   psql "$MIGRATION_DATABASE_URL" -v ON_ERROR_STOP=1 -f infra/supabase/schema.sql
--
-- It records Alembic revision 20261023_0006, so `alembic upgrade head` works
-- afterwards. If alembic_version already has a row the script stops with an
-- error instead of creating a second version row.
--
-- These tables are for the FastAPI service, not Supabase Auth / PostgREST.
-- RLS is enabled with no anon/authenticated policies so pin_hash and
-- token_hash stay off the public API. The postgres role used by FastAPI
-- bypasses RLS.

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

CREATE TABLE IF NOT EXISTS public.users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    login_id varchar(32) NOT NULL,
    mobile varchar(10) NOT NULL,
    email varchar(254),
    pin_hash varchar(255) NOT NULL,
    full_name varchar(120) NOT NULL,
    gender varchar(16),
    date_of_birth date,
    occupation varchar(32),
    address_line varchar(255),
    locality varchar(120),
    district varchar(120),
    state varchar(120),
    pincode varchar(6),
    status varchar(32) NOT NULL DEFAULT 'active'
        CONSTRAINT ck_users_status CHECK (status IN ('active', 'disabled')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_login_id_key UNIQUE (login_id),
    CONSTRAINT users_mobile_key UNIQUE (mobile),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_users_login_id ON public.users (login_id);
CREATE UNIQUE INDEX IF NOT EXISTS ix_users_mobile ON public.users (mobile);

CREATE TABLE IF NOT EXISTS public.registrations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    registration_id varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT registrations_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT registrations_user_id_key UNIQUE (user_id),
    CONSTRAINT registrations_registration_id_key UNIQUE (registration_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_registrations_registration_id
    ON public.registrations (registration_id);

CREATE TABLE IF NOT EXISTS public.sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT sessions_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT sessions_token_hash_key UNIQUE (token_hash)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_sessions_token_hash ON public.sessions (token_hash);
CREATE INDEX IF NOT EXISTS ix_sessions_expires_at ON public.sessions (expires_at);
CREATE INDEX IF NOT EXISTS ix_sessions_user_id ON public.sessions (user_id);

CREATE TABLE IF NOT EXISTS public.audit_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid,
    event_type varchar(64) NOT NULL,
    identifier_hash varchar(64),
    created_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb,
    CONSTRAINT audit_events_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_audit_events_user_id ON public.audit_events (user_id);
CREATE INDEX IF NOT EXISTS ix_audit_events_event_type ON public.audit_events (event_type);
CREATE INDEX IF NOT EXISTS ix_audit_events_identifier_hash ON public.audit_events (identifier_hash);
CREATE INDEX IF NOT EXISTS ix_audit_events_created_at ON public.audit_events (created_at);

CREATE TABLE IF NOT EXISTS public.phone_verifications (
    id uuid PRIMARY KEY,
    mobile varchar(10) NOT NULL,
    code_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    consumed_at timestamptz,
    token_hash varchar(64),
    token_expires_at timestamptz,
    token_used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT phone_verifications_token_hash_key UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS ix_phone_verifications_mobile
    ON public.phone_verifications (mobile);

CREATE TABLE IF NOT EXISTS public.rate_limit_buckets (
    key_hash varchar(64) PRIMARY KEY,
    window_start timestamptz NOT NULL,
    hits integer NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_rate_limit_buckets_window_start
    ON public.rate_limit_buckets (window_start);

COMMENT ON TABLE public.users IS 'Participant accounts. PIN hashes are Argon2id; FastAPI owns writes.';
COMMENT ON TABLE public.registrations IS 'One registration row per user. registration_id matches users.login_id.';
COMMENT ON TABLE public.sessions IS 'Opaque FastAPI bearer sessions. Not Supabase auth.sessions.';
COMMENT ON TABLE public.audit_events IS 'Login, logout, registration, and profile-update audit trail.';
COMMENT ON TABLE public.phone_verifications IS 'Hashed mobile OTP challenges and single-use registration tokens.';
COMMENT ON COLUMN public.users.pin_hash IS 'Argon2id hash. Never store a raw PIN.';
COMMENT ON COLUMN public.sessions.token_hash IS 'SHA-256 of the bearer token. The raw token is returned once.';
COMMENT ON TABLE public.rate_limit_buckets IS 'Shared fixed-window throttle counters (RATE_LIMIT_BACKEND=postgres). Keys are SHA-256 hashed.';

CREATE TABLE IF NOT EXISTS public.alembic_version (
    version_num varchar(32) NOT NULL,
    CONSTRAINT alembic_version_pkc PRIMARY KEY (version_num)
);

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.phone_verifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rate_limit_buckets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.alembic_version ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.users FROM anon, authenticated;
REVOKE ALL ON TABLE public.registrations FROM anon, authenticated;
REVOKE ALL ON TABLE public.sessions FROM anon, authenticated;
REVOKE ALL ON TABLE public.audit_events FROM anon, authenticated;
REVOKE ALL ON TABLE public.phone_verifications FROM anon, authenticated;
REVOKE ALL ON TABLE public.rate_limit_buckets FROM anon, authenticated;
REVOKE ALL ON TABLE public.alembic_version FROM anon, authenticated;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.alembic_version) THEN
        RAISE EXCEPTION 'alembic_version is not empty: run alembic upgrade head instead of schema.sql';
    END IF;
    INSERT INTO public.alembic_version (version_num) VALUES ('20261023_0006');
END
$$;
