from __future__ import annotations

import pytest
from pydantic import ValidationError

from app.core.config import Settings
from app.core.errors import NotFoundError, ServiceUnavailableError
from app.core.storage import S3DocumentStorage


class _Body:
    def __init__(self, data: bytes) -> None:
        self.data = data

    def read(self) -> bytes:
        return self.data


class _Store:
    def __init__(self) -> None:
        self.objects: dict[str, bytes] = {}

    def put_object(self, *, Bucket: str, Key: str, Body: bytes) -> None:
        assert Bucket == "private-docs"
        self.objects[Key] = Body

    def get_object(self, *, Bucket: str, Key: str) -> dict[str, _Body]:
        if Key not in self.objects:
            raise KeyError(Key)
        return {"Body": _Body(self.objects[Key])}

    def delete_object(self, *, Bucket: str, Key: str) -> None:
        self.objects.pop(Key, None)


def test_s3_adapter_round_trips_a_private_key_and_rejects_traversal() -> None:
    storage = S3DocumentStorage(_Store(), "private-docs")
    storage.put("a" * 32, b"%PDF-1.4")
    assert storage.read("a" * 32) == b"%PDF-1.4"
    storage.delete("a" * 32)
    with pytest.raises(NotFoundError):
        storage.read("a" * 32)
    with pytest.raises(NotFoundError):
        storage.put("../secret", b"no")


def test_s3_read_failure_does_not_expose_the_storage_key() -> None:
    class Broken:
        def get_object(self, *, Bucket: str, Key: str) -> dict[str, _Body]:
            raise RuntimeError(f"leaked {Key}")

        def put_object(self, *, Bucket: str, Key: str, Body: bytes) -> None:
            return None

        def delete_object(self, *, Bucket: str, Key: str) -> None:
            return None

    storage = S3DocumentStorage(Broken(), "private-docs")
    with pytest.raises(ServiceUnavailableError) as caught:
        storage.read("b" * 32)
    assert "b" * 32 not in caught.value.detail


def test_s3_settings_require_a_bucket_and_credentials() -> None:
    with pytest.raises(ValidationError, match="DOCUMENT_STORAGE_BUCKET"):
        Settings(document_storage_backend="s3")


def test_production_without_storage_backend_stays_closed() -> None:
    settings = Settings(
        environment="production",
        database_url="postgresql+psycopg://u:p@db.example.com:5432/app?sslmode=require",
        textbee_api_key="key",
        rate_limit_backend="postgres",
        client_ip_source="cf-connecting-ip",
        cors_origins="",
    )
    assert settings.document_backend() == ""
