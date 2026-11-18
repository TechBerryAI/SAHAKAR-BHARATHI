from __future__ import annotations

import uuid

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.protocol import ProtocolDocument, ProtocolItem, ProtocolTemplate


class ProtocolRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def active_templates(self) -> list[ProtocolTemplate]:
        rows = self.db.scalars(
            select(ProtocolTemplate)
            .where(ProtocolTemplate.active.is_(True))
            .order_by(ProtocolTemplate.sort_order.asc(), ProtocolTemplate.title.asc())
        )
        return list(rows)

    def templates(self) -> list[ProtocolTemplate]:
        rows = self.db.scalars(
            select(ProtocolTemplate).order_by(ProtocolTemplate.sort_order.asc(), ProtocolTemplate.title.asc())
        )
        return list(rows)

    def get_template(self, template_id: uuid.UUID) -> ProtocolTemplate | None:
        return self.db.get(ProtocolTemplate, template_id)

    def items(self, yatra_id: uuid.UUID) -> list[ProtocolItem]:
        rows = self.db.scalars(
            select(ProtocolItem)
            .where(ProtocolItem.yatra_id == yatra_id)
            .order_by(ProtocolItem.category.asc(), ProtocolItem.created_at.asc(), ProtocolItem.title.asc())
        )
        return list(rows)

    def get_item(self, yatra_id: uuid.UUID, item_id: uuid.UUID) -> ProtocolItem | None:
        return self.db.scalar(select(ProtocolItem).where(ProtocolItem.id == item_id, ProtocolItem.yatra_id == yatra_id))

    def get_item_for_update(self, yatra_id: uuid.UUID, item_id: uuid.UUID) -> ProtocolItem | None:
        return self.db.scalar(
            select(ProtocolItem)
            .where(ProtocolItem.id == item_id, ProtocolItem.yatra_id == yatra_id)
            .with_for_update()
        )

    def required_open(self, yatra_id: uuid.UUID) -> bool:
        count = self.db.scalar(
            select(func.count())
            .select_from(ProtocolItem)
            .where(
                ProtocolItem.yatra_id == yatra_id,
                ProtocolItem.required.is_(True),
                ProtocolItem.status != "approved",
            )
        )
        return int(count or 0) > 0

    def documents_for(self, item_ids: list[uuid.UUID]) -> dict[uuid.UUID, list[ProtocolDocument]]:
        grouped: dict[uuid.UUID, list[ProtocolDocument]] = {item_id: [] for item_id in item_ids}
        if not item_ids:
            return grouped
        rows = self.db.scalars(
            select(ProtocolDocument)
            .where(ProtocolDocument.item_id.in_(item_ids))
            .order_by(ProtocolDocument.created_at.asc())
        )
        for document in rows:
            grouped.setdefault(document.item_id, []).append(document)
        return grouped

    def get_document(self, item_id: uuid.UUID, document_id: uuid.UUID) -> ProtocolDocument | None:
        return self.db.scalar(
            select(ProtocolDocument).where(
                ProtocolDocument.id == document_id,
                ProtocolDocument.item_id == item_id,
            )
        )
