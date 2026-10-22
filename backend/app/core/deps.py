from fastapi import Depends, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.config import Settings, get_settings
from app.core.errors import SmsDeliveryError
from app.core.rate_limit import SlidingWindowLimiter
from app.db.session import get_db
from app.models.session import AuthSession
from app.models.user import User
from app.services.auth import AuthService
from app.services.phone_verification import PhoneVerificationService
from app.services.profile import ProfileService
from app.services.registration import RegistrationService
from app.services.sms import SmsGateway, TextbeeGateway

bearer_scheme = HTTPBearer(auto_error=False)


def get_limiter(request: Request) -> SlidingWindowLimiter:
    return request.app.state.limiter


def get_auth_service(
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> AuthService:
    return AuthService(db, settings)


def get_sms_gateway(
    request: Request,
    settings: Settings = Depends(get_settings),
) -> SmsGateway:
    override = getattr(request.app.state, "sms_gateway", None)
    if override is not None:
        return override
    if not settings.textbee_api_key.strip():
        raise SmsDeliveryError("SMS gateway is not configured.")
    return TextbeeGateway(
        api_key=settings.textbee_api_key.strip(),
        device_id=settings.textbee_device_id.strip(),
        base_url=settings.textbee_base_url.strip() or "https://api.textbee.dev/api/v1",
        resolve_sim=True,
    )


def get_phone_verification_service(
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
    gateway: SmsGateway = Depends(get_sms_gateway),
) -> PhoneVerificationService:
    return PhoneVerificationService(db, settings, gateway)


def get_registration_service(
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
    gateway: SmsGateway = Depends(get_sms_gateway),
) -> RegistrationService:
    return RegistrationService(db, PhoneVerificationService(db, settings, gateway))


def get_profile_service(db: Session = Depends(get_db)) -> ProfileService:
    return ProfileService(db)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    auth: AuthService = Depends(get_auth_service),
) -> User:
    token = credentials.credentials if credentials else None
    user, _session = auth.authenticate(token)
    return user


def get_current_session(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    auth: AuthService = Depends(get_auth_service),
) -> tuple[User, AuthSession]:
    token = credentials.credentials if credentials else None
    return auth.authenticate(token)
