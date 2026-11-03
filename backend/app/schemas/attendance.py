from datetime import date, datetime
from typing import Literal
from uuid import UUID

from pydantic import Field, field_validator

from app.schemas.common import ORMModel

AttendanceSource = Literal["qr", "code"]


class CheckInCreate(ORMModel):
    code: str = Field(min_length=6, max_length=6)
    source: AttendanceSource = "qr"

    @field_validator("code")
    @classmethod
    def digits_only(cls, value: str) -> str:
        cleaned = value.strip()
        if not cleaned.isdigit() or len(cleaned) != 6:
            raise ValueError("Enter the 6-digit booth code.")
        return cleaned


class ProfileSnapshot(ORMModel):
    login_id: str
    full_name: str
    mobile: str
    email: str | None = None
    gender: str | None = None
    occupation: str | None = None
    address_line: str | None = None
    locality: str | None = None
    district: str | None = None
    state: str | None = None
    pincode: str | None = None


class AttendanceRecord(ProfileSnapshot):
    id: UUID
    place_id: UUID
    place_name: str
    checked_in_at: datetime
    attendance_date: date
    source: AttendanceSource
    already_recorded: bool = False


class AttendanceList(ORMModel):
    items: list[AttendanceRecord]
    total: int


class AdminAttendanceList(ORMModel):
    items: list[AttendanceRecord]
    total: int
    limit: int
    offset: int
