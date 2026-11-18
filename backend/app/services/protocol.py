from __future__ import annotations

import hashlib
import uuid
from datetime import UTC, datetime

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.errors import AppError, ConflictError, ForbiddenError, InvalidStateError, NotFoundError
from app.core.roles import ROLE_ADMIN, ROLE_SUPER_ADMIN
from app.core.storage import DocumentStorage, declared_media_type, display_filename, sniff_document
from app.core.yatra_rules import (
    DOCUMENT_OPEN_STATUSES,
    MANAGE_ACCESS,
    OPERATE_ACCESS,
    REVIEW_SOURCES,
    SUBMIT_TARGETS,
)
from app.db.session import is_unique_violation
from app.models.protocol import ProtocolDocument, ProtocolItem, ProtocolTemplate
from app.models.user import User
from app.repositories.audit import AuditRepository
from app.repositories.protocol import ProtocolRepository
from app.repositories.yatras import YatraRepository
from app.schemas.protocol import (
    ProtocolDocumentResponse,
    ProtocolItemCreate,
    ProtocolItemResponse,
    ProtocolItemUpdate,
    ProtocolList,
    ProtocolReject,
    ProtocolTemplateCreate,
    ProtocolTemplateList,
    ProtocolTemplateResponse,
    ProtocolTemplateUpdate,
)
from app.services.yatra_access import YatraAccess


