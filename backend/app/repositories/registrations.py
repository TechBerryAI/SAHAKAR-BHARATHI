from sqlalchemy.orm import Session

from app.models.registration import Registration


class RegistrationRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def add(self, registration: Registration) -> Registration:
        self.db.add(registration)
        return registration
