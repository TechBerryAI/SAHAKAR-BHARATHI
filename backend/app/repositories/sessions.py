from __future__ import annotations

from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.models.session import AuthSession


class SessionRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def add(self, session: AuthSession) -> AuthSession:
        self.db.add(session)
        return session

    def get_active_by_token_hash(self, token_hash: str) -> AuthSession | None:
        now = datetime.now(UTC)
        return self.db.scalar(
            select(AuthSession)
            .options(joinedload(AuthSession.user))
            .where(
                AuthSession.token_hash == token_hash,
                AuthSession.revoked_at.is_(None),
                AuthSession.expires_at > now,
            )
        )

    def revoke(self, session: AuthSession) -> None:
        session.revoked_at = datetime.now(UTC)
