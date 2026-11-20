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

from app.core.errors import RateLimitedError, ServiceUnavailableError
from app.core.retention import prune_operational

logger = logging.getLogger(__name__)

_PRUNE_AFTER_KEYS = 256
_STALE_AFTER_SECONDS = 3600
# Roughly one call in this many also deletes expired challenges, sessions, and buckets.
_DB_PRUNE_ONE_IN = 200


class RateLimitUnavailable(Exception):
    """The shared counter could not be updated. Callers turn this into HTTP 503."""


class RateLimiter(Protocol):
    def allow(self, key: str, limit: int, window_seconds: int) -> bool: ...

    def reset(self) -> None: ...


def enforce_rate_limit(
    limiter: RateLimiter,
    key: str,
    limit: int,
    window_seconds: int,
    *,
    retry_after: int | None = None,
) -> None:
    """Count one hit. A full window is 429. A broken shared counter is 503, not a free pass."""
    try:
        allowed = limiter.allow(key, limit, window_seconds)
    except RateLimitUnavailable:
        logger.warning("Shared rate limiter unavailable; denying request")
        raise ServiceUnavailableError() from None
    if not allowed:
        raise RateLimitedError(retry_after=retry_after)


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


# The advisory lock serializes one key. The following statement keeps only
# accepted timestamps inside the window, so a full quota at the end of a
# window does not reset and allow another full quota immediately. Denied
# attempts are not stored: an attacker must not be able to grow the array.
_LOCK_SQL = text("SELECT pg_advisory_xact_lock(hashtextextended(:key_hash, 0::bigint))")
_SLIDE_SQL = text(
    """
    WITH prior AS (
        SELECT COALESCE(
            (
                SELECT array_agg(t ORDER BY t)
                FROM unnest(
                    COALESCE(
                        (SELECT hit_times FROM rate_limit_buckets WHERE key_hash = :key_hash),
                        ARRAY[]::timestamptz[]
                    )
                ) AS t
                WHERE t > now() - make_interval(secs => CAST(:window AS int))
            ),
            ARRAY[]::timestamptz[]
        ) AS times
    ),
    decision AS (
        SELECT
            cardinality(times) < CAST(:limit AS int) AS accept,
            CASE
                WHEN cardinality(times) < CAST(:limit AS int) THEN times || ARRAY[now()]
                ELSE times
            END AS next_times
        FROM prior
    ),
    written AS (
        INSERT INTO rate_limit_buckets (key_hash, window_start, hits, hit_times)
        SELECT :key_hash, now(), cardinality(next_times), next_times
        FROM decision
        ON CONFLICT (key_hash) DO UPDATE SET
            window_start = EXCLUDED.window_start,
            hits = EXCLUDED.hits,
            hit_times = EXCLUDED.hit_times
        RETURNING key_hash
    )
    SELECT decision.accept
    FROM decision
    JOIN written ON true
    """
)


class PostgresRateLimiter:
    """Shared sliding-window limiter stored in PostgreSQL.

    Keys are SHA-256 hashed so client IPs and login identifiers are not stored.
    If the database is unreachable the check fails closed (the caller returns 503).
    Allowing the request would drop the throttle during an outage.
    """

    def __init__(self, engine: Engine, *, owns_engine: bool = False) -> None:
        self._engine = engine
        self._owns_engine = owns_engine

    def dispose(self) -> None:
        if self._owns_engine:
            self._engine.dispose()

    def allow(self, key: str, limit: int, window_seconds: int) -> bool:
        key_hash = hashlib.sha256(key.encode("utf-8")).hexdigest()
        try:
            with self._engine.begin() as connection:
                connection.execute(_LOCK_SQL, {"key_hash": key_hash})
                allowed = connection.execute(
                    _SLIDE_SQL,
                    {"key_hash": key_hash, "window": window_seconds, "limit": limit},
                ).scalar_one()
        except SQLAlchemyError as exc:
            logger.warning("Shared rate limiter unavailable (%s)", type(exc).__name__)
            raise RateLimitUnavailable from None
        if secrets.randbelow(_DB_PRUNE_ONE_IN) == 0:
            self._prune()
        return bool(allowed)

    def _prune(self) -> None:
        # GPS history is pruned only by the retention cron. This path must not
        # call prune_yatra_positions or delete from yatra_positions.
        try:
            with self._engine.begin() as connection:
                prune_operational(connection)
        except SQLAlchemyError as exc:
            logger.warning("Retention cleanup skipped (%s)", type(exc).__name__)

    def reset(self) -> None:
        with self._engine.begin() as connection:
            connection.execute(text("DELETE FROM rate_limit_buckets"))
