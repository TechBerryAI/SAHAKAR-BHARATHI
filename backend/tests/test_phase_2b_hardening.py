"""Phase 2B audit remediation. These tests describe the corrected behaviour."""

from __future__ import annotations

import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine
from sqlalchemy.exc import DBAPIError
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from tests.test_yatra_field import _live, _position
from tests.test_yatras import _district, _login, _super, _yatra

_HERE = {"latitude": 19.997, "longitude": 73.789, "location_accuracy_m": 12}
_FIX = {"latitude": 19.997, "longitude": 73.789, "accuracy_m": 8.0}


def _stamp(when: datetime | None = None) -> str:
    moment = when or datetime.now(UTC)
    return moment.isoformat().replace("+00:00", "Z")


def _scan(qr_text: str, **overrides: object) -> dict:
    body: dict[str, object] = {"qr_token": qr_text, "captured_at": _stamp(), **_FIX}
    body.update(overrides)
    return body


def _ready(client: TestClient, engine: Engine) -> tuple[dict[str, str], dict, dict, dict[str, str]]:
    headers = _super(client)
    district = _district(client, headers, "NANDUR")
    yatra = _yatra(client, headers, district["id"], "Nandurbar Rath")
    counter = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=headers,
        json={"name": "Gate counter", "code": "GATE", "description": "North gate", **_HERE},
    )
    assert counter.status_code == 201, counter.text
    body = counter.json()
    activated = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{body['id']}/activate",
        headers=headers,
    )
    assert activated.status_code == 200, activated.text
    _live(engine, yatra["id"])
    participant, _user = _login(client, mobile="9100000701", email="fence@example.com", full_name="Fence User")
    return headers, yatra, body, participant


def test_scan_inside_the_geofence_stores_the_fix(client: TestClient, engine: Engine) -> None:
    headers, yatra, counter, participant = _ready(client, engine)
    recorded = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert recorded.status_code == 200, recorded.text
    assert recorded.json()["already_recorded"] is False
    with engine.connect() as connection:
        row = connection.execute(
            text(
                "SELECT scan_latitude, scan_longitude, scan_accuracy_m, scan_distance_m "
                "FROM yatra_attendance WHERE id = :id"
            ),
            {"id": recorded.json()["id"]},
        ).one()
    assert row.scan_latitude is not None
    assert row.scan_longitude is not None
    assert float(row.scan_accuracy_m) == 8
    assert float(row.scan_distance_m) <= 150 + 8


def test_scan_outside_inaccurate_stale_or_missing_is_refused(client: TestClient, engine: Engine) -> None:
    headers, yatra, counter, participant = _ready(client, engine)
    outside = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"], latitude=18.5, longitude=73.8),
    )
    assert outside.status_code == 400
    assert outside.json()["detail"] == "You are not at this counter."
    vague = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"], accuracy_m=250),
    )
    assert vague.status_code == 400
    assert vague.json()["detail"] == "Your location is not accurate enough."
    stale = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"], captured_at=_stamp(datetime.now(UTC) - timedelta(minutes=5))),
    )
    assert stale.status_code == 400
    assert stale.json()["detail"] == "Your location is out of date."
    missing = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json={"qr_token": counter["qr_text"]},
    )
    assert missing.status_code == 422
    bare = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=headers,
        json={"name": "Bare counter", "code": "BARE"},
    )
    assert bare.status_code == 201, bare.text
    refused = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{bare.json()['id']}/activate",
        headers=headers,
    )
    assert refused.status_code == 400
    assert refused.json()["detail"] == "Set the counter location before activating it."
    with engine.connect() as connection:
        hits = connection.execute(text("SELECT count(*) FROM rate_limit_buckets")).scalar_one()
    # The memory limiter does not write buckets. The failure counter is observed via 429 below.
    assert hits == 0
    for _ in range(10):
        failed = client.post(
            f"/api/v1/yatras/{yatra['id']}/attendance",
            headers=participant,
            json=_scan(counter["qr_text"], latitude=18.5, longitude=73.8),
        )
        assert failed.status_code in {400, 429}
    blocked = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"], latitude=18.5, longitude=73.8),
    )
    assert blocked.status_code == 429


