from fastapi import APIRouter, Depends

from app.core.config import Settings, get_settings
from app.core.deps import get_attendance_service, get_current_user, get_limiter
from app.core.rate_limit import RateLimiter
from app.models.user import User
from app.schemas.attendance import AttendanceList, AttendanceRecord, CheckInCreate
from app.services.attendance import AttendanceService

router = APIRouter(prefix="/attendance", tags=["attendance"])


@router.post("", response_model=AttendanceRecord)
def check_in(
    payload: CheckInCreate,
    user: User = Depends(get_current_user),
    service: AttendanceService = Depends(get_attendance_service),
    limiter: RateLimiter = Depends(get_limiter),
    settings: Settings = Depends(get_settings),
) -> AttendanceRecord:
    return service.check_in(user, payload, limiter, settings)


@router.get("", response_model=AttendanceList)
def list_my_attendance(
    user: User = Depends(get_current_user),
    service: AttendanceService = Depends(get_attendance_service),
) -> AttendanceList:
    return service.list_mine(user)
