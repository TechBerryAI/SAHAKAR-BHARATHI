from __future__ import annotations

import base64
import binascii
import math
import secrets
import uuid
from datetime import UTC, datetime, timedelta
from decimal import Decimal
from uuid import UUID
from zoneinfo import ZoneInfo

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import Settings, get_settings
from app.core.errors import (
    ConflictError,
    ForbiddenError,
    InactiveCounterError,
    InvalidCursorError,
    InvalidPositionError,
    InvalidStateError,
    NotFoundError,
    UnauthorizedError,
    UnknownCounterError,
)
from app.core.rate_limit import RateLimiter, enforce_rate_limit
from app.core.security import hash_token, issue_session_token
from app.core.yatra_rules import MANAGE_ACCESS, OPERATE_ACCESS
from app.db.session import constraint_name, is_unique_violation
from app.models.user import User
from app.models.yatra import Yatra
from app.models.yatra_field import YatraAttendance, YatraCounter, YatraLatestPosition, YatraPosition, YatraTracker
from app.repositories.audit import AuditRepository
from app.repositories.yatra_field import (
    YatraAttendanceRepository,
    YatraCounterRepository,
    YatraPositionRepository,
    YatraTrackerRepository,
)
from app.repositories.yatras import YatraRepository
from app.schemas.yatra_field import (
    AttendanceList,
    AttendanceRecord,
    AttendanceScan,
    CounterCreate,
    CounterDetail,
    CounterList,
    CounterSummary,
    CounterUpdate,
    LatestPositionResponse,
    PositionBatchCreate,
    PositionBatchItemResult,
    PositionBatchResponse,
    PositionCreate,
    PositionList,
    PositionResponse,
    TrackerCreate,
    TrackerCreated,
    TrackerList,
    TrackerSummary,
    qr_text,
)
from app.services.yatra_access import YatraAccess

_IST = ZoneInfo("Asia/Kolkata")
_EARTH_RADIUS_M = 6_371_000.0
_ACTIVE_TRACKER = "This Yatra already has an active tracker. Revoke it before assigning a new one."
_COUNTER_LOCATION = "Set the counter location before activating it."
_OFFLINE = LatestPositionResponse(
    freshness="offline",
    latitude=None,
    longitude=None,
    accuracy_m=None,
    captured_at=None,
    received_at=None,
    stop_id=None,
    tracker_id=None,
)


