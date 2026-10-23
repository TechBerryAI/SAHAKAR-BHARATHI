"""Fresh-database checks: migrations, schema.sql parity, and Supabase API-role lockdown.

Each test creates its own throwaway database (name contains "test") on the same
server as TEST_DATABASE_URL and drops it afterwards. The Supabase roles anon and
authenticated are simulated as NOLOGIN roles. Tests skip when the test role
cannot create databases or roles.
"""

from __future__ import annotations

import os
import uuid
from collections.abc import Iterator
from pathlib import Path

import psycopg
import pytest
from alembic import command
from alembic.autogenerate import compare_metadata
from alembic.config import Config
from alembic.migration import MigrationContext
from alembic.script import ScriptDirectory
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine, make_url
from sqlalchemy.exc import DBAPIError, ProgrammingError

from app.db.base import Base
from app.models import AuditEvent  # noqa: F401  (registers every model on Base.metadata)

BACKEND_ROOT = Path(__file__).resolve().parents[1]
SCHEMA_SQL = BACKEND_ROOT.parent / "infra" / "supabase" / "schema.sql"
RUNTIME_ROLE_SQL = BACKEND_ROOT.parent / "infra" / "supabase" / "runtime_role.sql"
API_ROLES = ("anon", "authenticated")
APP_TABLES = (
    "users",
    "registrations",
    "sessions",
    "audit_events",
    "phone_verifications",
    "rate_limit_buckets",
)


def _alembic_config(url: str) -> Config:
    config = Config(str(BACKEND_ROOT / "alembic.ini"))
    config.set_main_option("script_location", str(BACKEND_ROOT / "migrations"))
    config.set_main_option("sqlalchemy.url", url.replace("%", "%%"))
    return config


def _head_revision() -> str:
    return ScriptDirectory.from_config(_alembic_config("postgresql+psycopg://unused/x")).get_current_head()


@pytest.fixture
def admin_engine(database_url: str) -> Iterator[Engine]:
    engine = create_engine(database_url, isolation_level="AUTOCOMMIT")
    yield engine
    engine.dispose()


@pytest.fixture
def fresh_database(admin_engine: Engine, database_url: str, monkeypatch) -> Iterator[str]:
    name = f"sahakar_fresh_test_{uuid.uuid4().hex[:10]}"
    try:
        with admin_engine.connect() as connection:
            connection.execute(text(f'CREATE DATABASE "{name}"'))
            for role in API_ROLES:
                connection.execute(
                    text(
                        f"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '{role}') "
                        f"THEN CREATE ROLE {role} NOLOGIN; END IF; END $$"
                    )
                )
    except DBAPIError as exc:
        pytest.skip(f"Test role cannot create databases/roles: {exc.orig}")
    url = make_url(database_url).set(database=name).render_as_string(hide_password=False)
    # migrations/env.py reads the URL from the environment, not alembic.ini.
    monkeypatch.setenv("MIGRATION_DATABASE_URL", url)
    try:
        yield url
    finally:
        with admin_engine.connect() as connection:
            connection.execute(text(f'DROP DATABASE IF EXISTS "{name}" WITH (FORCE)'))


def _run_sql_file(engine: Engine, path: Path) -> None:
    # Raw driver call without parameters, so format('%I') in the SQL is not read as a placeholder.
    with engine.begin() as connection:
        connection.connection.driver_connection.execute(path.read_text(encoding="utf-8"))


def _simulate_supabase_defaults(engine: Engine) -> None:
    """Supabase grants anon/authenticated full privileges on new public tables."""
    with engine.begin() as connection:
        connection.execute(text("CREATE SCHEMA IF NOT EXISTS extensions"))
        connection.execute(text("CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions"))
        connection.execute(text("GRANT USAGE ON SCHEMA public TO anon, authenticated"))
        connection.execute(
            text("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon, authenticated")
        )


def _schema_diff(engine: Engine) -> list[object]:
    with engine.connect() as connection:
        context = MigrationContext.configure(connection, opts={"compare_type": True})
        return compare_metadata(context, Base.metadata)


