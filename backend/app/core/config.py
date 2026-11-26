from functools import lru_cache
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, quote_plus, urlparse

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

_BACKEND_ROOT = Path(__file__).resolve().parents[2]
_LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1", "0.0.0.0"}
_PRODUCTION_ENVIRONMENTS = {"production", "prod"}
# Anything else is rejected so a typo such as "prodution" cannot silently fall
# back to development defaults (localhost CORS, OpenAPI docs, optional TLS).
_KNOWN_ENVIRONMENTS = _PRODUCTION_ENVIRONMENTS | {"development", "dev", "test"}
_INSECURE_SSLMODES = {"disable", "allow", "prefer"}
_RATE_LIMIT_BACKENDS = {"memory", "postgres"}
_GEOFENCE_MODES = {"required", "off"}
_DEV_CORS_ORIGINS = (
    "http://127.0.0.1:5173",
    "http://localhost:5173",
    # start.js serves the web app on 5174.
    "http://127.0.0.1:5174",
    "http://localhost:5174",
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
    attendance_code_rate_limit: int = 10
    attendance_code_rate_window_seconds: int = 10 * 60
    # Failed Yatra counter scans, per signed-in user. Success does not count.
    counter_scan_rate_limit: int = 10
    counter_scan_rate_window_seconds: int = 10 * 60
    # New GPS fixes per tracker. An idempotent retry does not count.
    position_rate_limit: int = 12
    position_rate_window_seconds: int = 60
    position_auth_ip_rate_limit: int = 30
    # A rejected fix from an authenticated tracker. Separate from the accept bucket.
    position_invalid_rate_limit: int = 30
    position_invalid_rate_window_seconds: int = 60
    # Offline uploads. One request, its own bucket, not one hit per fix.
    position_batch_max: int = 50
    position_batch_rate_limit: int = 6
    position_batch_rate_window_seconds: int = 60
    # Reject a fix this far ahead of the server clock, or this far behind it.
    # Six hours covers a phone that was out of range. Freshness is separate.
    position_max_future_seconds: int = 30
    position_max_age_seconds: int = 6 * 60 * 60
    # Display thresholds against captured_at. Not stored on the row.
    position_stale_after_seconds: int = 90
    position_offline_after_seconds: int = 10 * 60
    position_max_accuracy_m: int = 100
    position_retention_days: int = 30
    # Attendance must be inside the counter geofence unless an operator turns it off.
    # Production refuses off.
    counter_geofence_mode: str = "required"
    counter_geofence_radius_m: int = 150
    counter_scan_max_accuracy_m: int = 100
    counter_scan_location_max_age_seconds: int = 120
    register_rate_limit: int = 10
    register_rate_window_seconds: int = 60 * 60
    login_lockout_failures: int = 5
    login_lockout_seconds: int = 15 * 60
    max_request_bytes: int = 65_536
    cors_origins: str = ""
    # "memory" is per-process and is rejected in production.
    # "postgres" shares counters through rate_limit_buckets (one extra round-trip
    # per throttled request) and is required before the process may serve production.
    rate_limit_backend: str = "memory"
    # Request SQLAlchemy pool. Size for expected concurrent in-flight DB work.
    # With a remote database (~200–300 ms RTT), each request holds a connection
    # for the whole handler; 20 simultaneous users need roughly that many slots.
    # Cap against the Postgres / Supabase connection budget for the role.
    db_pool_size: int = 5
    db_max_overflow: int = 5
    db_pool_timeout_seconds: int = 30
    # The limiter uses its own pool so a burst of throttled requests cannot
    # exhaust the request pool, and a stuck limiter checkout cannot stall reads.
    # Per instance: (DB_POOL_SIZE + DB_MAX_OVERFLOW) + (RATE_LIMIT_POOL_*).
    rate_limit_pool_size: int = 2
    rate_limit_max_overflow: int = 2
    rate_limit_pool_timeout_seconds: int = 5
    # "peer" uses the TCP connection and ignores forwarding headers.
    # "cf-connecting-ip" is the Render production source: Cloudflare overwrites
    # that header with the visitor address. See app/core/client_ip.py.
    client_ip_source: str = "peer"
    # Independent of the per-identifier login limit. Stops one network from
    # trying many accounts. 30 per 15 minutes leaves room for a shared phone.
    login_ip_rate_limit: int = 30

    textbee_api_key: str = ""
    textbee_device_id: str = ""
    textbee_base_url: str = "https://api.textbee.dev/api/v1"
    otp_ttl_seconds: int = 5 * 60
    otp_verification_ttl_seconds: int = 15 * 60
    otp_max_attempts: int = 5
    otp_resend_seconds: int = 60
    otp_send_rate_limit: int = 5
    otp_send_ip_rate_limit: int = 30
    otp_send_rate_window_seconds: int = 60 * 60
    otp_confirm_rate_limit: int = 30
    otp_confirm_rate_window_seconds: int = 15 * 60

    # Empty in production rejects uploads. "local" is ephemeral disk. "s3" is a private bucket.
    document_storage_backend: str = ""
    document_storage_dir: str = ""
    document_storage_bucket: str = ""
    document_storage_region: str = "ap-south-1"
    document_storage_endpoint: str = ""
    document_storage_access_key: str = ""
    document_storage_secret_key: str = ""
    document_max_bytes: int = 5_242_880

    # Used only by scripts/seed_super_admin.py. Empty here so the source
    # does not name a person. The seed script refuses to run without them.
    super_admin_mobile: str = ""
    super_admin_name: str = ""
    super_admin_login_id: str = ""

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
            "pool_size": self.db_pool_size,
            "max_overflow": self.db_max_overflow,
            "pool_timeout": self.db_pool_timeout_seconds,
            "connect_args": connect_args,
        }

    def rate_limit_engine_kwargs(self, url: str | None = None) -> dict[str, Any]:
        kwargs = self.engine_kwargs(url)
        kwargs["pool_size"] = self.rate_limit_pool_size
        kwargs["max_overflow"] = self.rate_limit_max_overflow
        kwargs["pool_timeout"] = self.rate_limit_pool_timeout_seconds
        return kwargs

    @model_validator(mode="after")
    def validate_production(self) -> "Settings":
        environment = self.environment.strip().lower()
        if environment not in _KNOWN_ENVIRONMENTS:
            raise ValueError(
                f"ENVIRONMENT must be one of {sorted(_KNOWN_ENVIRONMENTS)}; got {self.environment!r}"
            )
        if self.rate_limit_backend.strip().lower() not in _RATE_LIMIT_BACKENDS:
            raise ValueError(f"RATE_LIMIT_BACKEND must be one of {sorted(_RATE_LIMIT_BACKENDS)}")
        mode = self.counter_geofence_mode.strip().lower()
        if mode not in _GEOFENCE_MODES:
            raise ValueError("COUNTER_GEOFENCE_MODE must be required or off")
        self.counter_geofence_mode = mode
        if not 10 <= self.counter_geofence_radius_m <= 5000:
            raise ValueError("COUNTER_GEOFENCE_RADIUS_M must be from 10 to 5000")
        if not 1 <= self.counter_scan_max_accuracy_m <= 10000:
            raise ValueError("COUNTER_SCAN_MAX_ACCURACY_M must be from 1 to 10000")
        if not 1 <= self.position_batch_max <= 100:
            raise ValueError("POSITION_BATCH_MAX must be from 1 to 100")
        if self.position_max_age_seconds < self.position_offline_after_seconds:
            raise ValueError("POSITION_MAX_AGE_SECONDS must be at least POSITION_OFFLINE_AFTER_SECONDS")
        source = self.client_ip_source.strip().lower()
        if source not in {"peer", "cf-connecting-ip"}:
            raise ValueError("CLIENT_IP_SOURCE must be peer or cf-connecting-ip")
        self.client_ip_source = source
        backend = self.document_storage_backend.strip().lower()
        if backend not in {"", "local", "s3"}:
            raise ValueError("DOCUMENT_STORAGE_BACKEND must be empty, local, or s3")
        self.document_storage_backend = backend
        if backend == "s3":
            if not self.document_storage_bucket.strip():
                raise ValueError("DOCUMENT_STORAGE_BUCKET is required when DOCUMENT_STORAGE_BACKEND=s3")
            if not self.document_storage_access_key.strip() or not self.document_storage_secret_key.strip():
                raise ValueError(
                    "DOCUMENT_STORAGE_ACCESS_KEY and DOCUMENT_STORAGE_SECRET_KEY are required "
                    "when DOCUMENT_STORAGE_BACKEND=s3"
                )
            endpoint = self.document_storage_endpoint.strip()
            if endpoint and urlparse(endpoint).scheme != "https" and environment in _PRODUCTION_ENVIRONMENTS:
                raise ValueError("DOCUMENT_STORAGE_ENDPOINT must use https in production")
        if self.document_max_bytes <= 0 or self.document_max_bytes > 5_242_880:
            raise ValueError("DOCUMENT_MAX_BYTES must be from 1 to 5242880")
        if self.position_max_accuracy_m > 10_000:
            raise ValueError("POSITION_MAX_ACCURACY_M must be from 1 to 10000")
        if self.position_stale_after_seconds > self.position_offline_after_seconds:
            raise ValueError("POSITION_STALE_AFTER_SECONDS must not exceed POSITION_OFFLINE_AFTER_SECONDS")
        if self.position_retention_days > 3650:
            raise ValueError("POSITION_RETENTION_DAYS must be from 1 to 3650")
        for name, value in (
            ("SESSION_TTL_SECONDS", self.session_ttl_seconds),
            ("MAX_REQUEST_BYTES", self.max_request_bytes),
            ("LOGIN_RATE_LIMIT", self.login_rate_limit),
            ("LOGIN_IP_RATE_LIMIT", self.login_ip_rate_limit),
            ("LOGIN_RATE_WINDOW_SECONDS", self.login_rate_window_seconds),
            ("ATTENDANCE_CODE_RATE_LIMIT", self.attendance_code_rate_limit),
            ("ATTENDANCE_CODE_RATE_WINDOW_SECONDS", self.attendance_code_rate_window_seconds),
            ("COUNTER_SCAN_RATE_LIMIT", self.counter_scan_rate_limit),
            ("COUNTER_SCAN_RATE_WINDOW_SECONDS", self.counter_scan_rate_window_seconds),
            ("POSITION_RATE_LIMIT", self.position_rate_limit),
            ("POSITION_RATE_WINDOW_SECONDS", self.position_rate_window_seconds),
            ("POSITION_AUTH_IP_RATE_LIMIT", self.position_auth_ip_rate_limit),
            ("POSITION_INVALID_RATE_LIMIT", self.position_invalid_rate_limit),
            ("POSITION_INVALID_RATE_WINDOW_SECONDS", self.position_invalid_rate_window_seconds),
            ("POSITION_BATCH_RATE_LIMIT", self.position_batch_rate_limit),
            ("POSITION_BATCH_RATE_WINDOW_SECONDS", self.position_batch_rate_window_seconds),
            ("COUNTER_SCAN_LOCATION_MAX_AGE_SECONDS", self.counter_scan_location_max_age_seconds),
            ("DB_POOL_SIZE", self.db_pool_size),
            ("DB_MAX_OVERFLOW", self.db_max_overflow),
            ("DB_POOL_TIMEOUT_SECONDS", self.db_pool_timeout_seconds),
            ("RATE_LIMIT_POOL_SIZE", self.rate_limit_pool_size),
            ("RATE_LIMIT_MAX_OVERFLOW", self.rate_limit_max_overflow),
            ("RATE_LIMIT_POOL_TIMEOUT_SECONDS", self.rate_limit_pool_timeout_seconds),
            ("POSITION_MAX_FUTURE_SECONDS", self.position_max_future_seconds),
            ("POSITION_MAX_AGE_SECONDS", self.position_max_age_seconds),
            ("POSITION_STALE_AFTER_SECONDS", self.position_stale_after_seconds),
            ("POSITION_OFFLINE_AFTER_SECONDS", self.position_offline_after_seconds),
            ("POSITION_MAX_ACCURACY_M", self.position_max_accuracy_m),
            ("POSITION_RETENTION_DAYS", self.position_retention_days),
            ("REGISTER_RATE_LIMIT", self.register_rate_limit),
            ("REGISTER_RATE_WINDOW_SECONDS", self.register_rate_window_seconds),
            ("LOGIN_LOCKOUT_FAILURES", self.login_lockout_failures),
            ("LOGIN_LOCKOUT_SECONDS", self.login_lockout_seconds),
            ("OTP_TTL_SECONDS", self.otp_ttl_seconds),
            ("OTP_VERIFICATION_TTL_SECONDS", self.otp_verification_ttl_seconds),
            ("OTP_MAX_ATTEMPTS", self.otp_max_attempts),
            ("OTP_RESEND_SECONDS", self.otp_resend_seconds),
            ("OTP_SEND_RATE_LIMIT", self.otp_send_rate_limit),
            ("OTP_SEND_IP_RATE_LIMIT", self.otp_send_ip_rate_limit),
            ("OTP_SEND_RATE_WINDOW_SECONDS", self.otp_send_rate_window_seconds),
            ("OTP_CONFIRM_RATE_LIMIT", self.otp_confirm_rate_limit),
            ("OTP_CONFIRM_RATE_WINDOW_SECONDS", self.otp_confirm_rate_window_seconds),
        ):
            if value <= 0:
                raise ValueError(f"{name} must be positive")
        if not self.is_production:
            return self
        if not self.database_url.strip():
            raise ValueError("DATABASE_URL is required when ENVIRONMENT=production")
        parsed = urlparse(normalize_database_url(self.database_url))
        host = (parsed.hostname or "").lower()
        if not host:
            raise ValueError("DATABASE_URL must include a database host in production")
        if host in _LOCAL_HOSTS:
            raise ValueError("DATABASE_URL must not target localhost in production")
        sslmode = (parse_qs(parsed.query).get("sslmode", [""])[0] or "").lower()
        if sslmode in _INSECURE_SSLMODES:
            raise ValueError("DATABASE_URL must not disable or downgrade TLS (sslmode) in production")
        origins = self.cors_origin_list
        if "*" in origins:
            raise ValueError("CORS_ORIGINS=* is not allowed in production")
        for origin in origins:
            origin_url = urlparse(origin)
            if origin_url.scheme != "https" or (origin_url.hostname or "").lower() in _LOCAL_HOSTS:
                raise ValueError("CORS_ORIGINS must list only https:// non-local origins in production")
        if not self.textbee_api_key.strip():
            raise ValueError("TEXTBEE_API_KEY is required when ENVIRONMENT=production")
        if urlparse(self.textbee_base_url.strip()).scheme != "https":
            raise ValueError("TEXTBEE_BASE_URL must use https when ENVIRONMENT=production")
        if self.rate_limit_backend.strip().lower() != "postgres":
            raise ValueError("RATE_LIMIT_BACKEND=postgres is required when ENVIRONMENT=production")
        if self.counter_geofence_mode != "required":
            raise ValueError("COUNTER_GEOFENCE_MODE=required is required when ENVIRONMENT=production")
        if self.client_ip_source != "cf-connecting-ip":
            raise ValueError(
                "CLIENT_IP_SOURCE=cf-connecting-ip is required when ENVIRONMENT=production"
            )
        if self.document_storage_backend == "local" and not self.document_storage_dir.strip():
            raise ValueError("DOCUMENT_STORAGE_DIR is required when DOCUMENT_STORAGE_BACKEND=local")
        return self

    def document_backend(self) -> str:
        if self.document_storage_backend:
            return self.document_storage_backend
        if self.is_production:
            return ""
        return "local"

    def document_directory(self) -> Path:
        raw = self.document_storage_dir.strip()
        if raw:
            return Path(raw)
        return _BACKEND_ROOT / "var" / "documents"

    @property
    def uses_shared_rate_limits(self) -> bool:
        return self.rate_limit_backend.strip().lower() == "postgres"


@lru_cache
def get_settings() -> Settings:
    return Settings()
