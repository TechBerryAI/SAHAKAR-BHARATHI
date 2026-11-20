"""Delete rows that are past their usefulness.

The request path (the PostgreSQL rate limiter, about 1 request in 200) calls
`prune_operational`. It deletes expired challenges, sessions, and idle buckets.
It does not touch GPS history.

`prune_expired` is the daily cron (`python -m app.core.retention`). It does
the same operational cleanup and then calls `prune_yatra_positions` in bounded
batches. Audit events are never deleted here.
"""

from __future__ import annotations

import logging
import time

from sqlalchemy import text
from sqlalchemy.engine import Connection
from sqlalchemy.exc import SQLAlchemyError

logger = logging.getLogger(__name__)

# Stop a slow history prune rather than holding a cron transaction open.
_POSITION_PRUNE_BUDGET_SECONDS = 20

_PRUNE_PHONE_VERIFICATIONS = text(
    """
    DELETE FROM phone_verifications
    WHERE created_at < now() - interval '1 day'
    """
)
_PRUNE_SESSIONS = text(
    """
    DELETE FROM sessions
    WHERE expires_at < now() - interval '30 days'
       OR (revoked_at IS NOT NULL AND revoked_at < now() - interval '30 days')
    """
)
_PRUNE_BUCKETS = text(
    """
    DELETE FROM rate_limit_buckets
    WHERE window_start < now() - interval '1 day'
    """
)
_PRUNE_POSITIONS = text("SELECT public.prune_yatra_positions(:days)")


def prune_operational(connection: Connection) -> None:
    """Rows the runtime role is allowed to delete. Never GPS history."""
    connection.execute(_PRUNE_PHONE_VERIFICATIONS)
    connection.execute(_PRUNE_SESSIONS)
    connection.execute(_PRUNE_BUCKETS)


def prune_positions(connection: Connection, *, retention_days: int) -> int:
    """Delete old positions in batches. Keeps the row the latest pin points at."""
    started = time.monotonic()
    deleted_total = 0
    while True:
        if time.monotonic() - started >= _POSITION_PRUNE_BUDGET_SECONDS:
            logger.error(
                "prune_yatra_positions stopped after the time budget with %s rows deleted",
                deleted_total,
            )
            break
        try:
            deleted = connection.execute(_PRUNE_POSITIONS, {"days": retention_days}).scalar_one()
        except SQLAlchemyError:
            logger.error("prune_yatra_positions failed", exc_info=True)
            raise
        deleted_total += int(deleted)
        if not deleted:
            break
    return deleted_total


def prune_expired(connection: Connection, *, position_retention_days: int | None = None) -> None:
    if position_retention_days is None:
        from app.core.config import get_settings

        position_retention_days = get_settings().position_retention_days
    prune_operational(connection)
    prune_positions(connection, retention_days=position_retention_days)


def main() -> None:
    """Run retention once. Idempotent. Used by the Render cron service.

    Reads DATABASE_URL from the environment. Does not print the URL.
    On Render this must be the runtime role, not the migration owner.
    GPS pruning runs only from this entry point.
    """
    import os
    from urllib.parse import urlparse

    from sqlalchemy import create_engine

    from app.core.config import get_settings, normalize_database_url

    settings = get_settings()
    url = settings.database_url.strip()
    if not url:
        logger.error("retention cleanup failed: database URL is missing")
        raise SystemExit(1)
    if os.environ.get("RENDER", "").lower() == "true":
        host = (urlparse(normalize_database_url(url)).hostname or "").lower()
        if host in {"localhost", "127.0.0.1", "::1"}:
            logger.error("retention cleanup failed: local database on Render")
            raise SystemExit(1)
    engine = create_engine(settings.sqlalchemy_url(), pool_pre_ping=True)
    logger.info("retention cleanup starting")
    try:
        with engine.begin() as connection:
            prune_expired(connection)
    except SQLAlchemyError as exc:
        logger.error("retention cleanup failed (%s)", type(exc).__name__)
        raise SystemExit(1) from None
    finally:
        engine.dispose()
    logger.info("retention cleanup finished")


if __name__ == "__main__":
    main()
