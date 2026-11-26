from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.config import get_settings
from app.core.security import hash_pin, verify_pin
from tests.conftest import register_and_login, register_payload, verified_register_payload


def test_pin_hash_is_not_plaintext() -> None:
    hashed = hash_pin("2468")
    assert hashed != "2468"
    assert hashed.startswith("$argon2")
    assert verify_pin("2468", hashed)
    assert not verify_pin("0000", hashed)


def test_stored_pin_is_hashed_and_excluded_from_responses(
    client: TestClient,
    engine: Engine,
) -> None:
    response = client.post("/api/v1/registrations", json=verified_register_payload(client))
    assert response.status_code == 201
    dumped = response.text.lower()
    assert "2468" not in dumped
    assert "pin_hash" not in dumped

    with engine.connect() as connection:
        pin_hash = connection.execute(text("SELECT pin_hash FROM users")).scalar_one()
        assert pin_hash != "2468"
        assert verify_pin("2468", pin_hash)


def test_session_token_is_stored_hashed(client: TestClient, engine: Engine) -> None:
    _created, session = register_and_login(client)
    raw = session["access_token"]
    with engine.connect() as connection:
        stored = connection.execute(text("SELECT token_hash FROM sessions")).scalar_one()
    assert stored != raw
    assert len(stored) == 64


def test_failed_registration_does_not_leave_partial_rows(
    client: TestClient,
    engine: Engine,
) -> None:
    first = client.post("/api/v1/registrations", json=verified_register_payload(client))
    assert first.status_code == 201
    duplicate = register_payload(email="other@example.com")
    duplicate["verification_token"] = "not-a-real-verification-token"
    second = client.post("/api/v1/registrations", json=duplicate)
    assert second.status_code == 400
    assert "already exists" not in second.text
    with engine.connect() as connection:
        users = connection.execute(text("SELECT count(*) FROM users")).scalar_one()
        registrations = connection.execute(text("SELECT count(*) FROM registrations")).scalar_one()
    assert users == 1
    assert registrations == 1


def test_sql_injection_shaped_input_is_rejected(client: TestClient) -> None:
    mobile = client.post(
        "/api/v1/phone-verifications",
        json={"mobile": "9876543210' OR 1=1 --"},
    )
    assert mobile.status_code == 422
    assert "select" not in mobile.text.lower()
    assert "syntax" not in mobile.text.lower()

    login = client.post(
        "/api/v1/sessions",
        json={"identifier": "' OR 1=1 --", "pin": "2468"},
    )
    assert login.status_code == 422
    assert "select" not in login.text.lower()
    assert "2468" not in login.text


def test_one_network_cannot_rotate_login_identifiers(client: TestClient, monkeypatch) -> None:
    monkeypatch.setenv("LOGIN_IP_RATE_LIMIT", "2")
    monkeypatch.setenv("LOGIN_RATE_LIMIT", "10")
    monkeypatch.setenv("LOGIN_LOCKOUT_FAILURES", "100")
    get_settings.cache_clear()
    details = []
    statuses = []
    for mobile in ("9000000011", "9000000012", "9000000013"):
        response = client.post("/api/v1/sessions", json={"identifier": mobile, "pin": "0000"})
        statuses.append(response.status_code)
        details.append(response.json()["detail"])
    assert statuses == [401, 401, 429]
    assert details[0] == details[1]
    assert "exist" not in details[0].lower()
