import uuid
from urllib.parse import quote

from fastapi import APIRouter, Depends, Request
from fastapi.responses import Response

from app.core.deps import get_current_user, get_protocol_service, require_super_admin
from app.models.protocol import ProtocolDocument
from app.models.user import User
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
from app.services.protocol import ProtocolService

router = APIRouter(tags=["protocol"])


def _attachment(document: ProtocolDocument) -> dict[str, str]:
    fallback = "".join(
        char if 32 <= ord(char) < 127 and char not in {'"', "\\"} else "_"
        for char in document.original_filename
    )
    if not fallback.strip("._"):
        fallback = "document"
    encoded = quote(document.original_filename)
    return {
        "Content-Disposition": f"attachment; filename=\"{fallback}\"; filename*=UTF-8''{encoded}",
        "Cache-Control": "private, no-store",
        "X-Content-Type-Options": "nosniff",
    }


@router.get("/admin/protocol-templates", response_model=ProtocolTemplateList)
def list_protocol_templates(
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolTemplateList:
    return service.list_templates(actor)


@router.post("/admin/protocol-templates", response_model=ProtocolTemplateResponse, status_code=201)
def create_protocol_template(
    payload: ProtocolTemplateCreate,
    actor: User = Depends(require_super_admin),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolTemplateResponse:
    return service.add_template(actor, payload)


@router.patch("/admin/protocol-templates/{template_id}", response_model=ProtocolTemplateResponse)
def update_protocol_template(
    template_id: uuid.UUID,
    payload: ProtocolTemplateUpdate,
    actor: User = Depends(require_super_admin),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolTemplateResponse:
    return service.update_template(actor, template_id, payload)


@router.get("/yatras/{yatra_id}/protocol", response_model=ProtocolList)
def list_protocol(
    yatra_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolList:
    return service.list_items(actor, yatra_id)


@router.post("/yatras/{yatra_id}/protocol/items", response_model=ProtocolItemResponse, status_code=201)
def create_protocol_item(
    yatra_id: uuid.UUID,
    payload: ProtocolItemCreate,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolItemResponse:
    return service.add_item(actor, yatra_id, payload)


@router.patch("/yatras/{yatra_id}/protocol/items/{item_id}", response_model=ProtocolItemResponse)
def update_protocol_item(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    payload: ProtocolItemUpdate,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolItemResponse:
    return service.update_item(actor, yatra_id, item_id, payload)


@router.post("/yatras/{yatra_id}/protocol/items/{item_id}/submit", response_model=ProtocolItemResponse)
def submit_protocol_item(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolItemResponse:
    return service.submit(actor, yatra_id, item_id)


@router.post("/yatras/{yatra_id}/protocol/items/{item_id}/review", response_model=ProtocolItemResponse)
def review_protocol_item(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolItemResponse:
    return service.review(actor, yatra_id, item_id)


@router.post("/yatras/{yatra_id}/protocol/items/{item_id}/approve", response_model=ProtocolItemResponse)
def approve_protocol_item(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolItemResponse:
    return service.approve(actor, yatra_id, item_id)


@router.post("/yatras/{yatra_id}/protocol/items/{item_id}/reject", response_model=ProtocolItemResponse)
def reject_protocol_item(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    payload: ProtocolReject,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolItemResponse:
    return service.reject(actor, yatra_id, item_id, payload)


@router.post(
    "/yatras/{yatra_id}/protocol/items/{item_id}/documents",
    response_model=ProtocolDocumentResponse,
    status_code=201,
)
async def upload_protocol_document(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    request: Request,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> ProtocolDocumentResponse:
    body = await request.body()
    return service.add_document(
        actor,
        yatra_id,
        item_id,
        body,
        request.headers.get("content-type", ""),
        request.headers.get("x-document-filename", ""),
    )


@router.get("/yatras/{yatra_id}/protocol/items/{item_id}/documents/{document_id}")
def download_protocol_document(
    yatra_id: uuid.UUID,
    item_id: uuid.UUID,
    document_id: uuid.UUID,
    actor: User = Depends(get_current_user),
    service: ProtocolService = Depends(get_protocol_service),
) -> Response:
    data, document = service.read_document(actor, yatra_id, item_id, document_id)
    return Response(content=data, media_type=document.content_type, headers=_attachment(document))
