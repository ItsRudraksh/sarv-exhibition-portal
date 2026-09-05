# Sarv Biolabs Exhibition Portal

Reusable QR and website inquiry portal: **I want to sell** (supplier intake) and **I want to buy** (customer lead).

**Start here:** [specs/README.md](specs/README.md) · product [specs/PLATFORM_CONTEXT.md](specs/PLATFORM_CONTEXT.md) · delivery [specs/BUILD-PLAN.md](specs/BUILD-PLAN.md) · public Windows host [specs/DEPLOY-WINDOWS.md](specs/DEPLOY-WINDOWS.md)

| Path | Role |
|---|---|
| `specs/` | Product, database, build plan, testing, Windows/Jenkins deploy |
| `frontend/` | React + Vite visitor app + `/staff` (session pointer; no PII in localStorage) |
| `backend/` | Java 17 Spring Boot (Flyway V1–V7); `prod` profile is fail-closed |
| `Jenkinsfile` | npm + Maven (Java17) + Windows service |
| `deploy/windows/` | Native MySQL 3306, Windows service, `http://43.225.195.200/` |
| `raw/`, `.stitch/`, `concepts/` | Historical HLD, brochure, and design exports |

## Local development

```bash
cd backend
mvn test
.\run.ps1
```

In another terminal: `cd frontend && npm run dev`.

Needs **native MySQL 8** on `localhost:3306`. `mvn test` uses embedded MariaDB.

UI: `https://localhost:5173` · staff: `https://localhost:5173/staff` · API: `http://localhost:8080`.

## Production (Windows)

Profile `prod`: requires `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (not `poc-staff` / `change-me-staff`), `exhibition.poc=false`, receipt prefix `EP-`, Excel `.xlsx` lead export. Outbox still writes **local stub files** until CRM/vendor APIs are chosen.

Runbook: [specs/DEPLOY-WINDOWS.md](specs/DEPLOY-WINDOWS.md).

Still open (do not invent): live CRM/vendor, cloud OCR/voice, product catalogue, public HTTPS for camera.
