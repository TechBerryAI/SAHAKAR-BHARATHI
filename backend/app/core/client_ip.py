"""Canonical client address for throttles, request context, and logs.

Production on Render (docs checked 2026-11-16):

- Render: every public request follows client → Cloudflare → Render load
  balancer → service. The service port is not reachable from the internet.
  https://render.com/docs/uptime-best-practices
  https://render.com/docs/web-services
- Cloudflare sets ``CF-Connecting-IP`` to the single address that connected to
  Cloudflare. That header is how Cloudflare tells an origin the visitor
  address, and it is one address, not a caller-controlled list.
  https://developers.cloudflare.com/fundamentals/reference/http-headers/
  https://developers.cloudflare.com/support/troubleshooting/restoring-visitor-ips/restoring-original-visitor-ips/
- Cloudflare appends to ``X-Forwarded-For`` when the caller already sent one,
  so the left side of that header is caller-controlled. ``X-Real-IP`` is not
  the documented visitor address (Cloudflare strips it when no Worker is
  involved). Neither header is read.

A caller cannot choose the throttle identity: the only TCP peer of the process
is Render's load balancer, and the address we use is the one Cloudflare wrote
into ``CF-Connecting-IP``. A missing or malformed value falls back to that TCP
peer, which the caller also cannot choose.

``CLIENT_IP_SOURCE=peer`` (development and tests) ignores every forwarding
header, including ``CF-Connecting-IP``, because a process on the local network
can be reached directly and those headers would then be caller-controlled.

Do not put another CDN in front of the Render hostname. That would make
``CF-Connecting-IP`` the CDN's address instead of the visitor.
"""

from __future__ import annotations

import ipaddress

from fastapi import Request
from starlette.types import ASGIApp, Receive, Scope, Send

from app.core.request_context import reset_client_ip, set_client_ip

_CF_CONNECTING_IP = "cf-connecting-ip"
_PEER = "peer"


def client_host(request: Request, *, source: str) -> str:
    peer = request.client.host if request.client and request.client.host else "unknown"
    if source.strip().lower() != _CF_CONNECTING_IP:
        return peer
    parsed = _parse_single_ip(request.headers.get("cf-connecting-ip", ""))
    return parsed if parsed is not None else peer


def _parse_single_ip(value: str) -> str | None:
    candidate = value.strip()
    if not candidate or "," in candidate or any(character.isspace() for character in candidate):
        return None
    if candidate.startswith("[") and candidate.endswith("]"):
        candidate = candidate[1:-1]
    if not candidate or "%" in candidate:
        return None
    try:
        return str(ipaddress.ip_address(candidate))
    except ValueError:
        return None


class ClientIpMiddleware:
    """Store the canonical address for the rest of the request.

    ``X-Forwarded-Proto`` is applied only in the Cloudflare source, and only
    when the value is a single ``http`` or ``https``. Cloudflare overwrites
    that header. It does not affect the throttle address.
    """

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        from app.core.config import get_settings

        settings = get_settings()
        request = Request(scope)
        address = client_host(request, source=settings.client_ip_source)
        scope.setdefault("state", {})["client_ip"] = address
        token = set_client_ip(address)
        if settings.client_ip_source.strip().lower() == _CF_CONNECTING_IP:
            _apply_forwarded_proto(scope)
        try:
            await self.app(scope, receive, send)
        finally:
            reset_client_ip(token)


def _apply_forwarded_proto(scope: Scope) -> None:
    raw = ""
    for key, value in scope.get("headers", []):
        if key.decode("latin-1").lower() == "x-forwarded-proto":
            raw = value.decode("latin-1").strip().lower()
            break
    if raw in {"https", "http"}:
        scope["scheme"] = raw
