from __future__ import annotations

import uuid
from datetime import date, datetime
from typing import TYPE_CHECKING

from sqlalchemy import Date, DateTime, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base

if TYPE_CHECKING:
    from app.models.registration import Registration
    from app.models.session import AuthSession


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    login_id: Mapped[str] = mapped_column(String(32), unique=True, index=True, nullable=False)
    mobile: Mapped[str] = mapped_column(String(10), unique=True, index=True, nullable=False)
    email: Mapped[str | None] = mapped_column(String(254), unique=True, nullable=True)
    pin_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(120), nullable=False)
    gender: Mapped[str | None] = mapped_column(String(16), nullable=True)
    date_of_birth: Mapped[date | None] = mapped_column(Date, nullable=True)
    occupation: Mapped[str | None] = mapped_column(String(32), nullable=True)
    address_line: Mapped[str | None] = mapped_column(String(255), nullable=True)
    locality: Mapped[str | None] = mapped_column(String(120), nullable=True)
    district: Mapped[str | None] = mapped_column(String(120), nullable=True)
    state: Mapped[str | None] = mapped_column(String(120), nullable=True)
    pincode: Mapped[str | None] = mapped_column(String(6), nullable=True)
    status: Mapped[str] = mapped_column(
        String(32),
        nullable=False,
        default="active",
        server_default="active",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )

    registration: Mapped[Registration | None] = relationship(back_populates="user")
    sessions: Mapped[list[AuthSession]] = relationship(back_populates="user")
