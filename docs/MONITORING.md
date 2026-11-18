# Monitoring

Nothing in this repository sends metrics or pages anyone. Request IDs and
`audit_events` are the signals that exist today. The lists below are the
production requirements. Treat every alert as **recommended, not configured**
until an operator connects a checker and names who receives it.

`/health/live` does not touch the database. It returns `{"status":"ok","database":"unchecked"}`.
`/health` and `/health/ready` run `SELECT 1` and return `status` and `database`
only (`ok` / `unavailable`). They do not return configuration, row counts, or
errors from PostgreSQL. Use `/health/live` for "process is up" and `/health`
for "process can reach the database".

Suggested receiver: the person who holds the Render and Supabase accounts.
There is no on-call rotation in this repository.

## What to watch

| Signal | Where | Suggested threshold | Status |
|---|---|---|---|
| API availability | `GET /health/live` from outside Render | 2 failures in 5 minutes | Not configured |
| API latency | External probe of `GET /health/live`, and Render's own response-time graph | p95 over 5 seconds for 10 minutes, excluding the known free-tier cold start | Not configured |
| Database connectivity | `GET /health` | 2 failures in 5 minutes | Not configured |
| HTTP 5xx rate | Render service logs, status code | more than 5 responses with status 500 in 10 minutes | Not configured |
| Database failures | Log line `Readiness check failed` | any occurrence that lasts more than 2 minutes | Not configured |
| Rate limiter failures | Log `Shared rate limiter unavailable` | any occurrence. The API returns 503 rather than skipping the limit | Not configured |
| OTP request volume | `audit_events.event_type = 'otp_requested'` | more than 100 in 10 minutes | Not configured |
| OTP delivery failures | `otp_delivery_failed`, or log `phone verification delivery failed` | more than 5 in 15 minutes | Not configured |
| OTP abuse | HTTP 429 on `POST /api/v1/phone-verifications` | more than 50 in 15 minutes from the logs. A 429 is expected under abuse; page when it is sustained | Not configured |
| Login failures | `audit_events.event_type = 'login_failure'` | more than 50 in 15 minutes | Not configured |
| Authorization failures | HTTP 403 on `/api/v1` | more than 20 in 15 minutes | Not configured |
| Session anomalies | `login_success` followed by a surge of 401 on `/api/v1/me`, or many `logout` rows | more than 30 unexpected 401s in 15 minutes | Not configured |
| Registration failures | HTTP 500 on `POST /api/v1/registrations` | any 500 | Not configured |
| SMS provider failures and timeouts | Log `phone verification delivery failed` (exception type or operator string only) | more than 5 in 15 minutes | Not configured |
| Retention job | Render cron `sahakar-bharati-retention` logs `retention cleanup failed` | any failure | Not configured |
| Backup or restore | Supabase dashboard backup status. This API does not emit a backup event | a failed backup, or a missed daily backup | Not configured. Supabase must show it |
| Deployment failures | Render deploy events for `sahakar-bharati-api` | any failed deploy | Not configured. Render emails the account owner if notifications are on |

Logs include the request id, the canonical client IP (`CF-Connecting-IP`, or the TCP peer when that header is absent), and `cf_ray` when the header matches Cloudflare's ray format. A malformed `CF-Ray` is stored as `-` and is not written back to the client. Logs must not include the OTP, PIN, bearer token, verification token, SMS API key, database password, or raw mobile number. Audit rows store `identifier_hash` (SHA-256 of the identifier: a pseudonym, not anonymization), not the mobile number.

Connect a checker the account owner will actually read (Render log alerts, or an external probe such as the one Render names in its uptime guide). Nothing in this repository sends those alerts. Render's own health check already calls `/health`. An external probe of `/health/live` and `/health` is the availability pair. Backup failures are visible only in the Supabase dashboard.
