import json
import logging
import math
import re
import uuid
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.exc import TimeoutError as SATimeoutError
from starlette.datastructures import MutableHeaders
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from app.api.v1.router import api_router
from app.core.client_ip import ClientIpMiddleware
from app.core.config import Settings, get_settings
from app.core.logging import configure_logging
from app.core.rate_limit import PostgresRateLimiter, RateLimiter, SlidingWindowLimiter
from app.core.request_context import reset_cf_ray, reset_request_id, set_cf_ray, set_request_id
from app.db import session as db_session
from app.db.session import SessionLocal
from app.schemas.common import HealthResponse

logger = logging.getLogger(__name__)

_CORS_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
_CORS_HEADERS = ["Authorization", "Content-Type", "X-Document-Filename"]
_DOCUMENT_UPLOAD = re.compile(
    r"^/api/v1/yatras/[0-9a-fA-F-]{36}/protocol/items/[0-9a-fA-F-]{36}/documents$"
)
_READY_TIMEOUT_MS = 2000
_SENSITIVE_FIELDS = frozenset(
    {
        "pin",
        "password",
        "token",
        "access_token",
        "authorization",
        "code",
        "verification_token",
        "qr_token",
        "qr_text",
        "tracker_token",
    }
)


def _non_finite(value: object) -> bool:
    return isinstance(value, float) and not math.isfinite(value)


