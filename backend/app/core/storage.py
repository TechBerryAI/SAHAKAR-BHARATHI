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
