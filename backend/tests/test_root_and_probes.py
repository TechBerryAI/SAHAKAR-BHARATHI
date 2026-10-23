"""Root and probe endpoints. These tests do not need PostgreSQL, so they run everywhere."""

from __future__ import annotations

import contextlib
import importlib
import re
from collections.abc import Iterator
from pathlib import Path

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import app, create_app

REPO_ROOT = Path(__file__).resolve().parents[2]
ROOT_BODY = {"status": "ok", "service": "Sahakar Bharati API", "health": "/health"}


class FakeSession:
    def __init__(self) -> None:
        self.statements: list[str] = []

    def __enter__(self) -> FakeSession:
        return self

    def __exit__(self, *_args: object) -> bool:
        return False

    def begin(self) -> contextlib.AbstractContextManager[None]:
        return contextlib.nullcontext()

    def execute(self, statement: object) -> None:
        self.statements.append(str(statement))


@pytest.fixture
def db_calls(monkeypatch: pytest.MonkeyPatch) -> list[str]:
    """Fail any request that opens a database session, and record that it tried."""
    calls: list[str] = []

    def forbidden() -> None:
        calls.append("SessionLocal()")
        raise AssertionError("this endpoint must not use the database")

    monkeypatch.setattr("app.main.SessionLocal", forbidden)
    return calls


@pytest.fixture
def probe_client() -> Iterator[TestClient]:
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture
def production_app(monkeypatch: pytest.MonkeyPatch) -> Iterator[FastAPI]:
    monkeypatch.setenv("ENVIRONMENT", "production")
    monkeypatch.setenv("DATABASE_URL", "postgresql+psycopg://u:p@db.example.com:5432/app")
    monkeypatch.setenv("CORS_ORIGINS", "https://sahakarbharati-e3b04.web.app")
    get_settings.cache_clear()
    yield create_app()
    get_settings.cache_clear()


def test_get_root_returns_service_status(probe_client: TestClient, db_calls: list[str]) -> None:
    response = probe_client.get("/")
    assert response.status_code == 200
    assert response.headers["content-type"] == "application/json"
    assert response.json() == ROOT_BODY
    assert db_calls == []


def test_head_root_returns_200_without_body(probe_client: TestClient, db_calls: list[str]) -> None:
    response = probe_client.head("/")
    assert response.status_code == 200
    assert response.content == b""
    assert db_calls == []


def test_root_rejects_other_methods(probe_client: TestClient) -> None:
    assert probe_client.post("/").status_code == 405


def test_root_route_is_registered_once() -> None:
    routes = [route for route in app.routes if getattr(route, "path", None) == "/"]
    assert len(routes) == 1
    assert routes[0].methods == {"GET", "HEAD"}


def test_live_does_not_touch_database(probe_client: TestClient, db_calls: list[str]) -> None:
    response = probe_client.get("/health/live")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "unchecked"}
    assert db_calls == []


def test_ready_runs_bounded_database_check(probe_client: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    session = FakeSession()
    monkeypatch.setattr("app.main.SessionLocal", lambda: session)
    response = probe_client.get("/health/ready")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "ok"}
    assert any("statement_timeout" in statement for statement in session.statements)
    assert "SELECT 1" in session.statements


def test_ready_returns_503_when_database_unreachable(probe_client: TestClient, monkeypatch) -> None:
    def unreachable() -> None:
        raise OSError("connection refused")

    monkeypatch.setattr("app.main.SessionLocal", unreachable)
    for path in ("/health/ready", "/health"):
        response = probe_client.get(path)
        assert response.status_code == 503
        assert response.json() == {"status": "unavailable", "database": "unavailable"}
    assert probe_client.get("/health/live").status_code == 200


def test_production_serves_root_and_hides_docs(production_app: FastAPI, db_calls: list[str]) -> None:
    client = TestClient(production_app)
    assert client.get("/").status_code == 200
    assert client.get("/").json() == ROOT_BODY
    assert client.head("/").status_code == 200
    assert client.get("/health/live").status_code == 200
    assert client.get("/docs").status_code == 404
    assert client.get("/openapi.json").status_code == 404
    assert db_calls == []


def test_render_start_command_serves_this_app() -> None:
    render_yaml = REPO_ROOT / "render.yaml"
    if not render_yaml.exists():
        pytest.skip("render.yaml is not part of this checkout")
    text = render_yaml.read_text(encoding="utf-8")
    assert re.search(r"^\s*rootDir:\s*backend\s*$", text, re.MULTILINE)
    start = re.search(r"^\s*startCommand:\s*(.+)$", text, re.MULTILINE)
    assert start is not None
    target = re.search(r"uvicorn\s+(\S+):(\w+)", start.group(1))
    assert target is not None
    module_name, attribute = target.groups()
    assert (module_name, attribute) == ("app.main", "app")
    served = getattr(importlib.import_module(module_name), attribute)
    health = re.search(r"^\s*healthCheckPath:\s*(\S+)\s*$", text, re.MULTILINE)
    assert health is not None
    paths = {getattr(route, "path", None) for route in served.routes}
    assert {"/", health.group(1), "/health/live", "/health/ready"} <= paths
