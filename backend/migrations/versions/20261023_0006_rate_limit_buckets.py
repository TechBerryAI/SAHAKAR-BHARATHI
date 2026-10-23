"""Shared rate-limit counters for RATE_LIMIT_BACKEND=postgres.

Revision ID: 20261023_0006
Revises: 20261023_0005
Create Date: 2026-10-23
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "20261023_0006"
down_revision: Union[str, None] = "20261023_0005"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    bind = op.get_bind()
    if "rate_limit_buckets" not in sa.inspect(bind).get_table_names():
        op.create_table(
            "rate_limit_buckets",
            sa.Column("key_hash", sa.String(length=64), nullable=False),
            sa.Column("window_start", sa.DateTime(timezone=True), nullable=False),
            sa.Column("hits", sa.Integer(), nullable=False),
            sa.PrimaryKeyConstraint("key_hash"),
        )
        op.create_index("ix_rate_limit_buckets_window_start", "rate_limit_buckets", ["window_start"])
    # Same lock-down as 20261023_0005: keep the table off the Supabase public API.
    op.execute("ALTER TABLE public.rate_limit_buckets ENABLE ROW LEVEL SECURITY")
    op.execute(
        """
        DO $$
        BEGIN
            IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
                EXECUTE 'REVOKE ALL ON TABLE public.rate_limit_buckets FROM anon';
            END IF;
            IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
                EXECUTE 'REVOKE ALL ON TABLE public.rate_limit_buckets FROM authenticated';
            END IF;
        END
        $$
        """
    )


def downgrade() -> None:
    op.drop_index("ix_rate_limit_buckets_window_start", table_name="rate_limit_buckets")
    op.drop_table("rate_limit_buckets")
