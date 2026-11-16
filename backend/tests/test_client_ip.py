from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.core.client_ip import client_host
from app.core.config import get_settings
from app.core.request_context import current_client_ip


def _send(client: TestClient, mobile: str, **headers: str):
    return client.post("/api/v1/phone-verifications", json={"mobile": mobile}, headers=headers)


@pytest.fixture
def tight_ip_limit(monkeypatch):
    monkeypatch.setenv("OTP_SEND_IP_RATE_LIMIT", "1")
    monkeypatch.setenv("OTP_SEND_RATE_LIMIT", "10")
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()


def test_direct_requests_share_the_tcp_peer(client: TestClient, tight_ip_limit) -> None:
    first = _send(
        client,
        "9876543210",
        **{"CF-Connecting-IP": "203.0.113.9", "X-Forwarded-For": "198.51.100.8", "X-Real-IP": "198.51.100.9"},
    )
    second = _send(client, "9876543211", **{"CF-Connecting-IP": "203.0.113.10"})
    assert first.status_code == 202
    assert second.status_code == 429


def test_cloudflare_header_is_the_only_production_identity(client: TestClient, monkeypatch, tight_ip_limit) -> None:
    monkeypatch.setenv("CLIENT_IP_SOURCE", "cf-connecting-ip")
    get_settings.cache_clear()
    first = _send(
        client,
        "9876543210",
        **{
            "CF-Connecting-IP": "198.51.100.20",
            "X-Forwarded-For": "203.0.113.5, 198.51.100.20",
            "X-Real-IP": "203.0.113.5",
        },
    )
    same_visitor = _send(
        client,
        "9876543211",
        **{"CF-Connecting-IP": "198.51.100.20", "X-Forwarded-For": "8.8.8.8", "X-Real-IP": "1.1.1.1"},
    )
    other_visitor = _send(
        client,
        "9876543212",
        **{"CF-Connecting-IP": "198.51.100.21", "X-Forwarded-For": "198.51.100.20"},
    )
    assert first.status_code == 202
    assert same_visitor.status_code == 429
    assert other_visitor.status_code == 202


def test_malformed_cloudflare_header_uses_the_tcp_peer(client: TestClient, monkeypatch, tight_ip_limit) -> None:
    monkeypatch.setenv("CLIENT_IP_SOURCE", "cf-connecting-ip")
    get_settings.cache_clear()
    first = _send(client, "9876543210", **{"CF-Connecting-IP": "not-an-ip"})
    second = _send(client, "9876543211", **{"CF-Connecting-IP": "203.0.113.5, 198.51.100.20"})
    assert first.status_code == 202
    assert second.status_code == 429


def test_ipv6_identities_are_normalized(client: TestClient, monkeypatch, tight_ip_limit) -> None:
    monkeypatch.setenv("CLIENT_IP_SOURCE", "cf-connecting-ip")
    get_settings.cache_clear()
    first = _send(client, "9876543210", **{"CF-Connecting-IP": "2001:db8::1"})
    same = _send(client, "9876543211", **{"CF-Connecting-IP": "[2001:db8:0:0:0:0:0:1]"})
    other = _send(client, "9876543212", **{"CF-Connecting-IP": "2001:db8::2"})
    assert first.status_code == 202
    assert same.status_code == 429
    assert other.status_code == 202


def test_middleware_stores_the_address_once(monkeypatch) -> None:
    import asyncio

    from app.core.client_ip import ClientIpMiddleware

    monkeypatch.setenv("CLIENT_IP_SOURCE", "cf-connecting-ip")
    get_settings.cache_clear()
    captured: dict[str, str] = {}

    async def downstream(scope, receive, send):
        captured["state"] = scope["state"]["client_ip"]
        captured["context"] = current_client_ip()
        await send({"type": "http.response.start", "status": 200, "headers": []})
        await send({"type": "http.response.body", "body": b""})

    async def receive():
        return {"type": "http.request", "body": b"", "more_body": False}

    async def exercise() -> None:
        sent: list[object] = []

        async def send(message: object) -> None:
            sent.append(message)

        scope = {
            "type": "http",
            "http_version": "1.1",
            "method": "GET",
            "path": "/health/live",
            "raw_path": b"/health/live",
            "headers": [
                (b"cf-connecting-ip", b"198.51.100.40"),
                (b"x-forwarded-for", b"203.0.113.9"),
                (b"x-real-ip", b"203.0.113.8"),
            ],
            "client": ("10.0.0.8", 1234),
            "scheme": "http",
            "query_string": b"",
            "server": ("test", 80),
        }
        await ClientIpMiddleware(downstream)(scope, receive, send)

    asyncio.run(exercise())
    assert captured["state"] == captured["context"] == "198.51.100.40"


def test_parser_ignores_forwarded_headers_for_a_direct_peer() -> None:
    class _Client:
        host = "127.0.0.1"

    class _Request:
        client = _Client()
        headers = {
            "cf-connecting-ip": "203.0.113.5",
            "x-forwarded-for": "198.51.100.8",
            "x-real-ip": "198.51.100.9",
        }

    assert client_host(_Request(), source="peer") == "127.0.0.1"  # type: ignore[arg-type]
    assert client_host(_Request(), source="cf-connecting-ip") == "203.0.113.5"  # type: ignore[arg-type]
