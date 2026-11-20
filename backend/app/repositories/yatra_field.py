from __future__ import annotations

import uuid
from datetime import date

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.user import User
from app.models.yatra import YatraStop
from app.models.yatra_field import (
    YatraAttendance,
    YatraCounter,
    YatraLatestPosition,
    YatraPosition,
    YatraTracker,
)


class YatraCounterRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get(self, yatra_id: uuid.UUID, counter_id: uuid.UUID) -> YatraCounter | None:
        return self.db.scalar(
            select(YatraCounter).where(YatraCounter.id == counter_id, YatraCounter.yatra_id == yatra_id)
        )

    def get_by_token(self, token: str) -> YatraCounter | None:
        return self.db.scalar(select(YatraCounter).where(YatraCounter.qr_token == token))

    def page(
        self,
        yatra_id: uuid.UUID,
        limit: int,
        offset: int,
        status: str | None,
    ) -> tuple[list[YatraCounter], int]:
        filtered = select(YatraCounter).where(YatraCounter.yatra_id == yatra_id)
        if status is not None:
            filtered = filtered.where(YatraCounter.status == status)
        total = int(self.db.scalar(select(func.count()).select_from(filtered.subquery())) or 0)
        rows = self.db.scalars(
            filtered.order_by(YatraCounter.created_at.desc(), YatraCounter.id.desc()).limit(limit).offset(offset)
        )
        return list(rows), total


class YatraAttendanceRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def find_for_day(
        self,
        user_id: uuid.UUID,
        counter_id: uuid.UUID,
        attendance_date: date,
    ) -> YatraAttendance | None:
        return self.db.scalar(
            select(YatraAttendance).where(
                YatraAttendance.user_id == user_id,
                YatraAttendance.counter_id == counter_id,
                YatraAttendance.attendance_date == attendance_date,
            )
        )

    def page_for_yatra(self, yatra_id: uuid.UUID, limit: int, offset: int) -> tuple[list[tuple], int]:
        filtered = (
            select(YatraAttendance, YatraCounter.name, User.login_id, User.full_name)
            .join(YatraCounter, YatraCounter.id == YatraAttendance.counter_id)
            .join(User, User.id == YatraAttendance.user_id)
            .where(YatraAttendance.yatra_id == yatra_id)
        )
        total = int(self.db.scalar(select(func.count()).select_from(filtered.subquery())) or 0)
        rows = self.db.execute(
            filtered.order_by(YatraAttendance.checked_in_at.desc(), YatraAttendance.id.desc())
            .limit(limit)
            .offset(offset)
        ).all()
        return list(rows), total

    def page_for_user(
        self,
        yatra_id: uuid.UUID,
        user_id: uuid.UUID,
        limit: int,
        offset: int,
    ) -> tuple[list[tuple], int]:
        filtered = (
            select(YatraAttendance, YatraCounter.name, User.login_id, User.full_name)
            .join(YatraCounter, YatraCounter.id == YatraAttendance.counter_id)
            .join(User, User.id == YatraAttendance.user_id)
            .where(YatraAttendance.yatra_id == yatra_id, YatraAttendance.user_id == user_id)
        )
        total = int(self.db.scalar(select(func.count()).select_from(filtered.subquery())) or 0)
        rows = self.db.execute(
            filtered.order_by(YatraAttendance.checked_in_at.desc(), YatraAttendance.id.desc())
            .limit(limit)
            .offset(offset)
        ).all()
        return list(rows), total


class YatraTrackerRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get(self, tracker_id: uuid.UUID) -> YatraTracker | None:
        return self.db.get(YatraTracker, tracker_id)

    def get_for_yatra(self, yatra_id: uuid.UUID, tracker_id: uuid.UUID) -> YatraTracker | None:
        return self.db.scalar(
            select(YatraTracker).where(YatraTracker.id == tracker_id, YatraTracker.yatra_id == yatra_id)
        )

    def get_by_token_hash(self, token_hash: str) -> YatraTracker | None:
        return self.db.scalar(select(YatraTracker).where(YatraTracker.token_hash == token_hash))

    def page(self, yatra_id: uuid.UUID, limit: int, offset: int) -> tuple[list[YatraTracker], int]:
        filtered = select(YatraTracker).where(YatraTracker.yatra_id == yatra_id)
        total = int(self.db.scalar(select(func.count()).select_from(filtered.subquery())) or 0)
        rows = self.db.scalars(
            filtered.order_by(YatraTracker.assigned_at.desc(), YatraTracker.id.desc()).limit(limit).offset(offset)
        )
        return list(rows), total


class YatraPositionRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def by_client(self, tracker_id: uuid.UUID, client_position_id: uuid.UUID) -> YatraPosition | None:
        return self.db.scalar(
            select(YatraPosition).where(
                YatraPosition.tracker_id == tracker_id,
                YatraPosition.client_position_id == client_position_id,
            )
        )

    def latest(self, yatra_id: uuid.UUID) -> tuple[YatraLatestPosition, str] | None:
        row = self.db.execute(
            select(YatraLatestPosition, YatraTracker.status)
            .join(YatraTracker, YatraTracker.id == YatraLatestPosition.tracker_id)
            .where(YatraLatestPosition.yatra_id == yatra_id)
        ).one_or_none()
        if row is None:
            return None
        return row[0], row[1]

    def page(self, yatra_id: uuid.UUID, limit: int, offset: int) -> tuple[list[YatraPosition], int]:
        filtered = select(YatraPosition).where(YatraPosition.yatra_id == yatra_id)
        total = int(self.db.scalar(select(func.count()).select_from(filtered.subquery())) or 0)
        rows = self.db.scalars(
            filtered.order_by(YatraPosition.captured_at.desc(), YatraPosition.id.desc()).limit(limit).offset(offset)
        )
        return list(rows), total

    def stop_on_yatra(self, yatra_id: uuid.UUID, stop_id: uuid.UUID) -> YatraStop | None:
        return self.db.scalar(select(YatraStop).where(YatraStop.id == stop_id, YatraStop.yatra_id == yatra_id))