class YatraFieldService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.yatras = YatraRepository(db)
        self.counters = YatraCounterRepository(db)
        self.visits = YatraAttendanceRepository(db)
        self.trackers = YatraTrackerRepository(db)
        self.positions = YatraPositionRepository(db)
        self.audit = AuditRepository(db)
        self.access = YatraAccess(db)

    def create_counter(self, actor: User, yatra_id: UUID, payload: CounterCreate) -> CounterDetail:
        yatra, _level = self._operate(actor, yatra_id)
        self._stop_on_yatra(yatra.id, payload.stop_id)
        counter = self._insert_counter(actor, yatra, payload)
        self.audit.add(
            "counter_created",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "counter_id": str(counter.id)},
        )
        self.db.commit()
        self.db.refresh(counter)
        return self._detail(counter)

    def list_counters(
        self,
        actor: User,
        yatra_id: UUID,
        limit: int,
        offset: int,
        status: str | None,
    ) -> CounterList:
        yatra, _level = self._view(actor, yatra_id, OPERATE_ACCESS)
        if status is not None and status not in {"active", "inactive"}:
            raise InvalidStateError("That status is not valid.")
        rows, total = self.counters.page(yatra.id, limit, offset, status)
        items = [self._summary(row) for row in rows]
        return CounterList(
            items=items,
            total=total,
            limit=limit,
            offset=offset,
            has_more=offset + len(items) < total,
        )

    def get_counter(self, actor: User, yatra_id: UUID, counter_id: UUID) -> CounterDetail:
        yatra, _level = self._view(actor, yatra_id, OPERATE_ACCESS)
        counter = self._counter(yatra.id, counter_id)
        return self._detail(counter)

    def update_counter(
        self,
        actor: User,
        yatra_id: UUID,
        counter_id: UUID,
        payload: CounterUpdate,
    ) -> CounterDetail:
        yatra, _level = self._operate(actor, yatra_id)
        counter = self._counter(yatra.id, counter_id)
        if payload.name is not None:
            counter.name = payload.name
        if payload.code is not None:
            counter.code = payload.code
        if payload.clear_description:
            counter.description = None
        elif payload.description is not None:
            counter.description = payload.description
        if payload.clear_stop:
            counter.stop_id = None
        elif payload.stop_id is not None:
            self._stop_on_yatra(yatra.id, payload.stop_id)
            counter.stop_id = payload.stop_id
        if payload.clear_coordinates:
            if counter.status == "active" and get_settings().counter_geofence_mode == "required":
                raise InvalidStateError(_COUNTER_LOCATION)
            counter.latitude = None
            counter.longitude = None
            counter.location_accuracy_m = None
        elif payload.latitude is not None:
            counter.latitude = _coord(payload.latitude)
            counter.longitude = _coord(payload.longitude)
            counter.location_accuracy_m = _accuracy(payload.location_accuracy_m)
        if payload.clear_geofence:
            counter.geofence_radius_m = None
        elif payload.geofence_radius_m is not None:
            counter.geofence_radius_m = payload.geofence_radius_m
        self._location_ok(counter)
        self.audit.add(
            "counter_updated",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "counter_id": str(counter.id)},
        )
        self._commit_code()
        self.db.refresh(counter)
        return self._detail(counter)

    def set_counter_status(
        self,
        actor: User,
        yatra_id: UUID,
        counter_id: UUID,
        status: str,
    ) -> CounterDetail:
        yatra, _level = self._operate(actor, yatra_id)
        counter = self._counter(yatra.id, counter_id)
        if status == "active" and get_settings().counter_geofence_mode == "required":
            if counter.latitude is None or counter.longitude is None or counter.location_accuracy_m is None:
                raise InvalidStateError(_COUNTER_LOCATION)
        if counter.status != status:
            counter.status = status
            self.audit.add(
                "counter_activated" if status == "active" else "counter_deactivated",
                user_id=actor.id,
                extra={"yatra_id": str(yatra.id), "counter_id": str(counter.id)},
            )
            self.db.commit()
            self.db.refresh(counter)
        return self._detail(counter)

    def rotate_counter_qr(self, actor: User, yatra_id: UUID, counter_id: UUID) -> CounterDetail:
        yatra, _level = self._operate(actor, yatra_id)
        counter = self._counter(yatra.id, counter_id)
        for _ in range(8):
            counter.qr_token = secrets.token_urlsafe(32)
            try:
                self.db.flush()
            except IntegrityError as exc:
                self.db.rollback()
                if not is_unique_violation(exc):
                    raise
                yatra, _level = self._operate(actor, yatra_id)
                counter = self._counter(yatra.id, counter_id)
                continue
            self.audit.add(
                "counter_qr_rotated",
                user_id=actor.id,
                extra={"yatra_id": str(yatra.id), "counter_id": str(counter.id)},
            )
            self.db.commit()
            self.db.refresh(counter)
            return self._detail(counter)
        raise InvalidStateError("Could not assign a counter QR. Try again.")

    def scan(
        self,
        actor: User,
        yatra_id: UUID,
        payload: AttendanceScan,
        limiter: RateLimiter,
        settings: Settings,
    ) -> AttendanceRecord:
        counter = self.counters.get_by_token(payload.qr_token)
        if counter is None or counter.yatra_id != yatra_id:
            self._count_scan_failure(actor, limiter, settings)
            raise UnknownCounterError()
        yatra = self.yatras.get(yatra_id)
        if yatra is None or self.access.level(actor, yatra) is None:
            self._count_scan_failure(actor, limiter, settings)
            raise UnknownCounterError()
        if not yatra.published or yatra.status != "in_progress":
            self._count_scan_failure(actor, limiter, settings)
            raise InvalidStateError("This Yatra is not accepting attendance.")
        if counter.status != "active":
            self._count_scan_failure(actor, limiter, settings)
            raise InactiveCounterError()
        scan_fix = self._scan_fix(actor, counter, payload, limiter, settings)

        checked_in_at = datetime.now(UTC)
        today = checked_in_at.astimezone(_IST).date()
        existing = self.visits.find_for_day(actor.id, counter.id, today)
        if existing is not None:
            return self._attendance(existing, counter.name, actor, already_recorded=True)

        visit = YatraAttendance(
            id=uuid.uuid4(),
            yatra_id=yatra.id,
            counter_id=counter.id,
            user_id=actor.id,
            checked_in_at=checked_in_at,
            attendance_date=today,
            source="qr",
            scan_latitude=None if scan_fix is None else _coord(scan_fix[0]),
            scan_longitude=None if scan_fix is None else _coord(scan_fix[1]),
            scan_accuracy_m=None if scan_fix is None else _metres(scan_fix[2]),
            scan_distance_m=None if scan_fix is None else _metres(scan_fix[3]),
        )
        self.db.add(visit)
        self.audit.add(
            "attendance_recorded",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "counter_id": str(counter.id), "attendance_id": str(visit.id)},
        )
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if not is_unique_violation(exc):
                raise
            raced = self.visits.find_for_day(actor.id, counter.id, today)
            if raced is None:
                raise
            return self._attendance(raced, counter.name, actor, already_recorded=True)
        self.db.refresh(visit)
        return self._attendance(visit, counter.name, actor, already_recorded=False)

    def list_attendance(
        self,
        actor: User,
        yatra_id: UUID,
        limit: int,
        offset: int,
        *,
        attendance_date=None,
        counter_id: UUID | None = None,
    ) -> AttendanceList:
        yatra, _level = self._view(actor, yatra_id, OPERATE_ACCESS)
        rows, total = self.visits.page_for_yatra(
            yatra.id,
            limit,
            offset,
            attendance_date=attendance_date,
            counter_id=counter_id,
        )
        return self._attendance_page(rows, total, limit, offset)

    def list_my_attendance(self, actor: User, yatra_id: UUID, limit: int, offset: int) -> AttendanceList:
        yatra, _level = self._visible(actor, yatra_id)
        rows, total = self.visits.page_for_user(yatra.id, actor.id, limit, offset)
        return self._attendance_page(rows, total, limit, offset)

    def create_tracker(self, actor: User, yatra_id: UUID, payload: TrackerCreate) -> TrackerCreated:
        yatra, _level = self._manage(actor, yatra_id)
        if self.trackers.active_for_yatra(yatra.id) is not None:
            raise ConflictError(_ACTIVE_TRACKER)
        raw, token_hash = issue_session_token()
        tracker = YatraTracker(
            id=uuid.uuid4(),
            yatra_id=yatra.id,
            label=payload.label,
            token_hash=token_hash,
            status="active",
            assigned_by=actor.id,
        )
        self.db.add(tracker)
        self.audit.add(
            "tracker_assigned",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "tracker_id": str(tracker.id)},
        )
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                if constraint_name(exc) == "uq_yatra_trackers_one_active":
                    raise ConflictError(_ACTIVE_TRACKER) from None
                raise ConflictError("Could not assign a tracker. Try again.") from None
            raise
        self.db.refresh(tracker)
        summary = self._tracker(tracker)
        return TrackerCreated(**summary.model_dump(), tracker_token=raw)

    def list_trackers(self, actor: User, yatra_id: UUID, limit: int, offset: int) -> TrackerList:
        yatra, _level = self._view(actor, yatra_id, MANAGE_ACCESS)
        rows, total = self.trackers.page(yatra.id, limit, offset)
        items = [self._tracker(row) for row in rows]
        return TrackerList(
            items=items,
            total=total,
            limit=limit,
            offset=offset,
            has_more=offset + len(items) < total,
        )

    def revoke_tracker(self, actor: User, yatra_id: UUID, tracker_id: UUID) -> TrackerSummary:
        yatra, _level = self._manage(actor, yatra_id)
        tracker = self.trackers.get_for_yatra(yatra.id, tracker_id)
        if tracker is None:
            raise NotFoundError("That tracker was not found.")
        if tracker.status != "revoked":
            tracker.status = "revoked"
            tracker.revoked_at = datetime.now(UTC)
            tracker.revoked_by = actor.id
            self.audit.add(
                "tracker_revoked",
                user_id=actor.id,
                extra={"yatra_id": str(yatra.id), "tracker_id": str(tracker.id)},
            )
            self.db.commit()
            self.db.refresh(tracker)
        return self._tracker(tracker)

    def authenticate_tracker(
        self,
        raw_token: str | None,
        client_ip: str,
        limiter: RateLimiter,
        settings: Settings,
    ) -> YatraTracker:
        if not raw_token:
            self._count_position_auth_failure(client_ip, limiter, settings)
            raise UnauthorizedError("Session is invalid or expired.")
        tracker = self.trackers.get_by_token_hash(hash_token(raw_token))
        if tracker is None or tracker.status != "active":
            self._count_position_auth_failure(client_ip, limiter, settings)
            raise UnauthorizedError("Session is invalid or expired.")
        return tracker

    def ingest(
        self,
        tracker: YatraTracker,
        yatra_id: UUID,
        payload: PositionCreate,
        limiter: RateLimiter,
        settings: Settings,
    ) -> tuple[PositionResponse, bool]:
        if tracker.yatra_id != yatra_id:
            raise NotFoundError("That Yatra was not found.")
        existing = self.positions.by_client(tracker.id, payload.client_position_id)
        if existing is not None:
            return self._position(existing), False
        yatra = self.yatras.get(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        if yatra.status != "in_progress":
            self._count_invalid_position(tracker, limiter, settings)
            raise InvalidStateError("This Yatra is not accepting positions.")
        rejection = self._position_rejection(yatra.id, payload, settings, datetime.now(UTC))
        if rejection is not None:
            self._count_invalid_position(tracker, limiter, settings)
            if rejection[0] == 422:
                raise InvalidPositionError(rejection[1])
            raise InvalidStateError(rejection[1])
        received_at = datetime.now(UTC)
        enforce_rate_limit(
            limiter,
            f"position:{tracker.id}",
            settings.position_rate_limit,
            settings.position_rate_window_seconds,
            retry_after=settings.position_rate_window_seconds,
        )
        position = YatraPosition(
            yatra_id=yatra.id,
            tracker_id=tracker.id,
            stop_id=payload.stop_id,
            latitude=_coord(payload.latitude),
            longitude=_coord(payload.longitude),
            accuracy_m=_accuracy(payload.accuracy_m),
            captured_at=payload.captured_at,
            received_at=received_at,
            source="gps",
            client_position_id=payload.client_position_id,
        )
        self.db.add(position)
        try:
            self.db.flush()
        except IntegrityError as exc:
            self.db.rollback()
            if not is_unique_violation(exc):
                raise
            raced = self.positions.by_client(tracker.id, payload.client_position_id)
            if raced is None:
                raise
            return self._position(raced), False
        self._upsert_latest(position)
        self.db.commit()
        self.db.refresh(position)
        return self._position(position), True

    def ingest_batch(
        self,
        tracker: YatraTracker,
        yatra_id: UUID,
        payload: PositionBatchCreate,
        limiter: RateLimiter,
        settings: Settings,
    ) -> PositionBatchResponse:
        if tracker.yatra_id != yatra_id:
            raise NotFoundError("That Yatra was not found.")
        yatra = self.yatras.get(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        if yatra.status != "in_progress":
            raise InvalidStateError("This Yatra is not accepting positions.")
        enforce_rate_limit(
            limiter,
            f"position-batch:{tracker.id}",
            settings.position_batch_rate_limit,
            settings.position_batch_rate_window_seconds,
            retry_after=settings.position_batch_rate_window_seconds,
        )
        received_at = datetime.now(UTC)
        results: list[PositionBatchItemResult] = []
        accepted_rows: list[YatraPosition] = []
        seen: set[UUID] = set()
        for item in payload.positions:
            if item.client_position_id in seen:
                results.append(
                    PositionBatchItemResult(
                        client_position_id=item.client_position_id,
                        status="duplicate",
                        error=None,
                    )
                )
                continue
            seen.add(item.client_position_id)
            existing = self.positions.by_client(tracker.id, item.client_position_id)
            if existing is not None:
                results.append(
                    PositionBatchItemResult(
                        client_position_id=item.client_position_id,
                        status="duplicate",
                        error=None,
                    )
                )
                continue
            rejection = self._position_rejection(yatra.id, item, settings, received_at)
            if rejection is not None:
                self._count_invalid_position(tracker, limiter, settings)
                results.append(
                    PositionBatchItemResult(
                        client_position_id=item.client_position_id,
                        status="rejected",
                        error=rejection[1],
                    )
                )
                continue
            nested = self.db.begin_nested()
            position = YatraPosition(
                yatra_id=yatra.id,
                tracker_id=tracker.id,
                stop_id=item.stop_id,
                latitude=_coord(item.latitude),
                longitude=_coord(item.longitude),
                accuracy_m=_accuracy(item.accuracy_m),
                captured_at=item.captured_at,
                received_at=received_at,
                source="gps",
                client_position_id=item.client_position_id,
            )
            self.db.add(position)
            try:
                self.db.flush()
            except IntegrityError as exc:
                nested.rollback()
                if not is_unique_violation(exc):
                    raise
                results.append(
                    PositionBatchItemResult(
                        client_position_id=item.client_position_id,
                        status="duplicate",
                        error=None,
                    )
                )
                continue
            nested.commit()
            accepted_rows.append(position)
            results.append(
                PositionBatchItemResult(
                    client_position_id=item.client_position_id,
                    status="accepted",
                    error=None,
                )
            )
        if accepted_rows:
            newest = max(accepted_rows, key=lambda row: (row.captured_at, row.id.int))
            self._upsert_latest(newest)
        self.db.commit()
        return PositionBatchResponse(
            results=results,
            accepted=sum(item.status == "accepted" for item in results),
            duplicates=sum(item.status == "duplicate" for item in results),
            rejected=sum(item.status == "rejected" for item in results),
        )

    def latest_position(self, actor: User, yatra_id: UUID, settings: Settings) -> LatestPositionResponse:
        yatra, level = self._visible(actor, yatra_id)
        found = self.positions.latest(yatra.id)
        if found is None:
            return _OFFLINE
        row, tracker_status = found
        freshness = _freshness(row.captured_at, tracker_status, settings, datetime.now(UTC))
        if tracker_status != "active":
            return _OFFLINE
        hide = level not in OPERATE_ACCESS and (freshness == "offline" or yatra.status != "in_progress")
        if hide:
            return LatestPositionResponse(
                freshness=freshness,
                latitude=None,
                longitude=None,
                accuracy_m=None,
                captured_at=row.captured_at,
                received_at=row.received_at,
                stop_id=None,
                tracker_id=None,
            )
        return LatestPositionResponse(
            freshness=freshness,
            latitude=float(row.latitude),
            longitude=float(row.longitude),
            accuracy_m=float(row.accuracy_m),
            captured_at=row.captured_at,
            received_at=row.received_at,
            stop_id=row.stop_id,
            tracker_id=row.tracker_id,
        )

    def list_positions(
        self,
        actor: User,
        yatra_id: UUID,
        limit: int,
        *,
        before: str | None,
        captured_from: datetime | None,
        captured_to: datetime | None,
        tracker_id: UUID | None,
    ) -> PositionList:
        yatra, _level = self._view(actor, yatra_id, OPERATE_ACCESS)
        rows, has_more = self.positions.page(
            yatra.id,
            limit,
            before=decode_cursor(before) if before else None,
            captured_from=captured_from,
            captured_to=captured_to,
            tracker_id=tracker_id,
        )
        items = [self._position(row) for row in rows]
        next_cursor = encode_cursor(rows[-1].captured_at, rows[-1].id) if has_more and rows else None
        return PositionList(items=items, next_cursor=next_cursor, has_more=has_more, limit=limit)

    def _insert_counter(self, actor: User, yatra: Yatra, payload: CounterCreate) -> YatraCounter:
        for _ in range(8):
            counter = YatraCounter(
                yatra_id=yatra.id,
                stop_id=payload.stop_id,
                name=payload.name,
                code=payload.code,
                description=payload.description,
                latitude=_coord(payload.latitude),
                longitude=_coord(payload.longitude),
                location_accuracy_m=_accuracy(payload.location_accuracy_m) if payload.latitude is not None else None,
                geofence_radius_m=payload.geofence_radius_m,
                qr_token=secrets.token_urlsafe(32),
                status="inactive",
                created_by=actor.id,
            )
            self.db.add(counter)
            try:
                self.db.flush()
            except IntegrityError as exc:
                self.db.rollback()
                if not is_unique_violation(exc):
                    raise
                if self.counters.get_by_token(counter.qr_token) is not None:
                    continue
                raise ConflictError("A counter with this code already exists.") from None
            return counter
        raise InvalidStateError("Could not assign a counter QR. Try again.")

    def _upsert_latest(self, position: YatraPosition) -> None:
        values = {
            "yatra_id": position.yatra_id,
            "position_id": position.id,
            "tracker_id": position.tracker_id,
            "stop_id": position.stop_id,
            "latitude": position.latitude,
            "longitude": position.longitude,
            "accuracy_m": position.accuracy_m,
            "captured_at": position.captured_at,
            "received_at": position.received_at,
            "updated_at": position.received_at,
        }
        stmt = pg_insert(YatraLatestPosition).values(**values)
        excluded = stmt.excluded
        stmt = stmt.on_conflict_do_update(
            index_elements=[YatraLatestPosition.yatra_id],
            set_={
                "position_id": excluded.position_id,
                "tracker_id": excluded.tracker_id,
                "stop_id": excluded.stop_id,
                "latitude": excluded.latitude,
                "longitude": excluded.longitude,
                "accuracy_m": excluded.accuracy_m,
                "captured_at": excluded.captured_at,
                "received_at": excluded.received_at,
                "updated_at": excluded.updated_at,
            },
            where=YatraLatestPosition.captured_at <= excluded.captured_at,
        )
        self.db.execute(stmt)

    def _operate(self, actor: User, yatra_id: UUID, *, open_required: bool = True) -> tuple[Yatra, str]:
        return self._locked(actor, yatra_id, OPERATE_ACCESS, open_required=open_required)

    def _manage(self, actor: User, yatra_id: UUID, *, open_required: bool = True) -> tuple[Yatra, str]:
        return self._locked(actor, yatra_id, MANAGE_ACCESS, open_required=open_required)

    def _locked(
        self,
        actor: User,
        yatra_id: UUID,
        allowed: frozenset[str],
        *,
        open_required: bool,
    ) -> tuple[Yatra, str]:
        yatra = self.yatras.get_for_update(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        if level not in allowed:
            raise ForbiddenError()
        if open_required and yatra.status == "archived":
            raise InvalidStateError("This Yatra is archived.")
        return yatra, level

    def _view(self, actor: User, yatra_id: UUID, allowed: frozenset[str]) -> tuple[Yatra, str]:
        yatra = self.yatras.get(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        if level not in allowed:
            raise ForbiddenError()
        return yatra, level

    def _visible(self, actor: User, yatra_id: UUID) -> tuple[Yatra, str]:
        yatra = self.yatras.get(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        return yatra, level

    def _counter(self, yatra_id: UUID, counter_id: UUID) -> YatraCounter:
        counter = self.counters.get(yatra_id, counter_id)
        if counter is None:
            raise NotFoundError("That counter was not found.")
        return counter

    def _stop_on_yatra(self, yatra_id: UUID, stop_id: UUID | None) -> None:
        if stop_id is None:
            return
        if self.positions.stop_on_yatra(yatra_id, stop_id) is None:
            raise InvalidStateError("That stop is not on this Yatra.")

    def _commit_code(self) -> None:
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("A counter with this code already exists.") from None
            raise

    @staticmethod
    def _count_scan_failure(actor: User, limiter: RateLimiter, settings: Settings) -> None:
        enforce_rate_limit(
            limiter,
            f"counter-scan:{actor.id}",
            settings.counter_scan_rate_limit,
            settings.counter_scan_rate_window_seconds,
            retry_after=settings.counter_scan_rate_window_seconds,
        )

    @staticmethod
    def _count_position_auth_failure(client_ip: str, limiter: RateLimiter, settings: Settings) -> None:
        enforce_rate_limit(
            limiter,
            f"position-ip:{client_ip}",
            settings.position_auth_ip_rate_limit,
            settings.position_rate_window_seconds,
            retry_after=settings.position_rate_window_seconds,
        )

    def _attendance_page(self, rows: list[tuple], total: int, limit: int, offset: int) -> AttendanceList:
        items = [
            self._attendance(visit, counter_name, user_login=login_id, user_name=full_name, already_recorded=False)
            for visit, counter_name, login_id, full_name in rows
        ]
        return AttendanceList(
            items=items,
            total=total,
            limit=limit,
            offset=offset,
            has_more=offset + len(items) < total,
        )

    @staticmethod
    def _summary(counter: YatraCounter) -> CounterSummary:
        return CounterSummary(
            id=counter.id,
            yatra_id=counter.yatra_id,
            stop_id=counter.stop_id,
            name=counter.name,
            code=counter.code,
            description=counter.description,
            latitude=None if counter.latitude is None else float(counter.latitude),
            longitude=None if counter.longitude is None else float(counter.longitude),
            location_accuracy_m=None if counter.location_accuracy_m is None else float(counter.location_accuracy_m),
            geofence_radius_m=counter.geofence_radius_m,
            status=counter.status,
            created_at=counter.created_at,
            updated_at=counter.updated_at,
        )

    def _detail(self, counter: YatraCounter) -> CounterDetail:
        return CounterDetail(**self._summary(counter).model_dump(), qr_text=qr_text(counter.qr_token))

    @staticmethod
    def _attendance(
        visit: YatraAttendance,
        counter_name: str,
        actor: User | None = None,
        *,
        user_login: str | None = None,
        user_name: str | None = None,
        already_recorded: bool,
    ) -> AttendanceRecord:
        return AttendanceRecord(
            id=visit.id,
            yatra_id=visit.yatra_id,
            counter_id=visit.counter_id,
            counter_name=counter_name,
            checked_in_at=visit.checked_in_at,
            attendance_date=visit.attendance_date,
            source="qr",
            already_recorded=already_recorded,
            login_id=user_login if user_login is not None else actor.login_id,  # type: ignore[union-attr]
            full_name=user_name if user_name is not None else actor.full_name,  # type: ignore[union-attr]
            scan_distance_m=None if visit.scan_distance_m is None else float(visit.scan_distance_m),
        )

    @staticmethod
    def _tracker(tracker: YatraTracker) -> TrackerSummary:
        return TrackerSummary(
            id=tracker.id,
            label=tracker.label,
            status=tracker.status,
            assigned_at=tracker.assigned_at,
            revoked_at=tracker.revoked_at,
        )

    @staticmethod
    def _position(position: YatraPosition) -> PositionResponse:
        return PositionResponse(
            id=position.id,
            yatra_id=position.yatra_id,
            tracker_id=position.tracker_id,
            stop_id=position.stop_id,
            latitude=float(position.latitude),
            longitude=float(position.longitude),
            accuracy_m=float(position.accuracy_m),
            captured_at=position.captured_at,
            received_at=position.received_at,
            client_position_id=position.client_position_id,
        )

    def _scan_fix(
        self,
        actor: User,
        counter: YatraCounter,
        payload: AttendanceScan,
        limiter: RateLimiter,
        settings: Settings,
    ) -> tuple[float, float, float, float] | None:
        if settings.counter_geofence_mode != "required":
            if payload.latitude is None:
                return None
        assert payload.latitude is not None
        assert payload.longitude is not None
        assert payload.accuracy_m is not None
        assert payload.captured_at is not None
        if payload.accuracy_m > settings.counter_scan_max_accuracy_m:
            self._count_scan_failure(actor, limiter, settings)
            raise InvalidStateError("Your location is not accurate enough.")
        age = abs((datetime.now(UTC) - payload.captured_at).total_seconds())
        if age > settings.counter_scan_location_max_age_seconds:
            self._count_scan_failure(actor, limiter, settings)
            raise InvalidStateError("Your location is out of date.")
        if counter.latitude is None or counter.longitude is None:
            self._count_scan_failure(actor, limiter, settings)
            raise InvalidStateError("You are not at this counter.")
        radius = (
            float(counter.geofence_radius_m)
            if counter.geofence_radius_m is not None
            else float(settings.counter_geofence_radius_m)
        )
        distance = _haversine_m(
            float(counter.latitude),
            float(counter.longitude),
            payload.latitude,
            payload.longitude,
        )
        if distance > radius + payload.accuracy_m:
            self._count_scan_failure(actor, limiter, settings)
            raise InvalidStateError("You are not at this counter.")
        return payload.latitude, payload.longitude, payload.accuracy_m, distance

    def _position_rejection(
        self,
        yatra_id: UUID,
        payload: PositionCreate,
        settings: Settings,
        now: datetime,
    ) -> tuple[int, str] | None:
        if payload.accuracy_m > settings.position_max_accuracy_m:
            return 422, "Accuracy is too large."
        if payload.captured_at > now + timedelta(seconds=settings.position_max_future_seconds):
            return 400, "That position is in the future."
        if payload.captured_at < now - timedelta(seconds=settings.position_max_age_seconds):
            return 400, "That position is too old."
        if payload.stop_id is not None and self.positions.stop_on_yatra(yatra_id, payload.stop_id) is None:
            return 400, "That stop is not on this Yatra."
        return None

    @staticmethod
    def _count_invalid_position(tracker: YatraTracker, limiter: RateLimiter, settings: Settings) -> None:
        enforce_rate_limit(
            limiter,
            f"position-invalid:{tracker.id}",
            settings.position_invalid_rate_limit,
            settings.position_invalid_rate_window_seconds,
            retry_after=settings.position_invalid_rate_window_seconds,
        )

    @staticmethod
    def _location_ok(counter: YatraCounter) -> None:
        coords = (counter.latitude is None, counter.longitude is None, counter.location_accuracy_m is None)
        if len(set(coords)) != 1:
            raise InvalidStateError("Latitude, longitude, and accuracy are set together.")


def encode_cursor(captured_at: datetime, position_id: UUID) -> str:
    stamp = captured_at.astimezone(UTC).isoformat().replace("+00:00", "Z")
    raw = f"{stamp}|{position_id}".encode("utf-8")
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def decode_cursor(value: str) -> tuple[datetime, UUID]:
    padded = value + "=" * (-len(value) % 4)
    try:
        decoded = base64.urlsafe_b64decode(padded.encode("ascii")).decode("utf-8")
        stamp, raw_id = decoded.split("|", 1)
        captured_at = datetime.fromisoformat(stamp.replace("Z", "+00:00"))
        if captured_at.tzinfo is None or captured_at.tzinfo.utcoffset(captured_at) is None:
            raise InvalidCursorError()
        return captured_at, UUID(raw_id)
    except (InvalidCursorError, ValueError, binascii.Error, UnicodeDecodeError):
        raise InvalidCursorError() from None


def _freshness(captured_at: datetime, tracker_status: str, settings: Settings, now: datetime) -> str:
    if tracker_status != "active":
        return "offline"
    age = (now - captured_at).total_seconds()
    if age <= settings.position_stale_after_seconds:
        return "live"
    if age <= settings.position_offline_after_seconds:
        return "stale"
    return "offline"


def _haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lon2 - lon1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return 2 * _EARTH_RADIUS_M * math.asin(min(1.0, math.sqrt(a)))


def _coord(value: float | None) -> Decimal | None:
    if value is None:
        return None
    return Decimal(f"{value:.6f}")


def _accuracy(value: float | None) -> Decimal | None:
    if value is None:
        return None
    return Decimal(f"{value:.1f}")


def _metres(value: float | None) -> Decimal | None:
    if value is None:
        return None
    return Decimal(f"{value:.2f}")
