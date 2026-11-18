from __future__ import annotations

from sqlalchemy import text
from sqlalchemy.engine import Engine

from app.core.config import get_settings
from app.core.retention import prune_expired


def test_prune_expired_removes_only_stale_rows(engine: Engine) -> None:
    with engine.begin() as connection:
        user_id = connection.execute(
            text(
                "INSERT INTO users (id, login_id, mobile, pin_hash, full_name) "
                "VALUES (gen_random_uuid(), 'SB-00000077', '9000000001', 'x', 'Retention') "
                "RETURNING id"
            )
        ).scalar_one()
        connection.execute(
            text(
                """
                INSERT INTO sessions (id, user_id, token_hash, expires_at)
                VALUES
                    (gen_random_uuid(), :uid, :old_hash, now() - interval '40 days'),
                    (gen_random_uuid(), :uid, :live_hash, now() + interval '1 day'),
                    (gen_random_uuid(), :uid, :revoked_hash, now() + interval '1 day'),
                    (gen_random_uuid(), :uid, :recent_expiry_hash, now() - interval '29 days')
                """
            ),
            {
                "uid": user_id,
                "old_hash": "1" * 64,
                "live_hash": "2" * 64,
                "revoked_hash": "3" * 64,
                "recent_expiry_hash": "4" * 64,
            },
        )
        connection.execute(
            text(
                "UPDATE sessions SET revoked_at = now() - interval '1 day' "
                "WHERE token_hash = :revoked_hash"
            ),
            {"revoked_hash": "3" * 64},
        )
        connection.execute(
            text(
                """
                INSERT INTO phone_verifications (id, mobile, code_hash, expires_at, created_at)
                VALUES
                    (gen_random_uuid(), '9000000001', 'hash', now(), now() - interval '2 days'),
                    (gen_random_uuid(), '9000000002', 'hash', now() + interval '5 minutes', now()),
                    (gen_random_uuid(), '9000000003', 'hash', now(), now() - interval '23 hours')
                """
            )
        )
        connection.execute(
            text(
                """
                INSERT INTO rate_limit_buckets (key_hash, window_start, hits, hit_times)
                VALUES
                    ('retention-old', now() - interval '2 days', 1, '{}'),
                    ('retention-new', now(), 1, ARRAY[now()])
                """
            )
        )
        connection.execute(
            text(
                "INSERT INTO audit_events (id, event_type, created_at) "
                "VALUES (gen_random_uuid(), 'retention_probe', now() - interval '400 days')"
            )
        )
        prune_expired(connection)
        prune_expired(connection)
        sessions = connection.execute(
            text("SELECT token_hash FROM sessions WHERE user_id = :uid"),
            {"uid": user_id},
        ).scalars().all()
        phones = connection.execute(
            text("SELECT mobile FROM phone_verifications WHERE mobile IN ('9000000001', '9000000002', '9000000003')")
        ).scalars().all()
        buckets = connection.execute(
            text("SELECT key_hash FROM rate_limit_buckets WHERE key_hash LIKE 'retention-%'")
        ).scalars().all()
        audits = connection.execute(
            text("SELECT count(*) FROM audit_events WHERE event_type = 'retention_probe'")
        ).scalar_one()
    assert set(sessions) == {"2" * 64, "3" * 64, "4" * 64}
    assert set(phones) == {"9000000002", "9000000003"}
    assert buckets == ["retention-new"]
    assert audits == 1


def test_retention_command_does_not_log_the_database_url(monkeypatch, caplog) -> None:
    import logging

    from app.core.retention import main

    monkeypatch.setenv("DATABASE_URL", "")
    get_settings.cache_clear()
    with caplog.at_level(logging.WARNING):
        try:
            main()
        except SystemExit as exc:
            assert exc.code == 1
        else:
            raise AssertionError("missing database URL must stop the command")
    assert "postgresql" not in caplog.text
    assert "postgres://" not in caplog.text.lower()
