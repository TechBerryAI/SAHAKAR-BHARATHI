from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.security import hash_pin, verify_pin
from tests.conftest import register_and_login, register_payload


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
    response = client.post("/api/v1/registrations", json=register_payload())
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
    first = client.post("/api/v1/registrations", json=register_payload())
    assert first.status_code == 201
    second = client.post(
        "/api/v1/registrations",
        json=register_payload(email="other@example.com"),
    )
    assert second.status_code == 409
    with engine.connect() as connection:
        users = connection.execute(text("SELECT count(*) FROM users")).scalar_one()
        registrations = connection.execute(text("SELECT count(*) FROM registrations")).scalar_one()
    assert users == 1
    assert registrations == 1