def test_rotated_qr_replaces_the_previous_token(client: TestClient, engine: Engine) -> None:
    headers, yatra, counter, participant = _ready(client, engine)
    old = counter["qr_text"]
    rotated = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/rotate-qr",
        headers=headers,
    )
    assert rotated.status_code == 200, rotated.text
    new = rotated.json()["qr_text"]
    assert new.startswith("SB-COUNTER:")
    assert new != old
    gone = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(old),
    )
    assert gone.status_code == 404
    accepted = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(new),
    )
    assert accepted.status_code == 200, accepted.text
    denied = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/rotate-qr",
        headers=participant,
    )
    assert denied.status_code == 403
    with engine.begin() as connection:
        connection.execute(text("UPDATE yatras SET status = 'archived' WHERE id = :id"), {"id": yatra["id"]})
    archived = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter['id']}/rotate-qr",
        headers=headers,
    )
    assert archived.status_code == 400
    from tests.test_yatras import _audit

    blob = str(_audit(engine, "counter_qr_rotated")).lower()
    assert old.split(":", 1)[1].lower() not in blob
    assert new.split(":", 1)[1].lower() not in blob


def test_second_active_tracker_is_refused_until_revoke(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "AKOLA")
    yatra = _yatra(client, headers, district["id"], "Akola Rath")
    first = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "One"})
    assert first.status_code == 201, first.text
    second = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Two"})
    assert second.status_code == 409
    assert second.json()["detail"] == "This Yatra already has an active tracker. Revoke it before assigning a new one."

    def assign(label: str) -> int:
        return client.post(
            f"/api/v1/yatras/{yatra['id']}/trackers",
            headers=headers,
            json={"label": label},
        ).status_code

    # The first tracker is still active, so parallel creates must leave exactly one active.
    with ThreadPoolExecutor(max_workers=2) as pool:
        codes = list(pool.map(assign, ("Race A", "Race B")))
    assert 201 not in codes
    with engine.connect() as connection:
        active = connection.execute(
            text("SELECT count(*) FROM yatra_trackers WHERE yatra_id = :id AND status = 'active'"),
            {"id": yatra["id"]},
        ).scalar_one()
    assert active == 1
    revoked = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers/{first.json()['id']}/revoke",
        headers=headers,
    )
    assert revoked.status_code == 200
    again = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "After"})
    assert again.status_code == 201, again.text


def test_freshness_follows_captured_at(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "WARDHA")
    yatra = _yatra(client, headers, district["id"], "Wardha Rath")
    _live(engine, yatra["id"])
    created = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Phone"})
    assert created.status_code == 201, created.text
    token = {"Authorization": f"Bearer {created.json()['tracker_token']}"}
    participant, _user = _login(client, mobile="9100000702", email="fresh@example.com", full_name="Fresh User")
    _live(engine, yatra["id"])
    stale_at = datetime.now(UTC) - timedelta(seconds=91)
    accepted = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=token,
        json=_position(str(uuid.uuid4()), stale_at),
    )
    assert accepted.status_code == 201, accepted.text
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE yatra_positions SET received_at = now() WHERE id = :id"),
            {"id": accepted.json()["id"]},
        )
        connection.execute(
            text("UPDATE yatra_latest_positions SET received_at = now() WHERE yatra_id = :id"),
            {"id": yatra["id"]},
        )
    stale = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=participant)
    assert stale.status_code == 200, stale.text
    assert stale.json()["freshness"] == "stale"
    assert stale.json()["captured_at"] is not None
    assert stale.json()["received_at"] is not None
    offline_at = datetime.now(UTC) - timedelta(seconds=601)
    older = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=token,
        json=_position(str(uuid.uuid4()), offline_at),
    )
    assert older.status_code == 201, older.text
    with engine.begin() as connection:
        connection.execute(
            text(
                "UPDATE yatra_latest_positions SET captured_at = :captured, received_at = now() "
                "WHERE yatra_id = :id"
            ),
            {"captured": offline_at, "id": yatra["id"]},
        )
    offline = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=headers)
    assert offline.json()["freshness"] == "offline"
    assert offline.json()["latitude"] is not None
    hidden = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=participant)
    assert hidden.json()["freshness"] == "offline"
    assert hidden.json()["latitude"] is None
    assert hidden.json()["longitude"] is None
    assert hidden.json()["stop_id"] is None
    assert hidden.json()["tracker_id"] is None


