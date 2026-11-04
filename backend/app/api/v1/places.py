import uuid

from fastapi import APIRouter, Depends, Query

from app.core.deps import get_attendance_service, get_place_service, require_staff
from app.models.user import User
from app.schemas.attendance import AdminAttendanceList
from app.schemas.place import PlaceCreate, PlaceList, PlaceResponse, PlaceUpdate
from app.services.attendance import AttendanceService
from app.services.places import PlaceService

router = APIRouter(prefix="/admin/places", tags=["places"])


@router.post("", response_model=PlaceResponse, status_code=201)
def create_place(
    payload: PlaceCreate,
    actor: User = Depends(require_staff),
    service: PlaceService = Depends(get_place_service),
) -> PlaceResponse:
    return service.create(actor, payload)


@router.get("", response_model=PlaceList)
def list_places(
    _staff: User = Depends(require_staff),
    service: PlaceService = Depends(get_place_service),
) -> PlaceList:
    return service.list_places()


@router.patch("/{place_id}", response_model=PlaceResponse)
def update_place(
    place_id: uuid.UUID,
    payload: PlaceUpdate,
    _staff: User = Depends(require_staff),
    service: PlaceService = Depends(get_place_service),
) -> PlaceResponse:
    return service.update(place_id, payload)


@router.get("/{place_id}/attendance", response_model=AdminAttendanceList)
def list_place_attendance(
    place_id: uuid.UUID,
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    _staff: User = Depends(require_staff),
    service: AttendanceService = Depends(get_attendance_service),
) -> AdminAttendanceList:
    return service.list_for_place(place_id, limit, offset)
