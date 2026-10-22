from datetime import date, datetime

from pydantic import Field, field_validator

from app.core.security import is_valid_pin, normalize_name
from app.schemas.common import ORMModel


class SessionCreate(ORMModel):
    identifier: str = Field(min_length=4, max_length=64)
    pin: str = Field(min_length=4, max_length=6)

    @field_validator("identifier")
    @classmethod
    def clean_identifier(cls, value: str) -> str:
        cleaned = normalize_name(value)
        if not cleaned:
            raise ValueError("Login ID or mobile is required.")
        return cleaned

    @field_validator("pin")
    @classmethod
    def clean_pin(cls, value: str) -> str:
        if not is_valid_pin(value):
            raise ValueError("PIN must be 4 to 6 digits.")
        return value


class SessionResponse(ORMModel):
    access_token: str
    token_type: str = "bearer"
    expires_at: datetime
    login_id: str
    full_name: str
    mobile: str
    email: str | None = None
    gender: str | None = None
    date_of_birth: date | None = None
    occupation: str | None = None
    address_line: str | None = None
    locality: str | None = None
    state: str | None = None
    district: str | None = None
    pincode: str | None = None
    status: str = "active"
