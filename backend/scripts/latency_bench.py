"""Reproducible latency benchmark for the participant API.

Runs the real FastAPI app in-process against the local *test* database through a TCP
proxy that injects network delay and counts database round-trips. This shows how request
latency scales with the API-to-database round-trip time without touching Render or
Supabase.

    python scripts/latency_bench.py --rtt-ms 0
    python scripts/latency_bench.py --rtt-ms 270 --iterations 4

The proxy does not simulate TCP or TLS handshakes, so new-connection cost is understated
compared with a remote `sslmode=require` database. The SMS gateway is replaced with an
in-memory recorder; no SMS is sent. All tables in the test database are truncated.
"""

from __future__ import annotations

import argparse
import asyncio
import os
import statistics
import subprocess
import sys
import threading
import time
from collections import defaultdict
from pathlib import Path
from urllib.parse import urlparse, urlunparse

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

_LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}
_TABLES = "audit_events, sessions, registrations, users, phone_verifications"


class DelayProxy:
    """TCP proxy that delays each direction by half the RTT and counts client flights."""

    def __init__(self, target_host: str, target_port: int, rtt_seconds: float) -> None:
        self.target_host = target_host
        self.target_port = target_port
        self.one_way = rtt_seconds / 2
        self.round_trips = 0
        self.port = 0
        self._loop = asyncio.new_event_loop()
        self._ready = threading.Event()

    def start(self) -> None:
        threading.Thread(target=self._run, daemon=True).start()
        self._ready.wait(10)

    def _run(self) -> None:
        asyncio.set_event_loop(self._loop)
        server = self._loop.run_until_complete(asyncio.start_server(self._handle, "127.0.0.1", 0))
        self.port = server.sockets[0].getsockname()[1]
        self._ready.set()
        self._loop.run_forever()

    async def _handle(self, client_reader: asyncio.StreamReader, client_writer: asyncio.StreamWriter) -> None:
        server_reader, server_writer = await asyncio.open_connection(self.target_host, self.target_port)
        state = {"awaiting_reply": False}
        await asyncio.gather(
            self._pipe(client_reader, server_writer, state, upstream=True),
            self._pipe(server_reader, client_writer, state, upstream=False),
            return_exceptions=True,
        )

    async def _pipe(
        self,
        reader: asyncio.StreamReader,
        writer: asyncio.StreamWriter,
        state: dict[str, bool],
        upstream: bool,
    ) -> None:
        queue: asyncio.Queue[tuple[float, bytes | None]] = asyncio.Queue()

        async def pump() -> None:
            while True:
                deadline, data = await queue.get()
                if data is None:
                    writer.close()
                    return
                wait = deadline - self._loop.time()
                if wait > 0:
                    await asyncio.sleep(wait)
                writer.write(data)
                await writer.drain()

        task = asyncio.create_task(pump())
        try:
            while data := await reader.read(65536):
                if upstream and not state["awaiting_reply"]:
                    self.round_trips += 1
                    state["awaiting_reply"] = True
                elif not upstream:
                    state["awaiting_reply"] = False
                queue.put_nowait((self._loop.time() + self.one_way, data))
        finally:
            queue.put_nowait((0.0, None))
            await task


def _test_database_url() -> str:
    from app.core.config import Settings, normalize_database_url

    explicit = os.environ.get("TEST_DATABASE_URL", "").strip()
    url = normalize_database_url(explicit) if explicit else Settings().sqlalchemy_test_url()
    parsed = urlparse(url)
    if "test" not in (parsed.path or "").lower():
        raise SystemExit("Refusing to run: the test database name must contain 'test'.")
    if (parsed.hostname or "").lower() not in _LOCAL_HOSTS:
        raise SystemExit("Refusing to run: the benchmark only targets a local test database.")
    return url


def _via_proxy(url: str, port: int) -> str:
    parsed = urlparse(url)
    netloc = parsed.netloc.rsplit("@", 1)
    host_part = f"127.0.0.1:{port}"
    netloc_out = f"{netloc[0]}@{host_part}" if len(netloc) == 2 else host_part
    return urlunparse(parsed._replace(netloc=netloc_out))


def _import_seconds(database_url: str) -> float:
    code = "import time; t=time.perf_counter(); import app.main; print(time.perf_counter()-t)"
    env = {**os.environ, "DATABASE_URL": database_url}
    result = subprocess.run(
        [sys.executable, "-c", code],
        cwd=BACKEND_ROOT,
        env=env,
        capture_output=True,
        text=True,
        check=True,
    )
    return float(result.stdout.strip().splitlines()[-1])


