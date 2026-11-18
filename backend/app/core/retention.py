"""Delete rows that are past their usefulness.

Called occasionally by the PostgreSQL rate limiter (about 1 request in 200)
and safe to run by hand. Audit events are never deleted here: they are the
accountability record. Operators may archive them after 365 days; that archive
is not implemented in this repository.

Retention:
- phone_verifications: 1 day after creation. OTP lifetime is minutes and the
  registration token lasts 15 minutes, so a day covers incident review.
- sessions: 30 days after expiry, or 30 days after revocation. Active sessions
  are kept.
- rate_limit_buckets: 1 day after the last write. Active keys refresh
  window_start on each check.
"""

from __future__ import annotations

import logging

from sqlalchemy import text
from sqlalchemy.engine import Connection

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


def prune_expired(connection: Connection) -> None:
    connection.execute(_PRUNE_PHONE_VERIFICATIONS)
    connection.execute(_PRUNE_SESSIONS)
    connection.execute(_PRUNE_BUCKETS)


def main() -> None:
    """Run retention once. Idempotent. Used by the Render cron service.

    Reads DATABASE_URL from the environment. Does not print the URL.
    On Render this must be the runtime role, not the migration owner.
    """
    import os
    from urllib.parse import urlparse

    from sqlalchemy import create_engine
    from sqlalchemy.exc import SQLAlchemyError

    from app.core.config import get_settings, normalize_database_url

    logger = logging.getLogger(__name__)
    settings = get_settings()
    url = settings.database_url.strip()
    if not url:
        logger.warning("retention cleanup refused: database URL is missing")
        raise SystemExit(1)
    if os.environ.get("RENDER", "").lower() == "true":
        host = (urlparse(normalize_database_url(url)).hostname or "").lower()
        if host in {"localhost", "127.0.0.1", "::1"}:
            logger.warning("retention cleanup refused: local database on Render")
            raise SystemExit(1)
    engine = create_engine(settings.sqlalchemy_url(), pool_pre_ping=True)
    logger.info("retention cleanup starting")
    try:
        with engine.begin() as connection:
            prune_expired(connection)
    except SQLAlchemyError as exc:
        logger.warning("retention cleanup failed (%s)", type(exc).__name__)
        raise SystemExit(1) from None
    finally:
        engine.dispose()
    logger.info("retention cleanup finished")


if __name__ == "__main__":
    main()
