from __future__ import annotations

import uuid

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.core.security import is_valid_mobile, normalize_mobile
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
        if not is_valid_mobile(mobile):
            return None
        return self.db.scalar(select(User).where(User.mobile == mobile))

    def add(self, user: User) -> User:
        self.db.add(user)
        return user

    def search(self, query: str | None, limit: int, offset: int) -> tuple[list[User], int]:
        filters = []
        cleaned = (query or "").strip()
        if cleaned:
            escaped = cleaned.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            pattern = f"%{escaped}%"
            filters.append(
                or_(
                    User.full_name.ilike(pattern, escape="\\"),
                    User.mobile.ilike(pattern, escape="\\"),
                    User.login_id.ilike(pattern, escape="\\"),
                )
            )
        count_stmt = select(func.count()).select_from(User)
        list_stmt = select(User)
        if filters:
            count_stmt = count_stmt.where(*filters)
            list_stmt = list_stmt.where(*filters)
        total = int(self.db.scalar(count_stmt) or 0)
        rows = list(
            self.db.scalars(
                list_stmt.order_by(User.created_at.desc(), User.id.desc()).limit(limit).offset(offset)
            )
        )
        return rows, total
