from __future__ import annotations

from fastapi.testclient import TestClient

from tests.conftest import register_and_login


def test_root_returns_service_status(client: TestClient) -> None:
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "service": "Sahakar Bharati API",
        "health": "/health",
    }


def test_root_head_returns_200_without_body(client: TestClient) -> None:
    response = client.head("/")
    assert response.status_code == 200
    assert response.content == b""


def test_live_returns_200_without_database(client: TestClient) -> None:
    response = client.get("/health/live")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_ready_and_health_succeed_when_database_is_up(client: TestClient) -> None:
    ready = client.get("/health/ready")
    assert ready.status_code == 200
    assert ready.json() == {"status": "ok", "database": "ok"}
    health = client.get("/health")
    assert health.status_code == 200
    assert health.json()["database"] == "ok"


def test_ready_and_health_return_503_when_database_unavailable(
    client: TestClient,
    monkeypatch,
) -> None:
    class Unavailable:
        def __enter__(self) -> None:
            raise OSError("connection refused")

        def __exit__(self, *_args: object) -> bool:
            return False

    monkeypatch.setattr("app.main.SessionLocal", lambda: Unavailable())
    for path in ("/health/ready", "/health"):
        response = client.get(path)
        assert response.status_code == 503
        body = response.json()
        assert body["status"] == "unavailable"
        assert body["database"] == "unavailable"
        assert "sql" not in response.text.lower()
        assert "password" not in response.text.lower()


def test_unhandled_error_does_not_leak_details(client: TestClient, monkeypatch) -> None:
    from app.services.profile import ProfileService

    def boom(self, user):  # type: ignore[no-untyped-def]
        raise RuntimeError("SELECT * FROM users WHERE pin='2468'")

    monkeypatch.setattr(ProfileService, "to_response", boom)
    _created, session = register_and_login(client)
    response = client.get(
        "/api/v1/me",
        headers={"Authorization": f"Bearer {session['access_token']}"},
    )
    assert response.status_code == 500
    assert response.json()["detail"] == "An unexpected error occurred."
    assert "SELECT" not in response.text
    assert "2468" not in response.text
