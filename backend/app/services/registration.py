from __future__ import annotations

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import DuplicateAccountError
from app.core.security import generate_login_id, hash_identifier, hash_pin
from app.models.registration import Registration
from app.models.user import User
from app.repositories.audit import AuditRepository
from app.repositories.registrations import RegistrationRepository
from app.repositories.users import UserRepository
from app.schemas.registration import RegistrationCreate, RegistrationResponse


class RegistrationService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.users = UserRepository(db)
        self.registrations = RegistrationRepository(db)
        self.audit = AuditRepository(db)

    def register(self, payload: RegistrationCreate) -> RegistrationResponse:
        if self.users.get_by_mobile(payload.mobile):
            raise DuplicateAccountError()
        if payload.email and self.users.get_by_email(payload.email):
            raise DuplicateAccountError()

        login_id = generate_login_id()
        while self.users.get_by_login_id(login_id):
            login_id = generate_login_id()

        user = User(
            login_id=login_id,
            mobile=payload.mobile,
            email=payload.email,
            pin_hash=hash_pin(payload.pin),
            full_name=payload.full_name,
            gender=payload.gender,
            date_of_birth=payload.date_of_birth,
            occupation=payload.occupation,
            address_line=payload.address_line,
            locality=payload.locality,
            district=payload.district,
            state=payload.state,
            pincode=payload.pincode,
            status="active",
        )
        self.users.add(user)
        self.db.flush()
        registration = Registration(user_id=user.id, registration_id=login_id)
        self.registrations.add(registration)
        self.audit.add(
            "registration",
            user_id=user.id,
            identifier_hash=hash_identifier(payload.mobile),
        )
        try:
            self.db.commit()
        except IntegrityError:
            self.db.rollback()
            raise DuplicateAccountError() from None
        self.db.refresh(user)
        return RegistrationResponse(
            registration_id=login_id,
            login_id=login_id,
            full_name=user.full_name,
            mobile=user.mobile,
            occupation=user.occupation,
            state=user.state,
            district=user.district,
        )
