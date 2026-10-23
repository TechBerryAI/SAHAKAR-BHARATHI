from __future__ import annotations

import hashlib
import logging
import secrets
import time
from collections import defaultdict
from typing import Protocol

from sqlalchemy import text
from sqlalchemy.engine import Engine
from sqlalchemy.exc import SQLAlchemyError

logger = logging.getLogger(__name__)

_PRUNE_AFTER_KEYS = 256
_STALE_AFTER_SECONDS = 3600
# Roughly one call in this many also deletes buckets idle for a day.
_DB_PRUNE_ONE_IN = 200
_DB_STALE_AFTER_SECONDS = 24 * 60 * 60


class RateLimiter(Protocol):
    def allow(self, key: str, limit: int, window_seconds: int) -> bool: ...

    def reset(self) -> None: ...


class SlidingWindowLimiter:
    """Process-local sliding window. Not shared across API instances."""

    def __init__(self) -> None:
        self._hits: dict[str, list[float]] = defaultdict(list)

    def allow(self, key: str, limit: int, window_seconds: int) -> bool:
        now = time.monotonic()
        recent = [stamp for stamp in self._hits[key] if now - stamp < window_seconds]
        if len(recent) >= limit:
            self._hits[key] = recent
            self._prune(now)
            return False
        recent.append(now)
        self._hits[key] = recent
        self._prune(now)
        return True

    def reset(self) -> None:
        self._hits.clear()

    def _prune(self, now: float) -> None:
        if len(self._hits) < _PRUNE_AFTER_KEYS:
            return
        stale = [
            key
            for key, stamps in self._hits.items()
            if not stamps or now - stamps[-1] >= _STALE_AFTER_SECONDS
        ]
        for key in stale:
            del self._hits[key]


# One atomic statement per check, so concurrent requests on any number of API
# instances see a single counter. Fixed window: the bucket restarts once the
# window has elapsed since its first hit.
_HIT_SQL = text(
    """
    INSERT INTO rate_limit_buckets (key_hash, window_start, hits)
    VALUES (:key_hash, now(), 1)
    ON CONFLICT (key_hash) DO UPDATE SET
        hits = CASE
            WHEN rate_limit_buckets.window_start <= now() - make_interval(secs => :window)
            THEN 1 ELSE rate_limit_buckets.hits + 1 END,
        window_start = CASE
            WHEN rate_limit_buckets.window_start <= now() - make_interval(secs => :window)
            THEN now() ELSE rate_limit_buckets.window_start END
    RETURNING hits
    """
)
_PRUNE_SQL = text(
    "DELETE FROM rate_limit_buckets WHERE window_start < now() - make_interval(secs => :stale)"
)


class PostgresRateLimiter:
    """Shared fixed-window limiter stored in PostgreSQL.

    Keys are SHA-256 hashed so client IPs and login identifiers are not stored.
    If the database is unreachable the request is allowed: the request itself
    would fail on the same database, and the login lockout still applies.
    """

    def __init__(self, engine: Engine) -> None:
        self._engine = engine

    def allow(self, key: str, limit: int, window_seconds: int) -> bool:
        key_hash = hashlib.sha256(key.encode("utf-8")).hexdigest()
        try:
            with self._engine.begin() as connection:
                hits = connection.execute(
                    _HIT_SQL, {"key_hash": key_hash, "window": window_seconds}
                ).scalar_one()
                if secrets.randbelow(_DB_PRUNE_ONE_IN) == 0:
                    connection.execute(_PRUNE_SQL, {"stale": _DB_STALE_AFTER_SECONDS})
        except SQLAlchemyError:
            logger.warning("Shared rate limiter unavailable; allowing request", exc_info=True)
            return True
        return int(hits) <= limit

    def reset(self) -> None:
        with self._engine.begin() as connection:
            connection.execute(text("DELETE FROM rate_limit_buckets"))
