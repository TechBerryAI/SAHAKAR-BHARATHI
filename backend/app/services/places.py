from __future__ import annotations

import secrets
import uuid

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import NotFoundError
from app.db.session import is_unique_violation
from app.models.place import Place
from app.models.user import User
from app.repositories.places import PlaceRepository
from app.schemas.place import PlaceCreate, PlaceList, PlaceResponse, PlaceUpdate


class PlaceService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.places = PlaceRepository(db)

    def create(self, actor: User, payload: PlaceCreate) -> PlaceResponse:
        for _ in range(8):
            place = Place(name=payload.name, code=f"{secrets.randbelow(1_000_000):06d}", created_by=actor.id)
            self.places.add(place)
            try:
                self.db.commit()
            except IntegrityError as exc:
                self.db.rollback()
                if is_unique_violation(exc):
                    continue
                raise
            self.db.refresh(place)
            return self.to_response(place)
        raise NotFoundError("Could not assign a booth code. Try again.")

    def list_places(self) -> PlaceList:
        return PlaceList(items=[self.to_response(place) for place in self.places.list_recent()])

    def update(self, place_id: uuid.UUID, payload: PlaceUpdate) -> PlaceResponse:
        place = self.places.get(place_id)
        if place is None:
            raise NotFoundError("That booth was not found.")
        if payload.name is not None:
            place.name = payload.name
        if payload.active is not None:
            place.active = payload.active
        self.db.commit()
        self.db.refresh(place)
        return self.to_response(place)

    def get(self, place_id: uuid.UUID) -> Place:
        place = self.places.get(place_id)
        if place is None:
            raise NotFoundError("That booth was not found.")
        return place

    @staticmethod
    def to_response(place: Place) -> PlaceResponse:
        return PlaceResponse(
            id=place.id,
            name=place.name,
            code=place.code,
            active=place.active,
            created_at=place.created_at,
        )
