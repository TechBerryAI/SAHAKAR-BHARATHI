from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_and_login, verified_register_payload


def test_concurrent_duplicate_registration_creates_one_account(client: TestClient, engine: Engine) -> None:
    # A double-tapped submit sends the same payload (and verification token) twice at once.
    payload = verified_register_payload(client)
    with ThreadPoolExecutor(max_workers=4) as pool:
        responses = list(pool.map(lambda _: client.post("/api/v1/registrations", json=payload), range(4)))
    statuses = sorted(response.status_code for response in responses)
    assert statuses.count(201) == 1, statuses
    assert set(statuses) <= {201, 400, 409}, statuses
    with engine.connect() as connection:
        assert connection.execute(text("SELECT count(*) FROM users")).scalar_one() == 1
        assert connection.execute(text("SELECT count(*) FROM registrations")).scalar_one() == 1


def test_verification_token_is_single_use(client: TestClient) -> None:
    payload = verified_register_payload(client)
    first = client.post("/api/v1/registrations", json=payload)
    assert first.status_code == 201
    replay = client.post(
        "/api/v1/registrations",
        json={**payload, "mobile": "9123456780", "email": "other@example.com"},
    )
    assert replay.status_code == 400


def test_conflicting_profile_update_rolls_back_every_field(client: TestClient) -> None:
    _first, first_session = register_and_login(client)
    register_and_login(client, mobile="9123456780", email="taken@example.com", full_name="Ravi Kumar")
    headers = {"Authorization": f"Bearer {first_session['access_token']}"}

    conflict = client.patch(
        "/api/v1/me",
        headers=headers,
        json={"full_name": "Changed Name", "email": "taken@example.com"},
    )
    assert conflict.status_code == 409

    after = client.get("/api/v1/me", headers=headers)
    assert after.status_code == 200
    assert after.json()["full_name"] == "Anita Desai"
    assert after.json()["email"] == "anita@example.com"


def test_profile_update_cannot_change_identity_or_status(client: TestClient, engine: Engine) -> None:
    created, session = register_and_login(client)
    headers = {"Authorization": f"Bearer {session['access_token']}"}
    response = client.patch(
        "/api/v1/me",
        headers=headers,
        json={
            "mobile": "9000000000",
            "status": "disabled",
            "login_id": "SB-HACKED00",
            "pin_hash": "x",
            "gender": "Other",
        },
    )
    assert response.status_code == 200
    with engine.connect() as connection:
        row = connection.execute(text("SELECT mobile, status, login_id, gender FROM users")).one()
    assert row == ("9876543210", "active", created["login_id"], "Female")


def test_logout_is_not_repeatable_and_ends_only_that_session(client: TestClient) -> None:
    _created, first = register_and_login(client)
    second = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "2468"}).json()
    first_headers = {"Authorization": f"Bearer {first['access_token']}"}
    second_headers = {"Authorization": f"Bearer {second['access_token']}"}

    assert client.delete("/api/v1/sessions/current", headers=first_headers).status_code == 204
    assert client.delete("/api/v1/sessions/current", headers=first_headers).status_code == 401
    assert client.patch("/api/v1/me", headers=first_headers, json={"full_name": "Nope"}).status_code == 401
    assert client.get("/api/v1/me", headers=second_headers).status_code == 200


def test_logout_without_token_is_401(client: TestClient) -> None:
    assert client.delete("/api/v1/sessions/current").status_code == 401
