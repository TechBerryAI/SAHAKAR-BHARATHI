from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from tests.conftest import register_and_login


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _set_role(engine: Engine, mobile: str, role: str) -> None:
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE users SET role = :role WHERE mobile = :mobile"),
            {"role": role, "mobile": mobile},
        )


def _staff_session(client: TestClient, engine: Engine) -> dict[str, object]:
    _created, session = register_and_login(
        client,
        mobile="9123456780",
        email="staff@example.com",
        full_name="Ravi Kumar",
    )
    _set_role(engine, "9123456780", "admin")
    return session


def test_staff_can_load_a_place_by_id(client: TestClient, engine: Engine) -> None:
    _created, participant = register_and_login(client)
    staff = _staff_session(client, engine)
    created = client.post(
        "/api/v1/admin/places",
        headers=_headers(staff["access_token"]),
        json={"name": "Gate booth"},
    )
    assert created.status_code == 201, created.text
    place_id = created.json()["id"]
    loaded = client.get(
        f"/api/v1/admin/places/{place_id}",
        headers=_headers(staff["access_token"]),
    )
    assert loaded.status_code == 200, loaded.text
    assert loaded.json()["code"] == created.json()["code"]
    assert loaded.json()["name"] == "Gate booth"

    missing = client.get(
        "/api/v1/admin/places/00000000-0000-4000-8000-000000000001",
        headers=_headers(staff["access_token"]),
    )
    assert missing.status_code == 404

    denied = client.get(
        f"/api/v1/admin/places/{place_id}",
        headers=_headers(participant["access_token"]),
    )
    assert denied.status_code == 403


def test_participant_cannot_create_a_place(client: TestClient) -> None:
    _created, session = register_and_login(client)
    denied = client.post(
        "/api/v1/admin/places",
        headers=_headers(session["access_token"]),
        json={"name": "Gate 1"},
    )
    assert denied.status_code == 403


def test_check_in_copies_profile_and_repeat_is_the_same_visit(client: TestClient, engine: Engine) -> None:
    created, participant = register_and_login(client)
    staff = _staff_session(client, engine)
    place = client.post(
        "/api/v1/admin/places",
        headers=_headers(staff["access_token"]),
        json={"name": "Pune stall"},
    )
    assert place.status_code == 201, place.text
    body = place.json()
    assert len(body["code"]) == 6
    assert body["code"].isdigit()
    assert body["active"] is True

    headers = _headers(participant["access_token"])
    first = client.post(
        "/api/v1/attendance",
        headers=headers,
        json={"code": body["code"], "source": "qr"},
    )
    assert first.status_code == 200, first.text
    visit = first.json()
    assert visit["already_recorded"] is False
    assert visit["place_name"] == "Pune stall"
    assert visit["source"] == "qr"
    assert visit["login_id"] == created["login_id"]
    assert visit["full_name"] == "Anita Desai"
    assert visit["mobile"] == "9876543210"
    assert visit["email"] == "anita@example.com"
    assert visit["district"] == "Nashik"
    assert visit["state"] == "Maharashtra"
    assert visit["address_line"] == "12 Village Road"
    assert visit["pincode"] == "422001"

    again = client.post(
        "/api/v1/attendance",
        headers=headers,
        json={"code": body["code"], "source": "code"},
    )
    assert again.status_code == 200, again.text
    assert again.json()["id"] == visit["id"]
    assert again.json()["already_recorded"] is True
    assert again.json()["source"] == "qr"

    history = client.get("/api/v1/attendance", headers=headers)
    assert history.status_code == 200
    assert history.json()["total"] == 1
    assert history.json()["items"][0]["id"] == visit["id"]

    renamed = client.patch(
        "/api/v1/me",
        headers=headers,
        json={"full_name": "Anita Updated"},
    )
    assert renamed.status_code == 200
    still = client.get("/api/v1/attendance", headers=headers)
    assert still.json()["items"][0]["full_name"] == "Anita Desai"

    listed = client.get(
        f"/api/v1/admin/places/{body['id']}/attendance",
        headers=_headers(staff["access_token"]),
    )
    assert listed.status_code == 200, listed.text
    page = listed.json()
    assert page["total"] == 1
    assert page["items"][0]["full_name"] == "Anita Desai"
    assert page["items"][0]["mobile"] == "9876543210"
    assert page["items"][0]["login_id"] == created["login_id"]


def test_unknown_and_inactive_codes_are_rejected(client: TestClient, engine: Engine) -> None:
    _created, participant = register_and_login(client)
    staff = _staff_session(client, engine)
    headers = _headers(participant["access_token"])
    missing = client.post("/api/v1/attendance", headers=headers, json={"code": "000000", "source": "code"})
    assert missing.status_code == 404

    place = client.post(
        "/api/v1/admin/places",
        headers=_headers(staff["access_token"]),
        json={"name": "Closed gate"},
    )
    assert place.status_code == 201, place.text
    closed = client.patch(
        f"/api/v1/admin/places/{place.json()['id']}",
        headers=_headers(staff["access_token"]),
        json={"active": False},
    )
    assert closed.status_code == 200
    assert closed.json()["active"] is False

    rejected = client.post(
        "/api/v1/attendance",
        headers=headers,
        json={"code": place.json()["code"], "source": "qr"},
    )
    assert rejected.status_code == 400

    listed = client.get("/api/v1/admin/places", headers=_headers(staff["access_token"]))
    assert listed.status_code == 200
    assert any(item["id"] == place.json()["id"] and item["active"] is False for item in listed.json()["items"])


def test_failed_codes_are_rate_limited(client: TestClient) -> None:
    _created, participant = register_and_login(client)
    headers = _headers(participant["access_token"])
    for _ in range(10):
        response = client.post(
            "/api/v1/attendance",
            headers=headers,
            json={"code": "111111", "source": "code"},
        )
        assert response.status_code == 404
    blocked = client.post(
        "/api/v1/attendance",
        headers=headers,
        json={"code": "111111", "source": "code"},
    )
    assert blocked.status_code == 429
