from __future__ import annotations

import logging
import secrets
import uuid
from datetime import UTC, datetime, timedelta

from sqlalchemy import delete, text
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.errors import (
    InvalidOtpError,
    RateLimitedError,
    SmsDeliveryError,
    VerificationRequiredError,
)
from app.core.request_context import current_request_id
from app.core.security import hash_identifier, hash_pin, hash_token, issue_session_token, verify_pin
from app.models.phone_verification import PhoneVerification
from app.repositories.audit import AuditRepository
from app.repositories.phone_verifications import PhoneVerificationRepository
from app.repositories.users import UserRepository
from app.schemas.phone_verification import PhoneVerificationConfirmed, PhoneVerificationCreated
from app.services.sms import SmsGateway

logger = logging.getLogger(__name__)

_OTP_SMS = "SAHAKAR BHARTI: OTP is {code}. Do not share it."
# No numeric code. Only the handset sees this, so the HTTP response stays the same
# for a new number and a number that already has an account.
_SIGN_IN_SMS = (
    "SAHAKAR BHARTI: This number already has an account. "
    "Sign in with your PIN. Do not share this message."
)


class PhoneVerificationService:
    def __init__(
        self,
        db: Session,
        settings: Settings,
        gateway: SmsGateway | None = None,
    ) -> None:
        self.db = db
        self.settings = settings
        self.gateway = gateway
        self.challenges = PhoneVerificationRepository(db)
        self.users = UserRepository(db)
        self.audit = AuditRepository(db)

    def send(self, mobile: str) -> PhoneVerificationCreated:
        """Store a challenge, send the SMS, and return 202 only after the gateway accepts it.

        Textbee's HTTP accept means the gateway took the message. Handset delivery is not
        confirmed here; waiting on that would hold the request open for tens of seconds.
        """
        if self.gateway is None:
            logger.warning("SMS gateway is unset request_id=%s", current_request_id())
            raise SmsDeliveryError()

        identifier = hash_identifier(mobile)
        challenge_id = uuid.uuid4()
        code = f"{secrets.randbelow(1_000_000):06d}"
        now = datetime.now(UTC)
        registered = False
        try:
            self._lock_mobile(mobile)
            # Same work either way: hash, insert, and an SMS. The response does not
            # say whether the number is already registered.
            registered = self.users.get_by_mobile(mobile) is not None
            existing = self.challenges.for_mobile(mobile)
            latest = existing[0] if existing else None
            if latest is not None and _aware(latest.created_at) + timedelta(
                seconds=self.settings.otp_resend_seconds
            ) > now:
                self.db.rollback()
                raise RateLimitedError()

            challenge = PhoneVerification(
                id=challenge_id,
                mobile=mobile,
                code_hash=hash_pin(code),
                expires_at=now + timedelta(seconds=self.settings.otp_ttl_seconds),
                attempts=0,
            )
            self.challenges.retire_unused(existing, now)
            self.challenges.add(challenge)
            extra = {"challenge_id": str(challenge_id)}
            if registered:
                extra["notice"] = "sign_in"
            self.audit.add(
                "otp_requested",
                identifier_hash=identifier,
                extra=extra,
            )
            self.db.commit()
        except RateLimitedError:
            raise
        except Exception:
            self.db.rollback()
            raise

        try:
            message = _SIGN_IN_SMS if registered else _OTP_SMS.format(code=code)
            self.gateway.send(mobile, message)
        except Exception as exc:
            self._fail_delivery(challenge_id, identifier, exc)
            raise SmsDeliveryError() from None

        self._mark_delivered(challenge_id, identifier)
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
            if challenge is not None:
                self._audit_confirm_failure(challenge, "unusable")
                self.db.commit()
            else:
                self.db.rollback()
            raise InvalidOtpError()
        if challenge.attempts >= self.settings.otp_max_attempts:
            self._audit_confirm_failure(challenge, "attempt_limit")
            self.db.commit()
            raise RateLimitedError()
        challenge.attempts += 1
        try:
            matches = verify_pin(code, challenge.code_hash)
        except Exception:
            matches = False
        if not matches:
            self._audit_confirm_failure(challenge, "mismatch")
            self.db.commit()
            raise InvalidOtpError()

        raw, digest = issue_session_token()
        challenge.consumed_at = now
        challenge.token_hash = digest
        challenge.token_expires_at = now + timedelta(
            seconds=self.settings.otp_verification_ttl_seconds
        )
        self.audit.add(
            "otp_confirmed",
            identifier_hash=hash_identifier(challenge.mobile),
            extra={"challenge_id": str(challenge.id)},
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
            self.db.rollback()
            raise VerificationRequiredError()
        challenge.token_used_at = now

    def _lock_mobile(self, mobile: str) -> None:
        # Held until this transaction commits, so two sends for one mobile cannot
        # both pass the resend check. Released before the SMS call.
        self.db.execute(
            text("SELECT pg_advisory_xact_lock(hashtextextended(:lock_key, 0::bigint))"),
            {"lock_key": f"phone-verification:{mobile}"},
        )

    def _audit_confirm_failure(self, challenge: PhoneVerification, reason: str) -> None:
        self.audit.add(
            "otp_confirm_failed",
            identifier_hash=hash_identifier(challenge.mobile),
            extra={
                "challenge_id": str(challenge.id),
                "reason": reason,
                "attempts": challenge.attempts,
            },
        )

    def _fail_delivery(self, challenge_id: uuid.UUID, identifier: str, exc: Exception) -> None:
        reason = exc.detail if isinstance(exc, SmsDeliveryError) else type(exc).__name__
        logger.warning(
            "phone verification delivery failed challenge=%s identifier=%s reason=%s request_id=%s",
            challenge_id,
            identifier,
            reason,
            current_request_id(),
        )
        try:
            self.db.execute(delete(PhoneVerification).where(PhoneVerification.id == challenge_id))
            self.audit.add(
                "otp_delivery_failed",
                identifier_hash=identifier,
                extra={"challenge_id": str(challenge_id), "error_type": type(exc).__name__},
            )
            self.db.commit()
        except Exception as discard_exc:
            self.db.rollback()
            logger.warning(
                "could not discard undelivered phone verification %s error=%s request_id=%s",
                challenge_id,
                type(discard_exc).__name__,
                current_request_id(),
            )

    def _mark_delivered(self, challenge_id: uuid.UUID, identifier: str) -> None:
        try:
            self.audit.add(
                "otp_delivered",
                identifier_hash=identifier,
                extra={"challenge_id": str(challenge_id)},
            )
            self.db.commit()
        except Exception as exc:
            self.db.rollback()
            logger.warning(
                "phone verification delivery was not recorded challenge=%s error=%s request_id=%s",
                challenge_id,
                type(exc).__name__,
                current_request_id(),
            )


def _aware(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value
