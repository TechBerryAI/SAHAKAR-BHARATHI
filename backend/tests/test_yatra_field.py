from __future__ import annotations

import json
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.retention import prune_expired
from tests.test_yatras import _audit, _district, _headers, _login, _set_role, _super, _user_id, _yatra


def _live(engine: Engine, yatra_id: str, *, published: bool = True) -> None:
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE yatras SET status = 'in_progress', published = :published WHERE id = :id"),
            {"id": yatra_id, "published": published},
        )


_HERE = {"latitude": 19.997, "longitude": 73.789, "location_accuracy_m": 12}


def _counter(client: TestClient, headers: dict[str, str], yatra_id: str, code: str = "GATE-1") -> dict:
    created = client.post(
        f"/api/v1/yatras/{yatra_id}/counters",
        headers=headers,
        json={"name": "Gate counter", "code": code, "description": "North gate", **_HERE},
    )
    assert created.status_code == 201, created.text
    return created.json()


def _scan(qr_text: str, **overrides: object) -> dict:
    from datetime import UTC, datetime

    body: dict[str, object] = {
        "qr_token": qr_text,
        "latitude": 19.997,
        "longitude": 73.789,
        "accuracy_m": 8.0,
        "captured_at": datetime.now(UTC).isoformat().replace("+00:00", "Z"),
    }
    body.update(overrides)
    return body


def _token(length: int = 43) -> str:
    return "A" * length


def _signed(client: TestClient, mobile: str) -> dict[str, str]:
    signed = client.post("/api/v1/sessions", json={"identifier": mobile, "pin": "2468"})
    assert signed.status_code == 200, signed.text
    return _headers(signed.json()["access_token"])


def _position(client_position_id: str, captured_at: datetime, **overrides: object) -> dict:
    body: dict[str, object] = {
        "latitude": 19.997,
        "longitude": 73.789,
        "accuracy_m": 12.5,
        "captured_at": captured_at.isoformat().replace("+00:00", "Z"),
        "client_position_id": client_position_id,
    }
    body.update(overrides)
    return body


