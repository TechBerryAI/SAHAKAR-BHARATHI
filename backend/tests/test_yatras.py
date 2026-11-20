from __future__ import annotations

import json
import uuid

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.storage import UnavailableDocumentStorage
from app.db.session import SessionLocal
from app.services.super_admin import SUPER_ADMIN_MOBILE, seed_super_admin
from tests.conftest import register_and_login

START = "2026-12-01T04:00:00Z"
END = "2026-12-03T12:00:00Z"
PDF = b"%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF"


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _set_role(engine: Engine, mobile: str, role: str) -> None:
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE users SET role = :role WHERE mobile = :mobile"),
            {"role": role, "mobile": mobile},
        )


def _user_id(engine: Engine, mobile: str) -> str:
    with engine.connect() as connection:
        found = connection.execute(
            text("SELECT id FROM users WHERE mobile = :mobile"),
            {"mobile": mobile},
        ).scalar_one()
    return str(found)


def _audit(engine: Engine, event_type: str) -> list[dict]:
    with engine.connect() as connection:
        rows = connection.execute(
            text("SELECT extra FROM audit_events WHERE event_type = :event_type"),
            {"event_type": event_type},
        ).scalars()
        return [row if isinstance(row, dict) else json.loads(row) for row in rows]


def _login(client: TestClient, **overrides: object) -> tuple[dict[str, str], dict]:
    body, session = register_and_login(client, **overrides)
    return _headers(str(session["access_token"])), body


def _super(client: TestClient) -> dict[str, str]:
    db = SessionLocal()
    try:
        seed_super_admin(db, pin="1234")
        db.commit()
    finally:
        db.close()
    signed_in = client.post("/api/v1/sessions", json={"identifier": SUPER_ADMIN_MOBILE, "pin": "1234"})
    assert signed_in.status_code == 200, signed_in.text
    return _headers(signed_in.json()["access_token"])


def _district(client: TestClient, headers: dict[str, str], code: str = "NASHIK") -> dict:
    created = client.post(
        "/api/v1/admin/districts",
        headers=headers,
        json={"name": "Nashik", "code": code, "state_name": "Maharashtra"},
    )
    assert created.status_code == 201, created.text
    return created.json()


def _yatra(client: TestClient, headers: dict[str, str], district_id: str, name: str = "Nashik Rath") -> dict:
    created = client.post(
        "/api/v1/yatras",
        headers=headers,
        json={
            "name": name,
            "description": "Village route",
            "district_id": district_id,
            "starts_at": START,
            "ends_at": END,
        },
    )
    assert created.status_code == 201, created.text
    return created.json()


