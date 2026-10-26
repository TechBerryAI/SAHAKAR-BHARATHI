from datetime import date, datetime

from pydantic import Field, field_validator

from app.core.security import is_valid_mobile, is_valid_pin, normalize_mobile
from app.schemas.common import ORMModel


class SessionCreate(ORMModel):
    identifier: str = Field(min_length=10, max_length=16)
    pin: str = Field(min_length=4, max_length=6)

    @field_validator("identifier")
    @classmethod
    def clean_identifier(cls, value: str) -> str:
        mobile = normalize_mobile(value)
        if not is_valid_mobile(mobile):
            raise ValueError("Enter a valid 10-digit Indian mobile number.")
        return mobile

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
    role: str = "participant"
