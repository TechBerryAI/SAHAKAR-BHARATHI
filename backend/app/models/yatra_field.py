from __future__ import annotations

import uuid
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    ForeignKeyConstraint,
    Index,
    Integer,
    Numeric,
    String,
    UniqueConstraint,
    func,
    text,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class YatraCounter(Base):
    """A Yatra-scoped counter. The QR token is a posted capability, not a booth code."""

    __tablename__ = "yatra_counters"
    __table_args__ = (
        UniqueConstraint("yatra_id", "code", name="uq_yatra_counters_yatra_code"),
        UniqueConstraint("id", "yatra_id", name="uq_yatra_counters_id_yatra"),
        UniqueConstraint("qr_token", name="uq_yatra_counters_qr_token"),
        CheckConstraint("status IN ('active', 'inactive')", name="ck_yatra_counters_status"),
        CheckConstraint("code ~ '^[A-Z0-9][A-Z0-9-]{1,15}$'", name="ck_yatra_counters_code"),
        CheckConstraint("qr_token ~ '^[A-Za-z0-9_-]{32,64}$'", name="ck_yatra_counters_qr_token"),
        CheckConstraint(
            "latitude IS NULL OR (latitude >= -90 AND latitude <= 90)",
            name="ck_yatra_counters_latitude",
        ),
        CheckConstraint(
            "longitude IS NULL OR (longitude >= -180 AND longitude <= 180)",
            name="ck_yatra_counters_longitude",
        ),
        CheckConstraint(
            "(latitude IS NULL AND longitude IS NULL AND location_accuracy_m IS NULL) OR "
            "(latitude IS NOT NULL AND longitude IS NOT NULL AND location_accuracy_m IS NOT NULL "
            "AND location_accuracy_m > 0 AND location_accuracy_m <= 1000)",
            name="ck_yatra_counters_location",
        ),
        CheckConstraint(
            "geofence_radius_m IS NULL OR (geofence_radius_m >= 10 AND geofence_radius_m <= 5000)",
            name="ck_yatra_counters_geofence",
        ),
        ForeignKeyConstraint(
            ["stop_id", "yatra_id"],
            ["yatra_stops.id", "yatra_stops.yatra_id"],
            ondelete="RESTRICT",
            name="yatra_counters_stop_yatra_fkey",
        ),
        Index("ix_yatra_counters_yatra_status", "yatra_id", "status"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    yatra_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("yatras.id", ondelete="CASCADE"),
        nullable=False,
    )
    stop_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)
    name: Mapped[str] = mapped_column(String(160), nullable=False)
    code: Mapped[str] = mapped_column(String(16), nullable=False)
    description: Mapped[str | None] = mapped_column(String(400), nullable=True)
    latitude: Mapped[Decimal | None] = mapped_column(Numeric(9, 6), nullable=True)
    longitude: Mapped[Decimal | None] = mapped_column(Numeric(9, 6), nullable=True)
    location_accuracy_m: Mapped[Decimal | None] = mapped_column(Numeric(7, 1), nullable=True)
    geofence_radius_m: Mapped[int | None] = mapped_column(Integer, nullable=True)
    qr_token: Mapped[str] = mapped_column(String(64), nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="inactive", server_default="inactive")
    created_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
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


class YatraAttendance(Base):
    """One check-in per participant, counter, and Asia/Kolkata day. Append-only."""

    __tablename__ = "yatra_attendance"
    __table_args__ = (
        UniqueConstraint("user_id", "counter_id", "attendance_date", name="uq_yatra_attendance_user_counter_day"),
        CheckConstraint("source = 'qr'", name="ck_yatra_attendance_source"),
        CheckConstraint(
            "(scan_latitude IS NULL AND scan_longitude IS NULL AND scan_accuracy_m IS NULL "
            "AND scan_distance_m IS NULL) OR (scan_latitude IS NOT NULL AND scan_longitude IS NOT NULL "
            "AND scan_accuracy_m IS NOT NULL AND scan_distance_m IS NOT NULL)",
            name="ck_yatra_attendance_scan_present",
        ),
        CheckConstraint(
            "scan_latitude IS NULL OR (scan_latitude >= -90 AND scan_latitude <= 90)",
            name="ck_yatra_attendance_scan_latitude",
        ),
        CheckConstraint(
            "scan_longitude IS NULL OR (scan_longitude >= -180 AND scan_longitude <= 180)",
            name="ck_yatra_attendance_scan_longitude",
        ),
        CheckConstraint(
            "scan_accuracy_m IS NULL OR scan_accuracy_m > 0",
            name="ck_yatra_attendance_scan_accuracy",
        ),
        CheckConstraint(
            "scan_distance_m IS NULL OR scan_distance_m >= 0",
            name="ck_yatra_attendance_scan_distance",
        ),
        ForeignKeyConstraint(
            ["counter_id", "yatra_id"],
            ["yatra_counters.id", "yatra_counters.yatra_id"],
            ondelete="RESTRICT",
            name="yatra_attendance_counter_yatra_fkey",
        ),
        Index("ix_yatra_attendance_yatra_checked_in", "yatra_id", "checked_in_at", "id"),
        Index("ix_yatra_attendance_user_checked_in", "user_id", "checked_in_at", "id"),
        Index("ix_yatra_attendance_counter_date", "counter_id", "attendance_date"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    yatra_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("yatras.id", ondelete="RESTRICT"),
        nullable=False,
    )
    counter_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="RESTRICT"),
        nullable=False,
    )
    checked_in_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    attendance_date: Mapped[date] = mapped_column(Date, nullable=False)
    source: Mapped[str] = mapped_column(String(8), nullable=False, default="qr", server_default="qr")
    scan_latitude: Mapped[Decimal | None] = mapped_column(Numeric(9, 6), nullable=True)
    scan_longitude: Mapped[Decimal | None] = mapped_column(Numeric(9, 6), nullable=True)
    scan_accuracy_m: Mapped[Decimal | None] = mapped_column(Numeric(8, 2), nullable=True)
    scan_distance_m: Mapped[Decimal | None] = mapped_column(Numeric(10, 2), nullable=True)


