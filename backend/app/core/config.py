from functools import lru_cache
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, quote_plus, urlparse

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

_BACKEND_ROOT = Path(__file__).resolve().parents[2]
_LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}
_PRODUCTION_ENVIRONMENTS = {"production", "prod"}
_DEV_CORS_ORIGINS = (
    "http://127.0.0.1:5173",
    "http://localhost:5173",
    "https://sahakarbharati-e3b04.web.app",
    "https://sahakarbharati-e3b04.firebaseapp.com",
)


def normalize_database_url(url: str) -> str:
    cleaned = url.strip()
    if not cleaned:
        return ""
    lower = cleaned.lower()
    if lower.startswith("postgres://"):
        cleaned = "postgresql://" + cleaned[len("postgres://") :]
        lower = cleaned.lower()
    if lower.startswith("postgresql://"):
        cleaned = "postgresql+psycopg://" + cleaned[len("postgresql://") :]
    return cleaned


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

    textbee_api_key: str = ""
    textbee_device_id: str = ""
    textbee_base_url: str = "https://api.textbee.dev/api/v1"
    otp_ttl_seconds: int = 5 * 60
    otp_verification_ttl_seconds: int = 15 * 60
    otp_max_attempts: int = 5
    otp_resend_seconds: int = 60
    otp_send_rate_limit: int = 5
    otp_send_rate_window_seconds: int = 60 * 60
    otp_confirm_rate_limit: int = 10
    otp_confirm_rate_window_seconds: int = 15 * 60

    @property
    def is_production(self) -> bool:
        return self.environment.strip().lower() in _PRODUCTION_ENVIRONMENTS

    @property
    def cors_origin_list(self) -> list[str]:
        origins = [item.strip() for item in self.cors_origins.split(",") if item.strip()]
        if origins:
            return origins
        if not self.is_production:
            return list(_DEV_CORS_ORIGINS)
        return []

    def sqlalchemy_url(self, database: str | None = None) -> str:
        if database is None and self.database_url.strip():
            return normalize_database_url(self.database_url)
        return self._build_postgres_url(database or self.postgres_db)

    def sqlalchemy_test_url(self) -> str:
        if self.test_database_url.strip():
            return normalize_database_url(self.test_database_url)
        return self._build_postgres_url(self.postgres_test_db)

    def _build_postgres_url(self, database: str) -> str:
        user = quote_plus(self.postgres_user)
        password = quote_plus(self.postgres_password)
        auth = f"{user}:{password}" if self.postgres_password else user
        return (
            f"postgresql+psycopg://{auth}@{self.postgres_host}:{self.postgres_port}/{database}"
        )

    def is_transaction_pooler(self, url: str | None = None) -> bool:
        parsed = urlparse(url or self.sqlalchemy_url())
        host = (parsed.hostname or "").lower()
        return parsed.port == 6543 or "pooler.supabase" in host

    def engine_kwargs(self, url: str | None = None) -> dict[str, Any]:
        target = url or self.sqlalchemy_url()
        parsed = urlparse(target)
        query = parse_qs(parsed.query)
        host = (parsed.hostname or "").lower()
        # TCP keepalives stop idle pooled connections being silently dropped,
        # so they can be kept longer instead of reconnecting (TCP + TLS + auth).
        connect_args: dict[str, Any] = {
            "connect_timeout": 10,
            "keepalives": 1,
            "keepalives_idle": 30,
            "keepalives_interval": 10,
            "keepalives_count": 3,
        }

        sslmode = query.get("sslmode", [None])[0]
        if sslmode:
            connect_args["sslmode"] = sslmode
        elif self.is_production or "supabase" in host:
            connect_args["sslmode"] = "require"

        if self.is_transaction_pooler(target):
            connect_args["prepare_threshold"] = 0

        return {
            "pool_pre_ping": True,
            "pool_recycle": 1800,
            "pool_size": 5,
            "max_overflow": 5,
            "pool_timeout": 30,
            "connect_args": connect_args,
        }

    @model_validator(mode="after")
    def validate_production(self) -> "Settings":
        if not self.is_production:
            return self
        if not self.database_url.strip():
            raise ValueError("DATABASE_URL is required when ENVIRONMENT=production")
        parsed = urlparse(normalize_database_url(self.database_url))
        host = (parsed.hostname or "").lower()
        if host in _LOCAL_HOSTS:
            raise ValueError("DATABASE_URL must not target localhost in production")
        if "*" in self.cors_origin_list:
            raise ValueError("CORS_ORIGINS=* is not allowed in production")
        return self


@lru_cache
def get_settings() -> Settings:
    return Settings()
