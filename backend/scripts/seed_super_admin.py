"""Create or update the Jaydeep Patil super-admin account.

The PIN is passed on the command line and stored only as a hash:

    python scripts/seed_super_admin.py --pin <4-to-6-digit-pin>

Running it again updates the same mobile row instead of inserting a second user.
Do not use this against production with a guessable PIN.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="Seed the Jaydeep Patil super-admin account.")
    parser.add_argument("--pin", required=True, help="Sign-in PIN. Stored only as a hash.")
    args = parser.parse_args(argv)
    from app.db.session import SessionLocal
    from app.services.super_admin import seed_super_admin

    with SessionLocal() as db:
        user = seed_super_admin(db, args.pin)
    print(f"Super admin ready: {user.login_id} ({user.mobile})")


if __name__ == "__main__":
    main()
