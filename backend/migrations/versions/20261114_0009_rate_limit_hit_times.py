"""Sliding-window timestamps on rate_limit_buckets.

Revision ID: 20261114_0009
Revises: 20261103_0008
Create Date: 2026-11-14

Existing counters reset once: a fixed-window hit count cannot be turned into
timestamps. In-flight limits start over at deploy.
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "20261114_0009"
down_revision: Union[str, None] = "20261103_0008"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "rate_limit_buckets",
        sa.Column(
            "hit_times",
            postgresql.ARRAY(sa.DateTime(timezone=True)),
            nullable=False,
            server_default=sa.text("'{}'::timestamptz[]"),
        ),
    )


def downgrade() -> None:
    op.drop_column("rate_limit_buckets", "hit_times")
