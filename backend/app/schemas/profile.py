from datetime import date

from pydantic import EmailStr, Field, field_validator

from app.core.security import is_valid_pincode, normalize_email, normalize_name
from app.schemas.common import ORMModel
from app.schemas.registration import Occupation


class ProfileResponse(ORMModel):
    login_id: str
    full_name: str
    mobile: str
    email: str | None = None
    gender: str | None = None
    date_of_birth: date | None = None
    occupation: str | None = None
    address_line: str | None = None
    locality: str | None = None
    district: str | None = None
    state: str | None = None
    pincode: str | None = None
    status: str
    role: str = "participant"


class ProfileUpdate(ORMModel):
    full_name: str | None = Field(default=None, min_length=2, max_length=120)
    email: EmailStr | None = None
    occupation: Occupation | None = None
    address_line: str | None = Field(default=None, max_length=255)
    locality: str | None = Field(default=None, max_length=120)
    district: str | None = Field(default=None, max_length=120)
    state: str | None = Field(default=None, max_length=120)
    pincode: str | None = None

    @field_validator("full_name")
    @classmethod
    def clean_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Full name is required.")
        return cleaned

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: object) -> object:
        if value is None or value == "":
            return None
        return normalize_email(str(value))

    @field_validator("pincode")
    @classmethod
    def clean_pincode(cls, value: str | None) -> str | None:
        if value is None or value == "":
            return None
        if not is_valid_pincode(value):
            raise ValueError("PIN code must be 6 digits.")
        return value

    @field_validator("address_line", "locality", "district", "state", mode="before")
    @classmethod
    def blank_to_none(cls, value: object) -> object:
        if isinstance(value, str) and not value.strip():
            return None
        return value.strip() if isinstance(value, str) else value