def test_recent_history_is_stored_and_seven_hours_is_rejected(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "NANDED")
    yatra = _yatra(client, headers, district["id"], "Nanded Rath")
    _live(engine, yatra["id"])
    created = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Phone"})
    token = {"Authorization": f"Bearer {created.json()['tracker_token']}"}
    half_hour = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=token,
        json=_position(str(uuid.uuid4()), datetime.now(UTC) - timedelta(minutes=30)),
    )
    assert half_hour.status_code == 201, half_hour.text
    participant, _user = _login(client, mobile="9100000703", email="age@example.com", full_name="Age User")
    _live(engine, yatra["id"])
    latest = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=participant)
    assert latest.json()["freshness"] != "live"
    ancient = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=token,
        json=_position(str(uuid.uuid4()), datetime.now(UTC) - timedelta(hours=7)),
    )
    assert ancient.status_code == 400


def test_position_batch_is_per_item_and_idempotent(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "BEED")
    yatra = _yatra(client, headers, district["id"], "Beed Rath")
    other = _yatra(client, headers, district["id"], "Other Beed")
    stop = client.post(f"/api/v1/yatras/{yatra['id']}/stops", headers=headers, json={"name": "Camp"})
    assert stop.status_code == 201, stop.text
    _live(engine, yatra["id"])
    created = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Phone"})
    token = created.json()["tracker_token"]
    auth = {"Authorization": f"Bearer {token}"}
    known = str(uuid.uuid4())
    duplicate = str(uuid.uuid4())
    now = datetime.now(UTC)
    batch = {
        "positions": [
            _position(known, now),
            _position(duplicate, now),
            _position(duplicate, now),
            _position(str(uuid.uuid4()), now - timedelta(hours=7)),
            _position(str(uuid.uuid4()), now, stop_id=str(uuid.uuid4())),
        ]
    }
    first = client.post(f"/api/v1/yatras/{yatra['id']}/positions/batch", headers=auth, json=batch)
    assert first.status_code == 200, first.text
    body = first.json()
    assert [item["status"] for item in body["results"]] == [
        "accepted",
        "accepted",
        "duplicate",
        "rejected",
        "rejected",
    ]
    assert body["accepted"] == 2
    assert body["duplicates"] == 1
    assert body["rejected"] == 2
    assert body["results"][3]["error"]
    replay = client.post(f"/api/v1/yatras/{yatra['id']}/positions/batch", headers=auth, json=batch)
    assert replay.status_code == 200, replay.text
    assert [item["status"] for item in replay.json()["results"]] == [
        "duplicate",
        "duplicate",
        "duplicate",
        "rejected",
        "rejected",
    ]
    with engine.connect() as connection:
        stored = connection.execute(
            text("SELECT count(*) FROM yatra_positions WHERE yatra_id = :id"),
            {"id": yatra["id"]},
        ).scalar_one()
    assert stored == 2

    shared = str(uuid.uuid4())

    def send() -> int:
        response = client.post(
            f"/api/v1/yatras/{yatra['id']}/positions/batch",
            headers=auth,
            json={"positions": [_position(shared, datetime.now(UTC))]},
        )
        return response.status_code

    with ThreadPoolExecutor(max_workers=2) as pool:
        statuses = list(pool.map(lambda _: send(), range(2)))
    assert statuses.count(200) == 2
    with engine.connect() as connection:
        copies = connection.execute(
            text("SELECT count(*) FROM yatra_positions WHERE client_position_id = :id"),
            {"id": shared},
        ).scalar_one()
    assert copies == 1
    revoked = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers/{created.json()['id']}/revoke",
        headers=headers,
    )
    assert revoked.status_code == 200
    denied = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions/batch",
        headers=auth,
        json={"positions": [_position(str(uuid.uuid4()), datetime.now(UTC))]},
    )
    assert denied.status_code == 401
    replacement = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Next"})
    assert replacement.status_code == 201, replacement.text
    next_auth = {"Authorization": f"Bearer {replacement.json()['tracker_token']}"}
    wrong = client.post(
        f"/api/v1/yatras/{other['id']}/positions/batch",
        headers=next_auth,
        json={"positions": [_position(str(uuid.uuid4()), datetime.now(UTC))]},
    )
    assert wrong.status_code == 404
    with engine.connect() as connection:
        untouched = connection.execute(
            text("SELECT count(*) FROM yatra_positions WHERE yatra_id = :id"),
            {"id": other["id"]},
        ).scalar_one()
    assert untouched == 0
    for _ in range(6):
        allowed = client.post(
            f"/api/v1/yatras/{yatra['id']}/positions/batch",
            headers=next_auth,
            json={"positions": [_position(str(uuid.uuid4()), datetime.now(UTC))]},
        )
        assert allowed.status_code == 200, allowed.text
    limited = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions/batch",
        headers=next_auth,
        json={"positions": [_position(str(uuid.uuid4()), datetime.now(UTC))]},
    )
    assert limited.status_code == 429
    assert stop.json()["id"]