class YatraTracker(Base):
    """Device assignment. The bearer is stored as a SHA-256 hash and shown once."""

    __tablename__ = "yatra_trackers"
    __table_args__ = (
        UniqueConstraint("token_hash", name="uq_yatra_trackers_token_hash"),
        UniqueConstraint("id", "yatra_id", name="uq_yatra_trackers_id_yatra"),
        CheckConstraint("status IN ('active', 'revoked')", name="ck_yatra_trackers_status"),
        Index("ix_yatra_trackers_yatra_status", "yatra_id", "status"),
        Index(
            "uq_yatra_trackers_one_active",
            "yatra_id",
            unique=True,
            postgresql_where=text("status = 'active'"),
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    yatra_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("yatras.id", ondelete="CASCADE"),
        nullable=False,
    )
    label: Mapped[str] = mapped_column(String(120), nullable=False)
    token_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="active", server_default="active")
    assigned_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )
    assigned_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    revoked_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class YatraPosition(Base):
    """Append-only GPS history. received_at is the server clock."""

    __tablename__ = "yatra_positions"
    __table_args__ = (
        UniqueConstraint("tracker_id", "client_position_id", name="uq_yatra_positions_tracker_client"),
        CheckConstraint("latitude >= -90 AND latitude <= 90", name="ck_yatra_positions_latitude"),
        CheckConstraint("longitude >= -180 AND longitude <= 180", name="ck_yatra_positions_longitude"),
        CheckConstraint("accuracy_m > 0", name="ck_yatra_positions_accuracy"),
        CheckConstraint("source = 'gps'", name="ck_yatra_positions_source"),
        ForeignKeyConstraint(
            ["stop_id", "yatra_id"],
            ["yatra_stops.id", "yatra_stops.yatra_id"],
            ondelete="RESTRICT",
            name="yatra_positions_stop_yatra_fkey",
        ),
        ForeignKeyConstraint(
            ["tracker_id", "yatra_id"],
            ["yatra_trackers.id", "yatra_trackers.yatra_id"],
            ondelete="RESTRICT",
            name="yatra_positions_tracker_yatra_fkey",
        ),
        Index("ix_yatra_positions_yatra_captured", "yatra_id", "captured_at", "id"),
        Index("ix_yatra_positions_tracker_captured", "tracker_id", "captured_at", "id"),
        Index("ix_yatra_positions_received_at", "received_at"),
        Index(
            "ix_yatra_positions_stop",
            "stop_id",
            postgresql_where=text("stop_id IS NOT NULL"),
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    yatra_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("yatras.id", ondelete="RESTRICT"),
        nullable=False,
    )
    tracker_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    stop_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)
    latitude: Mapped[Decimal] = mapped_column(Numeric(9, 6), nullable=False)
    longitude: Mapped[Decimal] = mapped_column(Numeric(9, 6), nullable=False)
    accuracy_m: Mapped[Decimal] = mapped_column(Numeric(7, 1), nullable=False)
    captured_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    source: Mapped[str] = mapped_column(String(8), nullable=False, default="gps", server_default="gps")
    client_position_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)


class YatraLatestPosition(Base):
    """One current pin per Yatra. Freshness is computed from captured_at, not stored."""

    __tablename__ = "yatra_latest_positions"
    __table_args__ = (
        UniqueConstraint("position_id", name="uq_yatra_latest_positions_position"),
        CheckConstraint("latitude >= -90 AND latitude <= 90", name="ck_yatra_latest_positions_latitude"),
        CheckConstraint("longitude >= -180 AND longitude <= 180", name="ck_yatra_latest_positions_longitude"),
        CheckConstraint("accuracy_m > 0", name="ck_yatra_latest_positions_accuracy"),
        ForeignKeyConstraint(
            ["stop_id", "yatra_id"],
            ["yatra_stops.id", "yatra_stops.yatra_id"],
            ondelete="RESTRICT",
            name="yatra_latest_positions_stop_yatra_fkey",
        ),
        ForeignKeyConstraint(
            ["tracker_id", "yatra_id"],
            ["yatra_trackers.id", "yatra_trackers.yatra_id"],
            ondelete="RESTRICT",
            name="yatra_latest_positions_tracker_yatra_fkey",
        ),
    )

    yatra_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("yatras.id", ondelete="CASCADE"),
        primary_key=True,
    )
    position_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("yatra_positions.id", ondelete="RESTRICT"),
        nullable=False,
    )
    tracker_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    stop_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)
    latitude: Mapped[Decimal] = mapped_column(Numeric(9, 6), nullable=False)
    longitude: Mapped[Decimal] = mapped_column(Numeric(9, 6), nullable=False)
    accuracy_m: Mapped[Decimal] = mapped_column(Numeric(7, 1), nullable=False)
    captured_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )
