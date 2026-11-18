"""Yatra and protocol values. The database checks use the same literals."""

from __future__ import annotations

YATRA_STATUSES: tuple[str, ...] = (
    "draft",
    "planned",
    "approval_pending",
    "approved",
    "upcoming",
    "in_progress",
    "completed",
    "archived",
)

# One forward step. Callers cannot skip or reverse a state.
YATRA_TRANSITIONS: dict[str, str] = {
    "draft": "planned",
    "planned": "approval_pending",
    "approval_pending": "approved",
    "approved": "upcoming",
    "upcoming": "in_progress",
    "in_progress": "completed",
    "completed": "archived",
}

PUBLISHABLE_STATUSES: frozenset[str] = frozenset({"approved", "upcoming", "in_progress", "completed"})

PROTOCOL_CATEGORIES: tuple[str, ...] = ("pre", "during", "post")

PROTOCOL_STATUSES: tuple[str, ...] = (
    "pending",
    "submitted",
    "under_review",
    "approved",
    "rejected",
    "resubmitted",
)

# Statuses that still accept a document before a reviewer takes the item.
DOCUMENT_OPEN_STATUSES: frozenset[str] = frozenset({"pending", "rejected", "submitted", "resubmitted"})

SUBMIT_TARGETS: dict[str, str] = {"pending": "submitted", "rejected": "resubmitted"}
REVIEW_SOURCES: frozenset[str] = frozenset({"submitted", "resubmitted"})

ALLOWED_DOCUMENT_TYPES: frozenset[str] = frozenset({"application/pdf", "image/jpeg", "image/png"})

MANAGE_ACCESS: frozenset[str] = frozenset({"district_admin", "super_admin"})
OPERATE_ACCESS: frozenset[str] = frozenset({"yatra_admin", "district_admin", "super_admin"})

YATRA_STATUS_SQL = (
    "status IN ('draft', 'planned', 'approval_pending', 'approved', "
    "'upcoming', 'in_progress', 'completed', 'archived')"
)
PROTOCOL_CATEGORY_SQL = "category IN ('pre', 'during', 'post')"
PROTOCOL_STATUS_SQL = (
    "status IN ('pending', 'submitted', 'under_review', 'approved', 'rejected', 'resubmitted')"
)
DOCUMENT_TYPE_SQL = "content_type IN ('application/pdf', 'image/jpeg', 'image/png')"
