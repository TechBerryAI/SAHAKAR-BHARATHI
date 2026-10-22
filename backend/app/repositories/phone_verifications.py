from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import select
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
        rows = self.db.scalars(
            select(PhoneVerification).where(PhoneVerification.mobile == mobile)
        ).all()
        for row in rows:
            if row.consumed_at is None:
                row.consumed_at = now
            if row.token_used_at is None:
                row.token_used_at = now

    def add(self, challenge: PhoneVerification) -> PhoneVerification:
        self.db.add(challenge)
        return challenge
