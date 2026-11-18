"""Districts, Yatras, administrators, stops, and a route foundation.

Revision ID: 20261117_0010
Revises: 20261114_0009
Create Date: 2026-11-17
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "20261117_0010"
down_revision: Union[str, None] = "20261114_0009"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_TABLES = (
    "districts",
    "district_administrators",
    "yatras",
    "yatra_administrators",
    "yatra_stops",
    "yatra_routes",
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
        "districts",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("code", sa.String(length=16), nullable=False),
        sa.Column("state_name", sa.String(length=120), nullable=True),
        sa.Column("active", sa.Boolean(), server_default=sa.text("true"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("code ~ '^[A-Z0-9]{2,16}$'", name="ck_districts_code"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("code"),
    )
    op.create_index("ix_districts_active", "districts", ["active"], unique=False)

    op.create_table(
        "district_administrators",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("district_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("assigned_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["assigned_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["district_id"], ["districts.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("district_id", "user_id", name="uq_district_administrators_district_user"),
    )
    op.create_index("ix_district_administrators_user_id", "district_administrators", ["user_id"], unique=False)

    op.create_table(
        "yatras",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("public_code", sa.String(length=10), nullable=False),
        sa.Column("name", sa.String(length=160), nullable=False),
        sa.Column("description", sa.String(length=4000), nullable=True),
        sa.Column("district_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("starts_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("ends_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("status", sa.String(length=32), server_default="draft", nullable=False),
        sa.Column("published", sa.Boolean(), server_default=sa.text("false"), nullable=False),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint(
            "status IN ('draft', 'planned', 'approval_pending', 'approved', "
            "'upcoming', 'in_progress', 'completed', 'archived')",
            name="ck_yatras_status",
        ),
        sa.CheckConstraint("public_code ~ '^YT[0-9]{8}$'", name="ck_yatras_public_code"),
        sa.CheckConstraint("ends_at >= starts_at", name="ck_yatras_dates"),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["district_id"], ["districts.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("public_code"),
    )
    op.create_index("ix_yatras_district_status", "yatras", ["district_id", "status"], unique=False)
    op.create_index("ix_yatras_published_status", "yatras", ["published", "status"], unique=False)
    op.create_index("ix_yatras_starts_at", "yatras", ["starts_at"], unique=False)

    op.create_table(
        "yatra_administrators",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("assigned_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["assigned_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("yatra_id", "user_id", name="uq_yatra_administrators_yatra_user"),
    )
    op.create_index("ix_yatra_administrators_user_id", "yatra_administrators", ["user_id"], unique=False)

    op.create_table(
        "yatra_stops",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("sequence", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=160), nullable=False),
        sa.Column("address", sa.String(length=255), nullable=True),
        sa.Column("latitude", sa.Numeric(precision=9, scale=6), nullable=True),
        sa.Column("longitude", sa.Numeric(precision=9, scale=6), nullable=True),
        sa.Column("planned_arrival", sa.DateTime(timezone=True), nullable=True),
        sa.Column("planned_departure", sa.DateTime(timezone=True), nullable=True),
        sa.Column("active", sa.Boolean(), server_default=sa.text("true"), nullable=False),
        sa.CheckConstraint("sequence >= 1 AND sequence <= 999", name="ck_yatra_stops_sequence"),
        sa.CheckConstraint(
            "(latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)",
            name="ck_yatra_stops_coords",
        ),
        sa.CheckConstraint(
            "latitude IS NULL OR (latitude >= -90 AND latitude <= 90)",
            name="ck_yatra_stops_latitude",
        ),
        sa.CheckConstraint(
            "longitude IS NULL OR (longitude >= -180 AND longitude <= 180)",
            name="ck_yatra_stops_longitude",
        ),
        sa.CheckConstraint(
            "planned_departure IS NULL OR planned_arrival IS NULL OR planned_departure >= planned_arrival",
            name="ck_yatra_stops_times",
        ),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("yatra_id", "sequence", name="uq_yatra_stops_yatra_sequence"),
    )

    op.create_table(
        "yatra_routes",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("label", sa.String(length=120), nullable=True),
        sa.Column("geometry", sa.Text(), nullable=True),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint(
            "geometry IS NULL OR char_length(geometry) <= 8000",
            name="ck_yatra_routes_geometry",
        ),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("yatra_id"),
    )
    _lock_down(_TABLES)


def downgrade() -> None:
    op.drop_table("yatra_routes")
    op.drop_table("yatra_stops")
    op.drop_index("ix_yatra_administrators_user_id", table_name="yatra_administrators")
    op.drop_table("yatra_administrators")
    op.drop_index("ix_yatras_starts_at", table_name="yatras")
    op.drop_index("ix_yatras_published_status", table_name="yatras")
    op.drop_index("ix_yatras_district_status", table_name="yatras")
    op.drop_table("yatras")
    op.drop_index("ix_district_administrators_user_id", table_name="district_administrators")
    op.drop_table("district_administrators")
    op.drop_index("ix_districts_active", table_name="districts")
    op.drop_table("districts")
