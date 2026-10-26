"""Create or update the Jaydeep Patil super-admin account.

The PIN is passed on the command line and stored only as a hash:

    python -m app.scripts.seed_super_admin --pin 1234

Running it again updates the same mobile row instead of inserting a second user.
"""

from __future__ import annotations

import argparse

from sqlalchemy.orm import Session

from app.core.roles import ROLE_SUPER_ADMIN
from app.core.security import hash_pin, is_valid_pin
from app.models.user import User
from app.repositories.users import UserRepository

SUPER_ADMIN_LOGIN_ID = "SB-JAYDEEP"
SUPER_ADMIN_MOBILE = "9137449949"
SUPER_ADMIN_NAME = "Jaydeep Patil"


class LoginIdTakenError(Exception):
    """SB-JAYDEEP already belongs to a different mobile number."""


def seed_super_admin(db: Session, pin: str) -> User:
    if not is_valid_pin(pin):
        raise ValueError("PIN must be 4 to 6 digits.")
    users = UserRepository(db)
    user = users.get_by_mobile(SUPER_ADMIN_MOBILE)
    holder = users.get_by_login_id(SUPER_ADMIN_LOGIN_ID)
    pin_hash = hash_pin(pin)
    if user is None:
        if holder is not None:
            raise LoginIdTakenError(f"{SUPER_ADMIN_LOGIN_ID} is already used by another account.")
        user = User(
            login_id=SUPER_ADMIN_LOGIN_ID,
            mobile=SUPER_ADMIN_MOBILE,
            pin_hash=pin_hash,
            full_name=SUPER_ADMIN_NAME,
            role=ROLE_SUPER_ADMIN,
            status="active",
        )
        users.add(user)
    else:
        user.full_name = SUPER_ADMIN_NAME
        user.pin_hash = pin_hash
        user.role = ROLE_SUPER_ADMIN
        user.status = "active"
        if user.login_id != SUPER_ADMIN_LOGIN_ID and (holder is None or holder.id == user.id):
            user.login_id = SUPER_ADMIN_LOGIN_ID
    db.commit()
    db.refresh(user)
    return user


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="Seed the Jaydeep Patil super-admin account.")
    parser.add_argument("--pin", required=True, help="Sign-in PIN. Stored only as a hash.")
    args = parser.parse_args(argv)
    from app.db.session import SessionLocal

    with SessionLocal() as db:
        user = seed_super_admin(db, args.pin)
    print(f"Super admin ready: {user.login_id} ({user.mobile})")


if __name__ == "__main__":
    main()
