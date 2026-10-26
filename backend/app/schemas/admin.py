from datetime import datetime
from typing import Literal
from uuid import UUID

from app.schemas.common import ORMModel
from app.schemas.profile import ProfileResponse


class AdminUserSummary(ORMModel):
    id: UUID
    login_id: str
    full_name: str
    mobile: str
    district: str | None = None
    state: str | None = None
    role: str
    status: str


class AdminUserList(ORMModel):
    items: list[AdminUserSummary]
    total: int
    limit: int
    offset: int


class AdminUserDetail(ProfileResponse):
    id: UUID
    created_at: datetime


class RoleUpdate(ORMModel):
    role: Literal["admin", "participant"]


class StatusUpdate(ORMModel):
    status: Literal["active", "disabled"]
