from __future__ import annotations

from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.errors import InvalidCredentialsError, RateLimitedError, UnauthorizedError
from app.core.security import (
    canonical_login_identifier,
    dummy_pin_hash,
    hash_identifier,
    hash_token,
    issue_session_token,
    session_expiry,
    verify_pin,
)
from app.models.session import AuthSession
from app.models.user import User
from app.repositories.audit import AuditRepository
from app.repositories.sessions import SessionRepository
from app.repositories.users import UserRepository
from app.schemas.session import SessionCreate, SessionResponse

_ACTIVE_STATUS = "active"


class AuthService:
    def __init__(self, db: Session, settings: Settings) -> None:
        self.db = db
        self.settings = settings
        self.users = UserRepository(db)
        self.sessions = SessionRepository(db)
        self.audit = AuditRepository(db)

    def create_session(self, payload: SessionCreate) -> SessionResponse:
        identifier = canonical_login_identifier(payload.identifier)
        identifier_hash = hash_identifier(identifier)
        failures = self.audit.recent_failures(identifier_hash, self.settings.login_lockout_seconds)
        if failures >= self.settings.login_lockout_failures:
            raise RateLimitedError(retry_after=self.settings.login_lockout_seconds)

        user = self.users.find_for_login(payload.identifier)
        if user is None:
            verify_pin(payload.pin, dummy_pin_hash())
            pin_ok = False
        else:
            pin_ok = verify_pin(payload.pin, user.pin_hash) and user.status == _ACTIVE_STATUS

        if user is None or not pin_ok:
            self.audit.add("login_failure", identifier_hash=identifier_hash)
            self.db.commit()
            raise InvalidCredentialsError()

        raw_token, token_hash = issue_session_token()
        expires_at = session_expiry(self.settings)
        session = AuthSession(user_id=user.id, token_hash=token_hash, expires_at=expires_at)
        self.sessions.add(session)
        self.audit.add("login_success", user_id=user.id, identifier_hash=identifier_hash)
        self.db.commit()
        return SessionResponse(
            access_token=raw_token,
            expires_at=expires_at,
            login_id=user.login_id,
            full_name=user.full_name,
            mobile=user.mobile,
            occupation=user.occupation,
            state=user.state,
            district=user.district,
        )

    def authenticate(self, raw_token: str | None) -> tuple[User, AuthSession]:
        if not raw_token:
            raise UnauthorizedError()
        session = self.sessions.get_active_by_token_hash(hash_token(raw_token))
        if session is None or session.user is None or session.user.status != _ACTIVE_STATUS:
            raise UnauthorizedError("Session is invalid or expired.")
        return session.user, session

    def logout(self, raw_token: str | None) -> None:
        user, session = self.authenticate(raw_token)
        self.sessions.revoke(session)
        self.audit.add("logout", user_id=user.id)
        self.db.commit()
