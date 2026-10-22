from __future__ import annotations

import os
from collections.abc import Iterator
from pathlib import Path

import pytest
from alembic import command
from alembic.config import Config
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

BACKEND_ROOT = Path(__file__).resolve().parents[1]


def _database_url() -> str | None:
    explicit = os.environ.get("TEST_DATABASE_URL") or os.environ.get("DATABASE_URL")
    if explicit:
        return explicit
    from app.core.config import get_settings

    get_settings.cache_clear()
    settings = get_settings()
    return settings.sqlalchemy_test_url()


@pytest.fixture(scope="session")
def database_url() -> str:
    url = _database_url()
    if not url:
        pytest.skip("PostgreSQL is not configured. Set POSTGRES_* in backend/.env.")
    return url


@pytest.fixture(scope="session")
def engine(database_url: str) -> Iterator[Engine]:
    os.environ["DATABASE_URL"] = database_url
    from app.core.config import get_settings

    get_settings.cache_clear()
    engine = create_engine(database_url, pool_pre_ping=True)
    try:
        with engine.connect() as connection:
            if connection.dialect.name != "postgresql":
                pytest.skip("Integration tests require PostgreSQL.")
    except Exception as exc:
        pytest.skip(f"PostgreSQL is not reachable: {exc}")

    alembic_cfg = Config(str(BACKEND_ROOT / "alembic.ini"))
    alembic_cfg.set_main_option("sqlalchemy.url", database_url)
    command.upgrade(alembic_cfg, "head")
    yield engine
    engine.dispose()


@pytest.fixture(autouse=True)
def clean_tables(engine: Engine) -> Iterator[None]:
    yield
    with engine.begin() as connection:
        connection.execute(
            text(
                "TRUNCATE audit_events, sessions, registrations, users "
                "RESTART IDENTITY CASCADE"
            )
        )


@pytest.fixture
def client(engine: Engine, database_url: str) -> Iterator[TestClient]:
    os.environ["DATABASE_URL"] = database_url
    from app.core.config import get_settings
    from app.core.rate_limit import SlidingWindowLimiter
    from app.db import session as db_session
    from app.main import app

    get_settings.cache_clear()
    db_session.engine.dispose()
    db_session.engine = engine
    db_session.SessionLocal.configure(bind=engine)
    app.state.limiter = SlidingWindowLimiter()
    with TestClient(app) as test_client:
        yield test_client
    app.state.limiter.reset()


def register_payload(**overrides: object) -> dict[str, object]:
    payload: dict[str, object] = {
        "full_name": "Anita Desai",
        "gender": "Female",
        "date_of_birth": "1985-03-12",
        "occupation": "Farmer",
        "mobile": "9876543210",
        "email": "anita@example.com",
        "address_line": "12 Village Road",
        "locality": "Ward 4",
        "district": "Nashik",
        "state": "Maharashtra",
        "pincode": "422001",
        "pin": "2468",
    }
    payload.update(overrides)
    return payload


def register_and_login(
    client: TestClient,
    **overrides: object,
) -> tuple[dict[str, object], dict[str, object]]:
    created = client.post("/api/v1/registrations", json=register_payload(**overrides))
    assert created.status_code == 201, created.text
    body = created.json()
    identifier = overrides.get("mobile", body["login_id"])
    session = client.post(
        "/api/v1/sessions",
        json={"identifier": identifier, "pin": overrides.get("pin", "2468")},
    )
    assert session.status_code == 200, session.text
    return body, session.json()
