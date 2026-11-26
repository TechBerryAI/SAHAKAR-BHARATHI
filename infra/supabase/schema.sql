-- Sahakar Bharati participant schema for Supabase (PostgreSQL).
-- Matches Alembic head 20261121_0013 (verification_challenges is not created).
-- backend/tests/test_database_schema.py fails if this file drifts from the models.
--
-- USE ONLY ON AN EMPTY DATABASE. The preferred path is `alembic upgrade head`
-- (see docs/DATABASE.md). This file is an alternative for the Supabase SQL
-- Editor when Alembic cannot reach the database. Apply it with:
--   psql "$MIGRATION_DATABASE_URL" -v ON_ERROR_STOP=1 -f infra/supabase/schema.sql
--
-- It records Alembic revision 20261121_0013, so `alembic upgrade head` works
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
    role varchar(32) NOT NULL DEFAULT 'participant'
        CONSTRAINT ck_users_role CHECK (role IN ('participant', 'admin', 'super_admin')),
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
    hits integer NOT NULL,
    hit_times timestamptz[] NOT NULL DEFAULT '{}'::timestamptz[]
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
COMMENT ON TABLE public.rate_limit_buckets IS 'Shared sliding-window throttle counters (RATE_LIMIT_BACKEND=postgres). Keys are SHA-256 hashed. hit_times holds accepted timestamps.';

