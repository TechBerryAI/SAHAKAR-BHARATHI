from __future__ import annotations

import secrets
import uuid
from datetime import UTC, datetime, timedelta

from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.errors import (
    DuplicateAccountError,
    InvalidOtpError,
    RateLimitedError,
    SmsDeliveryError,
    VerificationRequiredError,
)
from app.core.security import hash_pin, hash_token, issue_session_token, verify_pin
from app.models.phone_verification import PhoneVerification
from app.repositories.phone_verifications import PhoneVerificationRepository
from app.repositories.users import UserRepository
from app.schemas.phone_verification import PhoneVerificationConfirmed, PhoneVerificationCreated
from app.services.sms import SmsGateway


class PhoneVerificationService:
    def __init__(self, db: Session, settings: Settings, gateway: SmsGateway) -> None:
        self.db = db
        self.settings = settings
        self.gateway = gateway
        self.challenges = PhoneVerificationRepository(db)
        self.users = UserRepository(db)

    def send(self, mobile: str) -> PhoneVerificationCreated:
        if self.users.get_by_mobile(mobile):
            raise DuplicateAccountError()
        now = datetime.now(UTC)
        latest = self.challenges.latest_for_mobile(mobile)
        if latest is not None and _aware(latest.created_at) + timedelta(
            seconds=self.settings.otp_resend_seconds
        ) > now:
            raise RateLimitedError()

        code = f"{secrets.randbelow(1_000_000):06d}"
        challenge = PhoneVerification(
            mobile=mobile,
            code_hash=hash_pin(code),
            expires_at=now + timedelta(seconds=self.settings.otp_ttl_seconds),
            attempts=0,
        )
        self.challenges.retire_unused(mobile, now)
        self.challenges.add(challenge)
        self.db.flush()
        try:
            # The handset radio rejects a leading +91. The 10-digit national
            # number is what this SIM actually sends.
            self.gateway.send(
                mobile,
                f"SAHAKAR BHARTI: Your registration OTP is {code}. Do not share it with anyone.",
            )
        except SmsDeliveryError:
            self.db.rollback()
            raise
        self.db.commit()
        return PhoneVerificationCreated(
            id=challenge.id,
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


def _aware(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value
