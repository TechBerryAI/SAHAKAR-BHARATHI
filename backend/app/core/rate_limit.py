from __future__ import annotations

import time
from collections import defaultdict

_PRUNE_AFTER_KEYS = 256
_STALE_AFTER_SECONDS = 3600


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
