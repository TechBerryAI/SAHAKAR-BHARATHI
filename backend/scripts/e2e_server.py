"""Local end-to-end server: the real API with SMS replaced by a file outbox.

For manual QA of registration on the emulator or in a browser without sending SMS.
OTP logic is unchanged; only delivery is swapped, the same way the tests do it.
Each "SMS" is appended to E2E_SMS_OUTBOX (default /tmp/sahakar-e2e-sms.log).

    DATABASE_URL=postgresql+psycopg://…/sahakar_e2e_test python scripts/e2e_server.py --port 8010

Refuses to start in production or against a database whose name lacks "test".
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path
from urllib.parse import urlparse

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))


class FileOutboxGateway:
    def __init__(self, outbox: Path) -> None:
        self.outbox = outbox

    def send(self, recipient_e164: str, message: str) -> None:
        with self.outbox.open("a", encoding="utf-8") as handle:
            handle.write(f"{recipient_e164}\t{message}\n")

    def wait_until_sent(self, timeout: float = 20.0) -> bool:
        return True


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8010)
    args = parser.parse_args()

    import uvicorn

    from app.core.config import get_settings
    from app.main import app

    settings = get_settings()
    database = urlparse(settings.sqlalchemy_url()).path.lstrip("/")
    if settings.is_production or "test" not in database.lower():
        sys.exit("e2e_server: refusing to run in production or against a non-test database.")

    outbox = Path(os.environ.get("E2E_SMS_OUTBOX", "/tmp/sahakar-e2e-sms.log"))
    app.state.sms_gateway = FileOutboxGateway(outbox)
    print(f"e2e_server: SMS messages go to {outbox}", flush=True)
    uvicorn.run(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
