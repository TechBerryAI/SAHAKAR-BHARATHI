# Phase 2B

Phase 2B adds Yatra counters, QR attendance, and live GPS on top of Phase 2A. It does not add subsidies, notifications, WhatsApp, reports, or AI scheduling. It does not link a counter or a stop to a Phase 1 booth (`places`). There is no `place_id` on stops or counters. Live positions are not stored in `yatra_routes.geometry`.

Account roles stay `participant`, `admin`, and `super_admin`. Authorization stays the Phase 2A Yatra access check: an unseen Yatra is 404, and a visible Yatra the caller cannot operate is 403. `manage_counters` is the operate capability (not archived). `manage_trackers` is the manage capability (not archived). `view_track` is true for anyone who can see the Yatra.

Alembic head is `20261121_0013`. After upgrading, re-run `infra/supabase/runtime_role.sql` so `sahakar_api` receives the new grants, including `EXECUTE` on `prune_yatra_positions`. Historical migrations `0001` through `0012` are unchanged.

## Decisions (audit remediation)

**D1 — Attendance must prove presence.** A static placard alone is not enough. When `COUNTER_GEOFENCE_MODE=required` (production default), the scan body must include a fresh device fix (`latitude`, `longitude`, `accuracy_m`, `captured_at`). The server rejects inaccurate, stale, or out-of-fence fixes with stable sentences and consumes the counter-scan failure bucket. Counters cannot activate without coordinates. Staff can rotate a counter QR (`POST .../rotate-qr`); the old token stops immediately. Residual risk: GPS spoofing on rooted devices remains possible. Geofencing raises the bar; it is not proof.

**D2 — One active tracker per Yatra.** Partial unique index `uq_yatra_trackers_one_active`. A second active assignment returns 409 until the current tracker is revoked.

Freshness is computed from `captured_at`, not `received_at`. History uses keyset cursors (`before`), not offset/total. Offline phones upload via `POST .../positions/batch` (up to `POSITION_BATCH_MAX`). Idempotent replay of the same `client_position_id` returns the original row. Ingest requires only `in_progress` (not published). A referenced stop delete returns 409. GPS pruning runs only from the retention cron (`python -m app.core.retention`), never from the in-request limiter.

## Tables

| Table | Role |
|---|---|
| `yatra_counters` | A counter on one Yatra. Human `code` is unique per Yatra. `qr_token` is the raw placard secret, stored so staff can reprint it. New counters are inactive. There is no delete. |
| `yatra_attendance` | One row per user, counter, and Asia/Kolkata day. Append-only for the runtime role. |
| `yatra_trackers` | An assignment credential for a phone that sends GPS. The API stores only the SHA-256 hash. |
| `yatra_positions` | Append-only history. The server sets `received_at`. |
| `yatra_latest_positions` | One row per Yatra. It moves forward only when the new `captured_at` is at least the stored one. |

`sahakar_api` may select, insert, and update counters, trackers, and the latest-position row. Revoke updates the tracker. Attendance and positions are select and insert only. Supabase `anon` and `authenticated` have no policies on these tables.

## Counters and QR

`code` matches `^[A-Z0-9][A-Z0-9-]{1,15}$`. It is a label, not a scan credential. The placard text is `SB-COUNTER:` plus `secrets.token_urlsafe(32)`. Detail and create return `qr_text` only to a caller who can operate the Yatra. List payloads and audit extras omit the token. Logs redact `qr_token`, `qr_text`, and `tracker_token`. `POST .../counters/{id}/rotate-qr` issues a new token and audits `counter_qr_rotated` with ids only.

`POST /api/v1/yatras/{id}/counters/{counter_id}/activate` and `.../deactivate` change status. Activation while geofence mode is required needs coordinates. A duplicate code is 409. A stop from another Yatra is 400. An archived Yatra rejects mutations with "This Yatra is archived."

## Attendance

`POST /api/v1/yatras/{id}/attendance` with `qr_token` plus the scan fix records attendance. The Yatra must be published and `in_progress`, and the counter must be active. Roster filters: `GET .../attendance?attendance_date=&counter_id=`. Participants use `GET .../attendance/mine`.

