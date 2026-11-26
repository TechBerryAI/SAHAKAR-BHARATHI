from fastapi import APIRouter, Depends, Request, status

from app.core.config import Settings, get_settings
from app.core.deps import get_limiter, get_registration_service
from app.core.rate_limit import RateLimiter, enforce_rate_limit
from app.schemas.registration import RegistrationCreate, RegistrationResponse
from app.services.registration import RegistrationService

router = APIRouter(prefix="/registrations", tags=["registrations"])


@router.post("", response_model=RegistrationResponse, status_code=status.HTTP_201_CREATED)
def create_registration(
    payload: RegistrationCreate,
    request: Request,
    service: RegistrationService = Depends(get_registration_service),
    limiter: RateLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> RegistrationResponse:
    client = request.state.client_ip
    enforce_rate_limit(
        limiter,
        f"register:{client}",
        settings.register_rate_limit,
        settings.register_rate_window_seconds,
        retry_after=settings.register_rate_window_seconds,
    )
    return service.register(payload)
