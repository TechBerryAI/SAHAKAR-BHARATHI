from __future__ import annotations

from datetime import datetime
from uuid import UUID

from pydantic import Field, field_validator, model_validator

from app.core.security import normalize_name
from app.schemas.common import ORMModel


def _clean_place_name(value: str) -> str:
    cleaned = normalize_name(value)
    if len(cleaned) < 2:
        raise ValueError("Booth name is required.")
    return cleaned


class PlaceCreate(ORMModel):
    name: str = Field(min_length=2, max_length=120)

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str) -> str:
        return _clean_place_name(value)


class PlaceUpdate(ORMModel):
    name: str | None = Field(default=None, min_length=2, max_length=120)
    active: bool | None = None

    @field_validator("name")
    @classmethod
    def clean_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        return _clean_place_name(value)

    @model_validator(mode="after")
    def something_to_change(self) -> PlaceUpdate:
        if self.name is None and self.active is None:
            raise ValueError("Nothing to update.")
        return self


class PlaceResponse(ORMModel):
    id: UUID
    name: str
    code: str
    active: bool
    created_at: datetime


class PlaceList(ORMModel):
    items: list[PlaceResponse]
