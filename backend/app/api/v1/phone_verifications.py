import uuid

from fastapi import APIRouter, Depends, Request, status

from app.core.config import Settings, get_settings
from app.core.deps import get_limiter, get_phone_verification_service
from app.core.errors import RateLimitedError
from app.core.rate_limit import SlidingWindowLimiter
from app.schemas.phone_verification import (
    PhoneVerificationConfirm,
    PhoneVerificationConfirmed,
    PhoneVerificationCreate,
    PhoneVerificationCreated,
)
from app.services.phone_verification import PhoneVerificationService

router = APIRouter(prefix="/phone-verifications", tags=["phone-verifications"])


@router.post("", response_model=PhoneVerificationCreated, status_code=status.HTTP_202_ACCEPTED)
def send_phone_verification(
    payload: PhoneVerificationCreate,
    request: Request,
    service: PhoneVerificationService = Depends(get_phone_verification_service),
    limiter: SlidingWindowLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> PhoneVerificationCreated:
    client = request.client.host if request.client else "unknown"
    if not limiter.allow(
        f"otp-send:{client}",
        settings.otp_send_rate_limit,
        settings.otp_send_rate_window_seconds,
    ):
        raise RateLimitedError()
    return service.send(payload.mobile)


@router.post(
    "/{challenge_id}/confirm",
    response_model=PhoneVerificationConfirmed,
)
def confirm_phone_verification(
    challenge_id: uuid.UUID,
    payload: PhoneVerificationConfirm,
    request: Request,
    service: PhoneVerificationService = Depends(get_phone_verification_service),
    limiter: SlidingWindowLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> PhoneVerificationConfirmed:
    client = request.client.host if request.client else "unknown"
    if not limiter.allow(
        f"otp-confirm:{client}",
        settings.otp_confirm_rate_limit,
        settings.otp_confirm_rate_window_seconds,
    ):
        raise RateLimitedError()
    return service.confirm(challenge_id, payload.code)
