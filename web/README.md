# Web

Vite + React participant app. Application data goes through FastAPI, not Firestore.

| Environment | `VITE_API_BASE_URL` |
|---|---|
| Local | `http://127.0.0.1:8000` — copy `.env.example` to `.env.local` |
| Production build | `https://sahakar-bharathi-api.onrender.com` — set in GitHub Actions |

```bash
cp .env.example .env.local
npm ci
npm run lint
npm run typecheck
npm run build
npm run dev
```

Sign in uses login ID or mobile and a 4–6 digit PIN. Firebase Hosting serves the static `dist/` build. Project: `sahakarbharati-e3b04`.