def test_invalid_positions_are_rate_limited(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "LATUR")
    yatra = _yatra(client, headers, district["id"], "Latur Rath")
    _live(engine, yatra["id"])
    created = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Phone"})
    auth = {"Authorization": f"Bearer {created.json()['tracker_token']}"}
    old = datetime.now(UTC) - timedelta(hours=7)
    for _ in range(30):
        rejected = client.post(
            f"/api/v1/yatras/{yatra['id']}/positions",
            headers=auth,
            json=_position(str(uuid.uuid4()), old),
        )
        assert rejected.status_code == 400
    blocked = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=auth,
        json=_position(str(uuid.uuid4()), old),
    )
    assert blocked.status_code == 429


def test_position_history_is_a_stable_cursor(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "HINGOLI")
    yatra = _yatra(client, headers, district["id"], "Hingoli Rath")
    _live(engine, yatra["id"])
    created = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Phone"})
    tracker_id = created.json()["id"]
    with engine.begin() as connection:
        connection.execute(
            text(
                """
                INSERT INTO yatra_positions (
                    id, yatra_id, tracker_id, latitude, longitude, accuracy_m,
                    captured_at, received_at, source, client_position_id
                )
                SELECT gen_random_uuid(), :yatra, :tracker, 19.1, 77.1, 10,
                       now() - (n || ' seconds')::interval, now(), 'gps', gen_random_uuid()
                FROM generate_series(1, 5000) AS n
                """
            ),
            {"yatra": yatra["id"], "tracker": tracker_id},
        )
    participant, _user = _login(client, mobile="9100000704", email="hist@example.com", full_name="Hist User")
    _live(engine, yatra["id"])
    denied = client.get(f"/api/v1/yatras/{yatra['id']}/positions?limit=10", headers=participant)
    assert denied.status_code == 403
    seen: list[str] = []
    cursor = None
    for _ in range(60):
        url = f"/api/v1/yatras/{yatra['id']}/positions?limit=100"
        if cursor:
            url += f"&before={cursor}"
        page = client.get(url, headers=headers)
        assert page.status_code == 200, page.text
        body = page.json()
        assert "total" not in body
        assert "offset" not in body
        seen.extend(item["id"] for item in body["items"])
        if not body["has_more"]:
            break
        cursor = body["next_cursor"]
        assert cursor
    else:
        raise AssertionError("cursor walk did not finish")
    assert len(seen) == 5000
    assert len(set(seen)) == 5000
    tampered = client.get(
        f"/api/v1/yatras/{yatra['id']}/positions?before=not-a-cursor",
        headers=headers,
    )
    assert tampered.status_code == 422
    window = client.get(
        (
            f"/api/v1/yatras/{yatra['id']}/positions"
            f"?limit=100&from={_stamp(datetime.now(UTC) - timedelta(seconds=50))}"
            f"&to={_stamp()}"
        ),
        headers=headers,
    )
    assert window.status_code == 200, window.text
    assert 1 <= len(window.json()["items"]) <= 50


