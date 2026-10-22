"""Per-request SQL budgets.

Each statement is a network round-trip to Supabase, measured at roughly 270 ms from the
Render service, so an extra query is user-visible latency. A statement after COMMIT also
opens a second transaction (BEGIN, the query, then ROLLBACK when the session closes).
"""

from __future__ import annotations

from collections.abc import Iterator
from contextlib import contextmanager

from fastapi.testclient import TestClient
from sqlalchemy import event
from sqlalchemy.engine import Engine

from tests.conftest import register_and_login, verified_register_payload


@contextmanager
def record_sql(engine: Engine) -> Iterator[list[str]]:
    events: list[str] = []

    def on_execute(_conn, _cursor, statement: str, *_args: object) -> None:
        events.append(statement.split(None, 1)[0].upper())

    def on_commit(_conn) -> None:
        events.append("COMMIT")

    event.listen(engine, "before_cursor_execute", on_execute)
    event.listen(engine, "commit", on_commit)
    try:
        yield events
    finally:
        event.remove(engine, "before_cursor_execute", on_execute)
        event.remove(engine, "commit", on_commit)


def assert_within_budget(events: list[str], budget: int) -> None:
    statements = [item for item in events if item != "COMMIT"]
    assert len(statements) <= budget, events
    if "COMMIT" in events:
        last_commit = len(events) - 1 - events[::-1].index("COMMIT")
        assert events[last_commit + 1 :] == [], f"SQL ran after COMMIT: {events}"


def test_otp_send_budget(client: TestClient, engine: Engine) -> None:
    with record_sql(engine) as events:
        response = client.post("/api/v1/phone-verifications", json={"mobile": "9876543210"})
    assert response.status_code == 202
    assert response.json()["id"]
    assert_within_budget(events, 3)


def test_registration_budget(client: TestClient, engine: Engine) -> None:
    payload = verified_register_payload(client)
    with record_sql(engine) as events:
        response = client.post("/api/v1/registrations", json=payload)
    assert response.status_code == 201
    body = response.json()
    assert body["full_name"] == "Anita Desai"
    assert body["mobile"] == "9876543210"
    assert body["district"] == "Nashik"
    assert_within_budget(events, 7)


def test_login_budget(client: TestClient, engine: Engine) -> None:
    created, _session = register_and_login(client)
    with record_sql(engine) as events:
        response = client.post("/api/v1/sessions", json={"identifier": created["login_id"], "pin": "2468"})
    assert response.status_code == 200
    body = response.json()
    assert body["login_id"] == created["login_id"]
    assert body["full_name"] == "Anita Desai"
    assert body["state"] == "Maharashtra"
    assert_within_budget(events, 4)


def test_profile_read_and_update_budget(client: TestClient, engine: Engine) -> None:
    _created, session = register_and_login(client)
    headers = {"Authorization": f"Bearer {session['access_token']}"}
    with record_sql(engine) as events:
        read = client.get("/api/v1/me", headers=headers)
    assert read.status_code == 200
    assert_within_budget(events, 1)

    with record_sql(engine) as events:
        updated = client.patch("/api/v1/me", headers=headers, json={"locality": "Ward 5", "full_name": "Anita Rao"})
    assert updated.status_code == 200
    assert_within_budget(events, 3)
    assert updated.json() == client.get("/api/v1/me", headers=headers).json()
    assert updated.json()["locality"] == "Ward 5"
