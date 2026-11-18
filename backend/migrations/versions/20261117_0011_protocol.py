"""Protocol templates, checklist items, and document metadata.

Revision ID: 20261117_0011
Revises: 20261117_0010
Create Date: 2026-11-17
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "20261117_0011"
down_revision: Union[str, None] = "20261117_0010"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_TABLES = ("protocol_templates", "protocol_items", "protocol_documents")
_API_ROLES = ("anon", "authenticated")

_SEED_TEMPLATES = (
    ("a0000001-0000-4000-8000-000000000001", "pre", "Police Permission", 10),
    ("a0000001-0000-4000-8000-000000000002", "pre", "Traffic Plan", 20),
    ("a0000001-0000-4000-8000-000000000003", "pre", "Health Department Approval", 30),
    ("a0000001-0000-4000-8000-000000000004", "pre", "Fire Safety Clearance", 40),
    ("a0000001-0000-4000-8000-000000000005", "during", "Crowd Management Plan", 50),
    ("a0000001-0000-4000-8000-000000000006", "during", "Medical Camps Setup", 60),
    ("a0000001-0000-4000-8000-000000000007", "during", "Volunteer Deployment", 70),
)


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
        "protocol_templates",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("category", sa.String(length=16), nullable=False),
        sa.Column("title", sa.String(length=160), nullable=False),
        sa.Column("description", sa.String(length=2000), nullable=True),
        sa.Column("required", sa.Boolean(), server_default=sa.text("true"), nullable=False),
        sa.Column("sort_order", sa.Integer(), server_default=sa.text("0"), nullable=False),
        sa.Column("active", sa.Boolean(), server_default=sa.text("true"), nullable=False),
        sa.CheckConstraint("category IN ('pre', 'during', 'post')", name="ck_protocol_templates_category"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("category", "title", name="uq_protocol_templates_category_title"),
    )
    op.execute(
        """
        INSERT INTO protocol_templates (id, category, title, description, required, sort_order, active)
        VALUES
        """
        + ",\n".join(
            f"('{item_id}', '{category}', '{title}', NULL, true, {sort_order}, true)"
            for item_id, category, title, sort_order in _SEED_TEMPLATES
        )
    )

    op.create_table(
        "protocol_items",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("yatra_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("template_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("category", sa.String(length=16), nullable=False),
        sa.Column("title", sa.String(length=160), nullable=False),
        sa.Column("description", sa.String(length=2000), nullable=True),
        sa.Column("required", sa.Boolean(), server_default=sa.text("true"), nullable=False),
        sa.Column("status", sa.String(length=32), server_default="pending", nullable=False),
        sa.Column("due_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("responsible_label", sa.String(length=120), nullable=True),
        sa.Column("submitted_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("submitted_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("reviewed_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.String(length=500), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("category IN ('pre', 'during', 'post')", name="ck_protocol_items_category"),
        sa.CheckConstraint(
            "status IN ('pending', 'submitted', 'under_review', 'approved', 'rejected', 'resubmitted')",
            name="ck_protocol_items_status",
        ),
        sa.ForeignKeyConstraint(["reviewed_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["submitted_by"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["template_id"], ["protocol_templates.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["yatra_id"], ["yatras.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_protocol_items_yatra_status", "protocol_items", ["yatra_id", "status"], unique=False)
    op.create_index("ix_protocol_items_status_due", "protocol_items", ["status", "due_at"], unique=False)

    op.create_table(
        "protocol_documents",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("item_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("storage_key", sa.String(length=32), nullable=False),
        sa.Column("content_type", sa.String(length=64), nullable=False),
        sa.Column("size_bytes", sa.Integer(), nullable=False),
        sa.Column("sha256", sa.String(length=64), nullable=False),
        sa.Column("original_filename", sa.String(length=255), nullable=False),
        sa.Column("uploaded_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint(
            "content_type IN ('application/pdf', 'image/jpeg', 'image/png')",
            name="ck_protocol_documents_content_type",
        ),
        sa.CheckConstraint("size_bytes > 0 AND size_bytes <= 5242880", name="ck_protocol_documents_size"),
        sa.CheckConstraint("storage_key ~ '^[0-9a-f]{32}$'", name="ck_protocol_documents_storage_key"),
        sa.ForeignKeyConstraint(["item_id"], ["protocol_items.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["uploaded_by"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("storage_key"),
    )
    op.create_index("ix_protocol_documents_item_id", "protocol_documents", ["item_id"], unique=False)
    _lock_down(_TABLES)


def downgrade() -> None:
    op.drop_index("ix_protocol_documents_item_id", table_name="protocol_documents")
    op.drop_table("protocol_documents")
    op.drop_index("ix_protocol_items_status_due", table_name="protocol_items")
    op.drop_index("ix_protocol_items_yatra_status", table_name="protocol_items")
    op.drop_table("protocol_items")
    op.drop_table("protocol_templates")
