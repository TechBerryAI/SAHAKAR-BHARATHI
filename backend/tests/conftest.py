from __future__ import annotations

import os
import re
from collections.abc import Iterator
from pathlib import Path
from urllib.parse import urlparse

import pytest
from alembic import command
from alembic.config import Config
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

from app.core.config import normalize_database_url

BACKEND_ROOT = Path(__file__).resolve().parents[1]


def _database_name(url: str) -> str:
    parsed = urlparse(normalize_database_url(url))
    return (parsed.path or "").lstrip("/").split("?")[0].lower()


def _database_url() -> str | None:
    explicit = os.environ.get("TEST_DATABASE_URL", "").strip()
    from app.core.config import get_settings

    get_settings.cache_clear()
    settings = get_settings()
    url = explicit or settings.sqlalchemy_test_url()
    if not url:
        return None
    if "test" not in _database_name(url):
        pytest.skip(
            "Refusing to run tests against a database whose name does not contain 'test'. "
            "Set TEST_DATABASE_URL or POSTGRES_TEST_DB."
        )
    production = os.environ.get("DATABASE_URL", "").strip()
    if (
        production
        and normalize_database_url(production) == normalize_database_url(url)
        and "test" not in _database_name(production)
    ):
        pytest.skip("TEST_DATABASE_URL must not be the same as DATABASE_URL.")
    return url


@pytest.fixture(scope="session")
def database_url() -> str:
    url = _database_url()
    if not url:
        pytest.skip("PostgreSQL is not configured. Set TEST_DATABASE_URL or POSTGRES_* in backend/.env.")
    return url


@pytest.fixture(scope="session")
def engine(database_url: str) -> Iterator[Engine]:
    os.environ["DATABASE_URL"] = database_url
    # migrations/env.py prefers MIGRATION_DATABASE_URL; never let a developer's
    # exported owner credential point test migrations at a real database.
    os.environ["MIGRATION_DATABASE_URL"] = database_url
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
    alembic_cfg.set_main_option("sqlalchemy.url", database_url.replace("%", "%%"))
    command.upgrade(alembic_cfg, "head")
    yield engine
    engine.dispose()


@pytest.fixture(autouse=True)
def clean_tables(request: pytest.FixtureRequest) -> Iterator[None]:
    if "engine" not in request.fixturenames and "client" not in request.fixturenames:
        yield
        return
    engine: Engine = request.getfixturevalue("engine")
    yield
    with engine.begin() as connection:
        connection.execute(
            text(
                "TRUNCATE audit_events, sessions, registrations, users, phone_verifications, rate_limit_buckets "
                "RESTART IDENTITY CASCADE"
            )
        )


class RecordingSmsGateway:
    def __init__(self) -> None:
        self.messages: list[tuple[str, str]] = []
        self.fail_waits = 0
        self.wait_calls = 0

    def send(self, recipient_e164: str, message: str) -> None:
        self.messages.append((recipient_e164, message))

    def wait_until_sent(self, timeout: float = 20.0) -> bool:
        self.wait_calls += 1
        return self.wait_calls > self.fail_waits

    def latest_code(self) -> str:
        for _recipient, message in reversed(self.messages):
            match = re.search(r"\b(\d{6})\b", message)
            if match is not None:
                return match.group(1)
        raise AssertionError("verification SMS did not contain a code")


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
    app.state.sms_gateway = RecordingSmsGateway()
    with TestClient(app) as test_client:
        yield test_client
    app.state.sms_gateway = None
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


def verify_mobile(client: TestClient, mobile: str) -> str:
    sent = client.post("/api/v1/phone-verifications", json={"mobile": mobile})
    assert sent.status_code == 202, sent.text
    code = client.app.state.sms_gateway.latest_code()
    confirmed = client.post(
        f"/api/v1/phone-verifications/{sent.json()['id']}/confirm",
        json={"code": code},
    )
    assert confirmed.status_code == 200, confirmed.text
    token = confirmed.json()["verification_token"]
    assert isinstance(token, str)
    return token


def verified_register_payload(client: TestClient, **overrides: object) -> dict[str, object]:
    payload = register_payload(**overrides)
    payload["verification_token"] = verify_mobile(client, str(payload["mobile"]))
    return payload


def register_and_login(
    client: TestClient,
    **overrides: object,
) -> tuple[dict[str, object], dict[str, object]]:
    created = client.post("/api/v1/registrations", json=verified_register_payload(client, **overrides))
    assert created.status_code == 201, created.text
    body = created.json()
    identifier = overrides.get("mobile", body["login_id"])
    session = client.post(
        "/api/v1/sessions",
        json={"identifier": identifier, "pin": overrides.get("pin", "2468")},
    )
    assert session.status_code == 200, session.text
    return body, session.json()
