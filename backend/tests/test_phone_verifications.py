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
    assert response.json()["detail"] == "Could not send the SMS. Please try again."
    assert "Textbee" not in response.text
    get_settings.cache_clear()


def test_resend_retires_the_previous_code(client: TestClient, engine: Engine) -> None:
    first = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert first.status_code == 202
    first_code = client.app.state.sms_gateway.latest_code()
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE phone_verifications SET created_at = created_at - interval '5 minutes'")
        )

    second = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert second.status_code == 202
    second_code = client.app.state.sms_gateway.latest_code()

    stale = client.post(
        f"/api/v1/phone-verifications/{first.json()['id']}/confirm",
        json={"code": first_code},
    )
    assert stale.status_code == 400
    fresh = client.post(
        f"/api/v1/phone-verifications/{second.json()['id']}/confirm",
        json={"code": second_code},
    )
    assert fresh.status_code == 200, fresh.text


def test_sms_is_accepted_before_the_response(client: TestClient, engine: Engine) -> None:
    response = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert response.status_code == 202, response.text
    assert client.app.state.sms_gateway.messages[-1][0] == "9876543210"
    with engine.connect() as connection:
        stored = connection.execute(text("SELECT count(*) FROM phone_verifications")).scalar_one()
        events = connection.execute(text("SELECT event_type FROM audit_events")).scalars().all()
    assert stored == 1
    assert set(events) == {"otp_requested", "otp_delivered"}


def test_failed_sms_lets_the_user_resend_at_once(client: TestClient, engine: Engine) -> None:
    from app.core.errors import SmsDeliveryError

    class FailingGateway:
        def send(self, recipient_e164: str, message: str) -> None:
            raise SmsDeliveryError("The SMS gateway is busy. Try again later.")

        def wait_until_sent(self, timeout: float = 20.0) -> bool:
            return False

    working = client.app.state.sms_gateway
    client.app.state.sms_gateway = FailingGateway()
    first = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert first.status_code == 503
    assert first.json()["detail"] == "Could not send the SMS. Please try again."
    assert "busy" not in first.text
    with engine.connect() as connection:
        stored = connection.execute(text("SELECT count(*) FROM phone_verifications")).scalar_one()
        failed = connection.execute(
            text("SELECT extra->>'error_type' FROM audit_events WHERE event_type = 'otp_delivery_failed'")
        ).scalar_one()
    assert stored == 0
    assert failed == "SmsDeliveryError"

    client.app.state.sms_gateway = working
    retry = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert retry.status_code == 202, retry.text


def test_sms_timeout_discards_the_challenge(client: TestClient, engine: Engine) -> None:
    import httpx

    class TimeoutGateway:
        def send(self, recipient_e164: str, message: str) -> None:
            raise httpx.TimeoutException("timed out")

        def wait_until_sent(self, timeout: float = 20.0) -> bool:
            return False

    client.app.state.sms_gateway = TimeoutGateway()
    response = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert response.status_code == 503
    assert response.json()["detail"] == "Could not send the SMS. Please try again."
    assert "timed out" not in response.text
    with engine.connect() as connection:
        stored = connection.execute(text("SELECT count(*) FROM phone_verifications")).scalar_one()
    assert stored == 0


def test_expired_challenge_and_attempt_lockout(client: TestClient, engine: Engine) -> None:
    expired = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert expired.status_code == 202
    with engine.begin() as connection:
        connection.execute(text("UPDATE phone_verifications SET expires_at = now() - interval '1 minute'"))
    rejected = client.post(
        f"/api/v1/phone-verifications/{expired.json()['id']}/confirm",
        json={"code": "123456"},
    )
    assert rejected.status_code == 400
    assert rejected.json()["detail"] == "The code is incorrect or has expired."

    sent = client.post("/api/v1/phone-verifications", json={"mobile": "9000000001"})
    assert sent.status_code == 202, sent.text
    code = client.app.state.sms_gateway.latest_code()
    wrong = "000000" if code != "000000" else "111111"
    for _ in range(5):
        attempt = client.post(
            f"/api/v1/phone-verifications/{sent.json()['id']}/confirm",
            json={"code": wrong},
        )
        assert attempt.status_code == 400
    locked = client.post(
        f"/api/v1/phone-verifications/{sent.json()['id']}/confirm",
        json={"code": code},
    )
    assert locked.status_code == 429


def test_expired_verification_token_cannot_register(client: TestClient, engine: Engine) -> None:
    token = verify_mobile(client, "9876543210")
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE phone_verifications SET token_expires_at = now() - interval '1 minute'")
        )
    payload = register_payload()
    payload["verification_token"] = token
    rejected = client.post("/api/v1/registrations", json=payload)
    assert rejected.status_code == 400
    assert "Verify this mobile" in rejected.json()["detail"]


