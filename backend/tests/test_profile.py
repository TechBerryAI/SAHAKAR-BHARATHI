from __future__ import annotations

from fastapi.testclient import TestClient

from tests.conftest import register_and_login


def test_me_returns_current_user(client: TestClient) -> None:
    created, session = register_and_login(client)
    response = client.get(
        "/api/v1/me",
        headers={"Authorization": f"Bearer {session['access_token']}"},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["login_id"] == created["login_id"]
    assert body["full_name"] == "Anita Desai"
    assert body["mobile"] == "9876543210"
    assert "pin_hash" not in body
    assert "access_token" not in body


def test_patch_me_updates_only_authenticated_user(client: TestClient) -> None:
    first, first_session = register_and_login(client)
    second, second_session = register_and_login(
        client,
        mobile="9123456780",
        email="second@example.com",
        full_name="Ravi Kumar",
    )
    response = client.patch(
        "/api/v1/me",
        headers={"Authorization": f"Bearer {first_session['access_token']}"},
        json={
            "full_name": "Anita Rao",
            "login_id": second["login_id"],
            "id": second["registration_id"],
            "status": "admin",
        },
    )
    assert response.status_code == 200
    assert response.json()["full_name"] == "Anita Rao"
    assert response.json()["login_id"] == first["login_id"]
    assert response.json()["status"] == "active"

    other = client.get(
        "/api/v1/me",
        headers={"Authorization": f"Bearer {second_session['access_token']}"},
    )
    assert other.json()["full_name"] == "Ravi Kumar"
    assert other.json()["login_id"] == second["login_id"]


def test_patch_me_validates_input(client: TestClient) -> None:
    _created, session = register_and_login(client)
    response = client.patch(
        "/api/v1/me",
        headers={"Authorization": f"Bearer {session['access_token']}"},
        json={"pincode": "12"},
    )
    assert response.status_code == 422
