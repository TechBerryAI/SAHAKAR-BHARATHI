from __future__ import annotations

import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.place import Place


class PlaceRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def add(self, place: Place) -> Place:
        self.db.add(place)
        return place

    def get(self, place_id: uuid.UUID) -> Place | None:
        return self.db.get(Place, place_id)

    def get_by_code(self, code: str) -> Place | None:
        return self.db.scalar(select(Place).where(Place.code == code))

    def list_recent(self, limit: int = 100) -> list[Place]:
        rows = self.db.scalars(select(Place).order_by(Place.created_at.desc()).limit(limit))
        return list(rows)

    def list_active(self) -> list[Place]:
        rows = self.db.scalars(
            select(Place).where(Place.active.is_(True)).order_by(Place.name.asc(), Place.created_at.asc())
        )
        return list(rows)
