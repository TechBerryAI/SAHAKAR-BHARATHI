"""Create or update the configured super-admin account.

Identity comes from the environment. The PIN is read from the terminal and
stored only as a hash:

    SUPER_ADMIN_MOBILE=… SUPER_ADMIN_NAME=… SUPER_ADMIN_LOGIN_ID=… \\
      python scripts/seed_super_admin.py

Running it again updates the same mobile row instead of inserting a second user.
Do not use this against production with a guessable PIN.
"""

from __future__ import annotations

import getpass
import sys
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))


def main() -> None:
    from app.db.session import SessionLocal
    from app.services.super_admin import SuperAdminConfigError, seed_super_admin

    pin = getpass.getpass("PIN (4 to 6 digits): ")
    try:
        with SessionLocal() as db:
            user = seed_super_admin(db, pin)
    except SuperAdminConfigError as exc:
        print(exc, file=sys.stderr)
        raise SystemExit(2) from None
    except ValueError as exc:
        print(exc, file=sys.stderr)
        raise SystemExit(2) from None
    print(f"Super admin ready: {user.login_id} ({user.mobile})")


if __name__ == "__main__":
    main()
