# Phase 2A: Yatra, districts, and protocol

Phase 2A is implemented in this repository. Phase 2 overall is not. This file describes the code that is here. It does not describe a live deploy, and it does not enable QR counters, live GPS, subsidies, notifications, WhatsApp, or AI scheduling.

## Domain

A district is an administrative record (`districts`: name, unique code, optional `state_name`, active flag). It is not linked to the free-text `users.district` profile field, and there is no state table.

A Rath Yatra belongs to one district. The server generates `public_code` (`YT` plus eight digits). Clients cannot set `status` on create or patch. `POST /api/v1/yatras/{id}/status` is the only transition. Yatras are archived, not deleted. Districts are deactivated, not deleted while Yatras reference them.

Stops (`yatra_stops`) are ordered points on a Yatra. They are not booths: there is no `place_id`. One `yatra_routes` row per Yatra stores a label and optional geometry text. There are no positions, GPS readings, or a routing engine.

## RBAC

Account roles stay `participant`, `admin`, and `super_admin`. Phase 1 staff access to people, booths, and attendance stays global. Yatra authority is a separate assignment, checked on the server for every Yatra read and write.

| Caller | Districts and Yatras |
|---|---|
| `super_admin` | Every district and Yatra. Creates districts, assigns district administrators, manages protocol templates, and can review protocol. |
| District administrator | An `admin` with a row in `district_administrators`. Scope is those districts. Creates and edits Yatras there, changes lifecycle, publishes, assigns Yatra administrators, and reviews protocol. A global `admin` with no row cannot create or edit Yatras. |
| Yatra administrator | Any active user with a row in `yatra_administrators`. Manages that Yatra’s stops, route label, and protocol submissions. Does not gain People, Booths, district-wide access, lifecycle changes, publish, or approval. |
| Participant | Published Yatras only. |

A Yatra the caller cannot see returns 404. A Yatra they can see, with an action they cannot take, returns 403.

## Lifecycle

```text
draft → planned → approval_pending → approved → upcoming → in_progress → completed → archived
```

There are no reverse transitions. `approval_pending` becomes `approved` only when every required protocol item is `approved`. Publish is allowed at `approved`, `upcoming`, `in_progress`, or `completed`.

## Protocol

Active `protocol_templates` are copied onto a Yatra when it is created. Seeded items:

- Pre: Police Permission, Traffic Plan, Health Department Approval, Fire Safety Clearance.
- During: Crowd Management Plan, Medical Camps Setup, Volunteer Deployment.
- Post: the category exists. No items are invented for it.

Item states: `pending` → `submitted` → `under_review` → `approved`, or `under_review` → `rejected` → `resubmitted` → `under_review`. Submit moves `pending` to `submitted` and `rejected` to `resubmitted`. Review, approve, and reject are district-administrator or super-admin actions. `due_at` in the past is an `overdue` flag on the response, not a background job.

## Documents

`protocol_documents` stores metadata (content type, size, sha256, display filename, uploader). Bytes go through a `DocumentStorage` port. The development and test adapter writes them under a server-generated key on local disk (`backend/var/documents` unless `DOCUMENT_STORAGE_DIR` is set). That directory is ephemeral. It is not a production object store, and it is not backed up with PostgreSQL.

The global request limit stays 64 KiB. Only `POST /api/v1/yatras/{id}/protocol/items/{item_id}/documents` may accept up to 5 MiB, as a raw body with `Content-Type` and `X-Document-Filename`. Allowed types are PDF, JPEG, and PNG, checked by magic bytes. Production with an empty `DOCUMENT_STORAGE_BACKEND` rejects uploads. Download is an authorized API response (`Cache-Control: private, no-store`). The API never returns a public URL, and audit rows do not store the storage key or the filename.

## Clients

Web admin, in the same shell as People and Booths, lists districts (super admin), Yatras, stops, lifecycle, publish, and the protocol checklist. Buttons follow the capabilities on the Yatra payload. Android, from Home, shows a read-only list of published Yatras and a detail screen with dates, district, status, and ordered active stops. It does not administer protocol.

## Deferred

Phase 2B and later: QR and counter attendance, live GPS, a nullable stop-to-booth link, a positions table, subsidy, notifications, WhatsApp, AI scheduling, and a real object-storage provider. Those can be added without rewriting `yatras`, `yatra_stops`, or `yatra_routes`.
