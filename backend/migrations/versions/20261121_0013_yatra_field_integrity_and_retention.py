"""Yatra field integrity, geofence, and position retention.

Revision ID: 20261121_0013
Revises: 20261120_0012
Create Date: 2026-11-21

Pre-checks raise and do not change data. Foreign keys are added NOT VALID
and then validated so the lock on existing rows stays short.
"""

from typing import Sequence, Union

from alembic import op

revision: str = "20261121_0013"
down_revision: Union[str, None] = "20261120_0012"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_PRUNE = """
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
$$
"""


def _prechecks() -> None:
    op.execute(
        """
        DO $$
        DECLARE
            duplicate_yatras integer;
        BEGIN
            SELECT count(*) INTO duplicate_yatras
            FROM (
                SELECT yatra_id
                FROM public.yatra_trackers
                WHERE status = 'active'
                GROUP BY yatra_id
                HAVING count(*) > 1
            ) extras;
            IF duplicate_yatras > 0 THEN
                RAISE EXCEPTION
                    'Cannot add uq_yatra_trackers_one_active: % Yatra(s) already have '
                    'more than one active tracker. Revoke the extras before migrating. '
                    'This migration does not revoke them.',
                    duplicate_yatras;
            END IF;
        END
        $$
        """
    )
    op.execute(
        """
        DO $$
        DECLARE
            mismatched integer;
        BEGIN
            SELECT count(*) INTO mismatched
            FROM public.yatra_attendance AS attendance
            JOIN public.yatra_counters AS counter ON counter.id = attendance.counter_id
            WHERE counter.yatra_id <> attendance.yatra_id;
            IF mismatched > 0 THEN
                RAISE EXCEPTION
                    'yatra_attendance has % row(s) whose counter belongs to another Yatra',
                    mismatched;
            END IF;

            SELECT count(*) INTO mismatched
            FROM public.yatra_positions AS position
            JOIN public.yatra_trackers AS tracker ON tracker.id = position.tracker_id
            WHERE tracker.yatra_id <> position.yatra_id;
            IF mismatched > 0 THEN
                RAISE EXCEPTION
                    'yatra_positions has % row(s) whose tracker belongs to another Yatra',
                    mismatched;
            END IF;

            SELECT count(*) INTO mismatched
            FROM public.yatra_latest_positions AS latest
            JOIN public.yatra_trackers AS tracker ON tracker.id = latest.tracker_id
            WHERE tracker.yatra_id <> latest.yatra_id;
            IF mismatched > 0 THEN
                RAISE EXCEPTION
                    'yatra_latest_positions has % row(s) whose tracker belongs to another Yatra',
                    mismatched;
            END IF;

            SELECT count(*) INTO mismatched
            FROM public.yatra_counters AS counter
            JOIN public.yatra_stops AS stop ON stop.id = counter.stop_id
            WHERE counter.stop_id IS NOT NULL AND stop.yatra_id <> counter.yatra_id;
            IF mismatched > 0 THEN
                RAISE EXCEPTION
                    'yatra_counters has % row(s) whose stop belongs to another Yatra',
                    mismatched;
            END IF;

            SELECT count(*) INTO mismatched
            FROM public.yatra_positions AS position
            JOIN public.yatra_stops AS stop ON stop.id = position.stop_id
            WHERE position.stop_id IS NOT NULL AND stop.yatra_id <> position.yatra_id;
            IF mismatched > 0 THEN
                RAISE EXCEPTION
                    'yatra_positions has % row(s) whose stop belongs to another Yatra',
                    mismatched;
            END IF;

            SELECT count(*) INTO mismatched
            FROM public.yatra_latest_positions AS latest
            JOIN public.yatra_stops AS stop ON stop.id = latest.stop_id
            WHERE latest.stop_id IS NOT NULL AND stop.yatra_id <> latest.yatra_id;
            IF mismatched > 0 THEN
                RAISE EXCEPTION
                    'yatra_latest_positions has % row(s) whose stop belongs to another Yatra',
                    mismatched;
            END IF;
        END
        $$
        """
    )


def _composite_fk(table: str, name: str, columns: str, ref_table: str, ref_columns: str) -> None:
    op.execute(
        f"""
        ALTER TABLE public.{table}
            ADD CONSTRAINT {name}
            FOREIGN KEY ({columns}) REFERENCES public.{ref_table} ({ref_columns})
            ON DELETE RESTRICT
            NOT VALID
        """
    )
    op.execute(f"ALTER TABLE public.{table} VALIDATE CONSTRAINT {name}")


def _revoke_prune() -> None:
    op.execute(
        "REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) FROM PUBLIC"
    )
    op.execute(
        """
        DO $$
        BEGIN
            IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
                EXECUTE 'REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) FROM anon';
            END IF;
            IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
                EXECUTE 'REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) FROM authenticated';
            END IF;
        END
        $$
        """
    )


