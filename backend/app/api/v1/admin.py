import uuid

from fastapi import APIRouter, Depends, Query

from app.core.deps import get_admin_service, require_staff, require_super_admin
from app.models.user import User
from app.schemas.admin import AdminUserDetail, AdminUserList, RoleUpdate, StatusUpdate
from app.services.admin import AdminService

router = APIRouter(prefix="/admin", tags=["admin"])


@router.get("/users", response_model=AdminUserList)
def list_users(
    q: str | None = Query(default=None, max_length=120),
    limit: int = Query(default=20, ge=1, le=50),
    offset: int = Query(default=0, ge=0),
    _staff: User = Depends(require_staff),
    service: AdminService = Depends(get_admin_service),
) -> AdminUserList:
    return service.list_users(q, limit, offset)


@router.get("/users/{user_id}", response_model=AdminUserDetail)
def read_user(
    user_id: uuid.UUID,
    _staff: User = Depends(require_staff),
    service: AdminService = Depends(get_admin_service),
) -> AdminUserDetail:
    return service.get_user(user_id)


@router.post("/users/{user_id}/role", response_model=AdminUserDetail)
def set_user_role(
    user_id: uuid.UUID,
    payload: RoleUpdate,
    actor: User = Depends(require_super_admin),
    service: AdminService = Depends(get_admin_service),
) -> AdminUserDetail:
    return service.set_role(actor, user_id, payload)


@router.post("/users/{user_id}/status", response_model=AdminUserDetail)
def set_user_status(
    user_id: uuid.UUID,
    payload: StatusUpdate,
    actor: User = Depends(require_super_admin),
    service: AdminService = Depends(get_admin_service),
) -> AdminUserDetail:
    return service.set_status(actor, user_id, payload)
