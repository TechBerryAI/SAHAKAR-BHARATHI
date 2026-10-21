"""Drop unused verification_challenges table.

Revision ID: 20261021_0002
Revises: 20261021_0001
Create Date: 2026-10-21
"""

from typing import Sequence, Union

from alembic import op

revision: str = "20261021_0002"
down_revision: Union[str, None] = "20261021_0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute("DROP TABLE IF EXISTS verification_challenges")


def downgrade() -> None:
    pass
