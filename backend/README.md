# Exhibition Portal API (POC)

Java 21 + Spring Boot 3.5 + PostgreSQL + Flyway. Draft and submit API for the visitor React app.

## Tests

`mvn test` uses **embedded PostgreSQL** (Zonky). Docker Desktop is not required for tests.

For `mvn spring-boot:run`, start Postgres first:

```bash
docker compose up -d db   # from repo root
```

## Start

```bash
docker compose up -d db
cd backend
mvn test
mvn spring-boot:run
```

API: `http://localhost:8080/api/v1`  
Health: `http://localhost:8080/actuator/health`  
Postgres: `localhost:5433` (user/password/db `exhibition`)

Then in `frontend/`: `npm run dev` (Vite proxies `/api` to port 8080).

## Visitor endpoints

| Method | Path |
|--------|------|
| POST | `/api/v1/inquiries` |
| GET | `/api/v1/inquiries/{id}` |
| PATCH | `/api/v1/inquiries/{id}` |
| POST | `/api/v1/inquiries/{id}/contact` |
| POST | `/api/v1/inquiries/{id}/submit` |
| POST | `/api/v1/inquiries/{id}/files` |
| GET | `/api/v1/inquiries/{id}/files/{assetId}` |
| POST | `/api/v1/inquiries/{id}/consents` |
| GET | `/api/v1/inquiries/{id}/consents` |
| GET | `/api/v1/taxonomy/departments` |
| GET | `/api/v1/taxonomy/product-types` |

## Staff endpoints (HTTP Basic)

Seeded local users (password `poc-staff`): `reviewer@sarv.local`, `marketing@sarv.local`, `admin@sarv.local`. Not SSO.

| Method | Path |
|--------|------|
| GET | `/api/v1/staff/me` |
| GET | `/api/v1/staff/suppliers` |
| POST | `/api/v1/staff/suppliers/{id}/decisions` |
| GET | `/api/v1/staff/buyers` |
| POST | `/api/v1/staff/buyers/{id}/notes` |
| POST | `/api/v1/staff/exports` |
| GET | `/api/v1/staff/exports/{id}` |
| GET | `/api/v1/staff/exports/{id}/file` |

Files are stored under `exhibition.storage-root` (default `./var/exhibition-files`). PostgreSQL holds metadata only. Content allowlist is not an antivirus product. Location is not collected.

POC limits: no OCR, live CRM, or live vendor API. Outbox stubs: `poc-mailbox` / `poc-vendor-stub`. See `specs/BUILD-PLAN.md`.
