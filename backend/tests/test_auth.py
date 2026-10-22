from __future__ import annotations

from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_and_login


def test_login_success_with_mobile(client: TestClient) -> None:
    created, session = register_and_login(client)
    assert session["token_type"] == "bearer"
    assert session["login_id"] == created["login_id"]
    assert session["access_token"]
    assert "pin" not in session


def test_login_success_with_login_id(client: TestClient) -> None:
    created, _session = register_and_login(client)
    response = client.post(
        "/api/v1/sessions",
        json={"identifier": created["login_id"], "pin": "2468"},
    )
    assert response.status_code == 200


def test_login_failure_wrong_pin(client: TestClient) -> None:
    register_and_login(client)
    response = client.post(
        "/api/v1/sessions",
        json={"identifier": "9876543210", "pin": "9999"},
    )
    assert response.status_code == 401


def test_protected_endpoint_requires_auth(client: TestClient) -> None:
    response = client.get("/api/v1/me")
    assert response.status_code == 401


def test_invalid_token_is_rejected(client: TestClient) -> None:
    response = client.get("/api/v1/me", headers={"Authorization": "Bearer not-a-real-token"})
    assert response.status_code == 401


def test_logout_revokes_session(client: TestClient) -> None:
    _created, session = register_and_login(client)
    headers = {"Authorization": f"Bearer {session['access_token']}"}
    logout = client.delete("/api/v1/sessions/current", headers=headers)
    assert logout.status_code == 204
    me = client.get("/api/v1/me", headers=headers)
    assert me.status_code == 401


def test_expired_session_is_rejected(client: TestClient, engine: Engine) -> None:
    _created, session = register_and_login(client)
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE sessions SET expires_at = :expires"),
            {"expires": datetime.now(UTC) - timedelta(minutes=1)},
        )
    me = client.get("/api/v1/me", headers={"Authorization": f"Bearer {session['access_token']}"})
    assert me.status_code == 401
