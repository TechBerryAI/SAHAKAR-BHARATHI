from __future__ import annotations

import re
from datetime import datetime
from uuid import UUID

from pydantic import ConfigDict, Field, field_validator, model_validator

from app.core.security import normalize_name
from app.schemas.common import ORMModel

_CODE = re.compile(r"^[A-Z0-9]{2,16}$")


def _aware(value: datetime) -> datetime:
    if value.tzinfo is None or value.tzinfo.utcoffset(value) is None:
        raise ValueError("Use a timezone-aware date and time.")
    return value


def _clean_text(value: str | None) -> str | None:
    if value is None:
        return None
    cleaned = value.strip()
    return cleaned or None


class DistrictCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=2, max_length=120)
    code: str = Field(min_length=2, max_length=16)
    state_name: str | None = Field(default=None, max_length=120)

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("District name is required.")
        return cleaned

    @field_validator("code")
    @classmethod
    def clean_code(cls, value: str) -> str:
        cleaned = value.strip().upper()
        if not _CODE.fullmatch(cleaned):
            raise ValueError("Use 2 to 16 letters or digits.")
        return cleaned

    @field_validator("state_name")
    @classmethod
    def clean_state(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        return cleaned or None


class DistrictUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    name: str | None = Field(default=None, min_length=2, max_length=120)
    state_name: str | None = Field(default=None, max_length=120)
    active: bool | None = None

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("District name is required.")
        return cleaned

    @field_validator("state_name")
    @classmethod
    def clean_state(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        return cleaned or None

    @model_validator(mode="after")
    def something_to_change(self) -> DistrictUpdate:
        if self.name is None and self.state_name is None and self.active is None:
            raise ValueError("Nothing to update.")
        return self


class AdministratorBrief(ORMModel):
    user_id: UUID
    login_id: str
    full_name: str


class DistrictResponse(ORMModel):
    id: UUID
    name: str
    code: str
    state_name: str | None
    active: bool
    administrators: list[AdministratorBrief]
    created_at: datetime
    updated_at: datetime


class DistrictList(ORMModel):
    items: list[DistrictResponse]
    total: int
    limit: int
    offset: int
    has_more: bool


class AssignmentRequest(ORMModel):
    model_config = ConfigDict(extra="forbid")

    user_id: UUID


class YatraCapabilities(ORMModel):
    edit: bool
    transition: bool
    publish: bool
    assign_admin: bool
    manage_stops: bool
    submit_protocol: bool
    review_protocol: bool
    manage_items: bool


class DistrictBrief(ORMModel):
    id: UUID
    name: str
    code: str


class StopResponse(ORMModel):
    id: UUID
    sequence: int
    name: str
    address: str | None
    latitude: float | None
    longitude: float | None
    planned_arrival: datetime | None
    planned_departure: datetime | None
    active: bool


class YatraSummary(ORMModel):
    id: UUID
    public_code: str
    name: str
    district: DistrictBrief
    starts_at: datetime
    ends_at: datetime
    status: str
    published: bool


class YatraDetail(YatraSummary):
    description: str | None
    next_status: str | None
    capabilities: YatraCapabilities
    administrators: list[AdministratorBrief]
    stops: list[StopResponse]
    created_at: datetime
    updated_at: datetime


class YatraList(ORMModel):
    items: list[YatraSummary]
    total: int
    limit: int
    offset: int
    has_more: bool


class StopList(ORMModel):
    items: list[StopResponse]


class YatraCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=4000)
    district_id: UUID
    starts_at: datetime
    ends_at: datetime

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Yatra name is required.")
        return cleaned

    @field_validator("description")
    @classmethod
    def clean_description(cls, value: str | None) -> str | None:
        return _clean_text(value)

    @field_validator("starts_at", "ends_at")
    @classmethod
    def timezone(cls, value: datetime) -> datetime:
        return _aware(value)

    @model_validator(mode="after")
    def dates_in_order(self) -> YatraCreate:
        if self.ends_at < self.starts_at:
            raise ValueError("The end must be after the start.")
        return self


class YatraUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    name: str | None = Field(default=None, min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=4000)
    starts_at: datetime | None = None
    ends_at: datetime | None = None

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Yatra name is required.")
        return cleaned

    @field_validator("description")
    @classmethod
    def clean_description(cls, value: str | None) -> str | None:
        return _clean_text(value)

    @field_validator("starts_at", "ends_at")
    @classmethod
    def timezone(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _aware(value)

    @model_validator(mode="after")
    def something_to_change(self) -> YatraUpdate:
        if self.name is None and self.description is None and self.starts_at is None and self.ends_at is None:
            raise ValueError("Nothing to update.")
        return self


class YatraStatusChange(ORMModel):
    model_config = ConfigDict(extra="forbid")

    status: str = Field(min_length=1, max_length=32)


class StopWrite(ORMModel):
    model_config = ConfigDict(extra="forbid")

    sequence: int | None = Field(default=None, ge=1, le=999)
    name: str = Field(min_length=2, max_length=160)
    address: str | None = Field(default=None, max_length=255)
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    planned_arrival: datetime | None = None
    planned_departure: datetime | None = None
    active: bool = True

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Stop name is required.")
        return cleaned

    @field_validator("address")
    @classmethod
    def clean_address(cls, value: str | None) -> str | None:
        return _clean_text(value)

    @field_validator("planned_arrival", "planned_departure")
    @classmethod
    def timezone(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _aware(value)

    @model_validator(mode="after")
    def coords_and_times(self) -> StopWrite:
        if (self.latitude is None) != (self.longitude is None):
            raise ValueError("Latitude and longitude are set together.")
        if (
            self.planned_arrival is not None
            and self.planned_departure is not None
            and self.planned_departure < self.planned_arrival
        ):
            raise ValueError("Departure must be after arrival.")
        return self


class StopUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    sequence: int | None = Field(default=None, ge=1, le=999)
    name: str | None = Field(default=None, min_length=2, max_length=160)
    address: str | None = Field(default=None, max_length=255)
    latitude: float | None = None
    longitude: float | None = None
    planned_arrival: datetime | None = None
    planned_departure: datetime | None = None
    active: bool | None = None
    clear_address: bool = False
    clear_coordinates: bool = False
    clear_times: bool = False

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Stop name is required.")
        return cleaned

    @field_validator("address")
    @classmethod
    def clean_address(cls, value: str | None) -> str | None:
        return _clean_text(value)

    @field_validator("latitude")
    @classmethod
    def latitude_range(cls, value: float | None) -> float | None:
        if value is not None and not -90 <= value <= 90:
            raise ValueError("Latitude must be between -90 and 90.")
        return value

    @field_validator("longitude")
    @classmethod
    def longitude_range(cls, value: float | None) -> float | None:
        if value is not None and not -180 <= value <= 180:
            raise ValueError("Longitude must be between -180 and 180.")
        return value

    @field_validator("planned_arrival", "planned_departure")
    @classmethod
    def timezone(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _aware(value)

    @model_validator(mode="after")
    def something_to_change(self) -> StopUpdate:
        changed = any(
            value is not None
            for value in (
                self.sequence,
                self.name,
                self.address,
                self.latitude,
                self.longitude,
                self.planned_arrival,
                self.planned_departure,
                self.active,
            )
        )
        if not changed and not (self.clear_address or self.clear_coordinates or self.clear_times):
            raise ValueError("Nothing to update.")
        return self


class RouteResponse(ORMModel):
    label: str | None
    geometry: str | None
    updated_at: datetime


class RouteUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    label: str | None = Field(default=None, max_length=120)
    geometry: str | None = Field(default=None, max_length=8000)

    @field_validator("label", "geometry")
    @classmethod
    def clean(cls, value: str | None) -> str | None:
        return _clean_text(value)
