import uuid

from pydantic import Field, field_validator

from app.core.security import is_valid_mobile, normalize_mobile
from app.schemas.common import ORMModel


class PhoneVerificationCreate(ORMModel):
    mobile: str

    @field_validator("mobile")
    @classmethod
    def clean_mobile(cls, value: str) -> str:
        mobile = normalize_mobile(value)
        if not is_valid_mobile(mobile):
            raise ValueError("Enter a valid 10-digit Indian mobile number.")
        return mobile


class PhoneVerificationCreated(ORMModel):
    id: uuid.UUID
    expires_in_seconds: int


class PhoneVerificationConfirm(ORMModel):
    code: str = Field(min_length=6, max_length=6)

    @field_validator("code")
    @classmethod
    def clean_code(cls, value: str) -> str:
        if not value.isdigit():
            raise ValueError("Enter the 6-digit code.")
        return value


class PhoneVerificationConfirmed(ORMModel):
    verification_token: str
    expires_in_seconds: int