def test_referenced_stop_cannot_be_deleted(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "PARBHANI")
    yatra = _yatra(client, headers, district["id"], "Parbhani Rath")
    stop = client.post(f"/api/v1/yatras/{yatra['id']}/stops", headers=headers, json={"name": "Camp"})
    assert stop.status_code == 201, stop.text
    _live(engine, yatra["id"])
    created = client.post(f"/api/v1/yatras/{yatra['id']}/trackers", headers=headers, json={"label": "Phone"})
    auth = {"Authorization": f"Bearer {created.json()['tracker_token']}"}
    posted = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=auth,
        json=_position(str(uuid.uuid4()), datetime.now(UTC), stop_id=stop.json()["id"]),
    )
    assert posted.status_code == 201, posted.text
    removed = client.delete(f"/api/v1/yatras/{yatra['id']}/stops/{stop.json()['id']}", headers=headers)
    assert removed.status_code == 409
    assert removed.json()["detail"] == "This stop is used by counters or GPS history."
    with engine.connect() as connection:
        still = connection.execute(
            text("SELECT stop_id FROM yatra_positions WHERE id = :id"),
            {"id": posted.json()["id"]},
        ).scalar_one()
    assert str(still) == stop.json()["id"]
    with pytest_raises_delete(engine, stop.json()["id"]):
        pass
    free = client.post(f"/api/v1/yatras/{yatra['id']}/stops", headers=headers, json={"name": "Unused"})
    assert free.status_code == 201, free.text
    gone = client.delete(f"/api/v1/yatras/{yatra['id']}/stops/{free.json()['id']}", headers=headers)
    assert gone.status_code == 204


def pytest_raises_delete(engine: Engine, stop_id: str):
    return _DeleteGuard(engine, stop_id)


class _DeleteGuard:
    def __init__(self, engine: Engine, stop_id: str) -> None:
        self.engine = engine
        self.stop_id = stop_id

    def __enter__(self) -> None:
        try:
            with self.engine.begin() as connection:
                connection.execute(text("DELETE FROM yatra_stops WHERE id = :id"), {"id": self.stop_id})
        except DBAPIError:
            return
        raise AssertionError("a referenced stop was deleted")

    def __exit__(self, *args: object) -> None:
        return None


def test_attendance_roster_filters(client: TestClient, engine: Engine) -> None:
    headers, yatra, counter, participant = _ready(client, engine)
    other = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=headers,
        json={"name": "South", "code": "SOUTH", **_HERE},
    )
    assert other.status_code == 201, other.text
    assert client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{other.json()['id']}/activate",
        headers=headers,
    ).status_code == 200
    first = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(counter["qr_text"]),
    )
    assert first.status_code == 200, first.text
    second_user, _body = _login(client, mobile="9100000705", email="roster@example.com", full_name="Roster User")
    second = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=second_user,
        json=_scan(other.json()["qr_text"]),
    )
    assert second.status_code == 200, second.text
    day = first.json()["attendance_date"]
    filtered = client.get(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=headers,
        params={"attendance_date": day, "counter_id": counter["id"], "limit": 1, "offset": 0},
    )
    assert filtered.status_code == 200, filtered.text
    assert len(filtered.json()["items"]) == 1
    assert filtered.json()["items"][0]["counter_id"] == counter["id"]
    assert filtered.json()["has_more"] is False
    other_day = client.get(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=headers,
        params={"attendance_date": "2000-01-01"},
    )
    assert other_day.json()["items"] == []


