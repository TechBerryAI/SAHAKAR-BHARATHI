from functools import lru_cache
from pathlib import Path
from urllib.parse import quote_plus

from pydantic_settings import BaseSettings, SettingsConfigDict

_BACKEND_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=_BACKEND_ROOT / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    environment: str = "development"
    app_name: str = "Sahakar Bharati API"
    app_host: str = "127.0.0.1"
    app_port: int = 8000

    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_user: str = "sahakar"
    postgres_password: str = ""
    postgres_db: str = "sahakar"
    postgres_test_db: str = "sahakar_test"

    database_url: str = ""
    test_database_url: str = ""

    session_ttl_seconds: int = 7 * 24 * 60 * 60
    login_rate_limit: int = 5
    login_rate_window_seconds: int = 15 * 60
    register_rate_limit: int = 10
    register_rate_window_seconds: int = 60 * 60
    login_lockout_failures: int = 5
    login_lockout_seconds: int = 15 * 60
    max_request_bytes: int = 65_536
    cors_origins: str = ""

    def sqlalchemy_url(self, database: str | None = None) -> str:
        if database is None and self.database_url.strip():
            return self.database_url.strip()
        return self._build_postgres_url(database or self.postgres_db)

    def sqlalchemy_test_url(self) -> str:
        if self.test_database_url.strip():
            return self.test_database_url.strip()
        return self._build_postgres_url(self.postgres_test_db)

    def _build_postgres_url(self, database: str) -> str:
        user = quote_plus(self.postgres_user)
        password = quote_plus(self.postgres_password)
        auth = f"{user}:{password}" if self.postgres_password else user
        return (
            f"postgresql+psycopg://{auth}@{self.postgres_host}:{self.postgres_port}/{database}"
        )

    @property
    def cors_origin_list(self) -> list[str]:
        return [item.strip() for item in self.cors_origins.split(",") if item.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