def test_counter_management_authorization_and_pages(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    district = _district(client, super_headers, "NASHIK")
    other = _district(client, super_headers, "PUNE")
    _login(client, mobile="9100000001", email="nashik-field@example.com", full_name="Nashik Field")
    _login(client, mobile="9100000002", email="pune-field@example.com", full_name="Pune Field")
    _login(client, mobile="9100000003", email="yatra-field@example.com", full_name="Yatra Field")
    _set_role(engine, "9100000001", "admin")
    _set_role(engine, "9100000002", "admin")
    nashik_id = _user_id(engine, "9100000001")
    pune_id = _user_id(engine, "9100000002")
    yatra_admin_id = _user_id(engine, "9100000003")
    assert client.post(
        f"/api/v1/admin/districts/{district['id']}/administrators",
        headers=super_headers,
        json={"user_id": nashik_id},
    ).status_code == 201
    assert client.post(
        f"/api/v1/admin/districts/{other['id']}/administrators",
        headers=super_headers,
        json={"user_id": pune_id},
    ).status_code == 201
    nashik = _signed(client, "9100000001")
    pune = _signed(client, "9100000002")
    operator = _signed(client, "9100000003")
    yatra = _yatra(client, nashik, district["id"])
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/administrators",
        headers=nashik,
        json={"user_id": yatra_admin_id},
    ).status_code == 201
    detail = client.get(f"/api/v1/yatras/{yatra['id']}", headers=operator)
    assert detail.json()["capabilities"]["manage_counters"] is True
    assert detail.json()["capabilities"]["manage_trackers"] is False
    assert detail.json()["capabilities"]["view_track"] is True

    participant, _body = _login(client, mobile="9100000004", email="field-viewer@example.com", full_name="Field Viewer")
    hidden = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=participant,
        json={"name": "Nope", "code": "NOPE"},
    )
    assert hidden.status_code == 404
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=pune,
        json={"name": "Nope", "code": "NOPE"},
    ).status_code == 404

    stop = client.post(
        f"/api/v1/yatras/{yatra['id']}/stops",
        headers=operator,
        json={"name": "Sinnar"},
    )
    assert stop.status_code == 201, stop.text
    other_yatra = _yatra(client, pune, other["id"], "Pune Rath")
    other_stop = client.post(
        f"/api/v1/yatras/{other_yatra['id']}/stops",
        headers=pune,
        json={"name": "Other"},
    )
    assert other_stop.status_code == 201, other_stop.text
    wrong_stop = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=operator,
        json={"name": "Bad", "code": "BAD", "stop_id": other_stop.json()["id"]},
    )
    assert wrong_stop.status_code == 400

    created = _counter(client, operator, yatra["id"])
    assert created["status"] == "inactive"
    assert created["qr_text"].startswith("SB-COUNTER:")
    assert "qr_token" not in created
    listed = client.get(f"/api/v1/yatras/{yatra['id']}/counters", headers=operator)
    assert listed.status_code == 200, listed.text
    assert "qr_text" not in listed.json()["items"][0]
    fetched = client.get(f"/api/v1/yatras/{yatra['id']}/counters/{created['id']}", headers=operator)
    assert fetched.json()["qr_text"] == created["qr_text"]
    foreign = client.get(
        f"/api/v1/yatras/{other_yatra['id']}/counters/{created['id']}",
        headers=pune,
    )
    assert foreign.status_code == 404

    renamed = client.patch(
        f"/api/v1/yatras/{yatra['id']}/counters/{created['id']}",
        headers=operator,
        json={"name": "Main gate", "stop_id": stop.json()["id"], "status": "active"},
    )
    assert renamed.status_code == 422
    renamed = client.patch(
        f"/api/v1/yatras/{yatra['id']}/counters/{created['id']}",
        headers=operator,
        json={"name": "Main gate", "stop_id": stop.json()["id"]},
    )
    assert renamed.status_code == 200, renamed.text
    assert renamed.json()["stop_id"] == stop.json()["id"]
    duplicate = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=operator,
        json={"name": "Second", "code": "gate-1"},
    )
    assert duplicate.status_code == 409
    active = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{created['id']}/activate",
        headers=operator,
    )
    assert active.status_code == 200, active.text
    assert active.json()["status"] == "active"
    inactive = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{created['id']}/deactivate",
        headers=operator,
    )
    assert inactive.json()["status"] == "inactive"

    _live(engine, yatra["id"])
    published = client.get(f"/api/v1/yatras/{yatra['id']}", headers=participant)
    assert published.json()["capabilities"]["manage_counters"] is False
    assert published.json()["capabilities"]["view_track"] is True
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=participant,
        json={"name": "Nope", "code": "NOPE2"},
    ).status_code == 403
    _login(client, mobile="9100000005", email="plain-field@example.com", full_name="Plain Field")
    _set_role(engine, "9100000005", "admin")
    plain_headers = _signed(client, "9100000005")
    assert client.get(f"/api/v1/yatras/{yatra['id']}/counters", headers=plain_headers).status_code == 403
    draft = _yatra(client, nashik, district["id"], "Draft Rath")
    assert client.get(f"/api/v1/yatras/{draft['id']}/counters", headers=plain_headers).status_code == 404

    for index in range(20):
        assert client.post(
            f"/api/v1/yatras/{yatra['id']}/counters",
            headers=operator,
            json={"name": f"Counter {index}", "code": f"C{index}"},
        ).status_code == 201
    page = client.get(f"/api/v1/yatras/{yatra['id']}/counters?limit=20&offset=0", headers=operator)
    assert page.status_code == 200, page.text
    assert page.json()["total"] == 21
    assert len(page.json()["items"]) == 20
    assert page.json()["has_more"] is True
    rest = client.get(f"/api/v1/yatras/{yatra['id']}/counters?limit=20&offset=20", headers=operator)
    assert len(rest.json()["items"]) == 1

    with engine.begin() as connection:
        connection.execute(text("UPDATE yatras SET status = 'archived' WHERE id = :id"), {"id": yatra["id"]})
    frozen = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=nashik,
        json={"name": "Late", "code": "LATE"},
    )
    assert frozen.status_code == 400

    blob = json.dumps(_audit(engine, "counter_created") + _audit(engine, "counter_updated")).lower()
    assert created["qr_text"].split(":", 1)[1].lower() not in blob
    assert "qr_token" not in blob


