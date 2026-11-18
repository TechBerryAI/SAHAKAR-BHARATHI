import uuid
from typing import Literal

from fastapi import APIRouter, Depends, Query, Response

from app.core.deps import get_current_user, get_yatra_service
from app.models.user import User
from app.schemas.yatra import (
    AdministratorBrief,
    AssignmentRequest,
    RouteResponse,
    RouteUpdate,
    StopList,
    StopResponse,
    StopUpdate,
    StopWrite,
    YatraCreate,
    YatraDetail,
    YatraList,
    YatraStatusChange,
    YatraUpdate,
)
from app.services.yatras import YatraService

router = APIRouter(prefix="/yatras", tags=["yatras"])

YatraStatusQuery = Literal[
    "draft",
    "planned",
    "approval_pending",
    "approved",
    "upcoming",
    "in_progress",
    "completed",
    "archived",
]


@router.get("", response_model=YatraList)
def list_yatras(
    limit: int = Query(default=20, ge=1, le=50),
    offset: int = Query(default=0, ge=0),
    district_id: uuid.UUID | None = None,
    status: YatraStatusQuery | None = None,
    q: str | None = Query(default=None, max_length=160),
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> YatraList:
    return service.list_yatras(actor, limit, offset, district_id=district_id, status=status, query=q)


@router.post("", response_model=YatraDetail, status_code=201)
def create_yatra(
    payload: YatraCreate,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> YatraDetail:
    return service.create(actor, payload)


@router.get("/{yatra_id}", response_model=YatraDetail)
def get_yatra(
    yatra_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> YatraDetail:
    return service.get(actor, yatra_id)


@router.patch("/{yatra_id}", response_model=YatraDetail)
def update_yatra(
    yatra_id: uuid.UUID,
    payload: YatraUpdate,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> YatraDetail:
    return service.update(actor, yatra_id, payload)


@router.post("/{yatra_id}/status", response_model=YatraDetail)
def change_yatra_status(
    yatra_id: uuid.UUID,
    payload: YatraStatusChange,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> YatraDetail:
    return service.change_status(actor, yatra_id, payload)


@router.post("/{yatra_id}/publish", response_model=YatraDetail)
def publish_yatra(
    yatra_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> YatraDetail:
    return service.publish(actor, yatra_id)


@router.post("/{yatra_id}/administrators", response_model=AdministratorBrief, status_code=201)
def assign_yatra_administrator(
    yatra_id: uuid.UUID,
    payload: AssignmentRequest,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> AdministratorBrief:
    return service.assign(actor, yatra_id, payload)


@router.delete("/{yatra_id}/administrators/{user_id}", status_code=204)
def remove_yatra_administrator(
    yatra_id: uuid.UUID,
    user_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> Response:
    service.remove_administrator(actor, yatra_id, user_id)
    return Response(status_code=204)


@router.get("/{yatra_id}/stops", response_model=StopList)
def list_stops(
    yatra_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> StopList:
    return service.list_stops(actor, yatra_id)


@router.post("/{yatra_id}/stops", response_model=StopResponse, status_code=201)
def create_stop(
    yatra_id: uuid.UUID,
    payload: StopWrite,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> StopResponse:
    return service.add_stop(actor, yatra_id, payload)


@router.patch("/{yatra_id}/stops/{stop_id}", response_model=StopResponse)
def update_stop(
    yatra_id: uuid.UUID,
    stop_id: uuid.UUID,
    payload: StopUpdate,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> StopResponse:
    return service.update_stop(actor, yatra_id, stop_id, payload)


@router.delete("/{yatra_id}/stops/{stop_id}", status_code=204)
def delete_stop(
    yatra_id: uuid.UUID,
    stop_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> Response:
    service.delete_stop(actor, yatra_id, stop_id)
    return Response(status_code=204)


@router.get("/{yatra_id}/route", response_model=RouteResponse)
def get_route(
    yatra_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> RouteResponse:
    return service.get_route(actor, yatra_id)


@router.put("/{yatra_id}/route", response_model=RouteResponse)
def update_route(
    yatra_id: uuid.UUID,
    payload: RouteUpdate,
    actor: User = Depends(get_current_user),
    service: YatraService = Depends(get_yatra_service),
) -> RouteResponse:
    return service.update_route(actor, yatra_id, payload)
