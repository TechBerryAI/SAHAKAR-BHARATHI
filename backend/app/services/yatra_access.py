from __future__ import annotations

from dataclasses import dataclass
from uuid import UUID

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.core.roles import ROLE_ADMIN, ROLE_SUPER_ADMIN
from app.core.yatra_rules import MANAGE_ACCESS, OPERATE_ACCESS, PUBLISHABLE_STATUSES, YATRA_TRANSITIONS
from app.models.district import DistrictAdministrator
from app.models.user import User
from app.models.yatra import Yatra, YatraAdministrator
from app.schemas.yatra import YatraCapabilities


@dataclass(frozen=True)
class AccessScope:
    unrestricted: bool
    district_ids: frozenset[UUID]
    yatra_ids: frozenset[UUID]


class YatraAccess:
    def __init__(self, db: Session) -> None:
        self.db = db

    def scope(self, actor: User) -> AccessScope:
        if actor.role == ROLE_SUPER_ADMIN:
            return AccessScope(True, frozenset(), frozenset())
        district_ids: set[UUID] = set()
        if actor.role == ROLE_ADMIN:
            district_ids = set(
                self.db.scalars(
                    select(DistrictAdministrator.district_id).where(DistrictAdministrator.user_id == actor.id)
                )
            )
        yatra_ids = set(
            self.db.scalars(select(YatraAdministrator.yatra_id).where(YatraAdministrator.user_id == actor.id))
        )
        return AccessScope(False, frozenset(district_ids), frozenset(yatra_ids))

    def level(self, actor: User, yatra: Yatra, scope: AccessScope | None = None) -> str | None:
        current = scope if scope is not None else self.scope(actor)
        if current.unrestricted:
            return "super_admin"
        if actor.role == ROLE_ADMIN and yatra.district_id in current.district_ids:
            return "district_admin"
        if yatra.id in current.yatra_ids:
            return "yatra_admin"
        if yatra.published:
            return "public"
        return None

    def visible_clause(self, actor: User):
        if actor.role == ROLE_SUPER_ADMIN:
            return None
        assigned = select(YatraAdministrator.yatra_id).where(YatraAdministrator.user_id == actor.id)
        clauses = [Yatra.published.is_(True), Yatra.id.in_(assigned)]
        if actor.role == ROLE_ADMIN:
            districts = select(DistrictAdministrator.district_id).where(DistrictAdministrator.user_id == actor.id)
            clauses.append(Yatra.district_id.in_(districts))
        return or_(*clauses)


def capabilities_for(level: str, yatra: Yatra) -> YatraCapabilities:
    mutable = yatra.status != "archived"
    manage = level in MANAGE_ACCESS and mutable
    operate = level in OPERATE_ACCESS and mutable
    return YatraCapabilities(
        edit=manage,
        transition=manage and yatra.status in YATRA_TRANSITIONS,
        publish=manage and (not yatra.published) and yatra.status in PUBLISHABLE_STATUSES,
        assign_admin=level in MANAGE_ACCESS and mutable,
        manage_stops=operate,
        submit_protocol=operate,
        review_protocol=manage,
        manage_items=manage,
        manage_counters=operate,
        manage_trackers=manage,
        view_track=True,
    )


def next_status_for(level: str, yatra: Yatra) -> str | None:
    if level not in MANAGE_ACCESS or yatra.status == "archived":
        return None
    return YATRA_TRANSITIONS.get(yatra.status)