class ProtocolService:
    def __init__(self, db: Session, storage: DocumentStorage) -> None:
        self.db = db
        self.storage = storage
        self.yatras = YatraRepository(db)
        self.protocol = ProtocolRepository(db)
        self.audit = AuditRepository(db)
        self.access = YatraAccess(db)

    def list_items(self, actor: User, yatra_id: uuid.UUID) -> ProtocolList:
        yatra, level = self._visible(actor, yatra_id)
        if level == "public":
            raise ForbiddenError()
        return ProtocolList(items=self._items(yatra.id))

    def add_item(self, actor: User, yatra_id: uuid.UUID, payload: ProtocolItemCreate) -> ProtocolItemResponse:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        item = ProtocolItem(
            yatra_id=yatra.id,
            category=payload.category,
            title=payload.title,
            description=payload.description,
            required=payload.required,
            due_at=payload.due_at,
            responsible_label=payload.responsible_label,
            status="pending",
        )
        self.db.add(item)
        self.db.flush()
        self.audit.add(
            "protocol_item_created",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "item_id": str(item.id), "category": item.category},
        )
        self.db.commit()
        self.db.refresh(item)
        return self._item(item, [])

    def update_item(
        self,
        actor: User,
        yatra_id: uuid.UUID,
        item_id: uuid.UUID,
        payload: ProtocolItemUpdate,
    ) -> ProtocolItemResponse:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        item = self._item_for_update(yatra.id, item_id)
        if item.status == "approved":
            raise InvalidStateError("An approved item cannot be edited.")
        if payload.category is not None:
            item.category = payload.category
        if payload.title is not None:
            item.title = payload.title
        if payload.clear_description:
            item.description = None
        elif payload.description is not None:
            item.description = payload.description
        if payload.required is not None:
            item.required = payload.required
        if payload.clear_due_at:
            item.due_at = None
        elif payload.due_at is not None:
            item.due_at = payload.due_at
        if payload.clear_responsible_label:
            item.responsible_label = None
        elif payload.responsible_label is not None:
            item.responsible_label = payload.responsible_label
        self.db.commit()
        self.db.refresh(item)
        documents = self.protocol.documents_for([item.id]).get(item.id, [])
        return self._item(item, documents)

    def submit(self, actor: User, yatra_id: uuid.UUID, item_id: uuid.UUID) -> ProtocolItemResponse:
        yatra, _level = self._locked(actor, yatra_id, OPERATE_ACCESS)
        self._ensure_open(yatra)
        item = self._item_for_update(yatra.id, item_id)
        target = SUBMIT_TARGETS.get(item.status)
        if target is None:
            raise InvalidStateError("This item cannot be submitted now.")
        previous = item.status
        item.status = target
        item.submitted_by = actor.id
        item.submitted_at = datetime.now(UTC)
        event = "protocol_item_resubmitted" if previous == "rejected" else "protocol_item_submitted"
        self.audit.add(
            event,
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "item_id": str(item.id), "from": previous, "to": target},
        )
        self.db.commit()
        self.db.refresh(item)
        documents = self.protocol.documents_for([item.id]).get(item.id, [])
        return self._item(item, documents)

    def review(self, actor: User, yatra_id: uuid.UUID, item_id: uuid.UUID) -> ProtocolItemResponse:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        item = self._item_for_update(yatra.id, item_id)
        if item.status not in REVIEW_SOURCES:
            raise InvalidStateError("This item is not waiting for review.")
        item.status = "under_review"
        item.reviewed_by = actor.id
        item.reviewed_at = datetime.now(UTC)
        self.audit.add(
            "protocol_item_reviewed",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "item_id": str(item.id)},
        )
        self.db.commit()
        self.db.refresh(item)
        documents = self.protocol.documents_for([item.id]).get(item.id, [])
        return self._item(item, documents)

    def approve(self, actor: User, yatra_id: uuid.UUID, item_id: uuid.UUID) -> ProtocolItemResponse:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        item = self._item_for_update(yatra.id, item_id)
        if item.status != "under_review":
            raise InvalidStateError("This item is not under review.")
        item.status = "approved"
        item.rejection_reason = None
        item.reviewed_by = actor.id
        item.reviewed_at = datetime.now(UTC)
        self.audit.add(
            "approval_granted",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "item_id": str(item.id)},
        )
        self.db.commit()
        self.db.refresh(item)
        documents = self.protocol.documents_for([item.id]).get(item.id, [])
        return self._item(item, documents)

    def reject(
        self,
        actor: User,
        yatra_id: uuid.UUID,
        item_id: uuid.UUID,
        payload: ProtocolReject,
    ) -> ProtocolItemResponse:
        yatra, _level = self._locked(actor, yatra_id, MANAGE_ACCESS)
        self._ensure_open(yatra)
        item = self._item_for_update(yatra.id, item_id)
        if item.status != "under_review":
            raise InvalidStateError("This item is not under review.")
        item.status = "rejected"
        item.rejection_reason = payload.reason
        item.reviewed_by = actor.id
        item.reviewed_at = datetime.now(UTC)
        self.audit.add(
            "approval_rejected",
            user_id=actor.id,
            extra={"yatra_id": str(yatra.id), "item_id": str(item.id)},
        )
        self.db.commit()
        self.db.refresh(item)
        documents = self.protocol.documents_for([item.id]).get(item.id, [])
        return self._item(item, documents)

    def add_document(
        self,
        actor: User,
        yatra_id: uuid.UUID,
        item_id: uuid.UUID,
        data: bytes,
        content_type: str,
        filename: str,
    ) -> ProtocolDocumentResponse:
        yatra, _level = self._locked(actor, yatra_id, OPERATE_ACCESS)
        self._ensure_open(yatra)
        item = self._item_for_update(yatra.id, item_id)
        if item.status not in DOCUMENT_OPEN_STATUSES:
            raise InvalidStateError("This item is not open for documents.")
        if not data:
            raise InvalidStateError("The file is empty.")
        if len(data) > get_settings().document_max_bytes:
            raise AppError(413, "Request too large.")
        declared = declared_media_type(content_type)
        sniffed = sniff_document(data)
        if sniffed is None or declared != sniffed:
            raise InvalidStateError("This file type is not allowed.")
        try:
            safe_name = display_filename(filename)
        except ValueError:
            raise InvalidStateError("The file name is not valid.") from None
        key = uuid.uuid4().hex
        document = ProtocolDocument(
            item_id=item.id,
            storage_key=key,
            content_type=sniffed,
            size_bytes=len(data),
            sha256=hashlib.sha256(data).hexdigest(),
            original_filename=safe_name,
            uploaded_by=actor.id,
        )
        self.storage.put(key, data)
        self.db.add(document)
        self.db.flush()
        self.audit.add(
            "document_uploaded",
            user_id=actor.id,
            extra={
                "yatra_id": str(yatra.id),
                "item_id": str(item.id),
                "document_id": str(document.id),
                "content_type": sniffed,
                "size_bytes": len(data),
            },
        )
        try:
            self.db.commit()
        except Exception:
            self.db.rollback()
            self.storage.delete(key)
            raise
        self.db.refresh(document)
        return self._document(document)

    def read_document(
        self,
        actor: User,
        yatra_id: uuid.UUID,
        item_id: uuid.UUID,
        document_id: uuid.UUID,
    ) -> tuple[bytes, ProtocolDocument]:
        _yatra, level = self._visible(actor, yatra_id)
        if level == "public":
            raise ForbiddenError()
        item = self.protocol.get_item(yatra_id, item_id)
        if item is None:
            raise NotFoundError("That protocol item was not found.")
        document = self.protocol.get_document(item.id, document_id)
        if document is None:
            raise NotFoundError("That document was not found.")
        return self.storage.read(document.storage_key), document

    def list_templates(self, actor: User) -> ProtocolTemplateList:
        if actor.role not in {ROLE_SUPER_ADMIN, ROLE_ADMIN}:
            raise ForbiddenError()
        return ProtocolTemplateList(items=[self._template(row) for row in self.protocol.templates()])

    def add_template(self, actor: User, payload: ProtocolTemplateCreate) -> ProtocolTemplateResponse:
        self._super(actor)
        template = ProtocolTemplate(
            category=payload.category,
            title=payload.title,
            description=payload.description,
            required=payload.required,
            sort_order=payload.sort_order,
            active=True,
        )
        self.db.add(template)
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("A checklist template with this title already exists.") from None
            raise
        self.db.refresh(template)
        return self._template(template)

    def update_template(
        self,
        actor: User,
        template_id: uuid.UUID,
        payload: ProtocolTemplateUpdate,
    ) -> ProtocolTemplateResponse:
        self._super(actor)
        template = self.protocol.get_template(template_id)
        if template is None:
            raise NotFoundError("That checklist template was not found.")
        if payload.title is not None:
            template.title = payload.title
        if payload.description is not None:
            template.description = payload.description
        if payload.required is not None:
            template.required = payload.required
        if payload.sort_order is not None:
            template.sort_order = payload.sort_order
        if payload.active is not None:
            template.active = payload.active
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            if is_unique_violation(exc):
                raise ConflictError("A checklist template with this title already exists.") from None
            raise
        self.db.refresh(template)
        return self._template(template)

    def _visible(self, actor: User, yatra_id: uuid.UUID):
        yatra = self.yatras.get(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        return yatra, level

    def _locked(self, actor: User, yatra_id: uuid.UUID, allowed: frozenset[str]):
        yatra = self.yatras.get_for_update(yatra_id)
        if yatra is None:
            raise NotFoundError("That Yatra was not found.")
        level = self.access.level(actor, yatra)
        if level is None:
            raise NotFoundError("That Yatra was not found.")
        if level not in allowed:
            raise ForbiddenError()
        return yatra, level

    def _item_for_update(self, yatra_id: uuid.UUID, item_id: uuid.UUID) -> ProtocolItem:
        item = self.protocol.get_item_for_update(yatra_id, item_id)
        if item is None:
            raise NotFoundError("That protocol item was not found.")
        return item

    def _items(self, yatra_id: uuid.UUID) -> list[ProtocolItemResponse]:
        rows = self.protocol.items(yatra_id)
        documents = self.protocol.documents_for([row.id for row in rows])
        return [self._item(row, documents.get(row.id, [])) for row in rows]

    @staticmethod
    def _ensure_open(yatra) -> None:
        if yatra.status == "archived":
            raise InvalidStateError("This Yatra is archived.")

    @staticmethod
    def _super(actor: User) -> None:
        if actor.role != ROLE_SUPER_ADMIN:
            raise ForbiddenError()

    @staticmethod
    def _item(item: ProtocolItem, documents: list[ProtocolDocument]) -> ProtocolItemResponse:
        now = datetime.now(UTC)
        overdue = bool(item.due_at is not None and item.due_at < now and item.status != "approved")
        return ProtocolItemResponse(
            id=item.id,
            category=item.category,
            title=item.title,
            description=item.description,
            required=item.required,
            status=item.status,
            due_at=item.due_at,
            overdue=overdue,
            responsible_label=item.responsible_label,
            submitted_at=item.submitted_at,
            reviewed_at=item.reviewed_at,
            rejection_reason=item.rejection_reason,
            documents=[ProtocolService._document(document) for document in documents],
            created_at=item.created_at,
            updated_at=item.updated_at,
        )

    @staticmethod
    def _document(document: ProtocolDocument) -> ProtocolDocumentResponse:
        return ProtocolDocumentResponse(
            id=document.id,
            original_filename=document.original_filename,
            content_type=document.content_type,
            size_bytes=document.size_bytes,
            created_at=document.created_at,
        )

    @staticmethod
    def _template(template: ProtocolTemplate) -> ProtocolTemplateResponse:
        return ProtocolTemplateResponse(
            id=template.id,
            category=template.category,
            title=template.title,
            description=template.description,
            required=template.required,
            sort_order=template.sort_order,
            active=template.active,
        )
