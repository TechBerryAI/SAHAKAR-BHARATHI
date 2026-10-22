from datetime import date
from typing import Literal

from pydantic import EmailStr, Field, field_validator, model_validator

from app.core.security import is_valid_mobile, is_valid_pin, is_valid_pincode, normalize_email, normalize_mobile, normalize_name
from app.schemas.common import ORMModel

Gender = Literal["Male", "Female", "Other"]
Occupation = Literal["Farmer", "Fisherman", "Both", "Allied"]


class RegistrationCreate(ORMModel):
    full_name: str = Field(min_length=2, max_length=120)
    gender: Gender | None = None
    date_of_birth: date | None = None
    occupation: Occupation | None = None
    mobile: str
    email: EmailStr | None = None
    address_line: str | None = Field(default=None, max_length=255)
    locality: str | None = Field(default=None, max_length=120)
    district: str | None = Field(default=None, max_length=120)
    state: str | None = Field(default=None, max_length=120)
    pincode: str | None = None
    pin: str = Field(min_length=4, max_length=6)
    verification_token: str = Field(min_length=20, max_length=200)

    @field_validator("full_name")
    @classmethod
    def clean_name(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if len(cleaned) < 2:
            raise ValueError("Full name is required.")
        return cleaned

    @field_validator("mobile")
    @classmethod
    def clean_mobile(cls, value: str) -> str:
        mobile = normalize_mobile(value)
        if not is_valid_mobile(mobile):
            raise ValueError("Enter a valid 10-digit Indian mobile number.")
        return mobile

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: object) -> object:
        if value is None or value == "":
            return None
        return normalize_email(str(value))

    @field_validator("pin")
    @classmethod
    def clean_pin(cls, value: str) -> str:
        if not is_valid_pin(value):
            raise ValueError("PIN must be 4 to 6 digits.")
        return value

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

    @model_validator(mode="after")
    def check_age(self) -> "RegistrationCreate":
        if self.date_of_birth is not None:
            today = date.today()
            years = today.year - self.date_of_birth.year - (
                (today.month, today.day) < (self.date_of_birth.month, self.date_of_birth.day)
            )
            if years < 18:
                raise ValueError("Minimum age is 18.")
        return self


class RegistrationResponse(ORMModel):
    registration_id: str
    login_id: str
    full_name: str
    mobile: str
    occupation: str | None = None
    state: str | None = None
    district: str | None = None
