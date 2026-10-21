from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_and_login, register_payload


def test_login_rate_limit_returns_429(client: TestClient) -> None:
    client.post("/api/v1/registrations", json=register_payload())
    last = None
    for _ in range(6):
        last = client.post(
            "/api/v1/sessions",
            json={"identifier": "9876543210", "pin": "0000"},
        )
    assert last is not None
    assert last.status_code == 429
    assert "Too many attempts" in last.json()["detail"]
    assert last.headers.get("retry-after")


def test_login_lockout_after_repeated_failures(
    client: TestClient,
    monkeypatch,
) -> None:
    monkeypatch.setenv("LOGIN_LOCKOUT_FAILURES", "2")
    monkeypatch.setenv("LOGIN_RATE_LIMIT", "20")
    from app.core.config import get_settings

    get_settings.cache_clear()
    client.post("/api/v1/registrations", json=register_payload())
    first = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "0000"})
    second = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "1111"})
    third = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "2468"})
    assert first.status_code == 401
    assert second.status_code == 401
    assert third.status_code == 429


def test_login_normalizes_mobile_variants_for_lockout(
    client: TestClient,
    engine: Engine,
) -> None:
    client.post("/api/v1/registrations", json=register_payload())
    client.post("/api/v1/sessions", json={"identifier": "+91 98765 43210", "pin": "0000"})
    client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "1111"})
    with engine.connect() as connection:
        hashes = connection.execute(
            text(
                "SELECT DISTINCT identifier_hash FROM audit_events "
                "WHERE event_type = 'login_failure'"
            )
        ).all()
    assert len(hashes) == 1


def test_login_accepts_plus_ninety_one_mobile(client: TestClient) -> None:
    created, _session = register_and_login(client)
    response = client.post(
        "/api/v1/sessions",
        json={"identifier": "+919876543210", "pin": "2468"},
    )
    assert response.status_code == 200
    assert response.json()["login_id"] == created["login_id"]


def test_inactive_user_cannot_login_or_use_session(
    client: TestClient,
    engine: Engine,
) -> None:
    _created, session = register_and_login(client)
    with engine.begin() as connection:
        connection.execute(text("UPDATE users SET status = 'disabled'"))
    login = client.post(
        "/api/v1/sessions",
        json={"identifier": "9876543210", "pin": "2468"},
    )
    assert login.status_code == 401
    me = client.get(
        "/api/v1/me",
        headers={"Authorization": f"Bearer {session['access_token']}"},
    )
    assert me.status_code == 401


def test_register_rate_limit_returns_429(client: TestClient, monkeypatch) -> None:
    monkeypatch.setenv("REGISTER_RATE_LIMIT", "1")
    from app.core.config import get_settings

    get_settings.cache_clear()
    first = client.post("/api/v1/registrations", json=register_payload())
    second = client.post(
        "/api/v1/registrations",
        json=register_payload(mobile="9123456789", email="other@example.com"),
    )
    assert first.status_code == 201
    assert second.status_code == 429