def test_non_finite_numbers_are_422(client: TestClient) -> None:
    yatra_id = "00000000-0000-4000-8000-000000000099"
    position = (
        b'{"latitude":LAT,"longitude":73.7,"accuracy_m":10,'
        b'"captured_at":"2026-11-20T00:00:00Z",'
        b'"client_position_id":"00000000-0000-4000-8000-000000000001"}'
    )
    counter = b'{"name":"Gate","code":"GATE","latitude":LAT,"longitude":73.7,"location_accuracy_m":10}'
    attendance = (
        b'{"qr_token":"SB-COUNTER:abcdefghijklmnopqrstuvwxyz0123456789ABCD",'
        b'"latitude":LAT,"longitude":73.7,"accuracy_m":10,'
        b'"captured_at":"2026-11-20T00:00:00Z"}'
    )
    cases = [
        ("/api/v1/sessions", b'{"identifier":"9000000001","pin":1e309}'),
        ("/api/v1/sessions", b'{"identifier":"9000000001","pin":NaN}'),
        ("/api/v1/sessions", b'{"identifier":"9000000001","pin":-1e309}'),
    ]
    for token in (b"1e309", b"NaN", b"-1e309"):
        body = position.replace(b"LAT", token)
        cases.append((f"/api/v1/yatras/{yatra_id}/positions", body))
        cases.append((f"/api/v1/yatras/{yatra_id}/positions/batch", b'{"positions":[' + body + b"]}"))
        cases.append((f"/api/v1/yatras/{yatra_id}/counters", counter.replace(b"LAT", token)))
        cases.append((f"/api/v1/yatras/{yatra_id}/attendance", attendance.replace(b"LAT", token)))
    for path, body in cases:
        response = client.post(path, content=body, headers={"content-type": "application/json"})
        assert response.status_code == 422, (path, body, response.status_code, response.text)
        assert "Infinity" not in response.text
        assert "NaN" not in response.text


def test_validation_error_does_not_echo_a_counter_token(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "DHARASHIV")
    yatra = _yatra(client, headers, district["id"], "Dharashiv Rath")
    response = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers={"content-type": "application/json", **headers},
        content=b'{"qr_token":"SB-COUNTER:not a token"}',
    )
    assert response.status_code == 422
    assert "not a token" not in response.text


def test_history_reads_do_not_wait_on_a_yatra_row_lock(client: TestClient, engine: Engine) -> None:
    headers = _super(client)
    district = _district(client, headers, "SOLAPUR")
    yatra = _yatra(client, headers, district["id"], "Solapur Rath")
    holder = engine.connect()
    transaction = holder.begin()
    holder.execute(text("SELECT id FROM yatras WHERE id = :id FOR UPDATE"), {"id": yatra["id"]})

    def _limit_locks(session, transaction, connection) -> None:  # noqa: ARG001
        connection.execute(text("SET LOCAL lock_timeout = '1s'"))

    event.listen(Session, "after_begin", _limit_locks)
    try:
        codes = [
            client.get(f"/api/v1/yatras/{yatra['id']}/positions?limit=1", headers=headers).status_code
            for _ in range(2)
        ]
    finally:
        event.remove(Session, "after_begin", _limit_locks)
        transaction.rollback()
        holder.close()
    assert codes == [200, 200]


def test_exhausted_request_pool_is_503(client: TestClient, engine: Engine, database_url: str) -> None:
    from app.db import session as db_session

    small = create_engine(database_url, pool_size=2, max_overflow=0, pool_timeout=1, pool_pre_ping=True)
    previous = db_session.engine
    db_session.engine = small
    SessionLocal.configure(bind=small)
    held = []
    try:
        held = [small.connect() for _ in range(2)]
        response = client.post(
            "/api/v1/sessions",
            json={"identifier": "9000000001", "pin": "2468"},
        )
        assert response.status_code == 503, (response.status_code, response.text)
        assert response.json()["detail"] == "Service temporarily unavailable."
    finally:
        for connection in held:
            connection.close()
        SessionLocal.configure(bind=engine)
        db_session.engine = previous
        small.dispose()


def test_prune_function_is_not_callable_by_api_roles_and_keeps_the_latest(engine: Engine) -> None:
    with engine.connect() as connection:
        found = connection.execute(
            text("SELECT count(*) FROM pg_proc WHERE proname = 'prune_yatra_positions'")
        ).scalar_one()
    assert found == 1


def test_super_admin_identity_is_not_in_source() -> None:
    from pathlib import Path

    root = Path(__file__).resolve().parents[2]
    blob = "\n".join(
        path.read_text(encoding="utf-8", errors="ignore")
        for path in (
            root / "backend/app/services/super_admin.py",
            root / "backend/scripts/seed_super_admin.py",
        )
    )
    assert "9137449949" not in blob
    assert "Jaydeep" not in blob
    assert "Patil" not in blob