def test_otp_send_is_capped_per_mobile_and_more_loosely_per_ip(
    client: TestClient, engine: Engine, monkeypatch
) -> None:
    monkeypatch.setenv("OTP_SEND_RATE_LIMIT", "1")
    monkeypatch.setenv("OTP_SEND_IP_RATE_LIMIT", "3")
    from app.core.config import get_settings

    get_settings.cache_clear()

    def send(mobile: str):
        return client.post("/api/v1/phone-verifications", json={"mobile": mobile})

    assert send("9000000001").status_code == 202
    assert send("9000000002").status_code == 202
    with engine.begin() as connection:
        connection.execute(
            text(
                "UPDATE phone_verifications SET created_at = created_at - interval '5 minutes' "
                "WHERE mobile = '9000000001'"
            )
        )
    # The resend cooldown has passed. This request is still inside the IP cap, so the
    # 429 is the per-mobile cap. The next new mobile crosses the shared-network cap.
    assert send("9000000001").status_code == 429
    assert send("9000000003").status_code == 429
    get_settings.cache_clear()


def test_otp_confirm_is_limited_per_ip(client: TestClient, monkeypatch) -> None:
    monkeypatch.setenv("OTP_CONFIRM_RATE_LIMIT", "2")
    from app.core.config import get_settings

    get_settings.cache_clear()
    challenge = "00000000-0000-4000-8000-000000000001"
    for _ in range(2):
        attempt = client.post(
            f"/api/v1/phone-verifications/{challenge}/confirm",
            json={"code": "123456"},
        )
        assert attempt.status_code == 400
    limited = client.post(
        f"/api/v1/phone-verifications/{challenge}/confirm",
        json={"code": "123456"},
    )
    assert limited.status_code == 429
    get_settings.cache_clear()


def test_audit_rows_omit_the_code_and_the_mobile(client: TestClient, engine: Engine) -> None:
    sent = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert sent.status_code == 202
    code = client.app.state.sms_gateway.latest_code()
    rejected = client.post(
        f"/api/v1/phone-verifications/{sent.json()['id']}/confirm",
        json={"code": "000000" if code != "000000" else "111111"},
    )
    assert rejected.status_code == 400
    with engine.connect() as connection:
        rows = connection.execute(
            text(
                "SELECT event_type || ' ' || coalesce(extra::text, '') || ' ' || "
                "coalesce(identifier_hash, '') FROM audit_events"
            )
        ).scalars().all()
    blob = " ".join(rows)
    assert code not in blob
    assert "9876543210" not in blob
    assert "otp_confirm_failed" in blob


def test_concurrent_confirm_issues_one_token(client: TestClient, engine: Engine) -> None:
    from concurrent.futures import ThreadPoolExecutor
    from uuid import UUID

    from app.core.config import get_settings
    from app.core.errors import InvalidOtpError
    from app.db.session import SessionLocal
    from app.services.phone_verification import PhoneVerificationService

    sent = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert sent.status_code == 202, sent.text
    code = client.app.state.sms_gateway.latest_code()
    challenge_id = UUID(sent.json()["id"])

    def confirm() -> str | None:
        with SessionLocal() as db:
            service = PhoneVerificationService(db, get_settings(), client.app.state.sms_gateway)
            try:
                return service.confirm(challenge_id, code).verification_token
            except InvalidOtpError:
                return None

    with ThreadPoolExecutor(max_workers=2) as pool:
        tokens = [token for token in pool.map(lambda _: confirm(), range(2)) if token is not None]
    assert len(tokens) == 1
    with engine.connect() as connection:
        winners = connection.execute(
            text("SELECT count(*) FROM phone_verifications WHERE token_hash IS NOT NULL AND consumed_at IS NOT NULL")
        ).scalar_one()
    assert winners == 1

    payload = register_payload()
    payload["verification_token"] = tokens[0]
    assert client.post("/api/v1/registrations", json=payload).status_code == 201
    replay = register_payload(mobile="9123456780", email="other@example.com")
    replay["verification_token"] = tokens[0]
    assert client.post("/api/v1/registrations", json=replay).status_code == 400


def test_registration_does_not_need_the_sms_gateway(client: TestClient, monkeypatch) -> None:
    from app.core.config import get_settings

    token = verify_mobile(client, "9876543210")
    monkeypatch.setenv("TEXTBEE_API_KEY", "")
    get_settings.cache_clear()
    client.app.state.sms_gateway = None
    payload = register_payload()
    payload["verification_token"] = token
    registered = client.post("/api/v1/registrations", json=payload)
    assert registered.status_code == 201, registered.text
    get_settings.cache_clear()