CREATE TABLE IF NOT EXISTS public.places (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL,
    code varchar(6) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT places_code_key UNIQUE (code),
    CONSTRAINT ck_places_code CHECK (code ~ '^[0-9]{6}$'),
    CONSTRAINT places_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_places_created_by ON public.places (created_by);

CREATE TABLE IF NOT EXISTS public.attendance (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    place_id uuid NOT NULL,
    checked_in_at timestamptz NOT NULL,
    attendance_date date NOT NULL,
    source varchar(8) NOT NULL,
    login_id varchar(32) NOT NULL,
    full_name varchar(120) NOT NULL,
    mobile varchar(10) NOT NULL,
    email varchar(254),
    gender varchar(16),
    occupation varchar(32),
    address_line varchar(255),
    locality varchar(120),
    district varchar(120),
    state varchar(120),
    pincode varchar(6),
    CONSTRAINT ck_attendance_source CHECK (source IN ('qr', 'code')),
    CONSTRAINT attendance_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT attendance_place_id_fkey
        FOREIGN KEY (place_id) REFERENCES public.places (id),
    CONSTRAINT uq_attendance_user_place_day UNIQUE (user_id, place_id, attendance_date)
);

CREATE INDEX IF NOT EXISTS ix_attendance_user_id ON public.attendance (user_id);
CREATE INDEX IF NOT EXISTS ix_attendance_place_id ON public.attendance (place_id);

COMMENT ON TABLE public.places IS 'Booths and other places. The QR encodes SB-PLACE: plus the 6-digit code.';
COMMENT ON TABLE public.attendance IS 'Check-ins. Profile columns are a snapshot from the moment of the visit.';

CREATE TABLE IF NOT EXISTS public.districts (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL,
    code varchar(16) NOT NULL,
    state_name varchar(120),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT districts_code_key UNIQUE (code),
    CONSTRAINT ck_districts_code CHECK (code ~ '^[A-Z0-9]{2,16}$')
);

CREATE INDEX IF NOT EXISTS ix_districts_active ON public.districts (active);

CREATE TABLE IF NOT EXISTS public.district_administrators (
    id uuid PRIMARY KEY,
    district_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT district_administrators_district_id_fkey
        FOREIGN KEY (district_id) REFERENCES public.districts (id) ON DELETE CASCADE,
    CONSTRAINT district_administrators_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT district_administrators_assigned_by_fkey
        FOREIGN KEY (assigned_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT uq_district_administrators_district_user UNIQUE (district_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_district_administrators_user_id
    ON public.district_administrators (user_id);

CREATE TABLE IF NOT EXISTS public.yatras (
    id uuid PRIMARY KEY,
    public_code varchar(10) NOT NULL,
    name varchar(160) NOT NULL,
    description varchar(4000),
    district_id uuid NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'draft',
    published boolean NOT NULL DEFAULT false,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT yatras_public_code_key UNIQUE (public_code),
    CONSTRAINT ck_yatras_status CHECK (
        status IN (
            'draft', 'planned', 'approval_pending', 'approved',
            'upcoming', 'in_progress', 'completed', 'archived'
        )
    ),
    CONSTRAINT ck_yatras_public_code CHECK (public_code ~ '^YT[0-9]{8}$'),
    CONSTRAINT ck_yatras_dates CHECK (ends_at >= starts_at),
    CONSTRAINT yatras_district_id_fkey
        FOREIGN KEY (district_id) REFERENCES public.districts (id) ON DELETE RESTRICT,
    CONSTRAINT yatras_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_yatras_district_status ON public.yatras (district_id, status);
CREATE INDEX IF NOT EXISTS ix_yatras_published_status ON public.yatras (published, status);
CREATE INDEX IF NOT EXISTS ix_yatras_starts_at ON public.yatras (starts_at);

CREATE TABLE IF NOT EXISTS public.yatra_administrators (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT yatra_administrators_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT yatra_administrators_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT yatra_administrators_assigned_by_fkey
        FOREIGN KEY (assigned_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT uq_yatra_administrators_yatra_user UNIQUE (yatra_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_yatra_administrators_user_id
    ON public.yatra_administrators (user_id);

CREATE TABLE IF NOT EXISTS public.yatra_stops (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    sequence integer NOT NULL,
    name varchar(160) NOT NULL,
    address varchar(255),
    latitude numeric(9, 6),
    longitude numeric(9, 6),
    planned_arrival timestamptz,
    planned_departure timestamptz,
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT yatra_stops_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT uq_yatra_stops_id_yatra UNIQUE (id, yatra_id),
    CONSTRAINT uq_yatra_stops_yatra_sequence UNIQUE (yatra_id, sequence),
    CONSTRAINT ck_yatra_stops_sequence CHECK (sequence >= 1 AND sequence <= 999),
    CONSTRAINT ck_yatra_stops_coords CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    CONSTRAINT ck_yatra_stops_latitude CHECK (
        latitude IS NULL OR (latitude >= -90 AND latitude <= 90)
    ),
    CONSTRAINT ck_yatra_stops_longitude CHECK (
        longitude IS NULL OR (longitude >= -180 AND longitude <= 180)
    ),
    CONSTRAINT ck_yatra_stops_times CHECK (
        planned_departure IS NULL OR planned_arrival IS NULL OR planned_departure >= planned_arrival
    )
);

CREATE TABLE IF NOT EXISTS public.yatra_routes (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    label varchar(120),
    geometry text,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT yatra_routes_yatra_id_key UNIQUE (yatra_id),
    CONSTRAINT yatra_routes_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT ck_yatra_routes_geometry CHECK (geometry IS NULL OR char_length(geometry) <= 8000)
);

CREATE TABLE IF NOT EXISTS public.protocol_templates (
    id uuid PRIMARY KEY,
    category varchar(16) NOT NULL,
    title varchar(160) NOT NULL,
    description varchar(2000),
    required boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT ck_protocol_templates_category CHECK (category IN ('pre', 'during', 'post')),
    CONSTRAINT uq_protocol_templates_category_title UNIQUE (category, title)
);

INSERT INTO public.protocol_templates (id, category, title, description, required, sort_order, active)
VALUES
    ('a0000001-0000-4000-8000-000000000001', 'pre', 'Police Permission', NULL, true, 10, true),
    ('a0000001-0000-4000-8000-000000000002', 'pre', 'Traffic Plan', NULL, true, 20, true),
    ('a0000001-0000-4000-8000-000000000003', 'pre', 'Health Department Approval', NULL, true, 30, true),
    ('a0000001-0000-4000-8000-000000000004', 'pre', 'Fire Safety Clearance', NULL, true, 40, true),
    ('a0000001-0000-4000-8000-000000000005', 'during', 'Crowd Management Plan', NULL, true, 50, true),
    ('a0000001-0000-4000-8000-000000000006', 'during', 'Medical Camps Setup', NULL, true, 60, true),
    ('a0000001-0000-4000-8000-000000000007', 'during', 'Volunteer Deployment', NULL, true, 70, true)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS public.protocol_items (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    template_id uuid,
    category varchar(16) NOT NULL,
    title varchar(160) NOT NULL,
    description varchar(2000),
    required boolean NOT NULL DEFAULT true,
    status varchar(32) NOT NULL DEFAULT 'pending',
    due_at timestamptz,
    responsible_label varchar(120),
    submitted_by uuid,
    submitted_at timestamptz,
    reviewed_by uuid,
    reviewed_at timestamptz,
    rejection_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_protocol_items_category CHECK (category IN ('pre', 'during', 'post')),
    CONSTRAINT ck_protocol_items_status CHECK (
        status IN ('pending', 'submitted', 'under_review', 'approved', 'rejected', 'resubmitted')
    ),
    CONSTRAINT protocol_items_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT protocol_items_template_id_fkey
        FOREIGN KEY (template_id) REFERENCES public.protocol_templates (id) ON DELETE SET NULL,
    CONSTRAINT protocol_items_submitted_by_fkey
        FOREIGN KEY (submitted_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT protocol_items_reviewed_by_fkey
        FOREIGN KEY (reviewed_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_protocol_items_yatra_status ON public.protocol_items (yatra_id, status);
CREATE INDEX IF NOT EXISTS ix_protocol_items_status_due ON public.protocol_items (status, due_at);

CREATE TABLE IF NOT EXISTS public.protocol_documents (
    id uuid PRIMARY KEY,
    item_id uuid NOT NULL,
    storage_key varchar(32) NOT NULL,
    content_type varchar(64) NOT NULL,
    size_bytes integer NOT NULL,
    sha256 varchar(64) NOT NULL,
    original_filename varchar(255) NOT NULL,
    uploaded_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT protocol_documents_storage_key_key UNIQUE (storage_key),
    CONSTRAINT ck_protocol_documents_content_type CHECK (
        content_type IN ('application/pdf', 'image/jpeg', 'image/png')
    ),
    CONSTRAINT ck_protocol_documents_size CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    CONSTRAINT ck_protocol_documents_storage_key CHECK (storage_key ~ '^[0-9a-f]{32}$'),
    CONSTRAINT protocol_documents_item_id_fkey
        FOREIGN KEY (item_id) REFERENCES public.protocol_items (id) ON DELETE CASCADE,
    CONSTRAINT protocol_documents_uploaded_by_fkey
        FOREIGN KEY (uploaded_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_protocol_documents_item_id ON public.protocol_documents (item_id);

COMMENT ON TABLE public.districts IS 'Administrative districts for Rath Yatra scope. Not the free-text users.district field.';
COMMENT ON TABLE public.yatras IS 'Rath Yatra events. public_code is server-generated. Status changes go through the API.';
COMMENT ON TABLE public.yatra_stops IS 'Ordered stops. Live positions are not stored here.';
COMMENT ON TABLE public.yatra_routes IS 'Optional route label and geometry text for a later tracker.';
COMMENT ON TABLE public.protocol_items IS 'Per-Yatra checklist copied from templates, plus items added later.';
COMMENT ON TABLE public.protocol_documents IS 'Document metadata. File bytes are not stored in PostgreSQL.';

CREATE TABLE IF NOT EXISTS public.yatra_counters (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    stop_id uuid,
    name varchar(160) NOT NULL,
    code varchar(16) NOT NULL,
    description varchar(400),
    latitude numeric(9, 6),
    longitude numeric(9, 6),
    location_accuracy_m numeric(7, 1),
    geofence_radius_m integer,
    qr_token varchar(64) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'inactive',
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_yatra_counters_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT ck_yatra_counters_code CHECK (code ~ '^[A-Z0-9][A-Z0-9-]{1,15}$'),
    CONSTRAINT ck_yatra_counters_qr_token CHECK (qr_token ~ '^[A-Za-z0-9_-]{32,64}$'),
    CONSTRAINT ck_yatra_counters_latitude CHECK (
        latitude IS NULL OR (latitude >= -90 AND latitude <= 90)
    ),
    CONSTRAINT ck_yatra_counters_longitude CHECK (
        longitude IS NULL OR (longitude >= -180 AND longitude <= 180)
    ),
    CONSTRAINT ck_yatra_counters_location CHECK (
        (latitude IS NULL AND longitude IS NULL AND location_accuracy_m IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL AND location_accuracy_m IS NOT NULL
            AND location_accuracy_m > 0 AND location_accuracy_m <= 1000)
    ),
    CONSTRAINT ck_yatra_counters_geofence CHECK (
        geofence_radius_m IS NULL OR (geofence_radius_m >= 10 AND geofence_radius_m <= 5000)
    ),
    CONSTRAINT yatra_counters_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT yatra_counters_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT uq_yatra_counters_id_yatra UNIQUE (id, yatra_id),
    CONSTRAINT uq_yatra_counters_qr_token UNIQUE (qr_token),
    CONSTRAINT uq_yatra_counters_yatra_code UNIQUE (yatra_id, code),
    CONSTRAINT yatra_counters_stop_yatra_fkey
        FOREIGN KEY (stop_id, yatra_id) REFERENCES public.yatra_stops (id, yatra_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_yatra_counters_yatra_status
    ON public.yatra_counters (yatra_id, status);

CREATE TABLE IF NOT EXISTS public.yatra_attendance (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    counter_id uuid NOT NULL,
    user_id uuid NOT NULL,
    checked_in_at timestamptz NOT NULL,
    attendance_date date NOT NULL,
    source varchar(8) NOT NULL DEFAULT 'qr',
    scan_latitude numeric(9, 6),
    scan_longitude numeric(9, 6),
    scan_accuracy_m numeric(8, 2),
    scan_distance_m numeric(10, 2),
    CONSTRAINT ck_yatra_attendance_source CHECK (source = 'qr'),
    CONSTRAINT ck_yatra_attendance_scan_present CHECK (
        (scan_latitude IS NULL AND scan_longitude IS NULL AND scan_accuracy_m IS NULL
            AND scan_distance_m IS NULL) OR
        (scan_latitude IS NOT NULL AND scan_longitude IS NOT NULL AND scan_accuracy_m IS NOT NULL
            AND scan_distance_m IS NOT NULL)
    ),
    CONSTRAINT ck_yatra_attendance_scan_latitude CHECK (
        scan_latitude IS NULL OR (scan_latitude >= -90 AND scan_latitude <= 90)
    ),
    CONSTRAINT ck_yatra_attendance_scan_longitude CHECK (
        scan_longitude IS NULL OR (scan_longitude >= -180 AND scan_longitude <= 180)
    ),
    CONSTRAINT ck_yatra_attendance_scan_accuracy CHECK (
        scan_accuracy_m IS NULL OR scan_accuracy_m > 0
    ),
    CONSTRAINT ck_yatra_attendance_scan_distance CHECK (
        scan_distance_m IS NULL OR scan_distance_m >= 0
    ),
    CONSTRAINT yatra_attendance_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT yatra_attendance_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE RESTRICT,
    CONSTRAINT uq_yatra_attendance_user_counter_day UNIQUE (user_id, counter_id, attendance_date),
    CONSTRAINT yatra_attendance_counter_yatra_fkey
        FOREIGN KEY (counter_id, yatra_id) REFERENCES public.yatra_counters (id, yatra_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_yatra_attendance_yatra_checked_in
    ON public.yatra_attendance (yatra_id, checked_in_at, id);
CREATE INDEX IF NOT EXISTS ix_yatra_attendance_user_checked_in
    ON public.yatra_attendance (user_id, checked_in_at, id);
CREATE INDEX IF NOT EXISTS ix_yatra_attendance_counter_date
    ON public.yatra_attendance (counter_id, attendance_date);

CREATE TABLE IF NOT EXISTS public.yatra_trackers (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    label varchar(120) NOT NULL,
    token_hash varchar(64) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'active',
    assigned_by uuid,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    revoked_by uuid,
    revoked_at timestamptz,
    CONSTRAINT ck_yatra_trackers_status CHECK (status IN ('active', 'revoked')),
    CONSTRAINT yatra_trackers_assigned_by_fkey
        FOREIGN KEY (assigned_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT yatra_trackers_revoked_by_fkey
        FOREIGN KEY (revoked_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT yatra_trackers_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT uq_yatra_trackers_token_hash UNIQUE (token_hash),
    CONSTRAINT uq_yatra_trackers_id_yatra UNIQUE (id, yatra_id)
);

CREATE INDEX IF NOT EXISTS ix_yatra_trackers_yatra_status
    ON public.yatra_trackers (yatra_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_yatra_trackers_one_active
    ON public.yatra_trackers (yatra_id)
    WHERE status = 'active';

CREATE TABLE IF NOT EXISTS public.yatra_positions (
    id uuid PRIMARY KEY,
    yatra_id uuid NOT NULL,
    tracker_id uuid NOT NULL,
    stop_id uuid,
    latitude numeric(9, 6) NOT NULL,
    longitude numeric(9, 6) NOT NULL,
    accuracy_m numeric(7, 1) NOT NULL,
    captured_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL,
    source varchar(8) NOT NULL DEFAULT 'gps',
    client_position_id uuid NOT NULL,
    CONSTRAINT ck_yatra_positions_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT ck_yatra_positions_longitude CHECK (longitude >= -180 AND longitude <= 180),
    CONSTRAINT ck_yatra_positions_accuracy CHECK (accuracy_m > 0),
    CONSTRAINT ck_yatra_positions_source CHECK (source = 'gps'),
    CONSTRAINT yatra_positions_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE RESTRICT,
    CONSTRAINT uq_yatra_positions_tracker_client UNIQUE (tracker_id, client_position_id),
    CONSTRAINT yatra_positions_stop_yatra_fkey
        FOREIGN KEY (stop_id, yatra_id) REFERENCES public.yatra_stops (id, yatra_id) ON DELETE RESTRICT,
    CONSTRAINT yatra_positions_tracker_yatra_fkey
        FOREIGN KEY (tracker_id, yatra_id) REFERENCES public.yatra_trackers (id, yatra_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_yatra_positions_yatra_captured
    ON public.yatra_positions (yatra_id, captured_at, id);
CREATE INDEX IF NOT EXISTS ix_yatra_positions_tracker_captured
    ON public.yatra_positions (tracker_id, captured_at, id);
CREATE INDEX IF NOT EXISTS ix_yatra_positions_received_at
    ON public.yatra_positions (received_at);
CREATE INDEX IF NOT EXISTS ix_yatra_positions_stop
    ON public.yatra_positions (stop_id)
    WHERE stop_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.yatra_latest_positions (
    yatra_id uuid PRIMARY KEY,
    position_id uuid NOT NULL,
    tracker_id uuid NOT NULL,
    stop_id uuid,
    latitude numeric(9, 6) NOT NULL,
    longitude numeric(9, 6) NOT NULL,
    accuracy_m numeric(7, 1) NOT NULL,
    captured_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_yatra_latest_positions_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT ck_yatra_latest_positions_longitude CHECK (longitude >= -180 AND longitude <= 180),
    CONSTRAINT ck_yatra_latest_positions_accuracy CHECK (accuracy_m > 0),
    CONSTRAINT yatra_latest_positions_position_id_fkey
        FOREIGN KEY (position_id) REFERENCES public.yatra_positions (id) ON DELETE RESTRICT,
    CONSTRAINT yatra_latest_positions_yatra_id_fkey
        FOREIGN KEY (yatra_id) REFERENCES public.yatras (id) ON DELETE CASCADE,
    CONSTRAINT uq_yatra_latest_positions_position UNIQUE (position_id),
    CONSTRAINT yatra_latest_positions_stop_yatra_fkey
        FOREIGN KEY (stop_id, yatra_id) REFERENCES public.yatra_stops (id, yatra_id) ON DELETE RESTRICT,
    CONSTRAINT yatra_latest_positions_tracker_yatra_fkey
        FOREIGN KEY (tracker_id, yatra_id) REFERENCES public.yatra_trackers (id, yatra_id) ON DELETE RESTRICT
);

COMMENT ON TABLE public.yatra_counters IS 'Yatra counters. qr_token is the posted QR capability, not a 6-digit booth code.';
COMMENT ON TABLE public.yatra_attendance IS 'One Yatra counter check-in per participant per Asia/Kolkata day.';
COMMENT ON TABLE public.yatra_trackers IS 'Tracker assignment. token_hash is SHA-256. The raw bearer is not stored.';
COMMENT ON TABLE public.yatra_positions IS 'Append-only GPS history. received_at is the server clock.';
COMMENT ON TABLE public.yatra_latest_positions IS 'One current pin per Yatra. Not route geometry.';

CREATE OR REPLACE FUNCTION public.prune_yatra_positions(
    retention_days integer,
    batch_size integer DEFAULT 10000
) RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    deleted integer;
BEGIN
    IF retention_days < 1 OR retention_days > 3650 THEN
        RAISE EXCEPTION 'retention_days must be between 1 and 3650';
    END IF;
    IF batch_size < 1 OR batch_size > 50000 THEN
        RAISE EXCEPTION 'batch_size must be between 1 and 50000';
    END IF;
    WITH doomed AS (
        SELECT id
        FROM public.yatra_positions
        WHERE received_at < now() - make_interval(days => retention_days)
          AND NOT EXISTS (
              SELECT 1
              FROM public.yatra_latest_positions
              WHERE yatra_latest_positions.position_id = yatra_positions.id
          )
        ORDER BY received_at
        LIMIT batch_size
    )
    DELETE FROM public.yatra_positions
    WHERE id IN (SELECT id FROM doomed);
    GET DIAGNOSTICS deleted = ROW_COUNT;
    RETURN deleted;
END;
$$;

REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        EXECUTE 'REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) FROM anon';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        EXECUTE 'REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) FROM authenticated';
    END IF;
END
$$;

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
ALTER TABLE public.places ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.districts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.district_administrators ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatras ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_administrators ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_stops ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_routes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protocol_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protocol_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.protocol_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_counters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_trackers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_positions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yatra_latest_positions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.alembic_version ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.users FROM anon, authenticated;
REVOKE ALL ON TABLE public.registrations FROM anon, authenticated;
REVOKE ALL ON TABLE public.sessions FROM anon, authenticated;
REVOKE ALL ON TABLE public.audit_events FROM anon, authenticated;
REVOKE ALL ON TABLE public.phone_verifications FROM anon, authenticated;
REVOKE ALL ON TABLE public.rate_limit_buckets FROM anon, authenticated;
REVOKE ALL ON TABLE public.places FROM anon, authenticated;
REVOKE ALL ON TABLE public.attendance FROM anon, authenticated;
REVOKE ALL ON TABLE public.districts FROM anon, authenticated;
REVOKE ALL ON TABLE public.district_administrators FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatras FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_administrators FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_stops FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_routes FROM anon, authenticated;
REVOKE ALL ON TABLE public.protocol_templates FROM anon, authenticated;
REVOKE ALL ON TABLE public.protocol_items FROM anon, authenticated;
REVOKE ALL ON TABLE public.protocol_documents FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_counters FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_attendance FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_trackers FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_positions FROM anon, authenticated;
REVOKE ALL ON TABLE public.yatra_latest_positions FROM anon, authenticated;
REVOKE ALL ON TABLE public.alembic_version FROM anon, authenticated;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.alembic_version) THEN
        RAISE EXCEPTION 'alembic_version is not empty: run alembic upgrade head instead of schema.sql';
    END IF;
    INSERT INTO public.alembic_version (version_num) VALUES ('20261121_0013');
END
$$;
