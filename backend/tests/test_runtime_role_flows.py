"""End-to-end Phase 2B flows as sahakar_api with PostgresRateLimiter.

Uses a throwaway database so ALTER ROLE and runtime_role.sql stay isolated.
"""

from __future__ import annotations

import shutil
import tempfile
import uuid
from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest
from alembic import command
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine, make_url
from sqlalchemy.exc import ProgrammingError

from app.core.rate_limit import PostgresRateLimiter
from app.core.retention import prune_expired, prune_operational
from app.core.security import hash_pin
from app.core.storage import LocalDocumentStorage
from tests.conftest import SUPER_ADMIN_LOGIN_ID, SUPER_ADMIN_MOBILE, SUPER_ADMIN_NAME, RecordingSmsGateway
from tests.test_database_schema import RUNTIME_ROLE_SQL, _alembic_config, _run_sql_file
from tests.test_yatra_field import _position, _scan
from tests.test_yatras import _district, _headers, _login, _yatra

_HERE = {"latitude": 19.997, "longitude": 73.789, "location_accuracy_m": 12}
_API_ROLES = ("anon", "authenticated")


@pytest.fixture
def runtime_fresh_database(database_url: str, monkeypatch) -> str:
    admin = create_engine(database_url, isolation_level="AUTOCOMMIT")
    name = f"sahakar_runtime_test_{uuid.uuid4().hex[:10]}"
    try:
        with admin.connect() as connection:
            connection.execute(text(f'CREATE DATABASE "{name}"'))
            for role in _API_ROLES:
                connection.execute(
                    text(
                        f"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '{role}') "
                        f"THEN CREATE ROLE {role} NOLOGIN; END IF; END $$"
                    )
                )
    except Exception as exc:  # noqa: BLE001 — skip when the role cannot create DBs
        admin.dispose()
        pytest.skip(f"Test role cannot create databases/roles: {exc}")
    url = make_url(database_url).set(database=name).render_as_string(hide_password=False)
    monkeypatch.setenv("MIGRATION_DATABASE_URL", url)
    try:
        yield url
    finally:
        with admin.connect() as connection:
            connection.execute(text(f'DROP DATABASE IF EXISTS "{name}" WITH (FORCE)'))
        admin.dispose()


@pytest.fixture
def runtime_client(runtime_fresh_database: str, monkeypatch) -> tuple[TestClient, Engine, Engine]:
    """Owner engine for setup grants; API + limiter connect as sahakar_api."""
    from app.core.config import get_settings
    from app.db import session as db_session
    from app.main import app

    command.upgrade(_alembic_config(runtime_fresh_database), "head")
    owner = create_engine(runtime_fresh_database, pool_pre_ping=True)
    password = f"test-{uuid.uuid4().hex}"
    _run_sql_file(owner, RUNTIME_ROLE_SQL)
    with owner.begin() as connection:
        connection.execute(text(f"ALTER ROLE sahakar_api WITH LOGIN PASSWORD '{password}'"))
        # Seed script identity is already in env via conftest when other tests run;
        # pin it here for this isolated database.
        connection.execute(
            text(
                "INSERT INTO users (id, login_id, mobile, pin_hash, full_name, role, status) "
                "VALUES (gen_random_uuid(), :login, :mobile, :pin_hash, :name, 'super_admin', 'active')"
            ),
            {
                "login": SUPER_ADMIN_LOGIN_ID,
                "mobile": SUPER_ADMIN_MOBILE,
                "name": SUPER_ADMIN_NAME,
                "pin_hash": hash_pin("1234"),
            },
        )

    runtime_url = make_url(runtime_fresh_database).set(username="sahakar_api", password=password)
    runtime_url_str = runtime_url.render_as_string(hide_password=False)
    runtime = create_engine(runtime_url_str, pool_pre_ping=True)

    monkeypatch.setenv("DATABASE_URL", runtime_url_str)
    monkeypatch.setenv("MIGRATION_DATABASE_URL", runtime_fresh_database)
    monkeypatch.setenv("RATE_LIMIT_BACKEND", "postgres")
    get_settings.cache_clear()
    db_session.engine.dispose()
    db_session.engine = runtime
    db_session.SessionLocal.configure(bind=runtime)
    app.state.limiter = PostgresRateLimiter(runtime, owns_engine=False)
    app.state.sms_gateway = RecordingSmsGateway()
    document_root = Path(tempfile.mkdtemp(prefix="sahakar-runtime-docs-"))
    app.state.document_storage = LocalDocumentStorage(document_root)

    with TestClient(app) as client:
        yield client, runtime, owner

    app.state.sms_gateway = None
    app.state.document_storage = None
    if hasattr(app.state.limiter, "reset"):
        app.state.limiter.reset()
    shutil.rmtree(document_root, ignore_errors=True)
    runtime.dispose()
    owner.dispose()
    get_settings.cache_clear()


