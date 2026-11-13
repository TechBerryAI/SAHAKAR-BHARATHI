from __future__ import annotations

from fastapi.testclient import TestClient
from sqlalchemy import select, text
from sqlalchemy.engine import Engine
from sqlalchemy.exc import IntegrityError

from app.core.config import get_settings
from app.core.rate_limit import SlidingWindowLimiter
from app.db.session import SessionLocal
from app.models.user import User
from app.schemas.attendance import CheckInCreate
from app.services.attendance import AttendanceService
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


def test_booth_directory_lists_only_active_codes_for_a_signed_in_user(
    client: TestClient, engine: Engine
) -> None:
    _created, participant = register_and_login(client)
    staff = _staff_session(client, engine)
    staff_headers = _headers(staff["access_token"])
    for name in ("Mango stall", "Apple stall", "Closed stall"):
        created = client.post("/api/v1/admin/places", headers=staff_headers, json={"name": name})
        assert created.status_code == 201, created.text
        if name == "Closed stall":
            closed = client.patch(
                f"/api/v1/admin/places/{created.json()['id']}",
                headers=staff_headers,
                json={"active": False},
            )
            assert closed.status_code == 200

    anonymous = client.get("/api/v1/places")
    assert anonymous.status_code == 401

    denied = client.get("/api/v1/admin/places", headers=_headers(participant["access_token"]))
    assert denied.status_code == 403

    listed = client.get("/api/v1/places", headers=_headers(participant["access_token"]))
    assert listed.status_code == 200, listed.text
    names = [item["name"] for item in listed.json()["items"]]
    assert names == ["Apple stall", "Mango stall"]
    assert all(len(item["code"]) == 6 and item["code"].isdigit() for item in listed.json()["items"])


def test_place_rename_empty_update_and_unknown_attendance_page(client: TestClient, engine: Engine) -> None:
    _created, participant = register_and_login(client)
    other, _other_session = register_and_login(
        client,
        mobile="9000000002",
        email="other@example.com",
        full_name="Other Person",
    )
    staff = _staff_session(client, engine)
    staff_headers = _headers(staff["access_token"])
    created = client.post("/api/v1/admin/places", headers=staff_headers, json={"name": "Gate booth"})
    assert created.status_code == 201, created.text
    place = created.json()

    renamed = client.patch(
        f"/api/v1/admin/places/{place['id']}",
        headers=staff_headers,
        json={"name": "  Renamed booth  "},
    )
    assert renamed.status_code == 200, renamed.text
    assert renamed.json()["name"] == "Renamed booth"
    assert renamed.json()["code"] == place["code"]

    empty = client.patch(f"/api/v1/admin/places/{place['id']}", headers=staff_headers, json={})
    assert empty.status_code == 422
    assert "Nothing to update" in empty.text

    short = client.patch(
        f"/api/v1/admin/places/{place['id']}",
        headers=staff_headers,
        json={"name": "A"},
    )
    assert short.status_code == 422

    missing = client.get(
        "/api/v1/admin/places/00000000-0000-4000-8000-000000000099/attendance",
        headers=staff_headers,
    )
    assert missing.status_code == 404
    assert missing.json()["detail"] == "That booth was not found."

    for token in (participant["access_token"],):
        checked = client.post(
            "/api/v1/attendance",
            headers=_headers(token),
            json={"code": place["code"], "source": "code"},
        )
        assert checked.status_code == 200, checked.text
    second = client.post(
        "/api/v1/sessions",
        json={"identifier": other["mobile"], "pin": "2468"},
    )
    assert second.status_code == 200, second.text
    checked = client.post(
        "/api/v1/attendance",
        headers=_headers(second.json()["access_token"]),
        json={"code": place["code"], "source": "qr"},
    )
    assert checked.status_code == 200, checked.text

    first_page = client.get(
        f"/api/v1/admin/places/{place['id']}/attendance",
        headers=staff_headers,
        params={"limit": 1, "offset": 0},
    )
    assert first_page.status_code == 200, first_page.text
    assert first_page.json()["total"] == 2
    assert first_page.json()["limit"] == 1
    assert first_page.json()["offset"] == 0
    assert len(first_page.json()["items"]) == 1

    second_page = client.get(
        f"/api/v1/admin/places/{place['id']}/attendance",
        headers=staff_headers,
        params={"limit": 1, "offset": 1},
    )
    assert second_page.status_code == 200
    assert second_page.json()["items"][0]["id"] != first_page.json()["items"][0]["id"]


