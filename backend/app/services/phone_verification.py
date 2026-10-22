from __future__ import annotations

import logging
import secrets
import uuid
from collections.abc import Callable
from datetime import UTC, datetime, timedelta

from sqlalchemy import delete
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.errors import (
    DuplicateAccountError,
    InvalidOtpError,
    RateLimitedError,
    VerificationRequiredError,
)
from app.core.security import hash_pin, hash_token, issue_session_token, verify_pin
from app.db.session import SessionLocal
from app.models.phone_verification import PhoneVerification
from app.repositories.phone_verifications import PhoneVerificationRepository
from app.repositories.users import UserRepository
from app.schemas.phone_verification import PhoneVerificationConfirmed, PhoneVerificationCreated
from app.services.sms import SmsGateway

logger = logging.getLogger(__name__)

class PhoneVerificationService:
    def __init__(self, db: Session, settings: Settings, gateway: SmsGateway) -> None:
        self.db = db
        self.settings = settings
        self.gateway = gateway
        self.challenges = PhoneVerificationRepository(db)
        self.users = UserRepository(db)

    def send(self, mobile: str, defer: Callable[..., object]) -> PhoneVerificationCreated:
        """Store a new challenge and hand the SMS to `defer` (e.g. BackgroundTasks.add_task),
        so the caller gets a response without waiting on the SMS gateway."""
        if self.users.get_by_mobile(mobile):
            raise DuplicateAccountError()
        now = datetime.now(UTC)
        latest = self.challenges.latest_for_mobile(mobile)
        if latest is not None and _aware(latest.created_at) + timedelta(
            seconds=self.settings.otp_resend_seconds
        ) > now:
            raise RateLimitedError()

        code = f"{secrets.randbelow(1_000_000):06d}"
        challenge_id = uuid.uuid4()
        challenge = PhoneVerification(
            id=challenge_id,
            mobile=mobile,
            code_hash=hash_pin(code),
            expires_at=now + timedelta(seconds=self.settings.otp_ttl_seconds),
            attempts=0,
        )
        self.challenges.retire_unused(mobile, now)
        self.challenges.add(challenge)
        self.db.commit()
        defer(
            deliver_sms,
            self.gateway,
            challenge_id,
            mobile,
            f"SAHAKAR BHARTI: OTP is {code}. Do not share it.",
        )
        return PhoneVerificationCreated(
            id=challenge_id,
            expires_in_seconds=self.settings.otp_ttl_seconds,
        )

    def confirm(self, challenge_id: uuid.UUID, code: str) -> PhoneVerificationConfirmed:
        challenge = self.challenges.get(challenge_id)
        now = datetime.now(UTC)
        if (
            challenge is None
            or challenge.consumed_at is not None
            or _aware(challenge.expires_at) <= now
        ):
            raise InvalidOtpError()
        if challenge.attempts >= self.settings.otp_max_attempts:
            raise RateLimitedError()
        challenge.attempts += 1
        try:
            matches = verify_pin(code, challenge.code_hash)
        except Exception:
            matches = False
        if not matches:
            self.db.commit()
            raise InvalidOtpError()

        raw, digest = issue_session_token()
        challenge.consumed_at = now
        challenge.token_hash = digest
        challenge.token_expires_at = now + timedelta(
            seconds=self.settings.otp_verification_ttl_seconds
        )
        self.db.commit()
        return PhoneVerificationConfirmed(
            verification_token=raw,
            expires_in_seconds=self.settings.otp_verification_ttl_seconds,
        )

    def consume(self, mobile: str, verification_token: str) -> None:
        challenge = self.challenges.get_by_token_hash(hash_token(verification_token))
        now = datetime.now(UTC)
        if (
            challenge is None
            or challenge.mobile != mobile
            or challenge.token_used_at is not None
            or challenge.token_expires_at is None
            or _aware(challenge.token_expires_at) <= now
        ):
            raise VerificationRequiredError()
        challenge.token_used_at = now


def deliver_sms(gateway: SmsGateway, challenge_id: uuid.UUID, mobile: str, message: str) -> None:
    """Runs after the response has been sent."""
    try:
        gateway.send(mobile, message)
    except Exception:
        logger.warning("SMS delivery failed for phone verification %s", challenge_id, exc_info=True)
        _discard_challenge(challenge_id)


def _discard_challenge(challenge_id: uuid.UUID) -> None:
    # The user never got this code; dropping it lifts the resend cooldown so
    # "Resend OTP" works straight away. The request's session is closed by now.
    try:
        with SessionLocal() as db:
            db.execute(delete(PhoneVerification).where(PhoneVerification.id == challenge_id))
            db.commit()
    except Exception:
        logger.exception("Could not discard undelivered phone verification %s", challenge_id)


def _aware(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value
