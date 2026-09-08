# Exhibition Portal API (POC)

Java 17 + Spring Boot 3.5 + **MySQL 8** + Flyway. Draft and submit API for the visitor React app. Same JDK and database engine as pharma-erp (`17.0.x`, MySQL on 3306).

## Tests

`mvn test` uses **embedded MariaDB** (mariaDB4j). Docker is not required.

For `mvn spring-boot:run` or `.\run.ps1`, use **native MySQL 8** on `localhost:3306` (database `exhibition_portal`, user `exhibition` / password `exhibition`). See `deploy/windows/init-mysql.sql`.

If Flyway reports **failed migration to version 1**, do not run `repair` on a half-created local schema. Drop the empty local tables and start again so V1–V7 (including seed) re-apply:

```powershell
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
& $mysql -u exhibition -p exhibition_portal
```

Then in `mysql`:

```sql
SET FOREIGN_KEY_CHECKS = 0;
SET GROUP_CONCAT_MAX_LEN = 1000000;
SELECT GROUP_CONCAT(CONCAT('`', table_name, '`')) INTO @tables
FROM information_schema.tables WHERE table_schema = 'exhibition_portal';
SET @sql = IF(@tables IS NULL, 'SELECT 1', CONCAT('DROP TABLE IF EXISTS ', @tables));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET FOREIGN_KEY_CHECKS = 1;
```

`'exhibition'@'127.0.0.1'` is optional for local `.\run.ps1` (`localhost` is enough). Create it only if `ALTER USER` error 1396 appears; `CREATE USER IF NOT EXISTS` first.

## Configuration

All tunable settings live in properties files (YAML removed to keep one source of truth):

| File | When used |
|------|-----------|
| `src/main/resources/application.properties` | Local / default |
| `src/main/resources/application-prod.properties` | `spring.profiles.active=prod` |
| `src/main/resources/application-ci.properties` | CI profile |
| `src/test/resources/application-test.properties` | `@ActiveProfiles("test")` — outbox stubs for `mvn test` |

Do **not** put a second `application.properties` under `src/test/resources/` — on the test classpath it replaces the main file (same resource name). Use `application-test.properties` instead.

Override any key with the matching environment variable (`DATASOURCE_*`, `EXHIBITION_*`, `SERVER_PORT`). Host example: `deploy/windows/portal.env.example.ps1`. On Windows service, Java also loads `portal.env.ps1` from the install working directory on boot (so JDBC flags do not stay stuck in a stale WinSW XML). When `exhibition.pharma-erp.enabled=true`, boot auto-syncs finished goods from `pharmadb`.

Notable keys: `exhibition.poc`, `exhibition.cors-origins`, `exhibition.storage-root`, file size caps, outbox destinations, `exhibition.staff-bootstrap-password`, and `exhibition.pharma-erp.*` (buyer finished-goods DB sync).

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