def test_counter_attendance_rules(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "DHULE")
    yatra = _yatra(client, headers, district["id"], "Dhule Rath")
    other = _yatra(client, headers, district["id"], "Other Rath")
    counter = _counter(client, headers, yatra["id"], "SCAN")
    other_counter = _counter(client, headers, other["id"], "OTHER")
    activated = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/activate",
        headers=headers,
    )
    assert activated.status_code == 200
    assert client.post(
        f"/api/v1/yatras/{other['id']}/counters/{other_counter['id']}/activate",
        headers=headers,
    ).status_code == 200
    participant, _body = _login(client, mobile="9100000011", email="scan@example.com", full_name="Scan User")

    not_open = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert not_open.status_code == 404
    _live(engine, yatra["id"])
    _live(engine, other["id"])
    inactive = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/deactivate",
        headers=headers,
    )
    assert inactive.status_code == 200
    refused = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert refused.status_code == 400
    assert refused.json()["detail"] == "This counter is not accepting attendance."
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/activate",
        headers=headers,
    ).status_code == 200

    malformed = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json={"qr_token": "SB-PLACE:123456"},
    )
    assert malformed.status_code == 422
    unknown = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(f"SB-COUNTER:{_token()}"),
    )
    assert unknown.status_code == 404
    crossed = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(other_counter["qr_text"]),
    )
    assert crossed.status_code == 404

    with engine.begin() as connection:
        connection.execute(text("UPDATE yatras SET status = 'upcoming' WHERE id = :id"), {"id": yatra["id"]})
    waiting = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert waiting.status_code == 400
    assert waiting.json()["detail"] == "This Yatra is not accepting attendance."
    _live(engine, yatra["id"])

    first = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert first.status_code == 200, first.text
    assert first.json()["already_recorded"] is False
    assert "mobile" not in first.json()
    again = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert again.status_code == 200, again.text
    assert again.json()["already_recorded"] is True
    assert again.json()["id"] == first.json()["id"]
    recorded = _audit(engine, "attendance_recorded")
    assert len(recorded) == 1
    assert counter["qr_text"].split(":", 1)[1] not in json.dumps(recorded)

    with engine.begin() as connection:
        connection.execute(text("UPDATE yatra_attendance SET attendance_date = attendance_date - 1"))
    later = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert later.status_code == 200, later.text
    assert later.json()["already_recorded"] is False
    assert later.json()["id"] != first.json()["id"]

    limited_user, _ignored = _login(client, mobile="9100000012", email="limited@example.com", full_name="Limited User")
    for _ in range(10):
        failed = client.post(
            f"/api/v1/yatras/{yatra['id']}/attendance",
            headers=limited_user,
            json=_scan(f"SB-COUNTER:{_token()}"),
        )
        assert failed.status_code == 404
    blocked = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=limited_user,
        json=_scan(f"SB-COUNTER:{_token()}"),
    )
    assert blocked.status_code == 429

    mine = client.get(f"/api/v1/yatras/{yatra['id']}/attendance/mine", headers=participant)
    assert mine.status_code == 200, mine.text
    assert mine.json()["total"] == 2
    staff = client.get(f"/api/v1/yatras/{yatra['id']}/attendance", headers=participant)
    assert staff.status_code == 403
    staff = client.get(f"/api/v1/yatras/{yatra['id']}/attendance", headers=headers)
    assert staff.status_code == 200, staff.text
    assert staff.json()["items"][0]["full_name"] == "Scan User"
    assert "mobile" not in staff.json()["items"][0]

