from __future__ import annotations

import re
from datetime import date, datetime
from typing import Literal
from uuid import UUID

from pydantic import ConfigDict, Field, field_validator, model_validator

from app.core.security import normalize_name
from app.schemas.common import ORMModel

_COUNTER_CODE = re.compile(r"^[A-Z0-9][A-Z0-9-]{1,15}$")
_QR_PAYLOAD = re.compile(r"^SB-COUNTER:([A-Za-z0-9_-]{32,64})$")
QR_PREFIX = "SB-COUNTER:"


def qr_text(token: str) -> str:
    return f"{QR_PREFIX}{token}"


def _clean(value: str | None) -> str | None:
    if value is None:
        return None
    cleaned = value.strip()
    return cleaned or None


def _aware(value: datetime) -> datetime:
    if value.tzinfo is None or value.tzinfo.utcoffset(value) is None:
        raise ValueError("Use a timezone-aware date and time.")
    return value


def _code(value: str) -> str:
    cleaned = value.strip().upper()
    if not _COUNTER_CODE.fullmatch(cleaned):
        raise ValueError("Use 2 to 16 letters, digits, or hyphens.")
    return cleaned


class CounterCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=2, max_length=160)
    code: str = Field(min_length=2, max_length=16)
    description: str | None = Field(default=None, max_length=400)
    stop_id: UUID | None = None
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    location_accuracy_m: float | None = Field(default=None, gt=0, le=1000)

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Counter name is required.")
        return cleaned

    @field_validator("code")
    @classmethod
    def clean_code(cls, value: str) -> str:
        return _code(value)

    @field_validator("description")
    @classmethod
    def clean_description(cls, value: str | None) -> str | None:
        return _clean(value)

    @model_validator(mode="after")
    def location_together(self) -> CounterCreate:
        coords = (self.latitude is None, self.longitude is None, self.location_accuracy_m is None)
        if len(set(coords)) != 1:
            raise ValueError("Latitude, longitude, and accuracy are set together.")
        return self


class CounterUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    name: str | None = Field(default=None, min_length=2, max_length=160)
    code: str | None = Field(default=None, min_length=2, max_length=16)
    description: str | None = Field(default=None, max_length=400)
    stop_id: UUID | None = None
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    location_accuracy_m: float | None = Field(default=None, gt=0, le=1000)
    clear_description: bool = False
    clear_stop: bool = False
    clear_coordinates: bool = False

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Counter name is required.")
        return cleaned

    @field_validator("code")
    @classmethod
    def clean_code(cls, value: str | None) -> str | None:
        if value is None:
            return None
        return _code(value)

    @field_validator("description")
    @classmethod
    def clean_description(cls, value: str | None) -> str | None:
        return _clean(value)

    @model_validator(mode="after")
    def something_to_change(self) -> CounterUpdate:
        changed = any(
            value is not None
            for value in (
                self.name,
                self.code,
                self.description,
                self.stop_id,
                self.latitude,
                self.longitude,
                self.location_accuracy_m,
            )
        )
        if not changed and not (self.clear_description or self.clear_stop or self.clear_coordinates):
            raise ValueError("Nothing to update.")
        sent = [self.latitude is not None, self.longitude is not None, self.location_accuracy_m is not None]
        if any(sent) and not all(sent) and not self.clear_coordinates:
            raise ValueError("Latitude, longitude, and accuracy are set together.")
        return self


class CounterSummary(ORMModel):
    id: UUID
    yatra_id: UUID
    stop_id: UUID | None
    name: str
    code: str
    description: str | None
    latitude: float | None
    longitude: float | None
    location_accuracy_m: float | None
    status: str
    created_at: datetime
    updated_at: datetime


class CounterDetail(CounterSummary):
    qr_text: str


class CounterList(ORMModel):
    items: list[CounterSummary]
    total: int
    limit: int
    offset: int
    has_more: bool


class AttendanceScan(ORMModel):
    model_config = ConfigDict(extra="forbid")

    qr_token: str = Field(min_length=12, max_length=80)

    @field_validator("qr_token")
    @classmethod
    def parse_payload(cls, value: str) -> str:
        match = _QR_PAYLOAD.fullmatch(value.strip())
        if match is None:
            raise ValueError("That QR code is not a counter.")
        return match.group(1)


class AttendanceRecord(ORMModel):
    id: UUID
    yatra_id: UUID
    counter_id: UUID
    counter_name: str
    checked_in_at: datetime
    attendance_date: date
    source: Literal["qr"]
    already_recorded: bool
    login_id: str
    full_name: str


class AttendanceList(ORMModel):
    items: list[AttendanceRecord]
    total: int
    limit: int
    offset: int
    has_more: bool


class TrackerCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    label: str = Field(min_length=1, max_length=120)

    @field_validator("label")
    @classmethod
    def clean_label(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if not cleaned:
            raise ValueError("Tracker label is required.")
        return cleaned


class TrackerSummary(ORMModel):
    id: UUID
    label: str
    status: str
    assigned_at: datetime
    revoked_at: datetime | None


class TrackerCreated(TrackerSummary):
    tracker_token: str


class TrackerList(ORMModel):
    items: list[TrackerSummary]
    total: int
    limit: int
    offset: int
    has_more: bool


class PositionCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    accuracy_m: float = Field(gt=0, le=10000)
    captured_at: datetime
    client_position_id: UUID
    stop_id: UUID | None = None

    @field_validator("captured_at")
    @classmethod
    def timezone(cls, value: datetime) -> datetime:
        return _aware(value)


class PositionResponse(ORMModel):
    id: UUID
    yatra_id: UUID
    tracker_id: UUID
    stop_id: UUID | None
    latitude: float
    longitude: float
    accuracy_m: float
    captured_at: datetime
    received_at: datetime
    client_position_id: UUID


class PositionList(ORMModel):
    items: list[PositionResponse]
    total: int
    limit: int
    offset: int
    has_more: bool


class LatestPositionResponse(ORMModel):
    freshness: Literal["live", "stale", "offline"]
    latitude: float | None
    longitude: float | None
    accuracy_m: float | None
    captured_at: datetime | None
    received_at: datetime | None
    stop_id: UUID | None
    tracker_id: UUID | None
