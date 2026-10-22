from __future__ import annotations

from fastapi.testclient import TestClient


def test_oversized_request_is_413(client: TestClient) -> None:
    response = client.post(
        "/api/v1/registrations",
        content=b"{" + (b"a" * 70_000) + b"}",
        headers={"Content-Type": "application/json"},
    )
    assert response.status_code == 413
    assert response.json()["detail"] == "Request too large."


def test_invalid_content_length_is_400(client: TestClient) -> None:
    response = client.post(
        "/api/v1/sessions",
        content=b'{"identifier":"9876543210","pin":"2468"}',
        headers={"Content-Type": "application/json", "Content-Length": "nope"},
    )
    assert response.status_code in {400, 422}
    if response.status_code == 400:
        assert "Content-Length" in response.json()["detail"]
