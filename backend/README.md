# Exhibition Portal API (POC)

Java 17 + Spring Boot 3.5 + **MySQL 8** + Flyway. Draft and submit API for the visitor React app. Same JDK and database engine as pharma-erp (`17.0.x`, MySQL on 3306).

## Tests

`mvn test` uses **embedded MariaDB** (mariaDB4j). Docker is not required.

For `mvn spring-boot:run` or `.\run.ps1`, use **native MySQL 8** on `localhost:3306` (database `exhibition_portal`, user `exhibition`). See `deploy/windows/init-mysql.sql`.

## Start

```bash
cd backend
mvn test
.\run.ps1
```

API: `http://localhost:8080/api/v1`  
Health: `http://localhost:8080/actuator/health`  
MySQL: `localhost:3306`

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
| POST | `/api/v1/inquiries/{id}/extractions` |
| GET | `/api/v1/inquiries/{id}/extractions/latest` |
| GET | `/api/v1/campaigns/{code}` |
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

Files are stored under `exhibition.storage-root` (default `./var/exhibition-files`). MySQL holds metadata only. Content allowlist is not an antivirus product. Location is not collected.

POC limits: local card-QR assist only (not cloud OCR), no live CRM or live vendor API. Outbox stubs: `poc-mailbox` / `poc-vendor-stub`. Create body may include `entryChannel`, `campaignCode`, `staffAssisted`. See `specs/BUILD-PLAN.md`.

## Public Windows Server + Jenkins

Java **17** only. Native MySQL on **3306**. Jenkinsfile at the repo root uses the same **Java17** / **Maven3** tool ids as pharma-erp and deploys with `net stop` / `net start` on Windows service `exhibition-portal`. Runbook: [specs/DEPLOY-WINDOWS.md](../specs/DEPLOY-WINDOWS.md).
