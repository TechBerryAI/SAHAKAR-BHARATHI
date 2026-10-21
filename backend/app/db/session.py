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
    orig = exc.orig
    sqlstate = getattr(orig, "sqlstate", None)
    return sqlstate == "23505"
