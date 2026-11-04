from __future__ import annotations

import uuid
from datetime import UTC, datetime
from zoneinfo import ZoneInfo

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.errors import InactivePlaceError, RateLimitedError, UnknownPlaceError
from app.core.rate_limit import RateLimiter
from app.db.session import is_unique_violation
from app.models.attendance import Attendance
from app.models.user import User
from app.repositories.attendance import AttendanceRepository
from app.repositories.places import PlaceRepository
from app.schemas.attendance import (
    AdminAttendanceList,
    AttendanceList,
    AttendanceRecord,
    CheckInCreate,
)

_IST = ZoneInfo("Asia/Kolkata")


class AttendanceService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.places = PlaceRepository(db)
        self.visits = AttendanceRepository(db)

    def check_in(
        self,
        user: User,
        payload: CheckInCreate,
        limiter: RateLimiter,
        settings: Settings,
    ) -> AttendanceRecord:
        place = self.places.get_by_code(payload.code)
        if place is None:
            self._count_failure(user, limiter, settings)
            raise UnknownPlaceError()
        if not place.active:
            self._count_failure(user, limiter, settings)
            raise InactivePlaceError()

        today = datetime.now(_IST).date()
        place_id = place.id
        place_name = place.name
        existing = self.visits.find_for_day(user.id, place_id, today)
        if existing is not None:
            return self.to_record(existing, place_name, already_recorded=True)

        visit = Attendance(
            user_id=user.id,
            place_id=place_id,
            checked_in_at=datetime.now(UTC),
            attendance_date=today,
            source=payload.source,
            login_id=user.login_id,
            full_name=user.full_name,
            mobile=user.mobile,
            email=user.email,
            gender=user.gender,
            occupation=user.occupation,
            address_line=user.address_line,
            locality=user.locality,
            district=user.district,
            state=user.state,
            pincode=user.pincode,
        )
        self.db.add(visit)
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if not is_unique_violation(exc):
                raise
            raced = self.visits.find_for_day(user.id, place_id, today)
            if raced is None:
                raise
            return self.to_record(raced, place_name, already_recorded=True)
        self.db.refresh(visit)
        return self.to_record(visit, place_name, already_recorded=False)

    def list_mine(self, user: User) -> AttendanceList:
        rows, total = self.visits.list_for_user(user.id)
        return AttendanceList(
            items=[self.to_record(visit, name, already_recorded=False) for visit, name in rows],
            total=total,
        )

    def list_for_place(self, place_id: uuid.UUID, limit: int, offset: int) -> AdminAttendanceList:
        place = self.places.get(place_id)
        if place is None:
            raise UnknownPlaceError()
        rows, total = self.visits.list_for_place(place_id, limit, offset)
        return AdminAttendanceList(
            items=[self.to_record(visit, name, already_recorded=False) for visit, name in rows],
            total=total,
            limit=limit,
            offset=offset,
        )

    @staticmethod
    def to_record(visit: Attendance, place_name: str, *, already_recorded: bool) -> AttendanceRecord:
        return AttendanceRecord(
            id=visit.id,
            place_id=visit.place_id,
            place_name=place_name,
            checked_in_at=visit.checked_in_at,
            attendance_date=visit.attendance_date,
            source=visit.source,  # type: ignore[arg-type]
            already_recorded=already_recorded,
            login_id=visit.login_id,
            full_name=visit.full_name,
            mobile=visit.mobile,
            email=visit.email,
            gender=visit.gender,
            occupation=visit.occupation,
            address_line=visit.address_line,
            locality=visit.locality,
            district=visit.district,
            state=visit.state,
            pincode=visit.pincode,
        )

    @staticmethod
    def _count_failure(user: User, limiter: RateLimiter, settings: Settings) -> None:
        allowed = limiter.allow(
            f"attendance-code:{user.id}",
            settings.attendance_code_rate_limit,
            settings.attendance_code_rate_window_seconds,
        )
        if not allowed:
            raise RateLimitedError(retry_after=settings.attendance_code_rate_window_seconds)
