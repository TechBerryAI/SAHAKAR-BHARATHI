"""Create or update the configured super-admin account.

Identity comes from settings (SUPER_ADMIN_MOBILE, SUPER_ADMIN_NAME,
SUPER_ADMIN_LOGIN_ID). The seed script refuses to run when those are empty.
"""

from __future__ import annotations

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.roles import ROLE_SUPER_ADMIN
from app.core.security import hash_pin, is_valid_pin
from app.models.user import User
from app.repositories.users import UserRepository


class LoginIdTakenError(Exception):
    """The configured login id already belongs to a different mobile number."""


class SuperAdminConfigError(Exception):
    """SUPER_ADMIN_* settings are missing."""


def seed_super_admin(db: Session, pin: str) -> User:
    if not is_valid_pin(pin):
        raise ValueError("PIN must be 4 to 6 digits.")
    settings = get_settings()
    mobile = settings.super_admin_mobile.strip()
    name = settings.super_admin_name.strip()
    login_id = settings.super_admin_login_id.strip()
    if not mobile or not name or not login_id:
        raise SuperAdminConfigError(
            "SUPER_ADMIN_MOBILE, SUPER_ADMIN_NAME, and SUPER_ADMIN_LOGIN_ID are required."
        )
    users = UserRepository(db)
    user = users.get_by_mobile(mobile)
    holder = users.get_by_login_id(login_id)
    pin_hash = hash_pin(pin)
    if user is None:
        if holder is not None:
            raise LoginIdTakenError(f"{login_id} is already used by another account.")
        user = User(
            login_id=login_id,
            mobile=mobile,
            pin_hash=pin_hash,
            full_name=name,
            role=ROLE_SUPER_ADMIN,
            status="active",
        )
        users.add(user)
    else:
        user.full_name = name
        user.pin_hash = pin_hash
        user.role = ROLE_SUPER_ADMIN
        user.status = "active"
        if user.login_id != login_id and (holder is None or holder.id == user.id):
            user.login_id = login_id
    db.commit()
    db.refresh(user)
    return user
