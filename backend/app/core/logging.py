from __future__ import annotations

import logging
from collections.abc import MutableMapping

_SENSITIVE = {
    "pin",
    "password",
    "token",
    "authorization",
    "otp",
    "access_token",
    "code",
    "verification_token",
    "api_key",
}


class RedactingFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        if isinstance(record.args, dict):
            record.args = _redact_mapping(record.args)
        if isinstance(record.msg, str):
            lowered = record.msg.lower()
            if any(key in lowered for key in _SENSITIVE):
                record.msg = "[redacted log message]"
        return True


def _redact_mapping(values: MutableMapping[str, object]) -> dict[str, object]:
    redacted: dict[str, object] = {}
    for key, value in values.items():
        redacted[key] = "[redacted]" if key.lower() in _SENSITIVE else value
    return redacted


def configure_logging() -> None:
    handler = logging.StreamHandler()
    handler.addFilter(RedactingFilter())
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
        handlers=[handler],
        force=True,
    )
