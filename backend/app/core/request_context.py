from __future__ import annotations

import re
from contextvars import ContextVar, Token

_request_id: ContextVar[str] = ContextVar("request_id", default="-")
_client_ip: ContextVar[str] = ContextVar("client_ip", default="-")
_cf_ray: ContextVar[str] = ContextVar("cf_ray", default="-")

# Cloudflare's ray id is a short hex token plus a colo code, for example
# 230b030023ae2822-SJC. Anything else is dropped so a caller cannot write
# arbitrary text into the logs.
_CF_RAY_RE = re.compile(r"^[A-Za-z0-9]{8,32}(?:-[A-Za-z0-9]{2,4})?$")


def current_request_id() -> str:
    return _request_id.get()


def set_request_id(value: str) -> Token[str]:
    return _request_id.set(value)


def reset_request_id(token: Token[str]) -> None:
    _request_id.reset(token)


def current_client_ip() -> str:
    return _client_ip.get()


def set_client_ip(value: str) -> Token[str]:
    return _client_ip.set(value)


def reset_client_ip(token: Token[str]) -> None:
    _client_ip.reset(token)


def current_cf_ray() -> str:
    return _cf_ray.get()


def set_cf_ray(value: str) -> Token[str]:
    return _cf_ray.set(sanitize_cf_ray(value))


def reset_cf_ray(token: Token[str]) -> None:
    _cf_ray.reset(token)


def sanitize_cf_ray(value: str) -> str:
    candidate = value.strip()
    if _CF_RAY_RE.fullmatch(candidate):
        return candidate
    return "-"
