from __future__ import annotations

import uuid

from sqlalchemy.orm import Session

from app.core.errors import ForbiddenError, NotFoundError
from app.core.roles import ROLE_SUPER_ADMIN
from app.models.user import User
from app.repositories.audit import AuditRepository
from app.repositories.users import UserRepository
from app.schemas.admin import AdminUserDetail, AdminUserList, AdminUserSummary, RoleUpdate, StatusUpdate
from app.services.profile import ProfileService


class AdminService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.users = UserRepository(db)
        self.audit = AuditRepository(db)
        self.profiles = ProfileService(db)

    def list_users(self, query: str | None, limit: int, offset: int) -> AdminUserList:
        rows, total = self.users.search(query, limit, offset)
        return AdminUserList(
            items=[
                AdminUserSummary(
                    id=user.id,
                    login_id=user.login_id,
                    full_name=user.full_name,
                    mobile=user.mobile,
                    district=user.district,
                    state=user.state,
                    role=user.role,
                    status=user.status,
                )
                for user in rows
            ],
            total=total,
            limit=limit,
            offset=offset,
        )

    def get_user(self, user_id: uuid.UUID) -> AdminUserDetail:
        user = self.users.get_by_id(user_id)
        if user is None:
            raise NotFoundError()
        profile = self.profiles.to_response(user)
        return AdminUserDetail(
            id=user.id,
            created_at=user.created_at,
            **profile.model_dump(),
        )

    def set_role(self, actor: User, user_id: uuid.UUID, payload: RoleUpdate) -> AdminUserDetail:
        if actor.role != ROLE_SUPER_ADMIN:
            raise ForbiddenError()
        target = self.users.get_by_id(user_id)
        if target is None:
            raise NotFoundError()
        if target.id == actor.id:
            raise ForbiddenError("You cannot change your own role.")
        if target.role == ROLE_SUPER_ADMIN:
            raise ForbiddenError("A super admin role cannot be changed here.")
        previous = target.role
        if previous != payload.role:
            target.role = payload.role
            self.audit.add(
                "role_change",
                user_id=actor.id,
                extra={
                    "target_user_id": str(target.id),
                    "from_role": previous,
                    "to_role": payload.role,
                },
            )
            self.db.commit()
            self.db.refresh(target)
        return self.get_user(target.id)

    def set_status(self, actor: User, user_id: uuid.UUID, payload: StatusUpdate) -> AdminUserDetail:
        if actor.role != ROLE_SUPER_ADMIN:
            raise ForbiddenError()
        target = self.users.get_by_id(user_id)
        if target is None:
            raise NotFoundError()
        if target.id == actor.id or target.role == ROLE_SUPER_ADMIN:
            raise ForbiddenError("You cannot change this account's sign-in status.")
        previous = target.status
        if previous != payload.status:
            target.status = payload.status
            self.audit.add(
                "status_change",
                user_id=actor.id,
                extra={
                    "target_user_id": str(target.id),
                    "from_status": previous,
                    "to_status": payload.status,
                },
            )
            self.db.commit()
            self.db.refresh(target)
        return self.get_user(target.id)
