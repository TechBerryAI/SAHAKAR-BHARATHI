from __future__ import annotations

import uuid

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session, joinedload

from app.models.district import District, DistrictAdministrator
from app.models.user import User
from app.models.yatra import Yatra, YatraAdministrator, YatraRoute, YatraStop


class DistrictRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def add(self, district: District) -> District:
        self.db.add(district)
        return district

    def get(self, district_id: uuid.UUID) -> District | None:
        return self.db.get(District, district_id)

    def page_all(self, limit: int, offset: int) -> tuple[list[District], int]:
        total = int(self.db.scalar(select(func.count()).select_from(District)) or 0)
        rows = self.db.scalars(
            select(District).order_by(District.name.asc(), District.code.asc()).limit(limit).offset(offset)
        )
        return list(rows), total

    def page_for_admin(self, user_id: uuid.UUID, limit: int, offset: int) -> tuple[list[District], int]:
        joined = (
            select(District)
            .join(DistrictAdministrator, DistrictAdministrator.district_id == District.id)
            .where(DistrictAdministrator.user_id == user_id)
        )
        total = int(self.db.scalar(select(func.count()).select_from(joined.subquery())) or 0)
        rows = self.db.scalars(
            joined.order_by(District.name.asc(), District.code.asc()).limit(limit).offset(offset)
        )
        return list(rows), total

    def is_assigned(self, district_id: uuid.UUID, user_id: uuid.UUID) -> bool:
        found = self.db.scalar(
            select(DistrictAdministrator.id)
            .where(
                DistrictAdministrator.district_id == district_id,
                DistrictAdministrator.user_id == user_id,
            )
            .limit(1)
        )
        return found is not None

    def get_assignment(self, district_id: uuid.UUID, user_id: uuid.UUID) -> DistrictAdministrator | None:
        return self.db.scalar(
            select(DistrictAdministrator).where(
                DistrictAdministrator.district_id == district_id,
                DistrictAdministrator.user_id == user_id,
            )
        )

    def administrators(self, district_ids: list[uuid.UUID]) -> dict[uuid.UUID, list[User]]:
        if not district_ids:
            return {}
        rows = self.db.execute(
            select(DistrictAdministrator.district_id, User)
            .join(User, User.id == DistrictAdministrator.user_id)
            .where(DistrictAdministrator.district_id.in_(district_ids))
            .order_by(User.full_name.asc())
        ).all()
        grouped: dict[uuid.UUID, list[User]] = {district_id: [] for district_id in district_ids}
        for district_id, user in rows:
            grouped.setdefault(district_id, []).append(user)
        return grouped


class YatraRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def add(self, yatra: Yatra) -> Yatra:
        self.db.add(yatra)
        return yatra

    def get(self, yatra_id: uuid.UUID) -> Yatra | None:
        return self.db.scalar(select(Yatra).options(joinedload(Yatra.district)).where(Yatra.id == yatra_id))

    def get_for_update(self, yatra_id: uuid.UUID) -> Yatra | None:
        return self.db.scalar(select(Yatra).where(Yatra.id == yatra_id).with_for_update())

    def page(
        self,
        limit: int,
        offset: int,
        *,
        visible,
        district_id: uuid.UUID | None,
        status: str | None,
        query: str | None,
        published: bool | None = None,
    ) -> tuple[list[Yatra], int]:
        filters = []
        if visible is not None:
            filters.append(visible)
        if district_id is not None:
            filters.append(Yatra.district_id == district_id)
        if status is not None:
            filters.append(Yatra.status == status)
        if published is not None:
            filters.append(Yatra.published.is_(published))
        cleaned = (query or "").strip()
        if cleaned:
            escaped = cleaned.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            pattern = f"%{escaped}%"
            filters.append(
                or_(
                    Yatra.name.ilike(pattern, escape="\\"),
                    Yatra.public_code.ilike(pattern, escape="\\"),
                )
            )
        count = select(func.count()).select_from(Yatra)
        listing = select(Yatra).options(joinedload(Yatra.district))
        if filters:
            count = count.where(*filters)
            listing = listing.where(*filters)
        total = int(self.db.scalar(count) or 0)
        rows = self.db.scalars(
            listing.order_by(Yatra.starts_at.desc(), Yatra.created_at.desc(), Yatra.id.desc())
            .limit(limit)
            .offset(offset)
        ).unique()
        return list(rows), total

    def is_assigned(self, yatra_id: uuid.UUID, user_id: uuid.UUID) -> bool:
        found = self.db.scalar(
            select(YatraAdministrator.id)
            .where(YatraAdministrator.yatra_id == yatra_id, YatraAdministrator.user_id == user_id)
            .limit(1)
        )
        return found is not None

    def get_assignment(self, yatra_id: uuid.UUID, user_id: uuid.UUID) -> YatraAdministrator | None:
        return self.db.scalar(
            select(YatraAdministrator).where(
                YatraAdministrator.yatra_id == yatra_id,
                YatraAdministrator.user_id == user_id,
            )
        )

    def administrators(self, yatra_id: uuid.UUID) -> list[User]:
        rows = self.db.scalars(
            select(User)
            .join(YatraAdministrator, YatraAdministrator.user_id == User.id)
            .where(YatraAdministrator.yatra_id == yatra_id)
            .order_by(User.full_name.asc())
        )
        return list(rows)

    def stops(self, yatra_id: uuid.UUID) -> list[YatraStop]:
        rows = self.db.scalars(
            select(YatraStop).where(YatraStop.yatra_id == yatra_id).order_by(YatraStop.sequence.asc())
        )
        return list(rows)

    def get_stop(self, yatra_id: uuid.UUID, stop_id: uuid.UUID) -> YatraStop | None:
        return self.db.scalar(select(YatraStop).where(YatraStop.id == stop_id, YatraStop.yatra_id == yatra_id))

    def next_sequence(self, yatra_id: uuid.UUID) -> int:
        current = self.db.scalar(select(func.max(YatraStop.sequence)).where(YatraStop.yatra_id == yatra_id))
        return int(current or 0) + 1

    def route(self, yatra_id: uuid.UUID) -> YatraRoute | None:
        return self.db.scalar(select(YatraRoute).where(YatraRoute.yatra_id == yatra_id))
