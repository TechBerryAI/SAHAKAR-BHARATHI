from __future__ import annotations

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import DuplicateAccountError
from app.db.session import is_unique_violation
from app.models.user import User
from app.repositories.audit import AuditRepository
from app.schemas.profile import ProfileResponse, ProfileUpdate


class ProfileService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.audit = AuditRepository(db)

    def to_response(self, user: User) -> ProfileResponse:
        return ProfileResponse(
            login_id=user.login_id,
            full_name=user.full_name,
            mobile=user.mobile,
            email=user.email,
            gender=user.gender,
            date_of_birth=user.date_of_birth,
            occupation=user.occupation,
            address_line=user.address_line,
            locality=user.locality,
            district=user.district,
            state=user.state,
            pincode=user.pincode,
            status=user.status,
            role=user.role,
        )

    def update(self, user: User, payload: ProfileUpdate) -> ProfileResponse:
        data = payload.model_dump(exclude_unset=True)
        for field, value in data.items():
            setattr(user, field, value)
        self.audit.add("profile_update", user_id=user.id)
        # Every response field is set in Python, so build it before commit instead of reloading the row.
        response = self.to_response(user)
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise DuplicateAccountError() from None
            raise
        return response
