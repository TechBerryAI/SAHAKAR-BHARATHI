"""Concurrent-user load test for the Sahakar Bharati API.

Scenarios:

  register  — N users simultaneously: OTP send → confirm → register → login → GET /me
  browse    — pre-seeded users hit the home-screen read path for --duration seconds
  both      — register burst, then browse with those accounts

Does not send real SMS. Point at ``load_test_server.py`` (mocked OTP outbox).
Prefer a non-production database. Cleanup with --cleanup.

Examples (from the repository root):

    # Terminal 1 — mocked SMS + raised registration limits
    make load-test-api

    # Terminal 2 — simultaneous registration at 15, 20, and 30 users
    make load-test
    make load-test USERS=10,15,20,30
    make load-test-browse USERS=20,30 DURATION=30
    make load-test-both
    make load-test-cleanup
"""

from __future__ import annotations

import argparse
import asyncio
import json
import statistics
import sys
import time
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

# 7000000001 .. 7000999999 (up to 999_999 concurrent synthetic users)
LOAD_MOBILE_PREFIX = "70"
MAX_LOAD_USERS = 999_999
DEFAULT_PIN = "2468"
THINK_SECONDS = 0.15
DEFAULT_OUTBOX = Path("/tmp/sahakar-load-sms.jsonl")
# High concurrency holds DB connections longer; allow the client to wait.
HTTP_TIMEOUT_S = 180.0


@dataclass
class Sample:
    name: str
    status: int
    elapsed_ms: float
    error: str = ""


@dataclass
class LevelResult:
    concurrency: int
    duration_s: float
    samples: list[Sample] = field(default_factory=list)
    registrations_ok: int = 0
    registrations_failed: int = 0

    @property
    def ok(self) -> list[Sample]:
        return [s for s in self.samples if 200 <= s.status < 400]

    @property
    def errors(self) -> list[Sample]:
        return [s for s in self.samples if not (200 <= s.status < 400)]


def _mobile(index: int) -> str:
    if not 1 <= index <= MAX_LOAD_USERS:
        raise ValueError(f"load-test user index must be 1..{MAX_LOAD_USERS}")
    return f"{LOAD_MOBILE_PREFIX}{index:08d}"


def _login_id(index: int) -> str:
    return f"load{index:06d}"


def _script_session():
    """Tiny one-off engine so cleanup/seed does not compete with the API pool."""
    from sqlalchemy import create_engine
    from sqlalchemy.orm import sessionmaker

    from app.core.config import get_settings

    settings = get_settings()
    engine = create_engine(
        settings.sqlalchemy_url(),
        pool_size=1,
        max_overflow=0,
        pool_pre_ping=True,
        **{"connect_args": settings.engine_kwargs().get("connect_args", {})},
    )
    return engine, sessionmaker(bind=engine, autoflush=False, autocommit=False)()


def cleanup_users() -> int:
    from sqlalchemy import delete, or_, text

    from app.models.user import User

    engine, db = _script_session()
    try:
        db.execute(
            text(
                "DELETE FROM phone_verifications WHERE mobile LIKE :p1 OR mobile LIKE :p2"
            ),
            {"p1": "70000000%", "p2": "70________"},  # legacy 2-digit + new 8-digit scheme
        )
        # Only rows created by this harness (login_id load###### / legacy load##).
        result = db.execute(
            delete(User).where(
                or_(
                    User.login_id.like("load%"),
                    User.full_name.like("Load Test User %"),
                )
            )
        )
        db.commit()
        return int(result.rowcount or 0)
    finally:
        db.close()
        engine.dispose()


