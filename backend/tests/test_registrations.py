from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_payload


def test_registration_success_persists_user_and_registration(
    client: TestClient,
    engine: Engine,
) -> None:
    response = client.post("/api/v1/registrations", json=register_payload())
    assert response.status_code == 201
    body = response.json()
    assert body["registration_id"].startswith("SB-")
    assert body["registration_id"] == body["login_id"]
    assert body["full_name"] == "Anita Desai"
    assert "verification_status" not in body
    assert "pin" not in body
    assert "pin_hash" not in body

    with engine.connect() as connection:
        user = connection.execute(text("SELECT login_id, mobile, pin_hash, status FROM users")).one()
        registration = connection.execute(text("SELECT registration_id, user_id FROM registrations")).one()
        assert user.login_id == body["login_id"]
        assert user.mobile == "9876543210"
        assert user.pin_hash != "2468"
        assert user.status == "active"
        assert registration.registration_id == body["registration_id"]


def test_registration_rejects_invalid_mobile(client: TestClient) -> None:
    response = client.post("/api/v1/registrations", json=register_payload(mobile="123"))
    assert response.status_code == 422


def test_registration_rejects_short_pin(client: TestClient) -> None:
    response = client.post("/api/v1/registrations", json=register_payload(pin="12"))
    assert response.status_code == 422
    assert "12" not in response.text
    for item in response.json()["detail"]:
        if "pin" in [str(part).lower() for part in item.get("loc", [])]:
            assert "input" not in item


def test_registration_ignores_client_supplied_ids_and_flags(client: TestClient) -> None:
    payload = register_payload()
    payload.update(
        {
            "id": "client-id",
            "login_id": "FF-IND-1042",
            "registration_id": "FF-IND-1042",
            "status": "admin",
            "role": "organiser",
            "verified": True,
            "aadhaar": "123412341234",
        }
    )
    response = client.post("/api/v1/registrations", json=payload)
    assert response.status_code == 201
    body = response.json()
    assert body["login_id"] != "FF-IND-1042"
    assert body["login_id"].startswith("SB-")
    assert "aadhaar" not in body
    assert "verification_status" not in body


def test_duplicate_mobile_returns_409(client: TestClient) -> None:
    first = client.post("/api/v1/registrations", json=register_payload())
    assert first.status_code == 201
    second = client.post(
        "/api/v1/registrations",
        json=register_payload(email="other@example.com"),
    )
    assert second.status_code == 409
    assert "already exists" in second.json()["detail"]


def test_duplicate_email_returns_409(client: TestClient) -> None:
    first = client.post("/api/v1/registrations", json=register_payload())
    assert first.status_code == 201
    second = client.post(
        "/api/v1/registrations",
        json=register_payload(mobile="9876543211"),
    )
    assert second.status_code == 409
