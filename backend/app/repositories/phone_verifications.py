from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import func, or_, select, update
from sqlalchemy.orm import Session

from app.models.phone_verification import PhoneVerification


class PhoneVerificationRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get(self, challenge_id: uuid.UUID) -> PhoneVerification | None:
        return self.db.get(PhoneVerification, challenge_id)

    def get_by_token_hash(self, token_hash: str) -> PhoneVerification | None:
        return self.db.scalar(
            select(PhoneVerification).where(PhoneVerification.token_hash == token_hash)
        )

    def latest_for_mobile(self, mobile: str) -> PhoneVerification | None:
        return self.db.scalar(
            select(PhoneVerification)
            .where(PhoneVerification.mobile == mobile)
            .order_by(PhoneVerification.created_at.desc())
            .limit(1)
        )

    def retire_unused(self, mobile: str, now: datetime) -> None:
        # One UPDATE instead of loading every past challenge and writing each back.
        self.db.execute(
            update(PhoneVerification)
            .where(
                PhoneVerification.mobile == mobile,
                or_(
                    PhoneVerification.consumed_at.is_(None),
                    PhoneVerification.token_used_at.is_(None),
                ),
            )
            .values(
                consumed_at=func.coalesce(PhoneVerification.consumed_at, now),
                token_used_at=func.coalesce(PhoneVerification.token_used_at, now),
            )
            .execution_options(synchronize_session=False)
        )

    def add(self, challenge: PhoneVerification) -> PhoneVerification:
        self.db.add(challenge)
        return challenge
