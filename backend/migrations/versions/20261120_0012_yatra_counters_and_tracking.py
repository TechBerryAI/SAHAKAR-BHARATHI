"""Yatra counters, attendance, trackers, and GPS positions.

Revision ID: 20261120_0012
Revises: 20261117_0011
Create Date: 2026-11-20
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "20261120_0012"
down_revision: Union[str, None] = "20261117_0011"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_TABLES = (
    "yatra_counters",
    "yatra_attendance",
    "yatra_trackers",
    "yatra_positions",
    "yatra_latest_positions",
)
_API_ROLES = ("anon", "authenticated")


def _lock_down(tables: Sequence[str]) -> None:
    for table in tables:
        op.execute(f"ALTER TABLE public.{table} ENABLE ROW LEVEL SECURITY")
    for role in _API_ROLES:
        statements = "\n".join(
            f"EXECUTE 'REVOKE ALL ON TABLE public.{table} FROM {role}';" for table in tables
        )
        op.execute(
            f"""
            DO $$
            BEGIN
                IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '{role}') THEN
                    {statements}
                END IF;
            END
            $$
            """
        )


def upgrade() -> None:
    op.create_table(
        "yatra_counters",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("stop_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("name", sa.String(length=160), nullable=False),
        sa.Column("code", sa.String(length=16), nullable=False),
        sa.Column("description", sa.String(length=400), nullable=True),
        sa.Column("latitude", sa.Numeric(9, 6), nullable=True),
        sa.Column("longitude", sa.Numeric(9, 6), nullable=True),
        sa.Column("location_accuracy_m", sa.Numeric(7, 1), nullable=True),
        sa.Column("qr_token", sa.String(length=64), nullable=False),
        sa.Column("status", sa.String(length=16), server_default="inactive", nullable=False),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("status IN ('active', 'inactive')", name="ck_yatra_counters_status"),
        sa.CheckConstraint("code ~ '^[A-Z0-9][A-Z0-9-]{1,15}$'", name="ck_yatra_counters_code"),
        sa.CheckConstraint("qr_token ~ '^[A-Za-z0-9_-]{32,64}$'", name="ck_yatra_counters_qr_token"),
        sa.CheckConstraint(
            "latitude IS NULL OR (latitude >= -90 AND latitude <= 90)",
            name="ck_yatra_counters_latitude",
        ),
        sa.CheckConstraint(
            "longitude IS NULL OR (longitude >= -180 AND longitude <= 180)",
            name="ck_yatra_counters_longitude",
        ),
        sa.CheckConstraint(
            "(latitude IS NULL AND longitude IS NULL AND location_accuracy_m IS NULL) OR "
            "(latitude IS NOT NULL AND longitude IS NOT NULL AND location_accuracy_m IS NOT NULL "
            "AND location_accuracy_m > 0 AND location_accuracy_m <= 1000)",
            name="ck_yatra_counters_location",
        ),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["stop_id"], ["yatra_stops.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("qr_token", name="uq_yatra_counters_qr_token"),
        sa.UniqueConstraint("yatra_id", "code", name="uq_yatra_counters_yatra_code"),
    )
    op.create_index("ix_yatra_counters_yatra_status", "yatra_counters", ["yatra_id", "status"], unique=False)

    op.create_table(
        "yatra_attendance",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("counter_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("checked_in_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("attendance_date", sa.Date(), nullable=False),
        sa.Column("source", sa.String(length=8), server_default="qr", nullable=False),
        sa.CheckConstraint("source = 'qr'", name="ck_yatra_attendance_source"),
        sa.ForeignKeyConstraint(["counter_id"], ["yatra_counters.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "counter_id", "attendance_date", name="uq_yatra_attendance_user_counter_day"),
    )
    op.create_index(
        "ix_yatra_attendance_yatra_checked_in",
        "yatra_attendance",
        ["yatra_id", "checked_in_at", "id"],
        unique=False,
    )
    op.create_index(
        "ix_yatra_attendance_user_checked_in",
        "yatra_attendance",
        ["user_id", "checked_in_at", "id"],
        unique=False,
    )

    op.create_table(
        "yatra_trackers",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("label", sa.String(length=120), nullable=False),
        sa.Column("token_hash", sa.String(length=64), nullable=False),
        sa.Column("status", sa.String(length=16), server_default="active", nullable=False),
        sa.Column("assigned_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("assigned_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("revoked_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.CheckConstraint("status IN ('active', 'revoked')", name="ck_yatra_trackers_status"),
        sa.ForeignKeyConstraint(["assigned_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["revoked_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("token_hash", name="uq_yatra_trackers_token_hash"),
    )
    op.create_index("ix_yatra_trackers_yatra_status", "yatra_trackers", ["yatra_id", "status"], unique=False)

    op.create_table(
        "yatra_positions",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("tracker_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("stop_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("latitude", sa.Numeric(9, 6), nullable=False),
        sa.Column("longitude", sa.Numeric(9, 6), nullable=False),
        sa.Column("accuracy_m", sa.Numeric(7, 1), nullable=False),
        sa.Column("captured_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("received_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("source", sa.String(length=8), server_default="gps", nullable=False),
        sa.Column("client_position_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.CheckConstraint("latitude >= -90 AND latitude <= 90", name="ck_yatra_positions_latitude"),
        sa.CheckConstraint("longitude >= -180 AND longitude <= 180", name="ck_yatra_positions_longitude"),
        sa.CheckConstraint("accuracy_m > 0", name="ck_yatra_positions_accuracy"),
        sa.CheckConstraint("source = 'gps'", name="ck_yatra_positions_source"),
        sa.ForeignKeyConstraint(["stop_id"], ["yatra_stops.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["tracker_id"], ["yatra_trackers.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("tracker_id", "client_position_id", name="uq_yatra_positions_tracker_client"),
    )
    op.create_index(
        "ix_yatra_positions_yatra_captured",
        "yatra_positions",
        ["yatra_id", "captured_at", "id"],
        unique=False,
    )
    op.create_index(
        "ix_yatra_positions_tracker_captured",
        "yatra_positions",
        ["tracker_id", "captured_at", "id"],
        unique=False,
    )

    op.create_table(
        "yatra_latest_positions",
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("position_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("tracker_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("stop_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("latitude", sa.Numeric(9, 6), nullable=False),
        sa.Column("longitude", sa.Numeric(9, 6), nullable=False),
        sa.Column("accuracy_m", sa.Numeric(7, 1), nullable=False),
        sa.Column("captured_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("received_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("latitude >= -90 AND latitude <= 90", name="ck_yatra_latest_positions_latitude"),
        sa.CheckConstraint("longitude >= -180 AND longitude <= 180", name="ck_yatra_latest_positions_longitude"),
        sa.CheckConstraint("accuracy_m > 0", name="ck_yatra_latest_positions_accuracy"),
        sa.ForeignKeyConstraint(["position_id"], ["yatra_positions.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["stop_id"], ["yatra_stops.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["tracker_id"], ["yatra_trackers.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("yatra_id"),
        sa.UniqueConstraint("position_id", name="uq_yatra_latest_positions_position"),
    )
    _lock_down(_TABLES)


def downgrade() -> None:
    op.drop_table("yatra_latest_positions")
    op.drop_index("ix_yatra_positions_tracker_captured", table_name="yatra_positions")
    op.drop_index("ix_yatra_positions_yatra_captured", table_name="yatra_positions")
    op.drop_table("yatra_positions")
    op.drop_index("ix_yatra_trackers_yatra_status", table_name="yatra_trackers")
    op.drop_table("yatra_trackers")
    op.drop_index("ix_yatra_attendance_user_checked_in", table_name="yatra_attendance")
    op.drop_index("ix_yatra_attendance_yatra_checked_in", table_name="yatra_attendance")
    op.drop_table("yatra_attendance")
    op.drop_index("ix_yatra_counters_yatra_status", table_name="yatra_counters")
    op.drop_table("yatra_counters")