def _hash_seconds(samples: int = 5) -> tuple[float, float]:
    from app.core.security import hash_pin, verify_pin

    hashes = []
    verifies = []
    for _ in range(samples):
        start = time.perf_counter()
        digest = hash_pin("2468")
        hashes.append(time.perf_counter() - start)
        start = time.perf_counter()
        verify_pin("2468", digest)
        verifies.append(time.perf_counter() - start)
    return statistics.median(hashes), statistics.median(verifies)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--rtt-ms", type=float, default=0.0, help="Injected API-to-database round-trip time")
    parser.add_argument("--iterations", type=int, default=4, help="Full user flows to run (first one is cold)")
    args = parser.parse_args()

    direct_url = _test_database_url()
    target = urlparse(direct_url)
    proxy = DelayProxy(target.hostname or "127.0.0.1", target.port or 5432, args.rtt_ms / 1000)
    proxy.start()
    proxied_url = _via_proxy(direct_url, proxy.port)

    import_seconds = _import_seconds(proxied_url)

    os.environ["DATABASE_URL"] = proxied_url
    from alembic import command
    from alembic.config import Config
    from sqlalchemy import create_engine, event, text

    from app.core.config import get_settings

    get_settings.cache_clear()
    alembic_cfg = Config(str(BACKEND_ROOT / "alembic.ini"))
    alembic_cfg.set_main_option("sqlalchemy.url", direct_url.replace("%", "%%"))
    command.upgrade(alembic_cfg, "head")
    admin = create_engine(direct_url)

    def truncate() -> None:
        with admin.begin() as connection:
            connection.execute(text(f"TRUNCATE {_TABLES} RESTART IDENTITY CASCADE"))

    truncate()

    from fastapi.testclient import TestClient

    from app.db import session as db_session
    from app.main import app
    from tests.conftest import RecordingSmsGateway, register_payload

    statements = 0

    @event.listens_for(db_session.engine, "before_cursor_execute")
    def _count(*_args: object) -> None:
        nonlocal statements
        statements += 1

    gateway = RecordingSmsGateway()
    app.state.sms_gateway = gateway
    timings: dict[str, list[tuple[float, int, int]]] = defaultdict(list)

    def call(name: str, method: str, path: str, expected: int, **kwargs: object) -> dict[str, object]:
        nonlocal statements
        trips_before, statements_before = proxy.round_trips, statements
        start = time.perf_counter()
        response = client.request(method, path, **kwargs)
        elapsed = time.perf_counter() - start
        if response.status_code != expected:
            raise SystemExit(f"{name} returned {response.status_code}, expected {expected}")
        timings[name].append((elapsed, proxy.round_trips - trips_before, statements - statements_before))
        return response.json() if response.content else {}

    try:
        with TestClient(app) as client:
            for index in range(args.iterations):
                app.state.limiter.reset()
                mobile = f"9{index:09d}"
                sent = call("otp_send", "POST", "/api/v1/phone-verifications", 202, json={"mobile": mobile})
                confirmed = call(
                    "otp_confirm",
                    "POST",
                    f"/api/v1/phone-verifications/{sent['id']}/confirm",
                    200,
                    json={"code": gateway.latest_code()},
                )
                payload = register_payload(mobile=mobile, email=f"bench{index}@example.com")
                payload["verification_token"] = confirmed["verification_token"]
                call("register", "POST", "/api/v1/registrations", 201, json=payload)
                session = call("login", "POST", "/api/v1/sessions", 200, json={"identifier": mobile, "pin": "2468"})
                headers = {"Authorization": f"Bearer {session['access_token']}"}
                call("me", "GET", "/api/v1/me", 200, headers=headers)
                call("me_patch", "PATCH", "/api/v1/me", 200, headers=headers, json={"locality": "Ward 5"})
                call("health_ready", "GET", "/health/ready", 200)
                call("health_live", "GET", "/health/live", 200)
                call("logout", "DELETE", "/api/v1/sessions/current", 204, headers=headers)
    finally:
        app.state.sms_gateway = None
        truncate()
        admin.dispose()

    hash_median, verify_median = _hash_seconds()
    print(f"injected DB RTT: {args.rtt_ms:.0f} ms   iterations: {args.iterations}")
    print(f"import app.main (fresh interpreter): {import_seconds * 1000:.0f} ms")
    print(f"argon2 hash_pin median: {hash_median * 1000:.0f} ms   verify_pin median: {verify_median * 1000:.0f} ms")
    print()
    print(f"{'endpoint':<14}{'first ms':>10}{'warm med ms':>13}{'DB trips':>10}{'SQL stmts':>11}")
    for name, rows in timings.items():
        first = rows[0][0] * 1000
        warm = statistics.median(row[0] for row in rows[1:]) * 1000 if len(rows) > 1 else float("nan")
        trips = statistics.median(row[1] for row in rows[1:] or rows)
        stmts = statistics.median(row[2] for row in rows[1:] or rows)
        print(f"{name:<14}{first:>10.0f}{warm:>13.0f}{trips:>10.0f}{stmts:>11.0f}")
    flows = [sum(timings[name][i][0] for name in ("register", "login", "me")) for i in range(1, args.iterations)]
    if flows:
        print()
        print(f"register + login + me (web 'Register' button) warm median: {statistics.median(flows) * 1000:.0f} ms")


if __name__ == "__main__":
    main()