def seed_users(count: int, pin: str = DEFAULT_PIN) -> list[str]:
    from app.core.security import hash_pin
    from app.models.user import User
    from app.repositories.users import UserRepository

    pin_hash = hash_pin(pin)
    mobiles: list[str] = []
    engine, db = _script_session()
    try:
        users = UserRepository(db)
        for i in range(1, count + 1):
            mobile = _mobile(i)
            mobiles.append(mobile)
            existing = users.get_by_mobile(mobile)
            if existing is None:
                users.add(
                    User(
                        login_id=_login_id(i),
                        mobile=mobile,
                        pin_hash=pin_hash,
                        full_name=f"Load Test User {i}",
                        role="participant",
                        status="active",
                        district="LoadTest",
                        state="Maharashtra",
                    )
                )
            else:
                existing.pin_hash = pin_hash
                existing.status = "active"
                existing.full_name = f"Load Test User {i}"
        db.commit()
    finally:
        db.close()
        engine.dispose()
    return mobiles


def _truncate_outbox(outbox: Path) -> None:
    outbox.parent.mkdir(parents=True, exist_ok=True)
    outbox.write_text("", encoding="utf-8")


async def wait_for_otp(outbox: Path, mobile: str, timeout: float = 30.0) -> str:
    deadline = time.perf_counter() + timeout
    needle = mobile[-10:]
    while time.perf_counter() < deadline:
        if outbox.exists():
            for line in outbox.read_text(encoding="utf-8").splitlines():
                if not line.strip():
                    continue
                try:
                    row = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if row.get("mobile") == needle and row.get("code"):
                    return str(row["code"])
        await asyncio.sleep(0.05)
    raise TimeoutError(f"no OTP in outbox for {mobile}")


def _registration_payload(index: int, mobile: str, pin: str, verification_token: str) -> dict[str, object]:
    return {
        "full_name": f"Load Test User {index}",
        "gender": "Female",
        "date_of_birth": "1990-01-15",
        "occupation": "Farmer",
        "mobile": mobile,
        "email": f"load{index:06d}@example.com",
        "address_line": "12 Village Road",
        "locality": "Ward 4",
        "district": "Nashik",
        "state": "Maharashtra",
        "pincode": "422001",
        "pin": pin,
        "verification_token": verification_token,
    }


