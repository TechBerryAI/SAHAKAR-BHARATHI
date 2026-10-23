from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.config import get_settings
from app.core.rate_limit import PostgresRateLimiter, SlidingWindowLimiter
from app.db import session as db_session
from app.main import build_limiter, create_app


def test_postgres_limiter_blocks_after_limit(engine: Engine) -> None:
    limiter = PostgresRateLimiter(engine)
    results = [limiter.allow("login:1.2.3.4:9876543210", 3, 60) for _ in range(5)]
    assert results == [True, True, True, False, False]
    assert limiter.allow("login:5.6.7.8:9876543210", 3, 60) is True


def test_postgres_limiter_restarts_after_window(engine: Engine) -> None:
    limiter = PostgresRateLimiter(engine)
    assert limiter.allow("register:9.9.9.9", 1, 60) is True
    assert limiter.allow("register:9.9.9.9", 1, 60) is False
    with engine.begin() as connection:
        connection.execute(text("UPDATE rate_limit_buckets SET window_start = now() - interval '2 minutes'"))
    assert limiter.allow("register:9.9.9.9", 1, 60) is True


def test_postgres_limiter_stores_only_hashed_keys(engine: Engine) -> None:
    PostgresRateLimiter(engine).allow("login:203.0.113.7:9876543210", 5, 60)
    with engine.connect() as connection:
        keys = connection.execute(text("SELECT key_hash FROM rate_limit_buckets")).scalars().all()
    assert len(keys) == 1
    assert "9876543210" not in keys[0]
    assert "203.0.113.7" not in keys[0]
    assert len(keys[0]) == 64


def test_postgres_limiter_is_atomic_under_concurrency(engine: Engine) -> None:
    # Separate limiter objects stand in for separate API instances sharing one database.
    limiters = [PostgresRateLimiter(engine) for _ in range(4)]
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(lambda i: limiters[i % 4].allow("shared-key", 10, 60), range(40)))
    assert results.count(True) == 10
    with engine.connect() as connection:
        hits = connection.execute(text("SELECT hits FROM rate_limit_buckets")).scalar_one()
    assert hits == 40


def test_app_uses_postgres_limiter_when_configured(engine: Engine, monkeypatch) -> None:
    monkeypatch.setenv("RATE_LIMIT_BACKEND", "postgres")
    # Keep the database lockout out of the way so the 429 below comes from the limiter.
    monkeypatch.setenv("LOGIN_LOCKOUT_FAILURES", "100")
    get_settings.cache_clear()
    try:
        assert isinstance(build_limiter(get_settings()), PostgresRateLimiter)
        application = create_app()
        db_session.SessionLocal.configure(bind=engine)
        original_engine = db_session.engine
        db_session.engine = engine
        try:
            with TestClient(application) as client:
                assert isinstance(application.state.limiter, PostgresRateLimiter)
                last = None
                for _ in range(6):
                    last = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "0000"})
                assert last is not None and last.status_code == 429
            with engine.connect() as connection:
                hits = connection.execute(text("SELECT max(hits) FROM rate_limit_buckets")).scalar_one()
            assert hits == 6
        finally:
            db_session.engine = original_engine
    finally:
        monkeypatch.delenv("RATE_LIMIT_BACKEND")
        get_settings.cache_clear()


def test_default_limiter_is_in_memory() -> None:
    get_settings.cache_clear()
    assert isinstance(build_limiter(get_settings()), SlidingWindowLimiter)
