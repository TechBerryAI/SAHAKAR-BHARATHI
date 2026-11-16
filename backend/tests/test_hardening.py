from __future__ import annotations

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.core.config import Settings
from tests.conftest import register_and_login

_REMOTE_DB = "postgresql+psycopg://u:p@db.example.com:5432/app"


def test_unknown_environment_is_rejected() -> None:
    with pytest.raises(ValidationError, match="ENVIRONMENT must be one of"):
        Settings(environment="prodution", database_url=_REMOTE_DB)


@pytest.mark.parametrize("sslmode", ["disable", "allow", "prefer"])
def test_production_rejects_downgraded_tls(sslmode: str) -> None:
    with pytest.raises(ValidationError, match="sslmode"):
        Settings(environment="production", database_url=f"{_REMOTE_DB}?sslmode={sslmode}")


def test_production_accepts_verify_full_tls() -> None:
    settings = Settings(
        environment="production",
        database_url=f"{_REMOTE_DB}?sslmode=verify-full",
        cors_origins="",
        textbee_api_key="test-key",
        rate_limit_backend="postgres",
        client_ip_source="cf-connecting-ip",
    )
    assert settings.engine_kwargs()["connect_args"]["sslmode"] == "verify-full"


@pytest.mark.parametrize(
    "origins",
    ["http://sahakarbharati-e3b04.web.app", "https://localhost:5173", "https://ok.example.com,http://127.0.0.1:5173"],
)
def test_production_rejects_insecure_cors_origins(origins: str) -> None:
    with pytest.raises(ValidationError, match="CORS_ORIGINS"):
        Settings(environment="production", database_url=_REMOTE_DB, cors_origins=origins)


def test_production_without_cors_origins_allows_no_browser_origin() -> None:
    settings = Settings(
        environment="production",
        database_url=_REMOTE_DB,
        cors_origins="",
        textbee_api_key="test-key",
        rate_limit_backend="postgres",
        client_ip_source="cf-connecting-ip",
    )
    assert settings.cors_origin_list == []


def test_production_requires_shared_rate_limits_and_an_sms_key() -> None:
    with pytest.raises(ValidationError, match="RATE_LIMIT_BACKEND=postgres"):
        Settings(
            environment="production",
            database_url=_REMOTE_DB,
            cors_origins="",
            textbee_api_key="test-key",
            rate_limit_backend="memory",
        )
    with pytest.raises(ValidationError, match="TEXTBEE_API_KEY"):
        Settings(
            environment="production",
            database_url=_REMOTE_DB,
            cors_origins="",
            textbee_api_key="",
            rate_limit_backend="postgres",
        )
    with pytest.raises(ValidationError, match="TEXTBEE_BASE_URL"):
        Settings(
            environment="production",
            database_url=_REMOTE_DB,
            cors_origins="",
            textbee_api_key="test-key",
            textbee_base_url="http://api.textbee.dev/api/v1",
            rate_limit_backend="postgres",
        )


def test_otp_timings_must_be_positive() -> None:
    with pytest.raises(ValidationError, match="OTP_TTL_SECONDS"):
        Settings(otp_ttl_seconds=0)


def test_request_id_is_generated_by_the_server(client: TestClient) -> None:
    response = client.get("/health/live", headers={"X-Request-ID": "client-supplied"})
    request_id = response.headers.get("x-request-id", "")
    assert request_id
    assert request_id != "client-supplied"
    assert len(request_id) == 32
    api = client.get("/api/v1/me")
    assert "strict-transport-security" not in api.headers


def test_cf_ray_is_kept_only_when_it_matches_cloudflare() -> None:
    import asyncio

    from app.core.request_context import current_cf_ray, sanitize_cf_ray
    from app.main import RequestIdMiddleware

    assert sanitize_cf_ray("230b030023ae2822-SJC") == "230b030023ae2822-SJC"
    assert sanitize_cf_ray("not a ray\n") == "-"
    assert sanitize_cf_ray("x" * 200) == "-"
    captured: dict[str, str] = {}

    async def downstream(scope, receive, send):
        captured["ray"] = current_cf_ray()
        await send({"type": "http.response.start", "status": 200, "headers": []})
        await send({"type": "http.response.body", "body": b""})

    async def receive():
        return {"type": "http.request", "body": b"", "more_body": False}

    async def exercise() -> None:
        scope = {
            "type": "http",
            "http_version": "1.1",
            "method": "GET",
            "path": "/health/live",
            "raw_path": b"/health/live",
            "headers": [(b"cf-ray", b"230b030023ae2822-SJC"), (b"x-request-id", b"client-supplied")],
            "client": ("127.0.0.1", 1234),
            "scheme": "http",
            "query_string": b"",
            "server": ("test", 80),
        }

        async def send(message: object) -> None:
            return None

        await RequestIdMiddleware(downstream)(scope, receive, send)

    asyncio.run(exercise())
    assert captured["ray"] == "230b030023ae2822-SJC"


def test_production_requires_cloudflare_client_ip() -> None:
    with pytest.raises(ValidationError, match="CLIENT_IP_SOURCE"):
        Settings(
            environment="production",
            database_url=_REMOTE_DB,
            cors_origins="",
            textbee_api_key="test-key",
            rate_limit_backend="postgres",
            client_ip_source="peer",
        )


def test_unknown_rate_limit_backend_is_rejected() -> None:
    with pytest.raises(ValidationError, match="RATE_LIMIT_BACKEND"):
        Settings(rate_limit_backend="redis")


def test_api_responses_are_not_cacheable(client: TestClient) -> None:
    _created, session = register_and_login(client)
    me = client.get("/api/v1/me", headers={"Authorization": f"Bearer {session['access_token']}"})
    assert me.headers["cache-control"] == "no-store"
    assert me.headers["x-content-type-options"] == "nosniff"
    failed = client.get("/api/v1/me")
    assert failed.status_code == 401
    assert failed.headers["cache-control"] == "no-store"


def test_cors_does_not_allow_credentials(client: TestClient) -> None:
    response = client.options(
        "/api/v1/me",
        headers={"Origin": "http://127.0.0.1:5173", "Access-Control-Request-Method": "GET"},
    )
    assert response.headers.get("access-control-allow-origin") == "http://127.0.0.1:5173"
    assert "access-control-allow-credentials" not in response.headers


def test_chunked_oversized_body_is_413(client: TestClient) -> None:
    def chunks():
        yield b'{"identifier": "'
        for _ in range(80):
            yield b"a" * 1024
        yield b'", "pin": "2468"}'

    response = client.post(
        "/api/v1/sessions",
        content=chunks(),
        headers={"Content-Type": "application/json"},
    )
    assert response.status_code == 413
    assert response.json()["detail"] == "Request too large."


def test_malformed_json_is_422_without_echo(client: TestClient) -> None:
    response = client.post(
        "/api/v1/sessions",
        content=b'{"identifier": "9876543210", "pin": "246',
        headers={"Content-Type": "application/json"},
    )
    assert response.status_code == 422
    assert "246" not in response.text
