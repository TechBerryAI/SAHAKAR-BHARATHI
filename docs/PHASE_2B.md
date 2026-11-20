# Phase 2B handoff

This file is a design note. None of it is implemented.

Phase 2A already has a Yatra, ordered stops, and one route-foundation row (label and optional geometry text). That geometry is not a live position.

Recommended next model:

```text
Yatra
  ↓
YatraStop
  ↓
Counter / Booth linkage
  ↓
LivePosition
```

A counter is a new record. It should point at a Yatra and, when the product needs it, at one stop and one existing booth (`places`). Do not overload `yatra_stops.place_id` until that link is a real product decision. QR passes and QR check-in stay out until the counter exists.

Live positions should be both:

- an append-only `yatra_positions` history (Yatra id, optional stop id, latitude, longitude, accuracy, `captured_at`, `received_at`, source/device)
- a `yatra_latest_positions` row per Yatra for the map read

Authorization stays the Phase 2A assignment model. Participants do not write positions. Writers are assigned Yatra staff or a later device role. Reads of the latest point can follow the same visibility rule as the Yatra (404 when the Yatra is unseen).

Keep rate limiting in PostgreSQL, scoped by Yatra and device. Reject positions older than a documented window, and mark a latest point stale when `captured_at` is older than that window instead of deleting history. Retention of history is an operator policy (for example 30 days) and is not a schema default.

Do not store this in the route geometry text. Do not add the table until Phase 2B starts.
