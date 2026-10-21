from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import text
from starlette.middleware.base import BaseHTTPMiddleware

from app.api.v1.router import api_router
from app.core.config import get_settings
from app.core.logging import configure_logging
from app.core.rate_limit import SlidingWindowLimiter
from app.db.session import SessionLocal
from app.schemas.common import HealthResponse

settings = get_settings()


class RequestSizeLimitMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):  # type: ignore[no-untyped-def]
        content_length = request.headers.get("content-length")
        if content_length and int(content_length) > settings.max_request_bytes:
            return JSONResponse({"detail": "Request too large."}, status_code=413)
        return await call_next(request)


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    configure_logging()
    _app.state.limiter = SlidingWindowLimiter()
    yield


def create_app() -> FastAPI:
    application = FastAPI(
        title="Sahakar Bharati API",
        version="0.1.0",
        lifespan=lifespan,
    )
    application.add_middleware(RequestSizeLimitMiddleware)
    if settings.cors_origin_list:
        application.add_middleware(
            CORSMiddleware,
            allow_origins=settings.cors_origin_list,
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )
    application.include_router(api_router, prefix="/api/v1")

    @application.get("/health", response_model=HealthResponse)
    def health() -> HealthResponse:
        db_status = "ok"
        try:
            with SessionLocal() as db:
                db.execute(text("SELECT 1"))
        except Exception:
            db_status = "unavailable"
        return HealthResponse(status="ok" if db_status == "ok" else "degraded", database=db_status)

    return application


app = create_app()
