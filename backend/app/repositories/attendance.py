from __future__ import annotations

import uuid
from datetime import date

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.attendance import Attendance
from app.models.place import Place


class AttendanceRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def find_for_day(self, user_id: uuid.UUID, place_id: uuid.UUID, day: date) -> Attendance | None:
        return self.db.scalar(
            select(Attendance).where(
                Attendance.user_id == user_id,
                Attendance.place_id == place_id,
                Attendance.attendance_date == day,
            )
        )

    def list_for_user(self, user_id: uuid.UUID, limit: int = 50) -> tuple[list[tuple[Attendance, str]], int]:
        filters = Attendance.user_id == user_id
        total = int(self.db.scalar(select(func.count()).select_from(Attendance).where(filters)) or 0)
        rows = self.db.execute(
            select(Attendance, Place.name)
            .join(Place, Attendance.place_id == Place.id)
            .where(filters)
            .order_by(Attendance.checked_in_at.desc())
            .limit(limit)
        ).all()
        return [(visit, name) for visit, name in rows], total

    def list_for_place(
        self,
        place_id: uuid.UUID,
        limit: int,
        offset: int,
    ) -> tuple[list[tuple[Attendance, str]], int]:
        filters = Attendance.place_id == place_id
        total = int(self.db.scalar(select(func.count()).select_from(Attendance).where(filters)) or 0)
        rows = self.db.execute(
            select(Attendance, Place.name)
            .join(Place, Attendance.place_id == Place.id)
            .where(filters)
            .order_by(Attendance.checked_in_at.desc())
            .limit(limit)
            .offset(offset)
        ).all()
        return [(visit, name) for visit, name in rows], total
