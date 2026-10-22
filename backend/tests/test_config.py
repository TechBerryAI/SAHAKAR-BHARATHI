from __future__ import annotations

import pytest
from pydantic import ValidationError

from app.core.config import Settings, get_settings, normalize_database_url
from app.main import create_app


def test_normalize_database_url_schemes() -> None:
    assert normalize_database_url("postgres://u:p@host:5432/db").startswith(
        "postgresql+psycopg://"
    )
    assert normalize_database_url("postgresql://u:p@host:5432/db").startswith(
        "postgresql+psycopg://"
    )
    already = "postgresql+psycopg://u:p@host:5432/db"
    assert normalize_database_url(already) == already


def test_production_requires_database_url() -> None:
    with pytest.raises(ValidationError, match="DATABASE_URL is required"):
        Settings(environment="production", database_url="")


def test_production_rejects_localhost_database() -> None:
    with pytest.raises(ValidationError, match="localhost"):
        Settings(
            environment="production",
            database_url="postgresql+psycopg://u:p@localhost:5432/app",
        )


def test_production_rejects_wildcard_cors() -> None:
    with pytest.raises(ValidationError, match="CORS_ORIGINS"):
        Settings(
            environment="production",
            database_url="postgresql+psycopg://u:p@db.example.com:5432/app",
            cors_origins="*",
        )


def test_production_accepts_remote_database_url() -> None:
    settings = Settings(
        environment="production",
        database_url="postgresql://u:p@db.example.com:5432/app",
    )
    assert settings.sqlalchemy_url().startswith("postgresql+psycopg://")
    assert settings.engine_kwargs()["connect_args"]["sslmode"] == "require"


def test_transaction_pooler_disables_prepared_statements() -> None:
    settings = Settings(
        database_url="postgresql://u:p@abc.pooler.supabase.com:6543/postgres"
    )
    kwargs = settings.engine_kwargs()
    assert kwargs["connect_args"]["prepare_threshold"] == 0
    assert kwargs["pool_size"] == 5


def test_alembic_head_is_applied(engine) -> None:
    from sqlalchemy import text

    with engine.connect() as connection:
        version = connection.execute(text("SELECT version_num FROM alembic_version")).scalar_one()
    assert version == "20261022_0004"


def test_production_disables_openapi_docs(monkeypatch) -> None:
    monkeypatch.setenv("ENVIRONMENT", "production")
    monkeypatch.setenv("DATABASE_URL", "postgresql+psycopg://u:p@db.example.com:5432/app")
    monkeypatch.setenv("CORS_ORIGINS", "")
    get_settings.cache_clear()
    try:
        application = create_app()
        paths = {getattr(route, "path", None) for route in application.routes}
        assert "/docs" not in paths
        assert "/redoc" not in paths
        assert "/openapi.json" not in paths
        assert "/" in paths
        assert "/health/live" in paths
        assert "/health/ready" in paths
    finally:
        get_settings.cache_clear()
