from fastapi import APIRouter, Depends, Request, status
from fastapi.security import HTTPAuthorizationCredentials

from app.core.config import Settings, get_settings
from app.core.deps import bearer_scheme, get_auth_service, get_limiter
from app.core.rate_limit import RateLimiter, enforce_rate_limit
from app.core.security import canonical_login_identifier, hash_identifier
from app.schemas.common import ErrorResponse
from app.schemas.session import SessionCreate, SessionResponse
from app.services.auth import AuthService

router = APIRouter(prefix="/sessions", tags=["sessions"])


@router.post("", response_model=SessionResponse)
def create_session(
    payload: SessionCreate,
    request: Request,
    service: AuthService = Depends(get_auth_service),
    limiter: RateLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> SessionResponse:
    client = request.state.client_ip
    identifier = canonical_login_identifier(payload.identifier)
    enforce_rate_limit(
        limiter,
        f"login-ip:{client}",
        settings.login_ip_rate_limit,
        settings.login_rate_window_seconds,
        retry_after=settings.login_rate_window_seconds,
    )
    enforce_rate_limit(
        limiter,
        f"login-identifier:{hash_identifier(identifier)}",
        settings.login_rate_limit,
        settings.login_rate_window_seconds,
        retry_after=settings.login_rate_window_seconds,
    )
    return service.create_session(payload)


@router.delete("/current", status_code=status.HTTP_204_NO_CONTENT, responses={401: {"model": ErrorResponse}})
def logout(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    service: AuthService = Depends(get_auth_service),
) -> None:
    token = credentials.credentials if credentials else None
    service.logout(token)
