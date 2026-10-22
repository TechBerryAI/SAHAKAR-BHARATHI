from __future__ import annotations

import json

import httpx

from app.services.sms import TextbeeGateway


def test_textbee_posts_e164_recipient_and_device() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["api_key"] = request.headers.get("x-api-key")
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"data": {"success": True, "smsBatchId": "batch-1"}})

    gateway = TextbeeGateway(
        api_key="test-key",
        device_id="device-1",
        base_url="https://api.textbee.dev/api/v1",
        transport=httpx.MockTransport(handler),
    )
    gateway.send("+919876543210", "Sahakar Bharati verification code is 123456.")

    assert captured["url"] == "https://api.textbee.dev/api/v1/gateway/send-sms"
    assert captured["api_key"] == "test-key"
    assert captured["body"] == {
        "recipients": ["+919876543210"],
        "message": "Sahakar Bharati verification code is 123456.",
        "deviceId": "device-1",
    }


def test_textbee_omits_device_when_unset() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"data": {"successCount": 1, "failureCount": 0}})

    TextbeeGateway(
        api_key="test-key",
        base_url="https://api.textbee.dev/api/v1/",
        transport=httpx.MockTransport(handler),
    ).send("+919876543210", "code")

    assert captured["body"] == {
        "recipients": ["+919876543210"],
        "message": "code",
    }


def test_textbee_sends_from_the_requested_sim() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"data": {"success": True}})

    TextbeeGateway(
        api_key="test-key",
        device_id="device-1",
        sim_subscription_id=7,
        transport=httpx.MockTransport(handler),
    ).send("9876543210", "code")

    assert captured["body"]["simSubscriptionId"] == 7
    assert captured["body"]["recipients"] == ["9876543210"]


def test_textbee_uses_the_sim_that_has_service() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.method == "GET":
            return httpx.Response(
                200,
                json={
                    "data": {
                        "simInfo": {
                            "sims": [
                                {
                                    "subscriptionId": 8,
                                    "serviceState": "OUT_OF_SERVICE",
                                    "simState": "READY",
                                    "signalLevel": 0,
                                },
                                {
                                    "subscriptionId": 7,
                                    "serviceState": "IN_SERVICE",
                                    "simState": "READY",
                                    "signalLevel": 4,
                                },
                            ]
                        }
                    }
                },
            )
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"data": {"success": True}})

    TextbeeGateway(
        api_key="test-key",
        device_id="device-1",
        resolve_sim=True,
        transport=httpx.MockTransport(handler),
    ).send("9876543210", "code")

    assert captured["body"]["simSubscriptionId"] == 7


def test_textbee_wait_until_sent_reads_batch_status() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.method == "GET":
            assert "smsBatchId=batch-1" in str(request.url)
            return httpx.Response(
                200,
                json={"data": [{"status": "delivered", "smsBatch": "batch-1"}]},
            )
        return httpx.Response(200, json={"data": {"success": True, "smsBatchId": "batch-1"}})

    gateway = TextbeeGateway(
        api_key="test-key",
        device_id="device-1",
        transport=httpx.MockTransport(handler),
    )
    gateway.send("9876543210", "code")
    assert gateway.wait_until_sent(timeout=1.0) is True


def test_textbee_rejects_failed_batch() -> None:
    from app.core.errors import SmsDeliveryError

    def handler(_request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"data": {"success": False}})

    gateway = TextbeeGateway(
        api_key="test-key",
        transport=httpx.MockTransport(handler),
    )
    try:
        gateway.send("+919876543210", "code")
    except SmsDeliveryError as exc:
        assert exc.status_code == 503
        assert "123456" not in exc.detail
    else:
        raise AssertionError("expected SMS delivery error")
