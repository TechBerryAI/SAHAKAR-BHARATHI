from fastapi import APIRouter, Depends

from app.core.deps import get_place_service, require_staff
from app.models.user import User
from app.schemas.place import BoothCodeList
from app.services.places import PlaceService

router = APIRouter(prefix="/places", tags=["places"])


@router.get("", response_model=BoothCodeList)
def list_booth_codes(
    _staff: User = Depends(require_staff),
    service: PlaceService = Depends(get_place_service),
) -> BoothCodeList:
    return service.list_active_codes()
