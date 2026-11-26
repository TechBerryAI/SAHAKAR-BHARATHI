import uuid
from datetime import date, datetime
from typing import Literal

from fastapi import APIRouter, Depends, Query, Request, Response
from fastapi.security import HTTPAuthorizationCredentials

from app.core.config import Settings, get_settings
from app.core.deps import bearer_scheme, get_current_user, get_limiter, get_yatra_field_service
from app.core.rate_limit import RateLimiter
from app.models.user import User
from app.schemas.yatra_field import (
    AttendanceList,
    AttendanceRecord,
    AttendanceScan,
    CounterCreate,
    CounterDetail,
    CounterList,
    CounterUpdate,
    LatestPositionResponse,
    PositionBatchCreate,
    PositionBatchResponse,
    PositionCreate,
    PositionList,
    PositionResponse,
    TrackerCreate,
    TrackerCreated,
    TrackerList,
    TrackerSummary,
)
from app.services.yatra_field import YatraFieldService

router = APIRouter(prefix="/yatras/{yatra_id}", tags=["yatra-field"])

CounterStatusQuery = Literal["active", "inactive"]


@router.get("/counters", response_model=CounterList)
def list_counters(
    yatra_id: uuid.UUID,
    limit: int = Query(default=20, ge=1, le=50),
    offset: int = Query(default=0, ge=0),
    status: CounterStatusQuery | None = None,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterList:
    return service.list_counters(actor, yatra_id, limit, offset, status)


@router.post("/counters", response_model=CounterDetail, status_code=201)
def create_counter(
    yatra_id: uuid.UUID,
    payload: CounterCreate,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterDetail:
    return service.create_counter(actor, yatra_id, payload)


@router.get("/counters/{counter_id}", response_model=CounterDetail)
def get_counter(
    yatra_id: uuid.UUID,
    counter_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterDetail:
    return service.get_counter(actor, yatra_id, counter_id)


@router.patch("/counters/{counter_id}", response_model=CounterDetail)
def update_counter(
    yatra_id: uuid.UUID,
    counter_id: uuid.UUID,
    payload: CounterUpdate,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterDetail:
    return service.update_counter(actor, yatra_id, counter_id, payload)


@router.post("/counters/{counter_id}/activate", response_model=CounterDetail)
def activate_counter(
    yatra_id: uuid.UUID,
    counter_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterDetail:
    return service.set_counter_status(actor, yatra_id, counter_id, "active")


@router.post("/counters/{counter_id}/deactivate", response_model=CounterDetail)
def deactivate_counter(
    yatra_id: uuid.UUID,
    counter_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterDetail:
    return service.set_counter_status(actor, yatra_id, counter_id, "inactive")


@router.post("/counters/{counter_id}/rotate-qr", response_model=CounterDetail)
def rotate_counter_qr(
    yatra_id: uuid.UUID,
    counter_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> CounterDetail:
    return service.rotate_counter_qr(actor, yatra_id, counter_id)


@router.post("/attendance", response_model=AttendanceRecord)
def scan_attendance(
    yatra_id: uuid.UUID,
    payload: AttendanceScan,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
    limiter: RateLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> AttendanceRecord:
    return service.scan(actor, yatra_id, payload, limiter, settings)


@router.get("/attendance/mine", response_model=AttendanceList)
def list_my_yatra_attendance(
    yatra_id: uuid.UUID,
    limit: int = Query(default=50, ge=1, le=50),
    offset: int = Query(default=0, ge=0),
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> AttendanceList:
    return service.list_my_attendance(actor, yatra_id, limit, offset)


@router.get("/attendance", response_model=AttendanceList)
def list_yatra_attendance(
    yatra_id: uuid.UUID,
    limit: int = Query(default=50, ge=1, le=50),
    offset: int = Query(default=0, ge=0),
    attendance_date: date | None = None,
    counter_id: uuid.UUID | None = None,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> AttendanceList:
    return service.list_attendance(
        actor,
        yatra_id,
        limit,
        offset,
        attendance_date=attendance_date,
        counter_id=counter_id,
    )


@router.post("/trackers", response_model=TrackerCreated, status_code=201)
def create_tracker(
    yatra_id: uuid.UUID,
    payload: TrackerCreate,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> TrackerCreated:
    return service.create_tracker(actor, yatra_id, payload)


@router.get("/trackers", response_model=TrackerList)
def list_trackers(
    yatra_id: uuid.UUID,
    limit: int = Query(default=50, ge=1, le=50),
    offset: int = Query(default=0, ge=0),
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> TrackerList:
    return service.list_trackers(actor, yatra_id, limit, offset)


@router.post("/trackers/{tracker_id}/revoke", response_model=TrackerSummary)
def revoke_tracker(
    yatra_id: uuid.UUID,
    tracker_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> TrackerSummary:
    return service.revoke_tracker(actor, yatra_id, tracker_id)


@router.post("/positions", response_model=PositionResponse)
def submit_position(
    yatra_id: uuid.UUID,
    payload: PositionCreate,
    request: Request,
    response: Response,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    service: YatraFieldService = Depends(get_yatra_field_service),
    limiter: RateLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> PositionResponse:
    raw = credentials.credentials if credentials is not None and credentials.scheme.lower() == "bearer" else None
    tracker = service.authenticate_tracker(raw, request.state.client_ip, limiter, settings)
    body, created = service.ingest(tracker, yatra_id, payload, limiter, settings)
    response.status_code = 201 if created else 200
    return body


@router.post("/positions/batch", response_model=PositionBatchResponse)
def submit_position_batch(
    yatra_id: uuid.UUID,
    payload: PositionBatchCreate,
    request: Request,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    service: YatraFieldService = Depends(get_yatra_field_service),
    limiter: RateLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> PositionBatchResponse:
    raw = credentials.credentials if credentials is not None and credentials.scheme.lower() == "bearer" else None
    tracker = service.authenticate_tracker(raw, request.state.client_ip, limiter, settings)
    return service.ingest_batch(tracker, yatra_id, payload, limiter, settings)


@router.get("/position", response_model=LatestPositionResponse)
def get_latest_position(
    yatra_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
    settings: Settings = Depends(get_settings),
) -> LatestPositionResponse:
    return service.latest_position(actor, yatra_id, settings)


@router.get("/positions", response_model=PositionList)
def list_positions(
    yatra_id: uuid.UUID,
    limit: int = Query(default=50, ge=1, le=100),
    before: str | None = None,
    captured_from: datetime | None = Query(default=None, alias="from"),
    captured_to: datetime | None = Query(default=None, alias="to"),
    tracker_id: uuid.UUID | None = None,
    actor: User = Depends(get_current_user),
    service: YatraFieldService = Depends(get_yatra_field_service),
) -> PositionList:
    return service.list_positions(
        actor,
        yatra_id,
        limit,
        before=before,
        captured_from=captured_from,
        captured_to=captured_to,
        tracker_id=tracker_id,
    )
