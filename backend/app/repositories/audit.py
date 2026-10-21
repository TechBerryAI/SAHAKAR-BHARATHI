from __future__ import annotations

from datetime import UTC, datetime, timedelta
from typing import Any
from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.audit import AuditEvent


class AuditRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def add(
        self,
        event_type: str,
        user_id: UUID | None = None,
        identifier_hash: str | None = None,
        extra: dict[str, Any] | None = None,
    ) -> AuditEvent:
        event = AuditEvent(
            event_type=event_type,
            user_id=user_id,
            identifier_hash=identifier_hash,
            extra=extra,
        )
        self.db.add(event)
        return event

    def recent_failures(self, identifier_hash: str, window_seconds: int) -> int:
        since = datetime.now(UTC) - timedelta(seconds=window_seconds)
        count = self.db.scalar(
            select(func.count()).select_from(AuditEvent).where(
                AuditEvent.event_type == "login_failure",
                AuditEvent.identifier_hash == identifier_hash,
                AuditEvent.created_at >= since,
            )
        )
        return int(count or 0)