def test_concurrent_counter_scans_record_one_row(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "JALGA")
    yatra = _yatra(client, headers, district["id"], "Jalgaon Rath")
    counter = _counter(client, headers, yatra["id"], "BOTH")
    activated = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/activate",
        headers=headers,
    )
    assert activated.status_code == 200
    _live(engine, yatra["id"])
    participant, _body = _login(client, mobile="9100000021", email="race@example.com", full_name="Race User")

    def scan() -> tuple[int, bool]:
        response = client.post(
            f"/api/v1/yatras/{yatra['id']}/attendance",
            headers=participant,
            json=_scan(counter["qr_text"]),
        )
        body = response.json()
        return response.status_code, bool(body.get("already_recorded"))

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda _index: scan(), range(2)))
    assert results.count((200, False)) == 1
    assert results.count((200, True)) == 1
    with engine.connect() as connection:
        count = connection.execute(
            text("SELECT count(*) FROM yatra_attendance WHERE counter_id = :id"),
            {"id": counter["id"]},
        ).scalar_one()
    assert count == 1


def test_tracker_assignment_and_gps_ingestion(client: TestClient, engine: Engine) -> None:
    super_headers = _super(client)
    district = _district(client, super_headers, "THANE")
    _login(client, mobile="9100000031", email="thane-admin@example.com", full_name="Thane Admin")
    _login(client, mobile="9100000032", email="thane-operator@example.com", full_name="Thane Operator")
    _set_role(engine, "9100000031", "admin")
    admin_id = _user_id(engine, "9100000031")
    operator_id = _user_id(engine, "9100000032")
    assert client.post(
        f"/api/v1/admin/districts/{district['id']}/administrators",
        headers=super_headers,
        json={"user_id": admin_id},
    ).status_code == 201
    manager = _signed(client, "9100000031")
    operator = _signed(client, "9100000032")
    yatra = _yatra(client, manager, district["id"], "Thane Rath")
    other = _yatra(client, manager, district["id"], "Other Thane")
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/administrators",
        headers=manager,
        json={"user_id": operator_id},
    ).status_code == 201
    denied = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers",
        headers=operator,
        json={"label": "Chariot phone"},
    )
    assert denied.status_code == 403
    created = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers",
        headers=manager,
        json={"label": "Chariot phone"},
    )
    assert created.status_code == 201, created.text
    token = created.json()["tracker_token"]
    assert token
    listed = client.get(f"/api/v1/yatras/{yatra['id']}/trackers", headers=manager)
    assert listed.status_code == 200, listed.text
    assert "tracker_token" not in listed.text
    assert "token_hash" not in listed.text
    assigned = _audit(engine, "tracker_assigned")
    assert token not in json.dumps(assigned)

    tracker = {"Authorization": f"Bearer {token}"}
    user_attempt = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=manager,
        json=_position(str(uuid.uuid4()), datetime.now(UTC)),
    )
    assert user_attempt.status_code == 401
    closed = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), datetime.now(UTC)),
    )
    assert closed.status_code == 400
    _live(engine, yatra["id"], published=False)
    stop = client.post(f"/api/v1/yatras/{yatra['id']}/stops", headers=operator, json={"name": "Camp"})
    assert stop.status_code == 201, stop.text
    wrong_stop = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), datetime.now(UTC), stop_id=str(uuid.uuid4())),
    )
    assert wrong_stop.status_code == 400

    now = datetime.now(UTC)
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now, latitude=91),
    ).status_code == 422
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now, longitude=181),
    ).status_code == 422
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now, accuracy_m=0),
    ).status_code == 422
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now, accuracy_m=101),
    ).status_code == 422
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now + timedelta(minutes=2)),
    ).status_code == 400
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now - timedelta(hours=7)),
    ).status_code == 400
    crossed = client.post(
        f"/api/v1/yatras/{other['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now),
    )
    assert crossed.status_code == 404

    first_id = str(uuid.uuid4())
    first = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(first_id, now, stop_id=stop.json()["id"]),
    )
    assert first.status_code == 201, first.text
    replay = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(first_id, now, stop_id=stop.json()["id"]),
    )
    assert replay.status_code == 200, replay.text
    assert replay.json()["id"] == first.json()["id"]
    older = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), now - timedelta(minutes=2)),
    )
    assert older.status_code == 201, older.text
    participant, _body = _login(client, mobile="9100000033", email="track-view@example.com", full_name="Track Viewer")
    hidden_latest = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=participant)
    assert hidden_latest.status_code == 404
    _live(engine, yatra["id"])
    latest = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=participant)
    assert latest.status_code == 200, latest.text
    assert latest.json()["freshness"] == "live"
    assert latest.json()["tracker_id"] == created.json()["id"]
    assert abs(latest.json()["latitude"] - 19.997) < 0.0001
    history = client.get(f"/api/v1/yatras/{yatra['id']}/positions?limit=1", headers=participant)
    assert history.status_code == 403
    history = client.get(f"/api/v1/yatras/{yatra['id']}/positions?limit=1", headers=operator)
    assert history.status_code == 200, history.text
    assert len(history.json()["items"]) == 1
    assert history.json()["has_more"] is True
    assert history.json()["next_cursor"]
    tampered = client.get(
        f"/api/v1/yatras/{yatra['id']}/positions?before=not-a-cursor",
        headers=operator,
    )
    assert tampered.status_code == 422

    for index in range(10):
        accepted = client.post(
            f"/api/v1/yatras/{yatra['id']}/positions",
            headers=tracker,
            json=_position(str(uuid.uuid4()), datetime.now(UTC)),
        )
        assert accepted.status_code == 201, accepted.text
    limited = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), datetime.now(UTC)),
    )
    assert limited.status_code == 429
    still = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(first_id, now),
    )
    assert still.status_code == 200, still.text

    revoked = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers/{created.json()['id']}/revoke",
        headers=manager,
    )
    assert revoked.status_code == 200, revoked.text
    assert revoked.json()["status"] == "revoked"
    denied_fix = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=tracker,
        json=_position(str(uuid.uuid4()), datetime.now(UTC)),
    )
    assert denied_fix.status_code == 401
    offline = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=participant)
    assert offline.json()["freshness"] == "offline"
    assert offline.json()["latitude"] is None
    with engine.connect() as connection:
        stored = connection.execute(
            text("SELECT count(*) FROM yatra_positions WHERE yatra_id = :id"),
            {"id": yatra["id"]},
        ).scalar_one()
    assert stored == 12

    kept = _counter(client, manager, yatra["id"], "KEEP")
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{kept['id']}/activate",
        headers=manager,
    ).status_code == 200
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(kept["qr_text"]),
    ).status_code == 200
    with engine.begin() as connection:
        connection.execute(
            text(
                "UPDATE yatra_positions SET received_at = now() - interval '40 days' "
                "WHERE id = :id"
            ),
            {"id": older.json()["id"]},
        )
        prune_expired(connection)
        remaining = connection.execute(
            text("SELECT id FROM yatra_positions WHERE yatra_id = :id"),
            {"id": yatra["id"]},
        ).scalars().all()
        attendance = connection.execute(text("SELECT count(*) FROM yatra_attendance")).scalar_one()
    assert uuid.UUID(older.json()["id"]) not in set(remaining)
    assert uuid.UUID(first.json()["id"]) in set(remaining)
    assert attendance == 1
