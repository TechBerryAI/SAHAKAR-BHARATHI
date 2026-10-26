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
    settings = Settings(environment="production", database_url=_REMOTE_DB, cors_origins="")
    assert settings.cors_origin_list == []


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
