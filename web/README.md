# Web

Vite + React 19 + TypeScript participant app, installable as a PWA, hosted on
Firebase Hosting (`sahakarbharati-e3b04`). All data goes through the FastAPI API.
Firebase Auth and Firestore are not used; `firebase.json` is hosting-only.

## Setup

Node 22+ (CI uses 22; Vite 8 does not run on Node 18).

From the repository root, copy `web/.env.example` to `web/.env.local` and set `VITE_API_BASE_URL=http://127.0.0.1:8000`. Then:

```bash
make install-web
make web
```

`make start` runs this app together with the API.

## Checks

From the repository root:

```bash
make lint-web
make test-web
```

The production bundle is built from `web/` with a public HTTPS API origin:

```bash
cd web
VITE_API_BASE_URL=https://sahakar-bharathi-api.onrender.com npm run build
```

## Configuration

| Variable | Where | Rule |
|---|---|---|
| `VITE_API_BASE_URL` | `.env.local` for dev; CI env for builds | Production builds fail unless it is an `https://` origin (no path) that is not local or a placeholder. |

The value is compiled into the bundle; it is public, not a secret.

## Security and sessions

- The bearer token is stored in `localStorage` (the API has no cookie sessions).
  Any script that runs on the page can read `sahakar_access_token`. A
  Content-Security-Policy (`script-src 'self'`, `connect-src` limited to the API
  origin) is injected at build time and repeated as a Firebase Hosting header.
  That policy blocks extra script sources. It does **not** make `localStorage`
  equivalent to an HttpOnly cookie: a script that is allowed by the policy, or
  an XSS that bypasses it, can still read the token. HttpOnly, Secure,
  SameSite cookies are a later change, not this phase.
  Other mitigations: `X-Frame-Options: DENY`, no `dangerouslySetInnerHTML`/`eval`,
  API responses sent with `Cache-Control: no-store`, logout clears the token
  and revokes the server session, and a 401 clears the saved session. Tokens
  are not written to application logs.
- API cold start (`src/lib/network.ts`): each attempt waits up to 75 s, GET requests
  are retried after a network failure, timeout or 502/503/504 (2 s, then 5 s), and
  POST/PATCH/DELETE are sent once. Any request pending over 4 s shows a "server is
  starting up" banner (`serverWaking`, translated in all 26 languages; the Santali,
  Manipuri, Bodo, Sanskrit, Kashmiri, Konkani, Dogri and Tulu wording should be
  checked by a native speaker).
- A 401 from any authenticated call clears the session. Logout clears locally and
  revokes on the server (`keepalive` request).
- If a saved session cannot be checked at start-up (offline / server error), the app
  shows a retry screen and keeps the token instead of appearing signed out.
- If registration succeeds but the automatic sign-in fails, the participant is sent
  to Sign in with the new Login ID prefilled.

## PWA

- Installable shell (manifest, icons, service worker via `vite-plugin-pwa`, `autoUpdate`).
- Only static assets are precached. **Nothing works offline**: sign-in, registration
  and profile need the API. API responses are never cached by the service worker.
- `firebase.json` serves everything `no-cache` except hashed `/assets/**`
  (immutable), so a new deploy is picked up on the next load.

See `docs/DEPLOYMENT.md` for deploy and rollback.