def _scrub_non_finite(value: object) -> object:
    if _non_finite(value):
        return None
    if isinstance(value, dict):
        return {key: _scrub_non_finite(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_scrub_non_finite(item) for item in value]
    return value


def sanitize_validation_errors(errors: list[object]) -> list[object]:
    sanitized: list[object] = []
    for error in errors:
        item = dict(error)
        loc = item.get("loc", ())
        names = {str(part).lower() for part in loc} if isinstance(loc, (list, tuple)) else set()
        raw_input = item.get("input")
        # Missing-field and model-level errors carry the whole parent object, including any PIN.
        if names & _SENSITIVE_FIELDS or isinstance(raw_input, (dict, list)) or _non_finite(raw_input):
            item.pop("input", None)
        elif "input" in item:
            item["input"] = _scrub_non_finite(raw_input)
        if "ctx" in item:
            item["ctx"] = _scrub_non_finite(item["ctx"])
        sanitized.append(item)
    return jsonable_encoder(sanitized)


def validation_error_response(exc: RequestValidationError) -> JSONResponse:
    return JSONResponse({"detail": sanitize_validation_errors(exc.errors())}, status_code=422)


class RequestTooLargeError(HTTPException):
    # A FastAPI HTTPException so body parsing re-raises it as 413 instead of
    # wrapping it in a generic 400 "error parsing the body".
    def __init__(self) -> None:
        super().__init__(status_code=413, detail="Request too large.")


class RequestSizeLimitMiddleware:
    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        settings = get_settings()
        path = scope.get("path", "")
        if scope.get("method") == "POST" and _DOCUMENT_UPLOAD.fullmatch(path):
            limit = settings.document_max_bytes
        else:
            limit = settings.max_request_bytes
        headers = {key.decode("latin-1").lower(): value.decode("latin-1") for key, value in scope.get("headers", [])}
        content_length = headers.get("content-length")
        if content_length is not None:
            try:
                size = int(content_length)
            except ValueError:
                response = JSONResponse({"detail": "Invalid Content-Length header."}, status_code=400)
                await response(scope, receive, send)
                return
            if size > limit:
                response = JSONResponse({"detail": "Request too large."}, status_code=413)
                await response(scope, receive, send)
                return

        # Chunked bodies carry no Content-Length, so count bytes as they arrive.
        received = 0
        response_started = False

        async def limited_receive() -> Message:
            nonlocal received
            message = await receive()
            if message["type"] == "http.request":
                received += len(message.get("body", b""))
                if received > limit:
                    raise RequestTooLargeError
            return message

        async def tracking_send(message: Message) -> None:
            nonlocal response_started
            if message["type"] == "http.response.start":
                response_started = True
            await send(message)

        try:
            await self.app(scope, limited_receive, tracking_send)
        except RequestTooLargeError:
            if not response_started:
                await JSONResponse({"detail": "Request too large."}, status_code=413)(scope, receive, send)


class NonFiniteJsonMiddleware:
    """Reject JSON bodies that decode to NaN or Infinity before auth runs."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http" or scope.get("method") not in {"POST", "PUT", "PATCH"}:
            await self.app(scope, receive, send)
            return
        headers = {key.decode("latin-1").lower(): value.decode("latin-1") for key, value in scope.get("headers", [])}
        content_type = headers.get("content-type", "")
        if "application/json" not in content_type:
            await self.app(scope, receive, send)
            return
        chunks: list[bytes] = []
        while True:
            message = await receive()
            if message["type"] != "http.request":
                await self.app(scope, receive, send)
                return
            chunks.append(message.get("body", b""))
            if not message.get("more_body", False):
                break
        body = b"".join(chunks)

        def replay() -> Receive:
            sent = False

            async def _receive() -> Message:
                nonlocal sent
                if sent:
                    return {"type": "http.request", "body": b"", "more_body": False}
                sent = True
                return {"type": "http.request", "body": body, "more_body": False}

            return _receive

        if body:
            try:
                parsed = json.loads(body.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError):
                await self.app(scope, replay(), send)
                return
            if _contains_non_finite(parsed):
                await JSONResponse(
                    {"detail": [{"type": "value_error", "msg": "Numbers must be finite."}]},
                    status_code=422,
                )(scope, replay(), send)
                return
        await self.app(scope, replay(), send)


def _contains_non_finite(value: object) -> bool:
    if _non_finite(value):
        return True
    if isinstance(value, dict):
        return any(_contains_non_finite(item) for item in value.values())
    if isinstance(value, list):
        return any(_contains_non_finite(item) for item in value)
    return False


class PrivateNetworkAccessMiddleware:
    """Let a local page call loopback. Chrome asks on the preflight and blocks without this."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        headers = {key.decode("latin-1").lower(): value.decode("latin-1") for key, value in scope.get("headers", [])}
        allow = headers.get("access-control-request-private-network") == "true"

        async def send_with_header(message: Message) -> None:
            if allow and message["type"] == "http.response.start":
                MutableHeaders(scope=message)["Access-Control-Allow-Private-Network"] = "true"
            await send(message)

        await self.app(scope, receive, send_with_header)


def _header(scope: Scope, name: str) -> str:
    target = name.lower().encode("latin-1")
    for key, value in scope.get("headers", []):
        if key.lower() == target:
            return value.decode("latin-1", errors="replace")[:128]
    return ""


class RequestIdMiddleware:
    """Server-generated id for logs and responses. A client-supplied id is ignored."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        request_id = uuid.uuid4().hex
        token = set_request_id(request_id)
        ray_token = set_cf_ray(_header(scope, "cf-ray"))

        async def send_with_id(message: Message) -> None:
            if message["type"] == "http.response.start":
                MutableHeaders(scope=message)["X-Request-ID"] = request_id
            await send(message)

        try:
            await self.app(scope, receive, send_with_id)
        finally:
            reset_request_id(token)
            reset_cf_ray(ray_token)


class ApiResponseHeadersMiddleware:
    """API responses carry participant data and tokens: never cache them, never sniff them."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http" or not scope.get("path", "").startswith("/api/"):
            await self.app(scope, receive, send)
            return

        async def send_with_headers(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                headers.setdefault("Cache-Control", "no-store")
                headers.setdefault("X-Content-Type-Options", "nosniff")
                if get_settings().is_production:
                    headers.setdefault(
                        "Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains",
                    )
            await send(message)

        await self.app(scope, receive, send_with_headers)


class SafeErrorMiddleware:
    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        try:
            await self.app(scope, receive, send)
        except StarletteHTTPException as exc:
            response = JSONResponse(
                {"detail": exc.detail},
                status_code=exc.status_code,
                headers=exc.headers,
            )
            await response(scope, receive, send)
        except RequestValidationError as exc:
            await validation_error_response(exc)(scope, receive, send)
        except SATimeoutError:
            await JSONResponse({"detail": "Service temporarily unavailable."}, status_code=503)(
                scope, receive, send
            )
        except Exception:
            logger.exception("Unhandled application error")
            await JSONResponse({"detail": "An unexpected error occurred."}, status_code=500)(scope, receive, send)


def build_limiter(settings: Settings) -> RateLimiter:
    if settings.uses_shared_rate_limits:
        # A separate engine so limiter checkouts cannot take the request pool,
        # and a request-pool timeout cannot turn into an unhandled 500.
        engine = create_engine(settings.sqlalchemy_url(), **settings.rate_limit_engine_kwargs())
        return PostgresRateLimiter(engine, owns_engine=True)
    return SlidingWindowLimiter()


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    configure_logging()
    settings = get_settings()
    # Tests install their own limiter before startup.
    if getattr(_app.state, "limiter", None) is None:
        _app.state.limiter = build_limiter(settings)
    yield
    limiter = getattr(_app.state, "limiter", None)
    dispose = getattr(limiter, "dispose", None)
    if callable(dispose):
        dispose()
    db_session.engine.dispose()


def _database_ready() -> bool:
    try:
        with SessionLocal() as db:
            with db.begin():
                db.execute(text(f"SET LOCAL statement_timeout = '{_READY_TIMEOUT_MS}'"))
                db.execute(text("SELECT 1"))
        return True
    except (SQLAlchemyError, OSError):
        logger.warning("Readiness check failed", exc_info=True)
        return False


async def _validation_error_handler(_request: Request, exc: RequestValidationError) -> JSONResponse:
    return validation_error_response(exc)


def _health_response(ready: bool) -> JSONResponse:
    payload = HealthResponse(
        status="ok" if ready else "unavailable",
        database="ok" if ready else "unavailable",
    )
    return JSONResponse(payload.model_dump(), status_code=200 if ready else 503)


def create_app() -> FastAPI:
    settings = get_settings()
    docs_enabled = not settings.is_production
    application = FastAPI(
        title="Sahakar Bharati API",
        version="0.1.0",
        lifespan=lifespan,
        docs_url="/docs" if docs_enabled else None,
        redoc_url="/redoc" if docs_enabled else None,
        openapi_url="/openapi.json" if docs_enabled else None,
    )
    application.add_middleware(RequestSizeLimitMiddleware)
    application.add_middleware(NonFiniteJsonMiddleware)
    application.add_middleware(SafeErrorMiddleware)
    application.add_middleware(ApiResponseHeadersMiddleware)
    if settings.cors_origin_list:
        application.add_middleware(
            CORSMiddleware,
            allow_origins=settings.cors_origin_list,
            # Clients send bearer tokens, never cookies.
            allow_credentials=False,
            allow_methods=_CORS_METHODS,
            # Dev pages and browser tooling add preflight headers we do not ship
            # in production. A rejected header is a 400 with no CORS response,
            # which the sign-in screen shows as "server could not be reached".
            allow_headers=["*"] if not settings.is_production else _CORS_HEADERS,
        )
    if not settings.is_production:
        application.add_middleware(PrivateNetworkAccessMiddleware)
    application.add_middleware(ClientIpMiddleware)
    application.add_middleware(RequestIdMiddleware)
    application.add_exception_handler(RequestValidationError, _validation_error_handler)

    async def _pool_timeout(_request: Request, _exc: SATimeoutError) -> JSONResponse:
        return JSONResponse({"detail": "Service temporarily unavailable."}, status_code=503)

    application.add_exception_handler(SATimeoutError, _pool_timeout)
    application.include_router(api_router, prefix="/api/v1")

    @application.api_route("/", methods=["GET", "HEAD"])
    def root() -> dict[str, str]:
        return {
            "status": "ok",
            "service": settings.app_name,
            "health": "/health",
        }

    @application.get("/health/live")
    def live() -> HealthResponse:
        return HealthResponse(status="ok", database="unchecked")

    @application.get("/health/ready", response_model=HealthResponse)
    def ready() -> JSONResponse:
        return _health_response(_database_ready())

    @application.get("/health", response_model=HealthResponse)
    def health() -> JSONResponse:
        return _health_response(_database_ready())

    return application


app = create_app()
