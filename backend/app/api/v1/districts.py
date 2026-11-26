import uuid

from fastapi import APIRouter, Depends, Query, Response

from app.core.deps import get_current_user, get_district_service, require_super_admin
from app.models.user import User
from app.schemas.yatra import AssignmentRequest, DistrictCreate, DistrictList, DistrictResponse, DistrictUpdate
from app.services.districts import DistrictService

router = APIRouter(prefix="/admin/districts", tags=["districts"])


@router.post("", response_model=DistrictResponse, status_code=201)
def create_district(
    payload: DistrictCreate,
    actor: User = Depends(require_super_admin),
    service: DistrictService = Depends(get_district_service),
) -> DistrictResponse:
    return service.create(actor, payload)


@router.get("", response_model=DistrictList)
def list_districts(
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    actor: User = Depends(get_current_user),
    service: DistrictService = Depends(get_district_service),
) -> DistrictList:
    return service.list_districts(actor, limit, offset)


@router.patch("/{district_id}", response_model=DistrictResponse)
def update_district(
    district_id: uuid.UUID,
    payload: DistrictUpdate,
    actor: User = Depends(require_super_admin),
    service: DistrictService = Depends(get_district_service),
) -> DistrictResponse:
    return service.update(actor, district_id, payload)


@router.post("/{district_id}/administrators", response_model=DistrictResponse, status_code=201)
def assign_district_administrator(
    district_id: uuid.UUID,
    payload: AssignmentRequest,
    actor: User = Depends(require_super_admin),
    service: DistrictService = Depends(get_district_service),
) -> DistrictResponse:
    return service.assign(actor, district_id, payload)


@router.delete("/{district_id}/administrators/{user_id}", status_code=204)
def remove_district_administrator(
    district_id: uuid.UUID,
    user_id: uuid.UUID,
    actor: User = Depends(require_super_admin),
    service: DistrictService = Depends(get_district_service),
) -> Response:
    service.remove(actor, district_id, user_id)
    return Response(status_code=204)
