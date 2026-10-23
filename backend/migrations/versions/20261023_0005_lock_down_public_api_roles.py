"""Enable RLS and revoke Supabase API roles on application tables.

Supabase exposes every table in the public schema through PostgREST using the
anon/authenticated roles and, by default, grants those roles full table
privileges. Databases created with ``alembic upgrade head`` (rather than
infra/supabase/schema.sql) therefore exposed users.pin_hash and
sessions.token_hash to anyone holding the public anon key.

RLS with no policies denies those roles; table owners (the FastAPI role that
runs migrations) are unaffected because RLS is not forced. REVOKE is applied
only where the roles exist, so plain PostgreSQL (CI, local) is unaffected.

Revision ID: 20261023_0005
Revises: 20261022_0004
Create Date: 2026-10-23
"""

from typing import Sequence, Union

from alembic import op

revision: str = "20261023_0005"
down_revision: Union[str, None] = "20261022_0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

TABLES = (
    "users",
    "registrations",
    "sessions",
    "audit_events",
    "phone_verifications",
    "alembic_version",
)
API_ROLES = ("anon", "authenticated")


def lock_down(tables: Sequence[str]) -> None:
    for table in tables:
        op.execute(f"ALTER TABLE public.{table} ENABLE ROW LEVEL SECURITY")
    for role in API_ROLES:
        statements = "\n".join(f"EXECUTE 'REVOKE ALL ON TABLE public.{table} FROM {role}';" for table in tables)
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
    lock_down(TABLES)


def downgrade() -> None:
    # Deliberately a no-op: re-exposing PIN and token hashes is never a safe rollback.
    pass
