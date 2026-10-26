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


def test_login_id_is_rejected(client: TestClient) -> None:
    created, _session = register_and_login(client)
    response = client.post(
        "/api/v1/sessions",
        json={"identifier": created["login_id"], "pin": "2468"},
    )
    assert response.status_code == 422


def test_login_validation_does_not_echo_pin(client: TestClient) -> None:
    response = client.post(
        "/api/v1/sessions",
        json={"identifier": "9876543210", "pin": "12"},
    )
    assert response.status_code == 422
    assert "12" not in response.text
    for item in response.json()["detail"]:
        if "pin" in [str(part).lower() for part in item.get("loc", [])]:
            assert "input" not in item


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


def test_login_returns_the_full_profile(client: TestClient) -> None:
    _created, session = register_and_login(client)
    assert session["email"] == "anita@example.com"
    assert session["date_of_birth"] == "1985-03-12"
    assert session["address_line"] == "12 Village Road"
    assert session["pincode"] == "422001"
    assert session["status"] == "active"
    assert "pin_hash" not in session


def test_login_upgrades_pin_hashes_with_outdated_parameters(client: TestClient, engine: Engine) -> None:
    from argon2 import PasswordHasher

    register_and_login(client)
    legacy = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=4).hash("2468")
    with engine.begin() as connection:
        connection.execute(text("UPDATE users SET pin_hash = :hash"), {"hash": legacy})

    response = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "2468"})
    assert response.status_code == 200

    with engine.connect() as connection:
        stored = connection.execute(text("SELECT pin_hash FROM users")).scalar_one()
    assert stored != legacy
    assert "m=19456,t=2,p=1" in stored
