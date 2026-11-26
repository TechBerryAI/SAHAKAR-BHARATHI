"""API server for concurrent registration load tests.

Replaces Textbee with a local SMS outbox so OTP codes are readable by
``scripts/load_test.py`` without sending real SMS. Raises abuse limits for the
process so 15–30 simultaneous registrations from one test client are not
blocked by the normal per-IP caps.

    make load-test-api

Refuses to start when ENVIRONMENT is production.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import threading
import time
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

DEFAULT_OUTBOX = Path(os.environ.get("LOAD_TEST_SMS_OUTBOX", "/tmp/sahakar-load-sms.jsonl"))


class LoadTestSmsGateway:
    """Thread-safe JSONL outbox: one line per SMS with mobile + 6-digit code."""

    def __init__(self, outbox: Path) -> None:
        self.outbox = outbox
        self._lock = threading.Lock()
        self.outbox.parent.mkdir(parents=True, exist_ok=True)
        self.outbox.write_text("", encoding="utf-8")

    def send(self, recipient_e164: str, message: str) -> None:
        mobile = re.sub(r"\D", "", recipient_e164)[-10:]
        match = re.search(r"\b(\d{6})\b", message)
        code = match.group(1) if match else ""
        row = {"mobile": mobile, "code": code, "message": message, "ts": time.time()}
        with self._lock:
            with self.outbox.open("a", encoding="utf-8") as handle:
                handle.write(json.dumps(row) + "\n")

    def wait_until_sent(self, timeout: float = 20.0) -> bool:
        return True


def _apply_load_test_env() -> None:
    # Only fill missing keys so an operator can still override from the shell.
    # Supabase session pooler (port 5432) often caps around 15 clients
    # (EMAXCONNSESSION). Keep the app pool under that so concurrent
    # registrations queue locally instead of failing open with HTTP 500.
    defaults = {
        "DB_POOL_SIZE": "10",
        "DB_MAX_OVERFLOW": "2",
        "DB_POOL_TIMEOUT_SECONDS": "60",
        "REGISTER_RATE_LIMIT": "50000",
        "REGISTER_RATE_WINDOW_SECONDS": "3600",
        "OTP_SEND_IP_RATE_LIMIT": "50000",
        "OTP_SEND_RATE_LIMIT": "500",
        "OTP_CONFIRM_RATE_LIMIT": "50000",
        # Same mobiles are reused across levels/runs; the normal per-id cap of 5
        # otherwise returns HTTP 429 on the 6th login within 15 minutes.
        "LOGIN_RATE_LIMIT": "500",
        "LOGIN_IP_RATE_LIMIT": "50000",
        "OTP_RESEND_SECONDS": "5",
        "RATE_LIMIT_BACKEND": "memory",
    }
    for key, value in defaults.items():
        os.environ.setdefault(key, value)
    # Always apply login caps for this process. A normal .env keeps
    # LOGIN_RATE_LIMIT=5, which breaks multi-level load tests that reuse mobiles.
    if os.environ.get("LOAD_TEST_KEEP_LOGIN_LIMITS", "").strip() not in {"1", "true", "yes"}:
        os.environ["LOGIN_RATE_LIMIT"] = os.environ.get("LOGIN_RATE_LIMIT") or defaults["LOGIN_RATE_LIMIT"]
        os.environ["LOGIN_IP_RATE_LIMIT"] = os.environ.get("LOGIN_IP_RATE_LIMIT") or defaults["LOGIN_IP_RATE_LIMIT"]
        # If the shell still has the restrictive production-like values, replace them.
        if int(os.environ["LOGIN_RATE_LIMIT"]) < 50:
            os.environ["LOGIN_RATE_LIMIT"] = defaults["LOGIN_RATE_LIMIT"]
        if int(os.environ["LOGIN_IP_RATE_LIMIT"]) < 500:
            os.environ["LOGIN_IP_RATE_LIMIT"] = defaults["LOGIN_IP_RATE_LIMIT"]
        if int(os.environ.get("OTP_SEND_IP_RATE_LIMIT", "0") or "0") < 1000:
            os.environ["OTP_SEND_IP_RATE_LIMIT"] = defaults["OTP_SEND_IP_RATE_LIMIT"]
        if int(os.environ.get("REGISTER_RATE_LIMIT", "0") or "0") < 1000:
            os.environ["REGISTER_RATE_LIMIT"] = defaults["REGISTER_RATE_LIMIT"]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--outbox", type=Path, default=DEFAULT_OUTBOX)
    args = parser.parse_args()

    _apply_load_test_env()

    import uvicorn

    from app.core.config import get_settings
    from app.main import app

    get_settings.cache_clear()
    settings = get_settings()
    if settings.is_production:
        sys.exit("load_test_server: refusing to run when ENVIRONMENT is production.")

    gateway = LoadTestSmsGateway(args.outbox)
    app.state.sms_gateway = gateway
    print(
        f"load_test_server: SMS outbox {args.outbox}  "
        f"register_limit={settings.register_rate_limit}  "
        f"login_limit={settings.login_rate_limit}/id  "
        f"login_ip_limit={settings.login_ip_rate_limit}  "
        f"pool={settings.db_pool_size}+{settings.db_max_overflow}",
        flush=True,
    )
    uvicorn.run(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
