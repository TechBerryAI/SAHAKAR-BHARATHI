from __future__ import annotations

import secrets
from decimal import Decimal
from uuid import UUID

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import ConflictError, ForbiddenError, InvalidStateError, NotFoundError
from app.core.roles import ROLE_ADMIN
from app.core.yatra_rules import (
    MANAGE_ACCESS,
    OPERATE_ACCESS,
    PUBLISHABLE_STATUSES,
    YATRA_STATUSES,
    YATRA_TRANSITIONS,
)
from app.db.session import is_unique_violation
from app.models.protocol import ProtocolItem
from app.models.user import User
from app.models.yatra import Yatra, YatraAdministrator, YatraRoute, YatraStop
from app.repositories.audit import AuditRepository
from app.repositories.protocol import ProtocolRepository
from app.repositories.users import UserRepository
from app.repositories.yatras import DistrictRepository, YatraRepository
from app.schemas.yatra import (
    AdministratorBrief,
    AssignmentRequest,
    DistrictBrief,
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
    YatraSummary,
    YatraUpdate,
)
from app.services.yatra_access import YatraAccess, capabilities_for, next_status_for


class YatraService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.yatras = YatraRepository(db)
        self.districts = DistrictRepository(db)
        self.protocol = ProtocolRepository(db)
        self.users = UserRepository(db)
        self.audit = AuditRepository(db)
        self.access = YatraAccess(db)

    def create(self, actor: User, payload: YatraCreate) -> YatraDetail:
        district = self.districts.get(payload.district_id)
        if district is None:
            raise NotFoundError("That district was not found.")
        if not district.active:
            raise InvalidStateError("That district is not active.")
        scope = self.access.scope(actor)
        allowed = scope.unrestricted or (actor.role == ROLE_ADMIN and district.id in scope.district_ids)
        if not allowed:
            raise ForbiddenError()
        yatra = self._insert_yatra(actor, payload)
        for template in self.protocol.active_templates():
            self.db.add(
                ProtocolItem(
                    yatra_id=yatra.id,
                    template_id=template.id,
                    category=template.category,
                    title=template.title,
                    description=template.description,
                    required=template.required,
                    status="pending",
                )
            )
        self.db.add(YatraRoute(yatra_id=yatra.id))
        self.audit.add(
            "yatra_created",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "district_id": str(district.id), "public_code": yatra.public_code},
        )
        self.db.commit()
        stored = self.yatras.get(yatra.id)
        if stored is None:
            raise NotFoundError("That Yatra was not found.")
        return self._detail(actor, stored, "super_admin" if scope.unrestricted else "district_admin")

    def list_yatras(
        self,
        actor: User,
        limit: int,
        offset: int,
        *,
        district_id: UUID | None,
        status: str | None,
        query: str | None,
    ) -> YatraList:
        if status is not None and status not in YATRA_STATUSES:
            raise InvalidStateError("That status is not valid.")
        rows, total = self.yatras.page(
            limit,
            offset,
            visible=self.access.visible_clause(actor),
            district_id=district_id,
            status=status,
            query=query,
        )
        return YatraList(
            items=[self._summary(row) for row in rows],
            total=total,
            limit=limit,
            offset=offset,
        )

    def get(self, actor: User, yatra_id: UUID) -> YatraDetail:
        yatra, level = self._visible(actor, yatra_id)
        return self._detail(actor, yatra, level)

    def update(self, actor: User, yatra_id: UUID, payload: YatraUpdate) -> YatraDetail:
        yatra, level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        if payload.name is not None:
            yatra.name = payload.name
        if payload.description is not None:
            yatra.description = payload.description
        starts_at = payload.starts_at or yatra.starts_at
        ends_at = payload.ends_at or yatra.ends_at
        if ends_at < starts_at:
            raise InvalidStateError("The end must be after the start.")
        yatra.starts_at = starts_at
        yatra.ends_at = ends_at
        self.audit.add(
            "yatra_updated",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id)},
        )
        self.db.commit()
        return self.get(actor, yatra.id)

    def change_status(self, actor: User, yatra_id: UUID, payload: YatraStatusChange) -> YatraDetail:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        target = payload.status
        expected = YATRA_TRANSITIONS.get(yatra.status)
        if target not in YATRA_STATUSES or target != expected:
            raise InvalidStateError("That status change is not allowed.")
        if target == "approved" and self.protocol.required_open(yatra.id):
            raise InvalidStateError("Required protocol items are still open.")
        previous = yatra.status
        yatra.status = target
        self.audit.add(
            "yatra_status_changed",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "from": previous, "to": target},
        )
        self.db.commit()
        return self.get(actor, yatra.id)

    def publish(self, actor: User, yatra_id: UUID) -> YatraDetail:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        if yatra.status not in PUBLISHABLE_STATUSES:
            raise InvalidStateError("This Yatra cannot be published yet.")
        if not yatra.published:
            yatra.published = True
            self.audit.add(
                "yatra_published",
                user_id=actor.id,
                extra={"yatra_id": str(yatra.id)},
            )
            self.db.commit()
        return self.get(actor, yatra.id)

    def assign(self, actor: User, yatra_id: UUID, payload: AssignmentRequest) -> AdministratorBrief:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        user = self.users.get_by_id(payload.user_id)
        if user is None:
            raise NotFoundError("That person was not found.")
        if user.status != "active":
            raise InvalidStateError("That person is not active.")
        if self.yatras.is_assigned(yatra.id, user.id):
            raise ConflictError("This assignment already exists.")
        self.db.add(YatraAdministrator(yatra_id=yatra.id, user_id=user.id, assigned_by=actor.id))
        self.audit.add(
            "yatra_administrator_assigned",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "user_id": str(user.id)},
        )
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("This assignment already exists.") from None
            raise
        return AdministratorBrief(user_id=user.id, login_id=user.login_id, full_name=user.full_name)

    def remove_administrator(self, actor: User, yatra_id: UUID, user_id: UUID) -> None:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        row = self.yatras.get_assignment(yatra.id, user_id)
        if row is None:
            raise NotFoundError("That assignment was not found.")
        self.db.delete(row)
        self.audit.add(
            "yatra_administrator_removed",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "user_id": str(user_id)},
        )
        self.db.commit()

    def list_stops(self, actor: User, yatra_id: UUID) -> StopList:
        yatra, _level = self._visible(actor, yatra_id)
        return StopList(items=[self._stop(stop) for stop in self.yatras.stops(yatra.id)])

    def add_stop(self, actor: User, yatra_id: UUID, payload: StopWrite) -> StopResponse:
        yatra, _level = self._locked(actor, yatra_id, OPERATE_ACCESS)
        self._ensure_open(yatra)
        sequence = payload.sequence if payload.sequence is not None else self.yatras.next_sequence(yatra.id)
        stop = YatraStop(
            yatra_id=yatra.id,
            sequence=sequence,
            name=payload.name,
            address=payload.address,
            latitude=_decimal(payload.latitude),
            longitude=_decimal(payload.longitude),
            planned_arrival=payload.planned_arrival,
            planned_departure=payload.planned_departure,
            active=payload.active,
        )
        self.db.add(stop)
        self.audit.add(
            "stop_created",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "stop_id": str(stop.id), "sequence": sequence},
        )
        self._commit_sequence()
        self.db.refresh(stop)
        return self._stop(stop)

    def update_stop(self, actor: User, yatra_id: UUID, stop_id: UUID, payload: StopUpdate) -> StopResponse:
        yatra, _level = self._locked(actor, yatra_id, OPERATE_ACCESS)
        self._ensure_open(yatra)
        stop = self.yatras.get_stop(yatra.id, stop_id)
        if stop is None:
            raise NotFoundError("That stop was not found.")
        if payload.sequence is not None:
            stop.sequence = payload.sequence
        if payload.name is not None:
            stop.name = payload.name
        if payload.clear_address:
            stop.address = None
        elif payload.address is not None:
            stop.address = payload.address
        if payload.clear_coordinates:
            stop.latitude = None
            stop.longitude = None
        else:
            if payload.latitude is not None:
                stop.latitude = _decimal(payload.latitude)
            if payload.longitude is not None:
                stop.longitude = _decimal(payload.longitude)
        if (stop.latitude is None) != (stop.longitude is None):
            raise InvalidStateError("Latitude and longitude are set together.")
        if payload.clear_times:
            stop.planned_arrival = None
            stop.planned_departure = None
        else:
            if payload.planned_arrival is not None:
                stop.planned_arrival = payload.planned_arrival
            if payload.planned_departure is not None:
                stop.planned_departure = payload.planned_departure
        if (
            stop.planned_arrival is not None
            and stop.planned_departure is not None
            and stop.planned_departure < stop.planned_arrival
        ):
            raise InvalidStateError("Departure must be after arrival.")
        if payload.active is not None:
            stop.active = payload.active
        self.audit.add(
            "stop_modified",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "stop_id": str(stop.id), "sequence": stop.sequence},
        )
        self._commit_sequence()
        self.db.refresh(stop)
        return self._stop(stop)

    def delete_stop(self, actor: User, yatra_id: UUID, stop_id: UUID) -> None:
        yatra, _level = self._locked(actor, yatra_id, OPERATE_ACCESS)
        self._ensure_open(yatra)
        stop = self.yatras.get_stop(yatra.id, stop_id)
        if stop is None:
            raise NotFoundError("That stop was not found.")
        self.audit.add(
            "stop_removed",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "stop_id": str(stop.id), "sequence": stop.sequence},
        )
        self.db.delete(stop)
        self.db.commit()

    def get_route(self, actor: User, yatra_id: UUID) -> RouteResponse:
        yatra, level = self._visible(actor, yatra_id)
        if level not in OPERATE_ACCESS and level not in MANAGE_ACCESS:
            raise ForbiddenError()
        route = self._route(yatra.id)
        return RouteResponse(label=route.label, geometry=route.geometry, updated_at=route.updated_at)

    def update_route(self, actor: User, yatra_id: UUID, payload: RouteUpdate) -> RouteResponse:
        yatra, _level = self._locked(actor, yatra_id, OPERATE_ACCESS)
        self._ensure_open(yatra)
        route = self._route(yatra.id)
        route.label = payload.label
        route.geometry = payload.geometry
        self.audit.add(
            "route_updated",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id)},
        )
        self.db.commit()
        self.db.refresh(route)
        return RouteResponse(label=route.label, geometry=route.geometry, updated_at=route.updated_at)

    def _insert_yatra(self, actor: User, payload: YatraCreate) -> Yatra:
        for _ in range(8):
            yatra = Yatra(
                public_code=f"YT{secrets.randbelow(100_000_000):08d}",
                name=payload.name,
                description=payload.description,
                district_id=payload.district_id,
                starts_at=payload.starts_at,
                ends_at=payload.ends_at,
                status="draft",
                published=False,
                created_by=actor.id,
            )
            self.db.add(yatra)
            try:
                self.db.flush()
            except IntegrityError as exc:
                self.db.rollback()
                if is_unique_violation(exc):
                    continue
                raise
            return yatra
        raise InvalidStateError("Could not assign a Yatra code. Try again.")

    def _visible(self, actor: User, yatra_id: UUID) -> tuple[Yatra, str]:
        yatra = self.yatras.get(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        return yatra, level

    def _locked(self, actor: User, yatra_id: UUID, allowed: frozenset[str]) -> tuple[Yatra, str]:
        yatra = self.yatras.get_for_update(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        if level not in allowed:
            raise ForbiddenError()
        return yatra, level

    def _route(self, yatra_id: UUID) -> YatraRoute:
        route = self.yatras.route(yatra_id)
        if route is None:
            raise NotFoundError("That route was not found.")
        return route

    def _detail(self, actor: User, yatra: Yatra, level: str) -> YatraDetail:
        summary = self._summary(yatra)
        people = [] if level == "public" else self.yatras.administrators(yatra.id)
        return YatraDetail(
            **summary.model_dump(),
            description=yatra.description,
            next_status=next_status_for(level, yatra),
            capabilities=capabilities_for(level, yatra),
            administrators=[
                AdministratorBrief(user_id=person.id, login_id=person.login_id, full_name=person.full_name)
                for person in people
            ],
            stops=[self._stop(stop) for stop in self.yatras.stops(yatra.id)],
            created_at=yatra.created_at,
            updated_at=yatra.updated_at,
        )

    def _commit_sequence(self) -> None:
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("A stop with this order already exists.") from None
            raise

    @staticmethod
    def _ensure_open(yatra: Yatra) -> None:
        if yatra.status == "archived":
            raise InvalidStateError("This Yatra is archived.")

    @staticmethod
    def _summary(yatra: Yatra) -> YatraSummary:
        district = yatra.district
        return YatraSummary(
            id=yatra.id,
            public_code=yatra.public_code,
            name=yatra.name,
            district=DistrictBrief(id=district.id, name=district.name, code=district.code),
            starts_at=yatra.starts_at,
            ends_at=yatra.ends_at,
            status=yatra.status,
            published=yatra.published,
        )

    @staticmethod
    def _stop(stop: YatraStop) -> StopResponse:
        return StopResponse(
            id=stop.id,
            sequence=stop.sequence,
            name=stop.name,
            address=stop.address,
            latitude=None if stop.latitude is None else float(stop.latitude),
            longitude=None if stop.longitude is None else float(stop.longitude),
            planned_arrival=stop.planned_arrival,
            planned_departure=stop.planned_departure,
            active=stop.active,
        )


def _decimal(value: float | None) -> Decimal | None:
    if value is None:
        return None
    return Decimal(f"{value:.6f}")
