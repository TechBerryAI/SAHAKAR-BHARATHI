"""Booths and the visits recorded when a participant checks in.

Revision ID: 20261103_0008
Revises: 20261026_0007
Create Date: 2026-11-03
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "20261103_0008"
down_revision: Union[str, None] = "20261026_0007"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_TABLES = ("places", "attendance")
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
        "places",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("code", sa.String(length=6), nullable=False),
        sa.Column("active", sa.Boolean(), server_default=sa.text("true"), nullable=False),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("code ~ '^[0-9]{6}$'", name="ck_places_code"),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("code"),
    )
    op.create_index("ix_places_created_by", "places", ["created_by"], unique=False)

    op.create_table(
        "attendance",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("place_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("checked_in_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("attendance_date", sa.Date(), nullable=False),
        sa.Column("source", sa.String(length=8), nullable=False),
        sa.Column("login_id", sa.String(length=32), nullable=False),
        sa.Column("full_name", sa.String(length=120), nullable=False),
        sa.Column("mobile", sa.String(length=10), nullable=False),
        sa.Column("email", sa.String(length=254), nullable=True),
        sa.Column("gender", sa.String(length=16), nullable=True),
        sa.Column("occupation", sa.String(length=32), nullable=True),
        sa.Column("address_line", sa.String(length=255), nullable=True),
        sa.Column("locality", sa.String(length=120), nullable=True),
        sa.Column("district", sa.String(length=120), nullable=True),
        sa.Column("state", sa.String(length=120), nullable=True),
        sa.Column("pincode", sa.String(length=6), nullable=True),
        sa.CheckConstraint("source IN ('qr', 'code')", name="ck_attendance_source"),
        sa.ForeignKeyConstraint(["place_id"], ["places.id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "place_id", "attendance_date", name="uq_attendance_user_place_day"),
    )
    op.create_index("ix_attendance_place_id", "attendance", ["place_id"], unique=False)
    op.create_index("ix_attendance_user_id", "attendance", ["user_id"], unique=False)
    _lock_down(_TABLES)


def downgrade() -> None:
    op.drop_index("ix_attendance_user_id", table_name="attendance")
    op.drop_index("ix_attendance_place_id", table_name="attendance")
    op.drop_table("attendance")
    op.drop_index("ix_places_created_by", table_name="places")
    op.drop_table("places")
