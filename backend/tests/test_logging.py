from __future__ import annotations

import logging

from app.core.logging import RedactingFilter


def _record(msg: str, args: dict[str, object]) -> logging.LogRecord:
    # LogRecord treats a one-key mapping as a %-format argument, so assign args after.
    record = logging.LogRecord(
        name="app.test",
        level=logging.INFO,
        pathname=__file__,
        lineno=1,
        msg=msg,
        args=None,
        exc_info=None,
    )
    record.args = args
    return record


def test_redacting_filter_drops_pin_otp_and_token_fields() -> None:
    record = _record(
        "login pin accepted with otp",
        {"pin": "2468", "otp": "123456", "access_token": "secret-token", "mobile": "9876543210"},
    )
    assert RedactingFilter().filter(record) is True
    assert record.msg == "[redacted log message]"
    assert record.args == {
        "pin": "[redacted]",
        "otp": "[redacted]",
        "access_token": "[redacted]",
        "mobile": "9876543210",
    }


def test_redacting_filter_drops_qr_and_tracker_tokens() -> None:
    record = _record(
        "counter issued",
        {"qr_token": "posted-secret", "tracker_token": "device-secret", "yatra_id": "1"},
    )
    assert RedactingFilter().filter(record) is True
    assert record.msg == "counter issued"
    assert record.args == {
        "qr_token": "[redacted]",
        "tracker_token": "[redacted]",
        "yatra_id": "1",
    }


def test_redacting_filter_keeps_an_ordinary_message() -> None:
    record = _record("participant signed in", {"mobile": "9876543210"})
    assert RedactingFilter().filter(record) is True
    assert record.msg == "participant signed in"
    assert record.args == {"mobile": "9876543210"}