def test_participant_and_unassigned_admin_cannot_create_a_yatra(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    district = _district(client, super_headers)
    participant, _body = _login(client, mobile="9000000001", email="p1@example.com", full_name="Participant One")
    denied = client.post(
        "/api/v1/yatras",
        headers=participant,
        json={"name": "Hidden", "district_id": district["id"], "starts_at": START, "ends_at": END},
    )
    assert denied.status_code == 403

    _login(client, mobile="9000000002", email="a2@example.com", full_name="Plain Admin")
    _set_role(engine, "9000000002", "admin")
    plain, _session = register_and_login(client, mobile="9000000003", email="a3@example.com", full_name="Other Plain")
    _set_role(engine, "9000000003", "admin")
    # The token was issued before the role change; the API reads the role from the database.
    plain_headers = _headers(
        client.post("/api/v1/sessions", json={"identifier": "9000000003", "pin": "2468"}).json()["access_token"]
    )
    assert plain["login_id"]
    still_denied = client.post(
        "/api/v1/yatras",
        headers=plain_headers,
        json={
            "name": "Hidden",
            "district_id": district["id"],
            "starts_at": START,
            "ends_at": END,
            "status": "approved",
        },
    )
    assert still_denied.status_code == 422

    without_status = client.post(
        "/api/v1/yatras",
        headers=plain_headers,
        json={"name": "Hidden", "district_id": district["id"], "starts_at": START, "ends_at": END},
    )
    assert without_status.status_code == 403


def test_district_scope_stops_and_state_machine(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    nashik = _district(client, super_headers, "NASHIK")
    pune = client.post(
        "/api/v1/admin/districts",
        headers=super_headers,
        json={"name": "Pune", "code": "PUNE", "state_name": "Maharashtra"},
    )
    assert pune.status_code == 201, pune.text
    duplicate = client.post(
        "/api/v1/admin/districts",
        headers=super_headers,
        json={"name": "Nashik again", "code": "nashik"},
    )
    assert duplicate.status_code == 409

    _login(client, mobile="9000000011", email="nashik-admin@example.com", full_name="Nashik Admin")
    _login(client, mobile="9000000012", email="pune-admin@example.com", full_name="Pune Admin")
    _login(client, mobile="9000000013", email="yatra-admin@example.com", full_name="Yatra Admin")
    _set_role(engine, "9000000011", "admin")
    _set_role(engine, "9000000012", "admin")
    nashik_admin_id = _user_id(engine, "9000000011")
    pune_admin_id = _user_id(engine, "9000000012")
    yatra_admin_id = _user_id(engine, "9000000013")
    assigned = client.post(
        f"/api/v1/admin/districts/{nashik['id']}/administrators",
        headers=super_headers,
        json={"user_id": nashik_admin_id},
    )
    assert assigned.status_code == 201, assigned.text
    again = client.post(
        f"/api/v1/admin/districts/{nashik['id']}/administrators",
        headers=super_headers,
        json={"user_id": nashik_admin_id},
    )
    assert again.status_code == 409
    pune_assigned = client.post(
        f"/api/v1/admin/districts/{pune.json()['id']}/administrators",
        headers=super_headers,
        json={"user_id": pune_admin_id},
    )
    assert pune_assigned.status_code == 201, pune_assigned.text

    nashik_headers = _headers(
        client.post("/api/v1/sessions", json={"identifier": "9000000011", "pin": "2468"}).json()["access_token"]
    )
    pune_headers = _headers(
        client.post("/api/v1/sessions", json={"identifier": "9000000012", "pin": "2468"}).json()["access_token"]
    )
    yatra_headers = _headers(
        client.post("/api/v1/sessions", json={"identifier": "9000000013", "pin": "2468"}).json()["access_token"]
    )
    created = _yatra(client, nashik_headers, nashik["id"])
    assert created["status"] == "draft"
    assert created["published"] is False
    assert created["public_code"].startswith("YT")
    assert len(created["stops"]) == 0
    checklist = client.get(f"/api/v1/yatras/{created['id']}/protocol", headers=nashik_headers)
    assert checklist.status_code == 200, checklist.text
    titles = {item["title"] for item in checklist.json()["items"]}
    assert titles >= {
        "Police Permission",
        "Traffic Plan",
        "Health Department Approval",
        "Fire Safety Clearance",
        "Crowd Management Plan",
        "Medical Camps Setup",
        "Volunteer Deployment",
    }

    hidden = client.get(f"/api/v1/yatras/{created['id']}", headers=pune_headers)
    assert hidden.status_code == 404
    hidden_patch = client.patch(
        f"/api/v1/yatras/{created['id']}",
        headers=pune_headers,
        json={"name": "Taken"},
    )
    assert hidden_patch.status_code == 404

    granted = client.post(
        f"/api/v1/yatras/{created['id']}/administrators",
        headers=nashik_headers,
        json={"user_id": yatra_admin_id},
    )
    assert granted.status_code == 201, granted.text
    duplicate_grant = client.post(
        f"/api/v1/yatras/{created['id']}/administrators",
        headers=nashik_headers,
        json={"user_id": yatra_admin_id},
    )
    assert duplicate_grant.status_code == 409

    stop = client.post(
        f"/api/v1/yatras/{created['id']}/stops",
        headers=yatra_headers,
        json={"name": "Sinnar", "address": "Market square", "latitude": 19.85, "longitude": 74.0},
    )
    assert stop.status_code == 201, stop.text
    duplicate_stop = client.post(
        f"/api/v1/yatras/{created['id']}/stops",
        headers=yatra_headers,
        json={"name": "Again", "sequence": stop.json()["sequence"]},
    )
    assert duplicate_stop.status_code == 409
    inactive = client.post(
        f"/api/v1/yatras/{created['id']}/stops",
        headers=yatra_headers,
        json={"name": "Closed halt", "active": False},
    )
    assert inactive.status_code == 201, inactive.text
    listed = client.get(f"/api/v1/yatras/{created['id']}/stops", headers=yatra_headers)
    assert listed.status_code == 200
    assert {item["name"] for item in listed.json()["items"]} == {"Sinnar", "Closed halt"}

    blocked = client.post(
        f"/api/v1/yatras/{created['id']}/status",
        headers=yatra_headers,
        json={"status": "planned"},
    )
    assert blocked.status_code == 403
    skipped = client.post(
        f"/api/v1/yatras/{created['id']}/status",
        headers=nashik_headers,
        json={"status": "approved"},
    )
    assert skipped.status_code == 400

    for target in ("planned", "approval_pending"):
        moved = client.post(
            f"/api/v1/yatras/{created['id']}/status",
            headers=nashik_headers,
            json={"status": target},
        )
        assert moved.status_code == 200, moved.text
        assert moved.json()["status"] == target
    too_soon = client.post(
        f"/api/v1/yatras/{created['id']}/status",
        headers=nashik_headers,
        json={"status": "approved"},
    )
    assert too_soon.status_code == 400
    assert too_soon.json()["detail"] == "Required protocol items are still open."

    protocol = client.get(f"/api/v1/yatras/{created['id']}/protocol", headers=nashik_headers).json()["items"]
    for item in protocol:
        if not item["required"]:
            continue
        submitted = client.post(
            f"/api/v1/yatras/{created['id']}/protocol/items/{item['id']}/submit",
            headers=yatra_headers,
        )
        assert submitted.status_code == 200, submitted.text
        reviewed = client.post(
            f"/api/v1/yatras/{created['id']}/protocol/items/{item['id']}/review",
            headers=yatra_headers,
        )
        assert reviewed.status_code == 403
        reviewed = client.post(
            f"/api/v1/yatras/{created['id']}/protocol/items/{item['id']}/review",
            headers=nashik_headers,
        )
        assert reviewed.status_code == 200, reviewed.text
        approved = client.post(
            f"/api/v1/yatras/{created['id']}/protocol/items/{item['id']}/approve",
            headers=nashik_headers,
        )
        assert approved.status_code == 200, approved.text
        assert approved.json()["status"] == "approved"

    approved_yatra = client.post(
        f"/api/v1/yatras/{created['id']}/status",
        headers=nashik_headers,
        json={"status": "approved"},
    )
    assert approved_yatra.status_code == 200, approved_yatra.text
    published = client.post(f"/api/v1/yatras/{created['id']}/publish", headers=nashik_headers)
    assert published.status_code == 200, published.text
    assert published.json()["published"] is True

    participant, _body = _login(client, mobile="9000000014", email="viewer@example.com", full_name="Viewer")
    visible = client.get(f"/api/v1/yatras/{created['id']}", headers=participant)
    assert visible.status_code == 200
    assert [stop["name"] for stop in visible.json()["stops"]] == ["Sinnar"]
    public_stops = client.get(f"/api/v1/yatras/{created['id']}/stops", headers=participant)
    assert public_stops.status_code == 200
    assert [stop["name"] for stop in public_stops.json()["items"]] == ["Sinnar"]
    assert visible.json()["administrators"] == []
    protocol_hidden = client.get(f"/api/v1/yatras/{created['id']}/protocol", headers=participant)
    assert protocol_hidden.status_code == 403
    cannot_edit = client.patch(f"/api/v1/yatras/{created['id']}", headers=participant, json={"name": "No"})
    assert cannot_edit.status_code == 403

    page = client.get("/api/v1/yatras", headers=participant, params={"limit": 1, "q": "Nashik"})
    assert page.status_code == 200
    assert page.json()["total"] >= 1
    assert page.json()["limit"] == 1
    assert len(page.json()["items"]) == 1

    for target in ("upcoming", "in_progress", "completed", "archived"):
        moved = client.post(
            f"/api/v1/yatras/{created['id']}/status",
            headers=nashik_headers,
            json={"status": target},
        )
        assert moved.status_code == 200, moved.text
    frozen = client.post(
        f"/api/v1/yatras/{created['id']}/stops",
        headers=nashik_headers,
        json={"name": "Too late"},
    )
    assert frozen.status_code == 400
    hidden_archive = client.get(f"/api/v1/yatras/{created['id']}", headers=participant)
    assert hidden_archive.status_code == 404
    public_page = client.get("/api/v1/yatras", headers=participant, params={"q": "Nashik Rath"})
    assert public_page.status_code == 200
    assert created["id"] not in {item["id"] for item in public_page.json()["items"]}
    history = client.get(f"/api/v1/yatras/{created['id']}", headers=nashik_headers)
    assert history.status_code == 200
    assert history.json()["status"] == "archived"
    assert {stop["name"] for stop in history.json()["stops"]} == {"Sinnar", "Closed halt"}

    created_audit = _audit(engine, "yatra_created")
    status_audit = _audit(engine, "yatra_status_changed")
    assert created_audit
    assert any(row["to"] == "archived" for row in status_audit)
    blob = json.dumps({"created": created_audit, "status": status_audit}).lower()
    for secret in ("pin", "password", "access_token", "bearer", "otp"):
        assert secret not in blob


def test_protocol_rejection_resubmission_and_documents(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    district = _district(client, super_headers, "DHULE")
    _login(client, mobile="9000000021", email="dhule-admin@example.com", full_name="Dhule Admin")
    _set_role(engine, "9000000021", "admin")
    admin_id = _user_id(engine, "9000000021")
    assert client.post(
        f"/api/v1/admin/districts/{district['id']}/administrators",
        headers=super_headers,
        json={"user_id": admin_id},
    ).status_code == 201
    signed = client.post("/api/v1/sessions", json={"identifier": "9000000021", "pin": "2468"})
    headers = _headers(signed.json()["access_token"])
    yatra = _yatra(client, headers, district["id"], "Dhule Rath")
    items = client.get(f"/api/v1/yatras/{yatra['id']}/protocol", headers=headers).json()["items"]
    item_id = items[0]["id"]
    other_id = items[1]["id"]

    added = client.post(
        f"/api/v1/yatras/{yatra['id']}/protocol/items",
        headers=headers,
        json={"category": "post", "title": "Site cleanup", "required": False},
    )
    assert added.status_code == 201, added.text
    assert added.json()["category"] == "post"

    uploaded = client.post(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/documents",
        headers={**headers, "Content-Type": "application/pdf", "X-Document-Filename": "../../secret.pdf"},
        content=PDF,
    )
    assert uploaded.status_code == 201, uploaded.text
    assert uploaded.json()["original_filename"] == "secret.pdf"
    assert "storage_key" not in uploaded.json()
    bad_type = client.post(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/documents",
        headers={**headers, "Content-Type": "application/pdf", "X-Document-Filename": "notes.pdf"},
        content=b"this is not a pdf",
    )
    assert bad_type.status_code == 400
    bad_name = client.post(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/documents",
        headers={**headers, "Content-Type": "application/pdf", "X-Document-Filename": ".."},
        content=PDF,
    )
    assert bad_name.status_code == 400

    document_id = uploaded.json()["id"]
    downloaded = client.get(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/documents/{document_id}",
        headers=headers,
    )
    assert downloaded.status_code == 200
    assert downloaded.content == PDF
    assert "attachment" in downloaded.headers["content-disposition"]
    assert "secret.pdf" in downloaded.headers["content-disposition"]
    wrong_item = client.get(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{other_id}/documents/{document_id}",
        headers=headers,
    )
    assert wrong_item.status_code == 404

    submitted = client.post(f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/submit", headers=headers)
    assert submitted.status_code == 200
    assert submitted.json()["status"] == "submitted"
    early = client.post(f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/approve", headers=headers)
    assert early.status_code == 400
    reviewed = client.post(f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/review", headers=headers)
    assert reviewed.status_code == 200
    rejected = client.post(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/reject",
        headers=headers,
        json={"reason": "The permission date does not cover the route."},
    )
    assert rejected.status_code == 200, rejected.text
    assert rejected.json()["status"] == "rejected"
    assert "permission date" in rejected.json()["rejection_reason"]
    resubmitted = client.post(f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/submit", headers=headers)
    assert resubmitted.status_code == 200
    assert resubmitted.json()["status"] == "resubmitted"

    events = (
        _audit(engine, "document_uploaded")
        + _audit(engine, "approval_rejected")
        + _audit(engine, "protocol_item_resubmitted")
    )
    assert events
    blob = json.dumps(events).lower()
    for secret in ("pin", "password", "access_token", "bearer"):
        assert secret not in blob
    assert "storage_key" not in blob

    client.app.state.document_storage = UnavailableDocumentStorage()
    unavailable = client.post(
        f"/api/v1/yatras/{yatra['id']}/protocol/items/{item_id}/documents",
        headers={**headers, "Content-Type": "application/pdf", "X-Document-Filename": "again.pdf"},
        content=PDF,
    )
    assert unavailable.status_code == 503
    assert "pin" not in unavailable.text.lower()


def test_district_pages_stay_inside_the_caller_scope(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    created = []
    for index in range(3):
        created.append(_district(client, super_headers, f"D{index:02d}"))
    first = client.get("/api/v1/admin/districts", headers=super_headers, params={"limit": 1, "offset": 0})
    assert first.status_code == 200, first.text
    assert first.json()["limit"] == 1
    assert first.json()["offset"] == 0
    assert first.json()["total"] >= 3
    assert first.json()["has_more"] is True
    assert len(first.json()["items"]) == 1
    second = client.get("/api/v1/admin/districts", headers=super_headers, params={"limit": 1, "offset": 1})
    assert second.status_code == 200, second.text
    assert second.json()["has_more"] is True
    assert second.json()["items"][0]["id"] != first.json()["items"][0]["id"]

    _login(client, mobile="9000000031", email="scope-admin@example.com", full_name="Scope Admin")
    _set_role(engine, "9000000031", "admin")
    admin_id = _user_id(engine, "9000000031")
    assert (
        client.post(
            f"/api/v1/admin/districts/{created[0]['id']}/administrators",
            headers=super_headers,
            json={"user_id": admin_id},
        ).status_code
        == 201
    )
    scoped = _headers(
        client.post("/api/v1/sessions", json={"identifier": "9000000031", "pin": "2468"}).json()["access_token"]
    )
    page = client.get("/api/v1/admin/districts", headers=scoped, params={"limit": 1, "offset": 0})
    assert page.status_code == 200, page.text
    assert page.json()["total"] == 1
    assert page.json()["has_more"] is False
    assert page.json()["items"][0]["id"] == created[0]["id"]
    later = client.get("/api/v1/admin/districts", headers=scoped, params={"limit": 1, "offset": 1})
    assert later.status_code == 200
    assert later.json()["items"] == []
    assert later.json()["has_more"] is False
    denied = client.get("/api/v1/admin/districts", headers=_login(client, mobile="9000000032", email="scope-p@example.com", full_name="Scope Person")[0])
    assert denied.status_code == 403


def test_published_catalog_pages_before_drafts_and_archives(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    district = _district(client, super_headers, "PUBCAT")
    prefix = f"PubCat {uuid.uuid4().hex[:8]}"
    rows = [
        ("archived", True, "2026-07-01T00:00:00Z", f"{prefix} archived"),
        ("draft", False, "2026-06-04T00:00:00Z", f"{prefix} draft 4"),
        ("draft", False, "2026-06-03T00:00:00Z", f"{prefix} draft 3"),
        ("draft", False, "2026-06-02T00:00:00Z", f"{prefix} draft 2"),
        ("draft", False, "2026-06-01T00:00:00Z", f"{prefix} draft 1"),
        ("upcoming", True, "2026-03-01T00:00:00Z", f"{prefix} public mar"),
        ("upcoming", True, "2026-02-01T00:00:00Z", f"{prefix} public feb"),
        ("upcoming", True, "2026-01-01T00:00:00Z", f"{prefix} public jan"),
    ]
    with engine.begin() as connection:
        for status, published, start, name in rows:
            connection.execute(
                text(
                    """
                    INSERT INTO yatras (
                        id, public_code, name, district_id, starts_at, ends_at, status, published
                    )
                    VALUES (
                        :id, :code, :name, :district, :start, :end, :status, :published
                    )
                    """
                ),
                {
                    "id": str(uuid.uuid4()),
                    "code": f"YT{uuid.uuid4().int % 100_000_000:08d}",
                    "name": name,
                    "district": district["id"],
                    "start": start,
                    "end": "2026-12-01T00:00:00Z",
                    "status": status,
                    "published": published,
                },
            )
    participant, _body = _login(
        client,
        mobile="9000000041",
        email="catalog-viewer@example.com",
        full_name="Catalog Viewer",
    )
    first = client.get(
        "/api/v1/yatras",
        headers=participant,
        params={"published": "true", "limit": 2, "offset": 0, "q": prefix},
    )
    assert first.status_code == 200, first.text
    assert first.json()["total"] == 3
    assert first.json()["has_more"] is True
    assert [item["name"] for item in first.json()["items"]] == [
        f"{prefix} public mar",
        f"{prefix} public feb",
    ]
    assert all(item["published"] is True and item["status"] != "archived" for item in first.json()["items"])
    second = client.get(
        "/api/v1/yatras",
        headers=participant,
        params={"published": "true", "limit": 2, "offset": 2, "q": prefix},
    )
    assert second.status_code == 200, second.text
    assert second.json()["total"] == 3
    assert second.json()["has_more"] is False
    assert [item["name"] for item in second.json()["items"]] == [f"{prefix} public jan"]
    assert {item["id"] for item in first.json()["items"]}.isdisjoint({item["id"] for item in second.json()["items"]})

    hidden = client.get(
        "/api/v1/yatras",
        headers=participant,
        params={"published": "false", "q": prefix},
    )
    assert hidden.status_code == 200, hidden.text
    assert hidden.json()["total"] == 0
    assert hidden.json()["items"] == []
    drafts = client.get(
        "/api/v1/yatras",
        headers=participant,
        params={"status": "draft", "q": prefix},
    )
    assert drafts.status_code == 200, drafts.text
    assert drafts.json()["total"] == 0

    staff = client.get("/api/v1/yatras", headers=super_headers, params={"q": prefix, "limit": 20})
    assert staff.status_code == 200, staff.text
    assert staff.json()["total"] == 8
    assert any(item["status"] == "draft" for item in staff.json()["items"])
    staff_public = client.get(
        "/api/v1/yatras",
        headers=super_headers,
        params={"published": "true", "q": prefix, "limit": 20},
    )
    assert staff_public.status_code == 200, staff_public.text
    assert staff_public.json()["total"] == 3
    assert {item["name"] for item in staff_public.json()["items"]} == {
        f"{prefix} public mar",
        f"{prefix} public feb",
        f"{prefix} public jan",
    }

    _login(client, mobile="9000000042", email="catalog-admin@example.com", full_name="Catalog Admin")
    _set_role(engine, "9000000042", "admin")
    admin_id = _user_id(engine, "9000000042")
    assert (
        client.post(
            f"/api/v1/admin/districts/{district['id']}/administrators",
            headers=super_headers,
            json={"user_id": admin_id},
        ).status_code
        == 201
    )
    admin = _headers(client.post("/api/v1/sessions", json={"identifier": "9000000042", "pin": "2468"}).json()["access_token"])
    managed = client.get("/api/v1/yatras", headers=admin, params={"q": prefix, "limit": 20})
    assert managed.status_code == 200, managed.text
    assert any(item["status"] == "draft" for item in managed.json()["items"])
    managed_public = client.get(
        "/api/v1/yatras",
        headers=admin,
        params={"published": "true", "q": prefix, "limit": 20},
    )
    assert managed_public.json()["total"] == 3
    assert all(item["status"] != "draft" for item in managed_public.json()["items"])
