"""Add users.status server default and allowed-value check.

Revision ID: 20261022_0003
Revises: 20261021_0002
Create Date: 2026-10-22
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "20261022_0003"
down_revision: Union[str, None] = "20261021_0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.alter_column(
        "users",
        "status",
        existing_type=sa.String(length=32),
        server_default="active",
        existing_nullable=False,
    )
    op.create_check_constraint(
        "ck_users_status",
        "users",
        "status IN ('active', 'disabled')",
    )


def downgrade() -> None:
    op.drop_constraint("ck_users_status", "users", type_="check")
    op.alter_column(
        "users",
        "status",
        existing_type=sa.String(length=32),
        server_default=None,
        existing_nullable=False,
    )
