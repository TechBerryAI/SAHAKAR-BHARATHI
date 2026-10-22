from __future__ import annotations

import hashlib
import re
import secrets
from datetime import UTC, datetime, timedelta

from pwdlib import PasswordHash
from pwdlib.hashers.argon2 import Argon2Hasher

from app.core.config import Settings

_PIN_RE = re.compile(r"^\d{4,6}$")
_MOBILE_RE = re.compile(r"^[6-9]\d{9}$")
_PINCODE_RE = re.compile(r"^\d{6}$")
# OWASP minimum for Argon2id (19 MiB, 2 passes, 1 lane). The library default
# (64 MiB, 3 passes, 4 lanes) costs ~8x more CPU, which takes seconds on a
# fractional-CPU host. Older hashes still verify and are upgraded on login.
_HASHER = PasswordHash((Argon2Hasher(time_cost=2, memory_cost=19_456, parallelism=1),))
_DUMMY_PIN_HASH: str | None = None


def normalize_mobile(value: str) -> str:
    digits = "".join(ch for ch in value if ch.isdigit())
    if digits.startswith("91") and len(digits) == 12:
        digits = digits[2:]
    return digits


def normalize_email(value: str | None) -> str | None:
    if value is None:
        return None
    cleaned = value.strip().lower()
    return cleaned or None


def normalize_name(value: str) -> str:
    return " ".join(value.split())


def canonical_login_identifier(value: str) -> str:
    mobile = normalize_mobile(value)
    if is_valid_mobile(mobile):
        return mobile
    return value.replace(" ", "").upper()


def is_valid_pin(value: str) -> bool:
    return bool(_PIN_RE.fullmatch(value))


def is_valid_mobile(value: str) -> bool:
    return bool(_MOBILE_RE.fullmatch(value))


def is_valid_pincode(value: str | None) -> bool:
    if value is None or value == "":
        return True
    return bool(_PINCODE_RE.fullmatch(value))


def hash_pin(pin: str) -> str:
    return _HASHER.hash(pin)


def verify_pin(pin: str, pin_hash: str) -> bool:
    return _HASHER.verify(pin, pin_hash)


def verify_pin_and_update(pin: str, pin_hash: str) -> tuple[bool, str | None]:
    """Verify, and return a fresh hash when the stored one uses outdated parameters."""
    return _HASHER.verify_and_update(pin, pin_hash)


def dummy_pin_hash() -> str:
    global _DUMMY_PIN_HASH
    if _DUMMY_PIN_HASH is None:
        _DUMMY_PIN_HASH = hash_pin("000000")
    return _DUMMY_PIN_HASH


def generate_login_id() -> str:
    return "SB-" + secrets.token_hex(4).upper()


def issue_session_token() -> tuple[str, str]:
    raw = secrets.token_urlsafe(32)
    return raw, hash_token(raw)


def hash_token(raw: str) -> str:
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def session_expiry(settings: Settings, now: datetime | None = None) -> datetime:
    current = now or datetime.now(UTC)
    return current + timedelta(seconds=settings.session_ttl_seconds)


def hash_identifier(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()
