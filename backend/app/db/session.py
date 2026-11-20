from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, sessionmaker

from app.core.config import get_settings

settings = get_settings()
engine = create_engine(settings.sqlalchemy_url(), **settings.engine_kwargs())
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, class_=Session)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def is_unique_violation(exc: IntegrityError) -> bool:
    return _sqlstate(exc) == "23505"


def is_foreign_key_violation(exc: IntegrityError) -> bool:
    return _sqlstate(exc) == "23503"


def constraint_name(exc: IntegrityError) -> str:
    diag = getattr(exc.orig, "diag", None)
    return str(getattr(diag, "constraint_name", "") or "")


def _sqlstate(exc: IntegrityError) -> str | None:
    return getattr(exc.orig, "sqlstate", None)
