import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.types import ASGIApp, Receive, Scope, Send

from app.api.v1.router import api_router
from app.core.config import get_settings
from app.core.logging import configure_logging
from app.core.rate_limit import SlidingWindowLimiter
from app.db import session as db_session
from app.db.session import SessionLocal
from app.schemas.common import HealthResponse

logger = logging.getLogger(__name__)

_CORS_METHODS = ["GET", "POST", "PATCH", "DELETE", "OPTIONS"]
_CORS_HEADERS = ["Authorization", "Content-Type"]
_READY_TIMEOUT_MS = 2000
_SENSITIVE_FIELDS = frozenset({"pin", "password", "token", "access_token", "authorization"})


def sanitize_validation_errors(errors: list[object]) -> list[object]:
    sanitized: list[object] = []
    for error in errors:
        item = dict(error)
        loc = item.get("loc", ())
        names = {str(part).lower() for part in loc} if isinstance(loc, (list, tuple)) else set()
        if names & _SENSITIVE_FIELDS:
            item.pop("input", None)
        sanitized.append(item)
    return jsonable_encoder(sanitized)


def validation_error_response(exc: RequestValidationError) -> JSONResponse:
    return JSONResponse({"detail": sanitize_validation_errors(exc.errors())}, status_code=422)


class RequestSizeLimitMiddleware:
    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        headers = {key.decode("latin-1").lower(): value.decode("latin-1") for key, value in scope.get("headers", [])}
        content_length = headers.get("content-length")
        if content_length is not None:
            try:
                size = int(content_length)
            except ValueError:
                response = JSONResponse({"detail": "Invalid Content-Length header."}, status_code=400)
                await response(scope, receive, send)
                return
            if size > get_settings().max_request_bytes:
                response = JSONResponse({"detail": "Request too large."}, status_code=413)
                await response(scope, receive, send)
                return
        await self.app(scope, receive, send)


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
        except Exception:
            logger.exception("Unhandled application error")
            await JSONResponse({"detail": "An unexpected error occurred."}, status_code=500)(
                scope, receive, send
            )


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    configure_logging()
    _app.state.limiter = SlidingWindowLimiter()
    yield
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
    application.add_middleware(SafeErrorMiddleware)
    if settings.cors_origin_list:
        application.add_middleware(
            CORSMiddleware,
            allow_origins=settings.cors_origin_list,
            allow_credentials=True,
            allow_methods=_CORS_METHODS,
            allow_headers=_CORS_HEADERS,
        )
    application.add_exception_handler(RequestValidationError, _validation_error_handler)
    application.include_router(api_router, prefix="/api/v1")

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