def _assert_api_roles_locked_out(engine: Engine) -> None:
    with engine.connect() as connection:
        rls = dict(
            connection.execute(
                text("SELECT relname, relrowsecurity FROM pg_class WHERE relname = ANY(:names)"),
                {"names": [*APP_TABLES, "alembic_version"]},
            ).all()
        )
        assert set(rls) == {*APP_TABLES, "alembic_version"}
        assert all(rls.values()), rls
        for role in API_ROLES:
            for table in (*APP_TABLES, "alembic_version"):
                allowed = connection.execute(
                    text("SELECT has_table_privilege(:role, :table, 'SELECT,INSERT,UPDATE,DELETE')"),
                    {"role": role, "table": f"public.{table}"},
                ).scalar_one()
                assert allowed is False, f"{role} can access {table}"
        connection.execute(text("SET ROLE anon"))
        with pytest.raises(ProgrammingError, match="permission denied"):
            connection.execute(text("SELECT pin_hash FROM public.users"))
        connection.rollback()


# Both init paths create a UNIQUE constraint *and* a unique index on these columns,
# while the models declare only the unique index. Uniqueness is enforced either way;
# the duplicate index is left alone rather than altering the production schema.
_KNOWN_REDUNDANT_UNIQUE = {
    ("registrations", "registration_id"),
    ("sessions", "token_hash"),
    ("users", "login_id"),
    ("users", "mobile"),
}


def _relevant(diff: list[object]) -> list[object]:
    relevant = []
    for item in diff:
        if isinstance(item, list):  # column-level changes come wrapped in a list
            item = item[0]
        kind = item[0]
        if kind in {"remove_table_comment", "add_table_comment", "modify_comment"}:
            continue  # schema.sql documents tables with COMMENT ON; models carry no comments
        if kind == "remove_constraint":
            columns = tuple(item[1].columns)
            if len(columns) == 1 and (columns[0].table.name, columns[0].name) in _KNOWN_REDUNDANT_UNIQUE:
                continue
        relevant.append(item)
    return relevant


def test_migrations_build_the_model_schema_from_scratch(fresh_database: str) -> None:
    command.upgrade(_alembic_config(fresh_database), "head")
    engine = create_engine(fresh_database)
    try:
        assert _relevant(_schema_diff(engine)) == []
        with engine.connect() as connection:
            version = connection.execute(text("SELECT version_num FROM alembic_version")).scalar_one()
        assert version == _head_revision()
    finally:
        engine.dispose()


def test_migrations_lock_supabase_api_roles_out(fresh_database: str) -> None:
    engine = create_engine(fresh_database)
    try:
        # Grants arrive with every CREATE TABLE, as on Supabase; the migrations must revoke them.
        _simulate_supabase_defaults(engine)
        command.upgrade(_alembic_config(fresh_database), "head")
        _assert_api_roles_locked_out(engine)
    finally:
        engine.dispose()


def test_upgrade_from_previous_release_revision(fresh_database: str) -> None:
    config = _alembic_config(fresh_database)
    engine = create_engine(fresh_database)
    try:
        _simulate_supabase_defaults(engine)
        command.upgrade(config, "20261022_0004")
        with engine.connect() as connection:
            # Reproduces the pre-0005 exposure: the public API role could read PIN hashes.
            exposed = connection.execute(
                text("SELECT has_table_privilege('anon', 'public.users', 'SELECT')")
            ).scalar_one()
        assert exposed is True
        with engine.begin() as connection:
            connection.execute(
                text(
                    "INSERT INTO users (id, login_id, mobile, pin_hash, full_name) "
                    "VALUES (gen_random_uuid(), 'SB-00000001', '9876543210', 'x', 'Existing User')"
                )
            )
        command.upgrade(config, "head")
        with engine.connect() as connection:
            assert connection.execute(text("SELECT full_name FROM users")).scalar_one() == "Existing User"
        _assert_api_roles_locked_out(engine)
    finally:
        engine.dispose()


