from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import ConfigDict, Field, field_validator, model_validator

from app.core.security import normalize_name
from app.schemas.common import ORMModel

ProtocolCategory = Literal["pre", "during", "post"]


def _aware(value: datetime) -> datetime:
    if value.tzinfo is None or value.tzinfo.utcoffset(value) is None:
        raise ValueError("Use a timezone-aware date and time.")
    return value


def _clean(value: str | None) -> str | None:
    if value is None:
        return None
    cleaned = value.strip()
    return cleaned or None


class ProtocolDocumentResponse(ORMModel):
    id: UUID
    original_filename: str
    content_type: str
    size_bytes: int
    created_at: datetime


class ProtocolItemResponse(ORMModel):
    id: UUID
    category: str
    title: str
    description: str | None
    required: bool
    status: str
    due_at: datetime | None
    overdue: bool
    responsible_label: str | None
    submitted_at: datetime | None
    reviewed_at: datetime | None
    rejection_reason: str | None
    documents: list[ProtocolDocumentResponse]
    created_at: datetime
    updated_at: datetime


class ProtocolList(ORMModel):
    items: list[ProtocolItemResponse]


class ProtocolItemCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    category: ProtocolCategory
    title: str = Field(min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=2000)
    required: bool = True
    due_at: datetime | None = None
    responsible_label: str | None = Field(default=None, max_length=120)

    @field_validator("title")
    @classmethod
    def clean_title(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Checklist title is required.")
        return cleaned

    @field_validator("description", "responsible_label")
    @classmethod
    def clean_optional(cls, value: str | None) -> str | None:
        return _clean(value)

    @field_validator("due_at")
    @classmethod
    def timezone(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _aware(value)


class ProtocolItemUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    category: ProtocolCategory | None = None
    title: str | None = Field(default=None, min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=2000)
    required: bool | None = None
    due_at: datetime | None = None
    responsible_label: str | None = Field(default=None, max_length=120)
    clear_due_at: bool = False
    clear_description: bool = False
    clear_responsible_label: bool = False

    @field_validator("title")
    @classmethod
    def clean_title(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Checklist title is required.")
        return cleaned

    @field_validator("description", "responsible_label")
    @classmethod
    def clean_optional(cls, value: str | None) -> str | None:
        return _clean(value)

    @field_validator("due_at")
    @classmethod
    def timezone(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _aware(value)

    @model_validator(mode="after")
    def something_to_change(self) -> ProtocolItemUpdate:
        changed = any(
            value is not None
            for value in (
                self.category,
                self.title,
                self.description,
                self.required,
                self.due_at,
                self.responsible_label,
            )
        )
        if not changed and not (self.clear_due_at or self.clear_description or self.clear_responsible_label):
            raise ValueError("Nothing to update.")
        return self


class ProtocolReject(ORMModel):
    model_config = ConfigDict(extra="forbid")

    reason: str = Field(min_length=3, max_length=500)

    @field_validator("reason")
    @classmethod
    def clean_reason(cls, value: str) -> str:
        cleaned = " ".join(value.split())
        if len(cleaned) < 3:
            raise ValueError("A rejection reason is required.")
        return cleaned


class ProtocolTemplateResponse(ORMModel):
    id: UUID
    category: str
    title: str
    description: str | None
    required: bool
    sort_order: int
    active: bool


class ProtocolTemplateList(ORMModel):
    items: list[ProtocolTemplateResponse]


class ProtocolTemplateCreate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    category: ProtocolCategory
    title: str = Field(min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=2000)
    required: bool = True
    sort_order: int = Field(default=0, ge=0, le=10000)

    @field_validator("title")
    @classmethod
    def clean_title(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Checklist title is required.")
        return cleaned

    @field_validator("description")
    @classmethod
    def clean_description(cls, value: str | None) -> str | None:
        return _clean(value)


class ProtocolTemplateUpdate(ORMModel):
    model_config = ConfigDict(extra="forbid")

    title: str | None = Field(default=None, min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=2000)
    required: bool | None = None
    sort_order: int | None = Field(default=None, ge=0, le=10000)
    active: bool | None = None

    @field_validator("title")
    @classmethod
    def clean_title(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Checklist title is required.")
        return cleaned

    @field_validator("description")
    @classmethod
    def clean_description(cls, value: str | None) -> str | None:
        return _clean(value)

    @model_validator(mode="after")
    def something_to_change(self) -> ProtocolTemplateUpdate:
        if all(value is None for value in (self.title, self.description, self.required, self.sort_order, self.active)):
            raise ValueError("Nothing to update.")
        return self
