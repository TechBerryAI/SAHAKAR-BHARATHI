from fastapi import APIRouter, Depends

from app.core.deps import get_current_user, get_profile_service
from app.models.user import User
from app.schemas.profile import ProfileResponse, ProfileUpdate
from app.services.profile import ProfileService

router = APIRouter(prefix="/me", tags=["profile"])


@router.get("", response_model=ProfileResponse)
def read_me(
    user: User = Depends(get_current_user),
    service: ProfileService = Depends(get_profile_service),
) -> ProfileResponse:
    return service.to_response(user)


@router.patch("", response_model=ProfileResponse)
def update_me(
    payload: ProfileUpdate,
    user: User = Depends(get_current_user),
    service: ProfileService = Depends(get_profile_service),
) -> ProfileResponse:
    return service.update(user, payload)
