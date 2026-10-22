from __future__ import annotations

import uuid

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.core.security import normalize_mobile
from app.models.user import User


class UserRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_id(self, user_id: uuid.UUID) -> User | None:
        return self.db.get(User, user_id)

    def get_by_mobile(self, mobile: str) -> User | None:
        return self.db.scalar(select(User).where(User.mobile == mobile))

    def exists_with_mobile_or_email(self, mobile: str, email: str | None) -> bool:
        filters = [User.mobile == mobile]
        if email:
            filters.append(User.email == email)
        return self.db.scalar(select(User.id).where(or_(*filters)).limit(1)) is not None

    def get_by_login_id(self, login_id: str) -> User | None:
        return self.db.scalar(select(User).where(User.login_id == login_id.upper()))

    def find_for_login(self, identifier: str) -> User | None:
        mobile = normalize_mobile(identifier)
        login_key = identifier.replace(" ", "").upper()
        filters = [User.login_id == login_key]
        if mobile:
            filters.append(User.mobile == mobile)
        return self.db.scalar(select(User).where(or_(*filters)))

    def add(self, user: User) -> User:
        self.db.add(user)
        return user
