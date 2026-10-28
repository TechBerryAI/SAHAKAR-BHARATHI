from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.security import verify_pin
from app.db.session import SessionLocal
from app.services.super_admin import SUPER_ADMIN_LOGIN_ID, SUPER_ADMIN_MOBILE, seed_super_admin
from tests.conftest import register_and_login


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _set_role(engine: Engine, mobile: str, role: str) -> None:
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE users SET role = :role WHERE mobile = :mobile"),
            {"role": role, "mobile": mobile},
        )


def test_participant_cannot_list_users_or_change_roles(client: TestClient) -> None:
    _created, session = register_and_login(client)
    headers = _headers(session["access_token"])
    listed = client.get("/api/v1/admin/users", headers=headers)
    assert listed.status_code == 403
    ghost = "00000000-0000-0000-0000-000000000001"
    opened = client.get(f"/api/v1/admin/users/{ghost}", headers=headers)
    assert opened.status_code == 403
    changed = client.post(
        f"/api/v1/admin/users/{ghost}/role",
        headers=headers,
        json={"role": "admin"},
    )
    assert changed.status_code == 403

    patched = client.patch("/api/v1/me", headers=headers, json={"role": "super_admin"})
    assert patched.status_code == 200
    assert patched.json()["role"] == "participant"


def test_admin_can_read_users_but_cannot_change_roles(client: TestClient, engine: Engine) -> None:
    first, first_session = register_and_login(client)
    _second, second_session = register_and_login(
        client,
        mobile="9123456780",
        email="second@example.com",
        full_name="Ravi Kumar",
    )
    _set_role(engine, "9123456780", "admin")
    headers = _headers(second_session["access_token"])

    listed = client.get("/api/v1/admin/users", headers=headers, params={"q": "Anita"})
    assert listed.status_code == 200
    body = listed.json()
    assert body["total"] == 1
    assert body["items"][0]["login_id"] == first["login_id"]
    assert body["items"][0]["full_name"] == "Anita Desai"
    assert "pin_hash" not in body["items"][0]

    user_id = body["items"][0]["id"]
    detail = client.get(f"/api/v1/admin/users/{user_id}", headers=headers)
    assert detail.status_code == 200
    assert detail.json()["mobile"] == "9876543210"
    assert detail.json()["district"] == "Nashik"
    assert "pin_hash" not in detail.json()

    denied = client.post(
        f"/api/v1/admin/users/{user_id}/role",
        headers=headers,
        json={"role": "admin"},
    )
    assert denied.status_code == 403
    status_denied = client.post(
        f"/api/v1/admin/users/{user_id}/status",
        headers=headers,
        json={"status": "disabled"},
    )
    assert status_denied.status_code == 403


def test_super_admin_grants_and_removes_admin_but_cannot_demote_self(client: TestClient) -> None:
    _created, _session = register_and_login(
        client,
        mobile="9123456780",
        email="ravi@example.com",
        full_name="Ravi Kumar",
    )
    db = SessionLocal()
    try:
        seeded = seed_super_admin(db, pin="1234")
        again = seed_super_admin(db, pin="1234")
        assert seeded.id == again.id
        assert seeded.login_id == SUPER_ADMIN_LOGIN_ID
        assert seeded.mobile == SUPER_ADMIN_MOBILE
        assert seeded.role == "super_admin"
        assert seeded.full_name == "Jaydeep Patil"
        assert verify_pin("1234", seeded.pin_hash)
    finally:
        db.close()

    signed_in = client.post("/api/v1/sessions", json={"identifier": SUPER_ADMIN_MOBILE, "pin": "1234"})
    assert signed_in.status_code == 200, signed_in.text
    headers = _headers(signed_in.json()["access_token"])
    assert signed_in.json()["role"] == "super_admin"

    listed = client.get("/api/v1/admin/users", headers=headers, params={"q": "9123456780"})
    assert listed.status_code == 200
    target_id = listed.json()["items"][0]["id"]

    granted = client.post(f"/api/v1/admin/users/{target_id}/role", headers=headers, json={"role": "admin"})
    assert granted.status_code == 200, granted.text
    assert granted.json()["role"] == "admin"

    removed = client.post(
        f"/api/v1/admin/users/{target_id}/role",
        headers=headers,
        json={"role": "participant"},
    )
    assert removed.status_code == 200
    assert removed.json()["role"] == "participant"

    own_id = client.get(
        "/api/v1/admin/users",
        headers=headers,
        params={"q": SUPER_ADMIN_LOGIN_ID},
    ).json()["items"][0]["id"]
    demote = client.post(f"/api/v1/admin/users/{own_id}/role", headers=headers, json={"role": "participant"})
    assert demote.status_code == 403

    db = SessionLocal()
    try:
        count = db.execute(
            text("SELECT count(*) FROM users WHERE mobile = :mobile"),
            {"mobile": SUPER_ADMIN_MOBILE},
        ).scalar_one()
        assert count == 1
        changes = db.execute(
            text("SELECT count(*) FROM audit_events WHERE event_type = 'role_change'")
        ).scalar_one()
        assert changes == 2
    finally:
        db.close()


def test_super_admin_can_block_and_restore_sign_in(client: TestClient) -> None:
    created, _session = register_and_login(client)
    db = SessionLocal()
    try:
        seed_super_admin(db, pin="1234")
    finally:
        db.close()

    signed_in = client.post("/api/v1/sessions", json={"identifier": SUPER_ADMIN_MOBILE, "pin": "1234"})
    assert signed_in.status_code == 200, signed_in.text
    headers = _headers(signed_in.json()["access_token"])
    target_id = client.get(
        "/api/v1/admin/users",
        headers=headers,
        params={"q": created["login_id"]},
    ).json()["items"][0]["id"]

    blocked = client.post(f"/api/v1/admin/users/{target_id}/status", headers=headers, json={"status": "disabled"})
    assert blocked.status_code == 200, blocked.text
    assert blocked.json()["status"] == "disabled"

    rejected = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "2468"})
    assert rejected.status_code == 401

    own_id = client.get(
        "/api/v1/admin/users",
        headers=headers,
        params={"q": SUPER_ADMIN_LOGIN_ID},
    ).json()["items"][0]["id"]
    self_block = client.post(f"/api/v1/admin/users/{own_id}/status", headers=headers, json={"status": "disabled"})
    assert self_block.status_code == 403

    restored = client.post(f"/api/v1/admin/users/{target_id}/status", headers=headers, json={"status": "active"})
    assert restored.status_code == 200
    assert restored.json()["status"] == "active"
    allowed = client.post("/api/v1/sessions", json={"identifier": "9876543210", "pin": "2468"})
    assert allowed.status_code == 200