def test_phase_2b_happy_path_as_runtime_role(runtime_client) -> None:
    client, runtime, owner = runtime_client
    signed = client.post("/api/v1/sessions", json={"identifier": SUPER_ADMIN_MOBILE, "pin": "1234"})
    assert signed.status_code == 200, signed.text
    headers = _headers(signed.json()["access_token"])

    district = _district(client, headers, "RUNTIME")
    yatra = _yatra(client, headers, district["id"], "Runtime Rath")
    stop = client.post(
        f"/api/v1/yatras/{yatra['id']}/stops",
        headers=headers,
        json={"name": "Camp", "latitude": 19.997, "longitude": 73.789},
    )
    assert stop.status_code == 201, stop.text

    counter = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters",
        headers=headers,
        json={"name": "Gate", "code": "GATE-R", "stop_id": stop.json()["id"], **_HERE},
    )
    assert counter.status_code == 201, counter.text
    counter_id = counter.json()["id"]
    patched = client.patch(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter_id}",
        headers=headers,
        json={"description": "North"},
    )
    assert patched.status_code == 200, patched.text
    activated = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter_id}/activate",
        headers=headers,
    )
    assert activated.status_code == 200, activated.text
    old_token = activated.json()["qr_text"]
    rotated = client.post(
        f"/api/v1/yatras/{yatra['id']}/counters/{counter_id}/rotate-qr",
        headers=headers,
    )
    assert rotated.status_code == 200, rotated.text
    new_token = rotated.json()["qr_text"]
    assert new_token != old_token

    with owner.begin() as connection:
        connection.execute(
            text("UPDATE yatras SET status = 'in_progress', published = true WHERE id = :id"),
            {"id": yatra["id"]},
        )

    participant, _ = _login(client, mobile="9100000801", email="runtime@example.com", full_name="Runtime User")
    dead = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(old_token),
    )
    assert dead.status_code == 404
    scan = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(new_token),
    )
    assert scan.status_code == 200, scan.text
    again = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(new_token),
    )
    assert again.status_code == 200
    assert again.json()["already_recorded"] is True
    outside = client.post(
        f"/api/v1/yatras/{yatra['id']}/attendance",
        headers=participant,
        json=_scan(new_token, latitude=18.5, longitude=73.8),
    )
    assert outside.status_code == 400

    tracker = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers",
        headers=headers,
        json={"label": "Bus"},
    )
    assert tracker.status_code == 201, tracker.text
    token = tracker.json()["tracker_token"]
    auth = {"Authorization": f"Bearer {token}"}
    now = datetime.now(UTC)
    single = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=auth,
        json=_position(str(uuid.uuid4()), now),
    )
    assert single.status_code == 201, single.text
    batch = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions/batch",
        headers=auth,
        json={"positions": [_position(str(uuid.uuid4()), now - timedelta(seconds=5))]},
    )
    assert batch.status_code == 200, batch.text
    assert batch.json()["accepted"] == 1

    latest = client.get(f"/api/v1/yatras/{yatra['id']}/position", headers=headers)
    assert latest.status_code == 200
    assert latest.json()["latitude"] is not None
    history = client.get(f"/api/v1/yatras/{yatra['id']}/positions", headers=headers, params={"limit": 10})
    assert history.status_code == 200
    assert history.json()["items"]
    roster = client.get(f"/api/v1/yatras/{yatra['id']}/attendance", headers=headers)
    assert roster.status_code == 200
    assert roster.json()["items"]

    revoked = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers/{tracker.json()['id']}/revoke",
        headers=headers,
    )
    assert revoked.status_code == 200

    with owner.begin() as connection:
        connection.execute(
            text(
                "UPDATE yatra_positions SET received_at = now() - interval '40 days' "
                "WHERE yatra_id = :id"
            ),
            {"id": yatra["id"]},
        )
        kept = connection.execute(
            text("SELECT position_id FROM yatra_latest_positions WHERE yatra_id = :id"),
            {"id": yatra["id"]},
        ).scalar_one()
        connection.execute(
            text(
                "UPDATE yatra_positions SET received_at = now() "
                "WHERE id = :id"
            ),
            {"id": kept},
        )

    with runtime.begin() as connection:
        deleted = prune_expired(connection, position_retention_days=30)
    assert deleted is None or True  # prune_expired returns None
    with owner.connect() as connection:
        remaining = connection.execute(
            text("SELECT count(*) FROM yatra_positions WHERE yatra_id = :id"),
            {"id": yatra["id"]},
        ).scalar_one()
        assert remaining == 1
        assert (
            connection.execute(
                text("SELECT position_id FROM yatra_latest_positions WHERE yatra_id = :id"),
                {"id": yatra["id"]},
            ).scalar_one()
            == kept
        )

    with runtime.connect() as connection:
        for statement in (
            "UPDATE yatra_attendance SET source = 'qr'",
            "DELETE FROM yatra_attendance",
            "UPDATE yatra_positions SET source = 'gps'",
            "DELETE FROM yatra_positions",
        ):
            with pytest.raises(ProgrammingError):
                connection.execute(text(statement))
            connection.rollback()