def test_schema_sql_matches_models_and_alembic_head(fresh_database: str) -> None:
    engine = create_engine(fresh_database)
    try:
        _simulate_supabase_defaults(engine)
        _run_sql_file(engine, SCHEMA_SQL)
        assert _relevant(_schema_diff(engine)) == []
        with engine.connect() as connection:
            version = connection.execute(text("SELECT version_num FROM alembic_version")).scalar_one()
        assert version == _head_revision()
        _assert_api_roles_locked_out(engine)
        # Alembic accepts the stamped database and has nothing left to apply.
        command.upgrade(_alembic_config(fresh_database), "head")
    finally:
        engine.dispose()


def test_schema_sql_refuses_a_database_already_managed_by_alembic(fresh_database: str) -> None:
    engine = create_engine(fresh_database)
    try:
        _simulate_supabase_defaults(engine)
        command.upgrade(_alembic_config(fresh_database), "head")
        with pytest.raises((DBAPIError, psycopg.Error), match="alembic_version is not empty"):
            _run_sql_file(engine, SCHEMA_SQL)
        with engine.connect() as connection:
            assert connection.execute(text("SELECT count(*) FROM alembic_version")).scalar_one() == 1
    finally:
        engine.dispose()


def test_runtime_role_can_run_the_api_but_not_change_schema(fresh_database: str, admin_engine: Engine) -> None:
    command.upgrade(_alembic_config(fresh_database), "head")
    engine = create_engine(fresh_database)
    password = uuid.uuid4().hex
    try:
        _run_sql_file(engine, RUNTIME_ROLE_SQL)
        with engine.begin() as connection:
            connection.execute(text(f"ALTER ROLE sahakar_api WITH LOGIN PASSWORD '{password}'"))
        runtime_url = make_url(fresh_database).set(username="sahakar_api", password=password)
        runtime_url = runtime_url.render_as_string(hide_password=False)
        runtime = create_engine(runtime_url)
        try:
            with runtime.begin() as connection:
                user_id = connection.execute(
                    text(
                        "INSERT INTO users (id, login_id, mobile, pin_hash, full_name) "
                        "VALUES (gen_random_uuid(), 'SB-00000002', '9123456780', 'x', 'Runtime') RETURNING id"
                    )
                ).scalar_one()
                connection.execute(
                    text(
                        "INSERT INTO sessions (id, user_id, token_hash, expires_at) "
                        "VALUES (gen_random_uuid(), :uid, :hash, now() + interval '1 day')"
                    ),
                    {"uid": user_id, "hash": "a" * 64},
                )
                connection.execute(text("UPDATE sessions SET revoked_at = now()"))
                connection.execute(text("DELETE FROM rate_limit_buckets"))
            with runtime.connect() as connection:
                for statement in (
                    "DROP TABLE users",
                    "CREATE TABLE public.intruder (id int)",
                    "SELECT * FROM alembic_version",
                    "DELETE FROM users",
                ):
                    with pytest.raises(ProgrammingError):
                        connection.execute(text(statement))
                    connection.rollback()
        finally:
            runtime.dispose()
    finally:
        engine.dispose()
        with admin_engine.connect() as connection:
            connection.execute(
                text(
                    "DO $$ BEGIN IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sahakar_api') "
                    "THEN ALTER ROLE sahakar_api NOLOGIN; END IF; END $$"
                )
            )


def test_every_public_table_has_row_level_security(engine: Engine) -> None:
    with engine.connect() as connection:
        exposed = connection.execute(
            text(
                "SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                "WHERE n.nspname = 'public' AND c.relkind = 'r' AND NOT c.relrowsecurity"
            )
        ).scalars().all()
    assert exposed == [], f"Tables without RLS would be readable through the Supabase API: {exposed}"


def test_migration_url_override_is_scoped_to_tests() -> None:
    # conftest pins MIGRATION_DATABASE_URL to the test database for the whole session.
    assert "test" in (os.environ.get("MIGRATION_DATABASE_URL") or "test")
