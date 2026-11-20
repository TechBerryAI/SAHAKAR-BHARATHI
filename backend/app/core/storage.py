"""Document bytes stay out of PostgreSQL. Callers pass a server-generated key."""

from __future__ import annotations

import re
from pathlib import Path
from typing import Protocol

from app.core.errors import NotFoundError, ServiceUnavailableError

_KEY_RE = re.compile(r"^[0-9a-f]{32}$")
_UNSAFE_NAME = re.compile(r'[\x00-\x1f"]')


def display_filename(raw: str) -> str:
    cleaned = raw.replace("\\", "/").split("/")[-1]
    cleaned = _UNSAFE_NAME.sub("", cleaned).strip().strip(".")
    if cleaned in {"", ".", ".."}:
        raise ValueError("The file name is not valid.")
    if len(cleaned) > 255:
        cleaned = cleaned[-255:]
    return cleaned


def declared_media_type(header: str) -> str:
    return header.split(";", 1)[0].strip().lower()


def sniff_document(data: bytes) -> str | None:
    if data.startswith(b"%PDF-"):
        return "application/pdf"
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"
    return None


class DocumentStorage(Protocol):
    def put(self, key: str, data: bytes) -> None: ...

    def read(self, key: str) -> bytes: ...

    def delete(self, key: str) -> None: ...


class S3DocumentStorage:
    """Private S3-compatible objects. Keys stay server-generated; no public URL is returned."""

    def __init__(self, client: object, bucket: str) -> None:
        self.client = client
        self.bucket = bucket

    def _key(self, key: str) -> str:
        if not _KEY_RE.fullmatch(key):
            raise NotFoundError("That document was not found.")
        return key

    def put(self, key: str, data: bytes) -> None:
        safe = self._key(key)
        try:
            self.client.put_object(Bucket=self.bucket, Key=safe, Body=data)  # type: ignore[attr-defined]
        except Exception as exc:
            if _missing_object(exc):
                raise NotFoundError("That document was not found.") from None
            raise ServiceUnavailableError("Document storage is not available.") from None

    def read(self, key: str) -> bytes:
        try:
            response = self.client.get_object(Bucket=self.bucket, Key=self._key(key))  # type: ignore[attr-defined]
        except Exception as exc:
            if _missing_object(exc):
                raise NotFoundError("That document was not found.") from None
            raise ServiceUnavailableError("Document storage is not available.") from None
        body = response["Body"].read()
        return body if isinstance(body, bytes) else bytes(body)

    def delete(self, key: str) -> None:
        try:
            self.client.delete_object(Bucket=self.bucket, Key=self._key(key))  # type: ignore[attr-defined]
        except Exception as exc:
            if _missing_object(exc):
                return
            raise ServiceUnavailableError("Document storage is not available.") from None


def _missing_object(exc: Exception) -> bool:
    response = getattr(exc, "response", None)
    code = ""
    if isinstance(response, dict):
        code = str(response.get("Error", {}).get("Code", ""))
    return code in {"NoSuchKey", "404", "NotFound"} or isinstance(exc, KeyError)


def s3_client_from_settings(settings: object) -> S3DocumentStorage:
    import boto3

    endpoint = str(getattr(settings, "document_storage_endpoint", "") or "").strip() or None
    client = boto3.client(
        "s3",
        region_name=str(getattr(settings, "document_storage_region", "") or "ap-south-1"),
        endpoint_url=endpoint,
        aws_access_key_id=str(getattr(settings, "document_storage_access_key")),
        aws_secret_access_key=str(getattr(settings, "document_storage_secret_key")),
    )
    return S3DocumentStorage(client, str(getattr(settings, "document_storage_bucket")))


class UnavailableDocumentStorage:
    def put(self, key: str, data: bytes) -> None:
        raise ServiceUnavailableError("Document storage is not configured.")

    def read(self, key: str) -> bytes:
        raise ServiceUnavailableError("Document storage is not configured.")

    def delete(self, key: str) -> None:
        return None


class LocalDocumentStorage:
    def __init__(self, root: Path) -> None:
        self.root = root

    def _path(self, key: str) -> Path:
        if not _KEY_RE.fullmatch(key):
            raise NotFoundError("That document was not found.")
        directory = self.root.resolve()
        path = (directory / key).resolve()
        if path.parent != directory:
            raise NotFoundError("That document was not found.")
        return path

    def put(self, key: str, data: bytes) -> None:
        path = self._path(key)
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_name(f"{path.name}.part")
        temporary.write_bytes(data)
        temporary.replace(path)

    def read(self, key: str) -> bytes:
        path = self._path(key)
        if not path.is_file():
            raise NotFoundError("That document was not found.")
        return path.read_bytes()

    def delete(self, key: str) -> None:
        path = self._path(key)
        if path.is_file():
            path.unlink()
