from fastapi import APIRouter, Depends

from app.core.deps import get_current_user, get_place_service
from app.models.user import User
from app.schemas.place import BoothCodeList
from app.services.places import PlaceService

router = APIRouter(prefix="/places", tags=["places"])


@router.get("", response_model=BoothCodeList)
def list_booth_codes(
    _user: User = Depends(get_current_user),
    service: PlaceService = Depends(get_place_service),
) -> BoothCodeList:
    return service.list_active_codes()
