"""Add users.role, separate from the active/disabled status flag.

Revision ID: 20261026_0007
Revises: 20261023_0006
Create Date: 2026-10-26
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "20261026_0007"
down_revision: Union[str, None] = "20261023_0006"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("role", sa.String(length=32), nullable=False, server_default="participant"),
    )
    op.create_check_constraint(
        "ck_users_role",
        "users",
        "role IN ('participant', 'admin', 'super_admin')",
    )


def downgrade() -> None:
    op.drop_constraint("ck_users_role", "users", type_="check")
    op.drop_column("users", "role")
