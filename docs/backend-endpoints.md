# Backend Endpoints

All of these except `/health` require auth (Google ID token in `Authorization: Bearer`).

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /health | Health check (no auth) |
| GET | /me | Current user (auth required) |
| GET | /chat/history | List of chat messages (role, content, created_at) |
| POST | /chat | Main entry for logging, querying, corrections, etc. |
| GET | /sessions | List workout sessions |
| GET | /sessions/{id} | Session detail with log entries and sets |
| GET | /exercises | List categories and variants |
| GET | /query | History by exercise |
| GET | /prs | User's personal records |
| GET | /prs/{id}/image | Redirect to presigned PR image URL (when R2 is configured) |

There is no dedicated "backend endpoints list" API. There's no route like `GET /endpoints` that returns the list of available endpoints. If needed, that would be a new endpoint (e.g. `GET /endpoints` or `GET /admin/endpoints`) that returns this table in JSON.