def upgrade() -> None:
    _prechecks()
    op.execute(
        "ALTER TABLE public.yatra_stops "
        "ADD CONSTRAINT uq_yatra_stops_id_yatra UNIQUE (id, yatra_id)"
    )
    op.execute(
        "ALTER TABLE public.yatra_counters "
        "ADD CONSTRAINT uq_yatra_counters_id_yatra UNIQUE (id, yatra_id)"
    )
    op.execute(
        "ALTER TABLE public.yatra_trackers "
        "ADD CONSTRAINT uq_yatra_trackers_id_yatra UNIQUE (id, yatra_id)"
    )

    op.execute("ALTER TABLE public.yatra_counters DROP CONSTRAINT yatra_counters_stop_id_fkey")
    op.execute("ALTER TABLE public.yatra_positions DROP CONSTRAINT yatra_positions_stop_id_fkey")
    op.execute(
        "ALTER TABLE public.yatra_latest_positions DROP CONSTRAINT yatra_latest_positions_stop_id_fkey"
    )
    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT yatra_attendance_counter_id_fkey")
    op.execute("ALTER TABLE public.yatra_positions DROP CONSTRAINT yatra_positions_tracker_id_fkey")
    op.execute(
        "ALTER TABLE public.yatra_latest_positions DROP CONSTRAINT yatra_latest_positions_tracker_id_fkey"
    )
    _composite_fk(
        "yatra_counters",
        "yatra_counters_stop_yatra_fkey",
        "stop_id, yatra_id",
        "yatra_stops",
        "id, yatra_id",
    )
    _composite_fk(
        "yatra_positions",
        "yatra_positions_stop_yatra_fkey",
        "stop_id, yatra_id",
        "yatra_stops",
        "id, yatra_id",
    )
    _composite_fk(
        "yatra_latest_positions",
        "yatra_latest_positions_stop_yatra_fkey",
        "stop_id, yatra_id",
        "yatra_stops",
        "id, yatra_id",
    )
    _composite_fk(
        "yatra_attendance",
        "yatra_attendance_counter_yatra_fkey",
        "counter_id, yatra_id",
        "yatra_counters",
        "id, yatra_id",
    )
    _composite_fk(
        "yatra_positions",
        "yatra_positions_tracker_yatra_fkey",
        "tracker_id, yatra_id",
        "yatra_trackers",
        "id, yatra_id",
    )
    _composite_fk(
        "yatra_latest_positions",
        "yatra_latest_positions_tracker_yatra_fkey",
        "tracker_id, yatra_id",
        "yatra_trackers",
        "id, yatra_id",
    )

    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT yatra_attendance_user_id_fkey")
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT yatra_attendance_user_id_fkey
            FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT
        """
    )

    op.execute(
        """
        CREATE UNIQUE INDEX uq_yatra_trackers_one_active
            ON public.yatra_trackers (yatra_id)
            WHERE status = 'active'
        """
    )
    op.execute(
        "CREATE INDEX ix_yatra_positions_received_at ON public.yatra_positions (received_at)"
    )
    op.execute(
        """
        CREATE INDEX ix_yatra_positions_stop
            ON public.yatra_positions (stop_id)
            WHERE stop_id IS NOT NULL
        """
    )
    op.execute(
        """
        CREATE INDEX ix_yatra_attendance_counter_date
            ON public.yatra_attendance (counter_id, attendance_date)
        """
    )

    op.execute("ALTER TABLE public.yatra_counters ADD COLUMN geofence_radius_m integer")
    op.execute(
        """
        ALTER TABLE public.yatra_counters
            ADD CONSTRAINT ck_yatra_counters_geofence
            CHECK (geofence_radius_m IS NULL OR (geofence_radius_m >= 10 AND geofence_radius_m <= 5000))
        """
    )
    op.execute("ALTER TABLE public.yatra_attendance ADD COLUMN scan_latitude numeric(9, 6)")
    op.execute("ALTER TABLE public.yatra_attendance ADD COLUMN scan_longitude numeric(9, 6)")
    op.execute("ALTER TABLE public.yatra_attendance ADD COLUMN scan_accuracy_m numeric(8, 2)")
    op.execute("ALTER TABLE public.yatra_attendance ADD COLUMN scan_distance_m numeric(10, 2)")
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT ck_yatra_attendance_scan_present CHECK (
                (
                    scan_latitude IS NULL AND scan_longitude IS NULL
                    AND scan_accuracy_m IS NULL AND scan_distance_m IS NULL
                ) OR (
                    scan_latitude IS NOT NULL AND scan_longitude IS NOT NULL
                    AND scan_accuracy_m IS NOT NULL AND scan_distance_m IS NOT NULL
                )
            )
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT ck_yatra_attendance_scan_latitude CHECK (
                scan_latitude IS NULL OR (scan_latitude >= -90 AND scan_latitude <= 90)
            )
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT ck_yatra_attendance_scan_longitude CHECK (
                scan_longitude IS NULL OR (scan_longitude >= -180 AND scan_longitude <= 180)
            )
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT ck_yatra_attendance_scan_accuracy CHECK (
                scan_accuracy_m IS NULL OR scan_accuracy_m > 0
            )
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT ck_yatra_attendance_scan_distance CHECK (
                scan_distance_m IS NULL OR scan_distance_m >= 0
            )
        """
    )

    op.execute(_PRUNE)
    _revoke_prune()