| Case | Result |
|---|---|
| Malformed payload | 422 "That QR code is not a counter." |
| Unknown token, or a token from another Yatra | 404 "That counter was not found." |
| Unpublished or unseen Yatra | 404 |
| Published, but not `in_progress` | 400 "This Yatra is not accepting attendance." |
| Inactive counter | 400 "This counter is not accepting attendance." |
| First insert and the same user, counter, and Kolkata day | Both HTTP 200. `already_recorded` is false, then true. |

Audit `attendance_recorded` is written only on the first insert, with `yatra_id`, `counter_id`, and `attendance_id`. Failures consume `counter-scan:{user.id}` (default 10 per 600 seconds). Success and duplicates do not. This route is not `POST /api/v1/attendance`.

## Trackers and GPS

Assign, list, and revoke are manage actions. The raw tracker bearer is returned once on create. Later calls authenticate the hash with the existing session-token hasher. A revoked or unknown tracker, and a user session presented to `POST .../positions`, are 401 "Session is invalid or expired." A path Yatra that is not the tracker's Yatra is 404 and writes nothing.

Ingest requires Yatra status `in_progress`. Published is not required. An optional `stop_id` must belong to that Yatra. Accuracy above `POSITION_MAX_ACCURACY_M` (100) is 422 "Accuracy is too large." A fix more than 30 seconds in the future, or older than 600 seconds, is 400. The first accept is 201. The same `(tracker_id, client_position_id)` is 200 and is checked before the position rate bucket, so a retry of an accepted fix stays 200 after the bucket is full. New fixes use `position:{tracker_id}` at 12 per 60 seconds. Unknown or revoked callers also use `position-ip:{client_ip}` at 30 per 60 seconds. Position ingest does not write audit rows.

Latest-position freshness uses `received_at`: live at or under 90 seconds, stale at or under 600 seconds, otherwise offline. A revoked tracker's latest read is offline with null coordinates. History is operate-only, `limit` at most 50 and `offset` at most 2000. Latest is readable by anyone who can see the Yatra.

`python -m app.core.retention` deletes `yatra_positions` older than `POSITION_RETENTION_DAYS` (30) unless that row is still the latest position. It does not delete attendance, the latest-position row, or audit. `sahakar_api` cannot delete positions. That statement is isolated, so the daily cron still cleans sessions when it runs as the runtime role. Pruning GPS history needs an owner connection.

## Settings

| Setting | Default |
|---|---|
| `COUNTER_SCAN_RATE_LIMIT` | 10 |
| `COUNTER_SCAN_RATE_WINDOW_SECONDS` | 600 |
| `POSITION_RATE_LIMIT` | 12 |
| `POSITION_RATE_WINDOW_SECONDS` | 60 |
| `POSITION_AUTH_IP_RATE_LIMIT` | 30 |
| `POSITION_MAX_FUTURE_SECONDS` | 30 |
| `POSITION_MAX_AGE_SECONDS` | 600 |
| `POSITION_STALE_AFTER_SECONDS` | 90 |
| `POSITION_OFFLINE_AFTER_SECONDS` | 600 |
| `POSITION_MAX_ACCURACY_M` | 100 |
| `POSITION_RETENTION_DAYS` | 30 |

## Clients

Web Yatra screens show counters when `manage_counters` is true, tracker assign and revoke when `manage_trackers` is true, and a position panel when `view_track` is true. The panel polls `GET /api/v1/yatras/{id}/position` every 15 seconds and links to OpenStreetMap. It does not embed a map. History is on the same screen for operators. The tracker code is shown once and is not written to `localStorage`. Participants mark attendance at `/yatras/:yatraId/attendance`, which posts `qr_token` and does not call the booth attendance route.

Android adds destinations `yatra_attendance` and `yatra_tracking`. The scanner accepts only `SB-COUNTER:` payloads. Success and "already recorded" are shown after the HTTP response. The tracker bearer is stored in the same encrypted preferences as the user session, under a separate key. A 401 for that bearer clears the tracker key only when it is not the stored user token. Unsent fixes sit in a local file, capped at 50, dropping the oldest. A retry reuses `client_position_id`. HTTP 200 and 201 remove the fix, 400 and 422 drop it, and 429 or a network failure keeps it.

## Still absent

Subsidies, notifications, WhatsApp, reports, and AI scheduling or checklists are not in this release. There is no PostGIS, Redis, WebSocket, queue service, or map SDK.