def test_place_code_collision_retries_then_stops(client: TestClient, engine: Engine, monkeypatch) -> None:
    staff = _staff_session(client, engine)
    headers = _headers(staff["access_token"])
    codes = iter([42, 42, 77, 42, 42, 42, 42, 42, 42, 42, 42])

    def fixed(_limit: int) -> int:
        return next(codes)

    monkeypatch.setattr("app.services.places.secrets.randbelow", fixed)

    first = client.post("/api/v1/admin/places", headers=headers, json={"name": "First booth"})
    assert first.status_code == 201, first.text
    assert first.json()["code"] == "000042"

    retried = client.post("/api/v1/admin/places", headers=headers, json={"name": "Second booth"})
    assert retried.status_code == 201, retried.text
    assert retried.json()["code"] == "000077"

    exhausted = client.post("/api/v1/admin/places", headers=headers, json={"name": "Third booth"})
    assert exhausted.status_code == 404
    assert exhausted.json()["detail"] == "Could not assign a booth code. Try again."


def test_check_in_requires_auth_and_six_digits(client: TestClient) -> None:
    anonymous = client.post("/api/v1/attendance", json={"code": "123456", "source": "code"})
    assert anonymous.status_code == 401

    _created, participant = register_and_login(client)
    rejected = client.post(
        "/api/v1/attendance",
        headers=_headers(participant["access_token"]),
        json={"code": "12ab56", "source": "code"},
    )
    assert rejected.status_code == 422
    assert "6-digit" in rejected.text


class _HideFirstLookup:
    def __init__(self, inner: object) -> None:
        self._inner = inner
        self.calls = 0

    def find_for_day(self, *args: object, **kwargs: object) -> object:
        self.calls += 1
        if self.calls == 1:
            return None
        return self._inner.find_for_day(*args, **kwargs)  # type: ignore[attr-defined]

    def __getattr__(self, name: str) -> object:
        return getattr(self._inner, name)


def test_check_in_unique_race_returns_the_existing_visit(client: TestClient, engine: Engine) -> None:
    _created, participant = register_and_login(client)
    staff = _staff_session(client, engine)
    place = client.post(
        "/api/v1/admin/places",
        headers=_headers(staff["access_token"]),
        json={"name": "Race booth"},
    )
    assert place.status_code == 201, place.text
    code = place.json()["code"]
    recorded = client.post(
        "/api/v1/attendance",
        headers=_headers(participant["access_token"]),
        json={"code": code, "source": "qr"},
    )
    assert recorded.status_code == 200, recorded.text

    db = SessionLocal()
    try:
        user = db.scalar(select(User).where(User.mobile == "9876543210"))
        assert user is not None
        service = AttendanceService(db)
        service.visits = _HideFirstLookup(service.visits)  # type: ignore[assignment]
        raced = service.check_in(
            user,
            CheckInCreate(code=code, source="code"),
            SlidingWindowLimiter(),
            get_settings(),
        )
    finally:
        db.close()

    assert raced.already_recorded is True
    assert str(raced.id) == recorded.json()["id"]
    with engine.connect() as connection:
        assert connection.execute(text("SELECT count(*) FROM attendance")).scalar_one() == 1


def test_check_in_reraises_when_the_raced_visit_is_gone(client: TestClient, engine: Engine) -> None:
    _created, _participant = register_and_login(client)
    staff = _staff_session(client, engine)
    place = client.post(
        "/api/v1/admin/places",
        headers=_headers(staff["access_token"]),
        json={"name": "Gone booth"},
    )
    assert place.status_code == 201, place.text

    class _AlwaysEmpty:
        def find_for_day(self, *_args: object, **_kwargs: object) -> None:
            return None

    db = SessionLocal()
    try:
        user = db.scalar(select(User).where(User.mobile == "9876543210"))
        assert user is not None
        service = AttendanceService(db)
        service.visits = _AlwaysEmpty()  # type: ignore[assignment]

        def fail_commit() -> None:
            raise IntegrityError("INSERT", {}, type("Orig", (), {"sqlstate": "23505"})())

        service.db.commit = fail_commit  # type: ignore[method-assign]
        try:
            service.check_in(
                user,
                CheckInCreate(code=place.json()["code"], source="code"),
                SlidingWindowLimiter(),
                get_settings(),
            )
            raise AssertionError("expected the unique violation to propagate")
        except IntegrityError:
            pass
    finally:
        db.close()
