import logging
from functools import lru_cache

import httpx
from fastapi import Depends, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.config import Settings, get_settings
from app.core.errors import ForbiddenError, SmsDeliveryError
from app.core.rate_limit import SlidingWindowLimiter
from app.core.roles import ROLE_SUPER_ADMIN, STAFF_ROLES
from app.core.storage import DocumentStorage, LocalDocumentStorage, UnavailableDocumentStorage
from app.db.session import get_db
from app.models.session import AuthSession
from app.models.user import User
from app.services.admin import AdminService
from app.services.attendance import AttendanceService
from app.services.auth import AuthService
from app.services.districts import DistrictService
from app.services.phone_verification import PhoneVerificationService
from app.services.places import PlaceService
from app.services.profile import ProfileService
from app.services.protocol import ProtocolService
from app.services.registration import RegistrationService
from app.services.sms import SmsGateway, TextbeeGateway
from app.services.yatras import YatraService

logger = logging.getLogger(__name__)

bearer_scheme = HTTPBearer(auto_error=False)


def get_limiter(request: Request) -> SlidingWindowLimiter:
    return request.app.state.limiter


def get_auth_service(
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> AuthService:
    return AuthService(db, settings)


@lru_cache
def _textbee_http_client() -> httpx.Client:
    # One pooled client per process: skips TLS setup and keeps the connection
    # to Textbee warm between OTP requests.
    return httpx.Client(
        timeout=15.0,
        limits=httpx.Limits(max_keepalive_connections=5, keepalive_expiry=120.0),
    )


def get_sms_gateway(
    request: Request,
    settings: Settings = Depends(get_settings),
) -> SmsGateway:
    override = getattr(request.app.state, "sms_gateway", None)
    if override is not None:
        return override
    if not settings.textbee_api_key.strip():
        logger.warning("SMS gateway key is missing")
        raise SmsDeliveryError()
    return TextbeeGateway(
        api_key=settings.textbee_api_key.strip(),
        device_id=settings.textbee_device_id.strip(),
        base_url=settings.textbee_base_url.strip() or "https://api.textbee.dev/api/v1",
        resolve_sim=True,
        client=_textbee_http_client(),
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
) -> RegistrationService:
    # Consuming a verification token does not send SMS. A missing gateway must not block registration.
    return RegistrationService(db, PhoneVerificationService(db, settings))


def get_profile_service(db: Session = Depends(get_db)) -> ProfileService:
    return ProfileService(db)


def get_admin_service(db: Session = Depends(get_db)) -> AdminService:
    return AdminService(db)


def get_place_service(db: Session = Depends(get_db)) -> PlaceService:
    return PlaceService(db)


def get_attendance_service(db: Session = Depends(get_db)) -> AttendanceService:
    return AttendanceService(db)


def get_district_service(db: Session = Depends(get_db)) -> DistrictService:
    return DistrictService(db)


def get_yatra_service(db: Session = Depends(get_db)) -> YatraService:
    return YatraService(db)


def get_document_storage(request: Request) -> DocumentStorage:
    override = getattr(request.app.state, "document_storage", None)
    if override is not None:
        return override
    settings = get_settings()
    if settings.document_backend() != "local":
        return UnavailableDocumentStorage()
    return LocalDocumentStorage(settings.document_directory())


def get_protocol_service(
    db: Session = Depends(get_db),
    storage: DocumentStorage = Depends(get_document_storage),
) -> ProtocolService:
    return ProtocolService(db, storage)


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


def require_staff(user: User = Depends(get_current_user)) -> User:
    if user.role not in STAFF_ROLES:
        raise ForbiddenError()
    return user


def require_super_admin(user: User = Depends(get_current_user)) -> User:
    if user.role != ROLE_SUPER_ADMIN:
        raise ForbiddenError()
    return user
