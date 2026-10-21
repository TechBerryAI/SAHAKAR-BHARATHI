from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy.engine import Engine

from app.core.config import get_settings
from app.core.rate_limit import SlidingWindowLimiter
from app.db import session as db_session
from app.main import create_app


def test_cors_allows_approved_origin_and_rejects_other(
    engine: Engine,
    database_url: str,
    monkeypatch,
) -> None:
    monkeypatch.setenv("CORS_ORIGINS", "https://web.example.com")
    monkeypatch.setenv("ENVIRONMENT", "development")
    get_settings.cache_clear()
    application = create_app()
    db_session.SessionLocal.configure(bind=engine)
    application.state.limiter = SlidingWindowLimiter()
    try:
        with TestClient(application) as client:
            allowed = client.options(
                "/api/v1/me",
                headers={
                    "Origin": "https://web.example.com",
                    "Access-Control-Request-Method": "GET",
                },
            )
            assert allowed.headers.get("access-control-allow-origin") == "https://web.example.com"
            denied = client.options(
                "/api/v1/me",
                headers={
                    "Origin": "https://evil.example",
                    "Access-Control-Request-Method": "GET",
                },
            )
            assert denied.headers.get("access-control-allow-origin") != "https://evil.example"
    finally:
        get_settings.cache_clear()
        application.state.limiter.reset()
