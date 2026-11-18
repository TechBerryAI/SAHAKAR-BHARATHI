from __future__ import annotations

from uuid import UUID

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import ConflictError, ForbiddenError, InvalidStateError, NotFoundError
from app.core.roles import ROLE_ADMIN, ROLE_SUPER_ADMIN
from app.db.session import is_unique_violation
from app.models.district import District, DistrictAdministrator
from app.models.user import User
from app.repositories.audit import AuditRepository
from app.repositories.users import UserRepository
from app.repositories.yatras import DistrictRepository
from app.schemas.yatra import (
    AdministratorBrief,
    AssignmentRequest,
    DistrictCreate,
    DistrictList,
    DistrictResponse,
    DistrictUpdate,
)


class DistrictService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.districts = DistrictRepository(db)
        self.users = UserRepository(db)
        self.audit = AuditRepository(db)

    def create(self, actor: User, payload: DistrictCreate) -> DistrictResponse:
        self._require_super(actor)
        district = District(name=payload.name, code=payload.code, state_name=payload.state_name, active=True)
        self.districts.add(district)
        self.audit.add(
            "district_created",
            user_id=actor.id,
            extra={"district_id": str(district.id), "code": district.code},
        )
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("A district with this code already exists.") from None
            raise
        self.db.refresh(district)
        return self._to_response(district, [])

    def list_districts(self, actor: User, limit: int, offset: int) -> DistrictList:
        if actor.role == ROLE_SUPER_ADMIN:
            rows, total = self.districts.page_all(limit, offset)
        elif actor.role == ROLE_ADMIN:
            rows, total = self.districts.page_for_admin(actor.id, limit, offset)
        else:
            raise ForbiddenError()
        grouped = self.districts.administrators([row.id for row in rows])
        return DistrictList(
            items=[self._to_response(row, grouped.get(row.id, [])) for row in rows],
            total=total,
            limit=limit,
            offset=offset,
        )

    def update(self, actor: User, district_id: UUID, payload: DistrictUpdate) -> DistrictResponse:
        self._require_super(actor)
        district = self._get(district_id)
        if payload.name is not None:
            district.name = payload.name
        if payload.state_name is not None:
            district.state_name = payload.state_name
        if payload.active is not None:
            district.active = payload.active
        self.audit.add(
            "district_updated",
            user_id=actor.id,
            extra={"district_id": str(district.id), "active": district.active},
        )
        self.db.commit()
        self.db.refresh(district)
        people = self.districts.administrators([district.id]).get(district.id, [])
        return self._to_response(district, people)

    def assign(self, actor: User, district_id: UUID, payload: AssignmentRequest) -> DistrictResponse:
        self._require_super(actor)
        district = self._get(district_id)
        user = self._assignable_admin(payload.user_id)
        if self.districts.is_assigned(district.id, user.id):
            raise ConflictError("This assignment already exists.")
        self.db.add(
            DistrictAdministrator(district_id=district.id, user_id=user.id, assigned_by=actor.id)
        )
        self.audit.add(
            "district_administrator_assigned",
            user_id=actor.id,
            extra={"district_id": str(district.id), "user_id": str(user.id)},
        )
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("This assignment already exists.") from None
            raise
        people = self.districts.administrators([district.id]).get(district.id, [])
        return self._to_response(district, people)

    def remove(self, actor: User, district_id: UUID, user_id: UUID) -> None:
        self._require_super(actor)
        self._get(district_id)
        row = self.districts.get_assignment(district_id, user_id)
        if row is None:
            raise NotFoundError("That assignment was not found.")
        self.db.delete(row)
        self.audit.add(
            "district_administrator_removed",
            user_id=actor.id,
            extra={"district_id": str(district_id), "user_id": str(user_id)},
        )
        self.db.commit()

    def _assignable_admin(self, user_id: UUID) -> User:
        user = self.users.get_by_id(user_id)
        if user is None:
            raise NotFoundError("That person was not found.")
        if user.status != "active" or user.role != ROLE_ADMIN:
            raise InvalidStateError("That person is not an active admin.")
        return user

    def _get(self, district_id: UUID) -> District:
        district = self.districts.get(district_id)
        if district is None:
            raise NotFoundError("That district was not found.")
        return district

    @staticmethod
    def _require_super(actor: User) -> None:
        if actor.role != ROLE_SUPER_ADMIN:
            raise ForbiddenError()

    @staticmethod
    def _to_response(district: District, people: list[User]) -> DistrictResponse:
        return DistrictResponse(
            id=district.id,
            name=district.name,
            code=district.code,
            state_name=district.state_name,
            active=district.active,
            administrators=[
                AdministratorBrief(user_id=person.id, login_id=person.login_id, full_name=person.full_name)
                for person in people
            ],
            created_at=district.created_at,
            updated_at=district.updated_at,
        )