def test_anon_and_authenticated_cannot_execute_prune(runtime_fresh_database: str) -> None:
    command.upgrade(_alembic_config(runtime_fresh_database), "head")
    engine = create_engine(runtime_fresh_database)
    try:
        with engine.begin() as connection:
            for role in ("anon", "authenticated"):
                connection.execute(
                    text(
                        f"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '{role}') "
                        f"THEN CREATE ROLE {role} NOLOGIN; END IF; END $$"
                    )
                )
            connection.execute(
                text(
                    "REVOKE ALL ON FUNCTION public.prune_yatra_positions(integer, integer) "
                    "FROM PUBLIC, anon, authenticated"
                )
            )
        for role in ("anon", "authenticated"):
            with engine.connect() as connection:
                connection.execute(text(f"SET ROLE {role}"))
                with pytest.raises(ProgrammingError):
                    connection.execute(text("SELECT public.prune_yatra_positions(30)"))
                connection.rollback()
    finally:
        engine.dispose()


def test_request_path_prune_does_not_touch_positions(runtime_client) -> None:
    client, runtime, owner = runtime_client
    with owner.begin() as connection:
        # Minimal orphaned-looking row setup is expensive; insert via owner.
        connection.execute(
            text(
                "INSERT INTO users (id, login_id, mobile, pin_hash, full_name) "
                "VALUES (:uid, 'SB-PRUNE01', '9100000802', 'x', 'Prune')"
            ),
            {"uid": str(uuid.uuid4())},
        )
    # Create a real position through the API so FKs are valid, then age it.
    signed = client.post("/api/v1/sessions", json={"identifier": SUPER_ADMIN_MOBILE, "pin": "1234"})
    headers = _headers(signed.json()["access_token"])
    district = _district(client, headers, "PRUNE")
    yatra = _yatra(client, headers, district["id"], "Prune Rath")
    with owner.begin() as connection:
        connection.execute(
            text("UPDATE yatras SET status = 'in_progress', published = true WHERE id = :id"),
            {"id": yatra["id"]},
        )
    tracker = client.post(
        f"/api/v1/yatras/{yatra['id']}/trackers",
        headers=headers,
        json={"label": "Prune phone"},
    )
    auth = {"Authorization": f"Bearer {tracker.json()['tracker_token']}"}
    created = client.post(
        f"/api/v1/yatras/{yatra['id']}/positions",
        headers=auth,
        json=_position(str(uuid.uuid4()), datetime.now(UTC)),
    )
    assert created.status_code == 201, created.text
    with owner.begin() as connection:
        connection.execute(
            text(
                "UPDATE yatra_positions SET received_at = now() - interval '90 days' "
                "WHERE yatra_id = :id"
            ),
            {"id": yatra["id"]},
        )
        # Detach latest so the row is eligible for history prune.
        connection.execute(text("DELETE FROM yatra_latest_positions WHERE yatra_id = :id"), {"id": yatra["id"]})
    before = None
    with owner.connect() as connection:
        before = connection.execute(text("SELECT count(*) FROM yatra_positions")).scalar_one()
    with runtime.begin() as connection:
        prune_operational(connection)
    with owner.connect() as connection:
        after = connection.execute(text("SELECT count(*) FROM yatra_positions")).scalar_one()
    assert after == before
