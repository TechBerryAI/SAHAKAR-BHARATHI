from fastapi import APIRouter, Depends, Request, status

from app.core.config import Settings, get_settings
from app.core.deps import get_limiter, get_registration_service
from app.core.errors import RateLimitedError
from app.core.rate_limit import SlidingWindowLimiter
from app.schemas.registration import RegistrationCreate, RegistrationResponse
from app.services.registration import RegistrationService

router = APIRouter(prefix="/registrations", tags=["registrations"])


@router.post("", response_model=RegistrationResponse, status_code=status.HTTP_201_CREATED)
def create_registration(
    payload: RegistrationCreate,
    request: Request,
    service: RegistrationService = Depends(get_registration_service),
    limiter: SlidingWindowLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> RegistrationResponse:
    client = request.client.host if request.client else "unknown"
    if not limiter.allow(
        f"register:{client}",
        settings.register_rate_limit,
        settings.register_rate_window_seconds,
    ):
        raise RateLimitedError()
    return service.register(payload)