def downgrade() -> None:
    op.execute("DROP FUNCTION IF EXISTS public.prune_yatra_positions(integer, integer)")
    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT IF EXISTS ck_yatra_attendance_scan_distance")
    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT IF EXISTS ck_yatra_attendance_scan_accuracy")
    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT IF EXISTS ck_yatra_attendance_scan_longitude")
    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT IF EXISTS ck_yatra_attendance_scan_latitude")
    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT IF EXISTS ck_yatra_attendance_scan_present")
    op.execute("ALTER TABLE public.yatra_attendance DROP COLUMN IF EXISTS scan_distance_m")
    op.execute("ALTER TABLE public.yatra_attendance DROP COLUMN IF EXISTS scan_accuracy_m")
    op.execute("ALTER TABLE public.yatra_attendance DROP COLUMN IF EXISTS scan_longitude")
    op.execute("ALTER TABLE public.yatra_attendance DROP COLUMN IF EXISTS scan_latitude")
    op.execute("ALTER TABLE public.yatra_counters DROP CONSTRAINT IF EXISTS ck_yatra_counters_geofence")
    op.execute("ALTER TABLE public.yatra_counters DROP COLUMN IF EXISTS geofence_radius_m")
    op.execute("DROP INDEX IF EXISTS public.ix_yatra_attendance_counter_date")
    op.execute("DROP INDEX IF EXISTS public.ix_yatra_positions_stop")
    op.execute("DROP INDEX IF EXISTS public.ix_yatra_positions_received_at")
    op.execute("DROP INDEX IF EXISTS public.uq_yatra_trackers_one_active")

    op.execute("ALTER TABLE public.yatra_attendance DROP CONSTRAINT yatra_attendance_user_id_fkey")
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT yatra_attendance_user_id_fkey
            FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE
        """
    )

    for name, table in (
        ("yatra_latest_positions_tracker_yatra_fkey", "yatra_latest_positions"),
        ("yatra_positions_tracker_yatra_fkey", "yatra_positions"),
        ("yatra_attendance_counter_yatra_fkey", "yatra_attendance"),
        ("yatra_latest_positions_stop_yatra_fkey", "yatra_latest_positions"),
        ("yatra_positions_stop_yatra_fkey", "yatra_positions"),
        ("yatra_counters_stop_yatra_fkey", "yatra_counters"),
    ):
        op.execute(f"ALTER TABLE public.{table} DROP CONSTRAINT {name}")

    op.execute(
        """
        ALTER TABLE public.yatra_counters
            ADD CONSTRAINT yatra_counters_stop_id_fkey
            FOREIGN KEY (stop_id) REFERENCES public.yatra_stops (id) ON DELETE SET NULL
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_positions
            ADD CONSTRAINT yatra_positions_stop_id_fkey
            FOREIGN KEY (stop_id) REFERENCES public.yatra_stops (id) ON DELETE SET NULL
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_latest_positions
            ADD CONSTRAINT yatra_latest_positions_stop_id_fkey
            FOREIGN KEY (stop_id) REFERENCES public.yatra_stops (id) ON DELETE SET NULL
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_attendance
            ADD CONSTRAINT yatra_attendance_counter_id_fkey
            FOREIGN KEY (counter_id) REFERENCES public.yatra_counters (id) ON DELETE RESTRICT
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_positions
            ADD CONSTRAINT yatra_positions_tracker_id_fkey
            FOREIGN KEY (tracker_id) REFERENCES public.yatra_trackers (id) ON DELETE RESTRICT
        """
    )
    op.execute(
        """
        ALTER TABLE public.yatra_latest_positions
            ADD CONSTRAINT yatra_latest_positions_tracker_id_fkey
            FOREIGN KEY (tracker_id) REFERENCES public.yatra_trackers (id) ON DELETE RESTRICT
        """
    )

    op.execute("ALTER TABLE public.yatra_trackers DROP CONSTRAINT uq_yatra_trackers_id_yatra")
    op.execute("ALTER TABLE public.yatra_counters DROP CONSTRAINT uq_yatra_counters_id_yatra")
    op.execute("ALTER TABLE public.yatra_stops DROP CONSTRAINT uq_yatra_stops_id_yatra")
