from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_payload, verified_register_payload, verify_mobile


def test_registration_success_persists_user_and_registration(
    client: TestClient,
    engine: Engine,
) -> None:
    response = client.post("/api/v1/registrations", json=verified_register_payload(client))
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
    payload = verified_register_payload(client)
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
    first = client.post("/api/v1/registrations", json=verified_register_payload(client))
    assert first.status_code == 201
    duplicate = register_payload(email="other@example.com")
    duplicate["verification_token"] = "not-a-real-verification-token"
    second = client.post("/api/v1/registrations", json=duplicate)
    assert second.status_code == 409
    assert second.json()["detail"] == "An account already exists for this mobile number."


def test_duplicate_email_returns_409(client: TestClient) -> None:
    first = client.post("/api/v1/registrations", json=verified_register_payload(client))
    assert first.status_code == 201
    second = client.post(
        "/api/v1/registrations",
        json=verified_register_payload(client, mobile="9876543211"),
    )
    assert second.status_code == 409
    assert second.json()["detail"] == "An account already exists with this email address."


def test_registered_mobile_cannot_request_an_otp(client: TestClient) -> None:
    created = client.post("/api/v1/registrations", json=verified_register_payload(client))
    assert created.status_code == 201
    response = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert response.status_code == 409
    assert response.json()["detail"] == "An account already exists for this mobile number."


def test_verified_mobile_without_registration_creates_no_account(
    client: TestClient,
    engine: Engine,
) -> None:
    verify_mobile(client, "9876543212")
    with engine.connect() as connection:
        assert connection.execute(text("SELECT count(*) FROM users")).scalar_one() == 0
    sign_in = client.post("/api/v1/sessions", json={"identifier": "9876543212", "pin": "2468"})
    assert sign_in.status_code == 401
    # Coming back later starts over from the mobile step: the number can get a fresh OTP.
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE phone_verifications SET created_at = created_at - interval '5 minutes'")
        )
    again = client.post("/api/v1/phone-verifications", json={"mobile": "9876543212"})
    assert again.status_code == 202
