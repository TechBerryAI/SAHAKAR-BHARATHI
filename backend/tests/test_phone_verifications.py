from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_payload, verify_mobile


def test_send_sms_stores_hash_and_hides_the_code(client: TestClient, engine: Engine) -> None:
    response = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert response.status_code == 202
    body = response.json()
    assert "code" not in body
    code = client.app.state.sms_gateway.latest_code()
    assert code not in response.text
    recipient, message = client.app.state.sms_gateway.messages[-1]
    assert recipient == "9876543210"
    assert message == f"SAHAKAR BHARTI: OTP is {code}. Do not share it."

    with engine.connect() as connection:
        stored = connection.execute(text("SELECT code_hash FROM phone_verifications")).scalar_one()
    assert stored != code
    assert not stored.isdigit()


def test_confirm_returns_single_use_registration_token(client: TestClient, engine: Engine) -> None:
    token = verify_mobile(client, "9876543210")
    payload = register_payload()
    payload["verification_token"] = token
    registered = client.post("/api/v1/registrations", json=payload)
    assert registered.status_code == 201, registered.text

    with engine.connect() as connection:
        used = connection.execute(text("SELECT token_used_at FROM phone_verifications")).scalar_one()
    assert used is not None


def test_wrong_code_is_rejected(client: TestClient) -> None:
    sent = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert sent.status_code == 202
    code = client.app.state.sms_gateway.latest_code()
    wrong = "000000" if code != "000000" else "111111"
    rejected = client.post(
        f"/api/v1/phone-verifications/{sent.json()['id']}/confirm",
        json={"code": wrong},
    )
    assert rejected.status_code == 400
    assert "verification_token" not in rejected.json()


def test_resend_is_rate_limited(client: TestClient) -> None:
    first = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert first.status_code == 202
    second = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert second.status_code == 429


def test_registration_requires_a_token_for_that_mobile(client: TestClient) -> None:
    missing = client.post("/api/v1/registrations", json=register_payload())
    assert missing.status_code == 422

    token = verify_mobile(client, "9876543210")
    payload = register_payload(mobile="9123456780", email="other@example.com")
    payload["verification_token"] = token
    mismatched = client.post("/api/v1/registrations", json=payload)
    assert mismatched.status_code == 400


def test_unconfigured_gateway_does_not_call_textbee(client: TestClient, monkeypatch) -> None:
    monkeypatch.setenv("TEXTBEE_API_KEY", "")
    from app.core.config import get_settings

    get_settings.cache_clear()
    client.app.state.sms_gateway = None
    response = client.post("/api/v1/phone-verifications", json={"mobile": "9988776655"})
    assert response.status_code == 503
    assert response.json()["detail"] == "SMS gateway is not configured."
    get_settings.cache_clear()