async def register_one(
    client,
    index: int,
    pin: str,
    outbox: Path,
    samples: list[Sample],
) -> str | None:
    """Full registration path for one user. Returns access_token or None."""
    mobile = _mobile(index)

    started = time.perf_counter()
    try:
        sent = await client.post("/api/v1/phone-verifications", json={"mobile": mobile})
        samples.append(Sample("otp_send", sent.status_code, (time.perf_counter() - started) * 1000))
        if sent.status_code != 202:
            return None
        challenge_id = sent.json()["id"]
    except Exception as exc:  # noqa: BLE001
        samples.append(Sample("otp_send", 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
        return None

    started = time.perf_counter()
    try:
        code = await wait_for_otp(outbox, mobile)
        samples.append(Sample("otp_wait", 200, (time.perf_counter() - started) * 1000))
    except Exception as exc:  # noqa: BLE001
        samples.append(Sample("otp_wait", 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
        return None

    started = time.perf_counter()
    try:
        confirmed = await client.post(
            f"/api/v1/phone-verifications/{challenge_id}/confirm",
            json={"code": code},
        )
        samples.append(Sample("otp_confirm", confirmed.status_code, (time.perf_counter() - started) * 1000))
        if confirmed.status_code != 200:
            return None
        token = confirmed.json()["verification_token"]
    except Exception as exc:  # noqa: BLE001
        samples.append(Sample("otp_confirm", 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
        return None

    started = time.perf_counter()
    try:
        created = await client.post(
            "/api/v1/registrations",
            json=_registration_payload(index, mobile, pin, token),
        )
        samples.append(Sample("register", created.status_code, (time.perf_counter() - started) * 1000))
        if created.status_code != 201:
            return None
    except Exception as exc:  # noqa: BLE001
        samples.append(Sample("register", 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
        return None

    started = time.perf_counter()
    try:
        session = await _login_with_retry(client, mobile, pin)
        samples.append(Sample("login", session.status_code, (time.perf_counter() - started) * 1000))
        if session.status_code != 200:
            return None
        access = session.json()["access_token"]
    except Exception as exc:  # noqa: BLE001
        samples.append(Sample("login", 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
        return None

    started = time.perf_counter()
    try:
        me = await client.get("/api/v1/me", headers={"Authorization": f"Bearer {access}"})
        samples.append(Sample("me", me.status_code, (time.perf_counter() - started) * 1000))
        if me.status_code != 200:
            return None
    except Exception as exc:  # noqa: BLE001
        samples.append(Sample("me", 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
        return None

    return access


async def run_register_burst(
    base_url: str,
    concurrency: int,
    pin: str,
    outbox: Path,
) -> LevelResult:
    import httpx

    samples: list[Sample] = []
    _truncate_outbox(outbox)
    cleanup_users()
    limits = httpx.Limits(max_connections=concurrency + 10, max_keepalive_connections=concurrency + 10)
    wall_start = time.perf_counter()
    async with httpx.AsyncClient(base_url=base_url, timeout=HTTP_TIMEOUT_S, limits=limits) as client:
        results = await asyncio.gather(
            *(register_one(client, i, pin, outbox, samples) for i in range(1, concurrency + 1))
        )
    wall = time.perf_counter() - wall_start
    ok_tokens = [token for token in results if token]
    return LevelResult(
        concurrency=concurrency,
        duration_s=wall,
        samples=samples,
        registrations_ok=len(ok_tokens),
        registrations_failed=concurrency - len(ok_tokens),
    )


async def _login_with_retry(client, mobile: str, pin: str, *, attempts: int = 8):
    """Login, backing off on HTTP 429 (per-IP / per-identifier login caps)."""
    last = None
    for attempt in range(attempts):
        last = await client.post("/api/v1/sessions", json={"identifier": mobile, "pin": pin})
        if last.status_code != 429:
            return last
        retry_after = last.headers.get("Retry-After")
        delay = float(retry_after) if retry_after and retry_after.isdigit() else min(2 ** attempt, 30)
        await asyncio.sleep(delay)
    return last


async def login_all(base_url: str, mobiles: list[str], pin: str) -> list[str]:
    import httpx

    tokens: list[str] = []
    async with httpx.AsyncClient(base_url=base_url, timeout=HTTP_TIMEOUT_S) as client:
        for mobile in mobiles:
            response = await _login_with_retry(client, mobile, pin)
            if response.status_code != 200:
                raise RuntimeError(
                    f"login failed for {mobile}: {response.status_code} {response.text}\n"
                    "If this is HTTP 429, restart the API with `make load-test-api` "
                    "(raises LOGIN_RATE_LIMIT / LOGIN_IP_RATE_LIMIT for load tests)."
                )
            tokens.append(response.json()["access_token"])
            await asyncio.sleep(0.05)
    return tokens


async def user_loop(client, token: str, stop_at: float, samples: list[Sample]) -> None:
    headers = {"Authorization": f"Bearer {token}"}
    steps = (
        ("health_live", "GET", "/health/live", None),
        ("health_ready", "GET", "/health/ready", None),
        ("me", "GET", "/api/v1/me", headers),
        ("yatras", "GET", "/api/v1/yatras?limit=20", headers),
        ("attendance", "GET", "/api/v1/attendance", headers),
    )
    while time.perf_counter() < stop_at:
        for name, method, path, hdrs in steps:
            started = time.perf_counter()
            try:
                response = await client.request(method, path, headers=hdrs)
                samples.append(Sample(name, response.status_code, (time.perf_counter() - started) * 1000))
            except Exception as exc:  # noqa: BLE001
                samples.append(Sample(name, 0, (time.perf_counter() - started) * 1000, type(exc).__name__))
            if time.perf_counter() >= stop_at:
                return
        await asyncio.sleep(THINK_SECONDS)


async def run_browse_level(base_url: str, tokens: list[str], concurrency: int, duration: float) -> LevelResult:
    import httpx

    chosen = tokens[:concurrency]
    samples: list[Sample] = []
    stop_at = time.perf_counter() + duration
    limits = httpx.Limits(max_connections=concurrency + 5, max_keepalive_connections=concurrency + 5)
    async with httpx.AsyncClient(base_url=base_url, timeout=HTTP_TIMEOUT_S, limits=limits) as client:
        await asyncio.gather(*(user_loop(client, token, stop_at, samples) for token in chosen))
    return LevelResult(concurrency=concurrency, duration_s=duration, samples=samples)


def _percentile(values: list[float], pct: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    rank = (len(ordered) - 1) * pct
    low = int(rank)
    high = min(low + 1, len(ordered) - 1)
    weight = rank - low
    return ordered[low] * (1 - weight) + ordered[high] * weight


def print_report(result: LevelResult, *, label: str) -> None:
    total = len(result.samples)
    ok = result.ok
    errors = result.errors
    latencies = [s.elapsed_ms for s in ok]
    by_status = Counter(s.status for s in result.samples)
    by_name: dict[str, list[float]] = defaultdict(list)
    for sample in ok:
        by_name[sample.name].append(sample.elapsed_ms)

    error_rate = (len(errors) / total * 100) if total else 100.0
    print()
    print(f"=== {label}: {result.concurrency} concurrent ({result.duration_s:.1f}s wall) ===")
    if result.registrations_ok or result.registrations_failed:
        print(
            f"registrations_ok={result.registrations_ok}  "
            f"registrations_failed={result.registrations_failed}  "
            f"success_rate={result.registrations_ok / result.concurrency * 100:.1f}%"
        )
    print(f"requests={total}  ok={len(ok)}  errors={len(errors)}  error_rate={error_rate:.1f}%")
    print(f"status_codes={dict(sorted(by_status.items()))}")
    if latencies:
        print(
            "latency_ms: "
            f"p50={_percentile(latencies, 0.50):.0f}  "
            f"p95={_percentile(latencies, 0.95):.0f}  "
            f"p99={_percentile(latencies, 0.99):.0f}  "
            f"max={max(latencies):.0f}  "
            f"mean={statistics.fmean(latencies):.0f}"
        )
    for name, values in sorted(by_name.items()):
        print(
            f"  {name:12s} n={len(values):4d}  "
            f"p50={_percentile(values, 0.50):6.0f}  "
            f"p95={_percentile(values, 0.95):6.0f}  "
            f"max={max(values):6.0f}"
        )
    if errors:
        err_names = Counter((s.name, s.status, s.error) for s in errors)
        print("errors:")
        for (name, status, err), count in err_names.most_common(10):
            print(f"  {name} status={status} {err or ''} x{count}")


def pass_fail_register(results: list[LevelResult], *, min_success_pct: float = 95.0) -> bool:
    ok = True
    print()
    print("=== Pass / fail (simultaneous registration) ===")
    saw_429 = any(s.status == 429 for result in results for s in result.samples)
    for result in results:
        success = result.registrations_ok / result.concurrency * 100 if result.concurrency else 0.0
        level_ok = success >= min_success_pct
        mark = "PASS" if level_ok else "FAIL"
        print(
            f"{mark}  users={result.concurrency:4d}  "
            f"registered={result.registrations_ok}/{result.concurrency}  "
            f"({success:.0f}%)  wall={result.duration_s:.1f}s"
        )
        ok = ok and level_ok
    if not ok and saw_429:
        print(
            "\nHint: HTTP 429 means a rate limit (OTP/login/register).\n"
            "Restart terminal 1 with `make load-test-api` (raises those caps), then re-run."
        )
    return ok


def pass_fail_browse(results: list[LevelResult], max_error_rate: float, max_p95_ms: float) -> bool:
    ok = True
    print()
    print("=== Pass / fail (browse) ===")
    for result in results:
        total = len(result.samples)
        error_rate = (len(result.errors) / total * 100) if total else 100.0
        p95 = _percentile([s.elapsed_ms for s in result.ok], 0.95) if result.ok else float("inf")
        level_ok = error_rate <= max_error_rate and p95 <= max_p95_ms
        mark = "PASS" if level_ok else "FAIL"
        print(
            f"{mark}  users={result.concurrency:2d}  "
            f"error_rate={error_rate:5.1f}% (max {max_error_rate})  "
            f"p95={p95:7.0f}ms (max {max_p95_ms:.0f})"
        )
        ok = ok and level_ok
    return ok


def _parse_users(raw: str) -> list[int]:
    """Parse USERS=10,15,20. Reject typos like 30.40 (use 30,40)."""
    parts = [part.strip() for part in raw.replace(" ", ",").split(",") if part.strip()]
    if not parts:
        raise SystemExit("provide at least one concurrency level via --users / USERS=")
    levels: list[int] = []
    for part in parts:
        if not part.isdigit():
            raise SystemExit(
                f"invalid USERS value {part!r} in {raw!r}.\n"
                "Use commas only, e.g. USERS=10,15,20,30,40,50  (not 30.40)."
            )
        levels.append(int(part))
    max_users = max(levels)
    if max_users > MAX_LOAD_USERS:
        raise SystemExit(f"max users is {MAX_LOAD_USERS} for this seed scheme")
    if min(levels) < 1:
        raise SystemExit("each USERS level must be at least 1")
    return levels


async def async_main(args: argparse.Namespace) -> int:
    import httpx

    levels = _parse_users(args.users)
    max_users = max(levels)

    async with httpx.AsyncClient(base_url=args.base_url, timeout=15.0) as client:
        live = await client.get("/health/live")
        if live.status_code != 200:
            raise SystemExit(f"API not live at {args.base_url}: {live.status_code}")

    outbox = Path(args.outbox)
    passed = True

    if args.scenario in {"register", "both"}:
        print(f"Simultaneous registration levels {levels} against {args.base_url}")
        print(f"SMS outbox: {outbox}")
        register_results: list[LevelResult] = []
        for concurrency in levels:
            print(f"\nStarting simultaneous registration for {concurrency} users…")
            result = await run_register_burst(args.base_url, concurrency, args.pin, outbox)
            print_report(result, label="register")
            register_results.append(result)
            await asyncio.sleep(1.0)
        passed = pass_fail_register(register_results) and passed

    if args.scenario in {"browse", "both"}:
        print(f"\nBrowse scenario levels {levels} for {args.duration}s each")
        if args.scenario == "browse":
            print(f"Seeding {max_users} load-test users…")
            mobiles = seed_users(max_users, pin=args.pin)
        else:
            # Re-use accounts from the last registration level (largest).
            mobiles = [_mobile(i) for i in range(1, max_users + 1)]
        print(f"Logging in {len(mobiles)} users…")
        tokens = await login_all(args.base_url, mobiles, args.pin)
        browse_results: list[LevelResult] = []
        for concurrency in levels:
            print(f"\nWarming browse for {concurrency} users…")
            result = await run_browse_level(args.base_url, tokens, concurrency, args.duration)
            print_report(result, label="browse")
            browse_results.append(result)
            await asyncio.sleep(1.0)
        passed = pass_fail_browse(browse_results, args.max_error_rate, args.max_p95_ms) and passed

    if args.cleanup_after:
        removed = cleanup_users()
        print(f"\nCleaned up {removed} load-test users.")
    return 0 if passed else 1


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base-url", default="http://127.0.0.1:8000")
    parser.add_argument(
        "--scenario",
        choices=("register", "browse", "both"),
        default="register",
        help="register = simultaneous sign-up burst (default)",
    )
    parser.add_argument("--users", default="15,20,30", help="Comma-separated concurrency levels")
    parser.add_argument("--duration", type=float, default=20.0, help="Seconds per browse level")
    parser.add_argument("--pin", default=DEFAULT_PIN)
    parser.add_argument("--outbox", default=str(DEFAULT_OUTBOX), help="SMS outbox path from load_test_server")
    parser.add_argument("--max-error-rate", type=float, default=1.0)
    parser.add_argument("--max-p95-ms", type=float, default=8000.0)
    parser.add_argument("--cleanup", action="store_true", help="Delete load-test users and exit")
    parser.add_argument("--cleanup-after", action="store_true")
    args = parser.parse_args()

    if args.cleanup:
        removed = cleanup_users()
        print(f"Removed {removed} load-test users.")
        return

    raise SystemExit(asyncio.run(async_main(args)))


if __name__ == "__main__":
    main()
