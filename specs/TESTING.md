# Testing and verification

**Updated:** 9 September 2026 (supplier Other + free-text offering; multi-file 5 MB attachments)

After every substantive change: reproduce (if a bug) → fix → run the commands below → update specs. Runtime parity: lint/build is not a browser walkthrough. See `.cursor/rules/runtime-parity-definition-of-done.mdc`.

## Frontend (`frontend/`)

```bash
npm install    # if node_modules is missing
npm run lint
npm run build
npm run dev    # https://localhost:5173 — HTTPS required for in-page camera on phones
```

Vite proxies `/api` to `http://localhost:8080`. Loads `GET /api/v1/meta` — prototype banners only when `poc: true`.

**Entry URLs:** `https://localhost:5173/?c=POC-STALL-1` (stall), `https://localhost:5173/web` (website), `?channel=direct`, `?assist=1` (staff-assisted). Shared tablets show **Next visitor**.

**Phone on the same Wi‑Fi:** `npm run dev` (Vite `--host`, HTTPS) + elevated `.\scripts\allow-vite-lan.ps1` (TCP 5173). Open the Vite **Network** URL, e.g. `https://192.168.1.12:5173/?c=POC-STALL-1`. Accept the self-signed cert. Guest Wi‑Fi / AP isolation still fails. Default CORS allows `https://192.168.*.*:5173` and `https://10.*.*.*:5173` (Vite proxy is same-origin; CORS is backup). Do not open 5173 on the public Windows host.

## Backend (`backend/`)

```bash
mvn test
mvn spring-boot:run                          # default profile (poc=true)
# prod checks (expect fail without real password):
# mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Config is `.properties` only (`application.properties` / `application-prod.properties`). `mvn test` activates profile `test` (`application-test.properties`); do not add `src/test/resources/application.properties` (it shadows main). Keys: `backend/README.md` § Configuration.

Flyway V1–V11. Phase 8: `ProductionStartupGuardTest`, `MetaApiTest`; export is xlsx. Taxonomy: `TaxonomyApiTest`. Card assist: ZXing + `CLIENT_CARD_OCR`. Finished goods: `FinishedGoodsApiTest`; staff sync from pharmadb when `exhibition.pharma-erp.enabled=true` (local default is now `true`). Credentials alone do not fill the buy list — restart the API, then **Staff → Sync finished goods**. Trading/offline supplier products are tagged on `/admin` (`TradingCatalogueApiTest`). Per selected catalogue item, quantity is required (`InquiryRulesTest`). Supplier Other + capability notes (`InquiryRulesTest`). Supporting attachments 5 MiB / max 10 (`FileConsentAuditApiTest`, `FileContentRulesTest`). Admin staff CRUD: `StaffAdminApiTest` (`/api/v1/staff/users`, ADMIN only; deactivate not delete).

**Local MySQL first boot:** `.\run.ps1` needs native MySQL 3306 and user `exhibition` / `exhibition` (`deploy/windows/init-mysql.sql`). If Flyway says **failed migration to version 1**, the local schema already has tables from a prior attempt — drop those empty tables and re-run (see `backend/README.md`). Do not `flyway repair` that state: later `CREATE TABLE` migrations will fail and seed (V2/V7) will be missing. `mvn test` does not use this MySQL instance.


## Jenkins + public Windows host (`http://43.225.195.200/`)

Commands: [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md). **Java 17** only. Do not use `npm run dev` on the public IP.

Create a Jenkins Pipeline job from this repo’s **`Jenkinsfile`** (tools **`Java17`** and **`Maven3`**, same as pharma-erp). Node 22 must be visible to the **Jenkins Windows service** (`C:\Program Files\nodejs` or `NODE_HOME`); restart Jenkins after installing Node.

- **`poc` / `dev`:** Deploy staging → `C:\exhibition-portal-staging\exhibition-portal.jar`, service `exhibition-portal-staging`, health on port **8082** (8081 is pharma-erp-staging). First run needs real `DATASOURCE_PASSWORD` / `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD`. **Buy FG:** `GET /api/v1/finished-goods` stays `[]` until `EXHIBITION_PHARMA_ERP_*` is set in `portal.env.ps1` and the JAR that auto-syncs on boot is deployed (`pharmaErpEnabled` / `finishedGoodsActive` on `/api/v1/meta`). Git does not copy local catalogue rows ([DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md) §2b).
- **`main`:** Deploy production → `C:\exhibition-portal\`, health on port **80**.

**Public smoke:** `http://43.225.195.200/actuator/health` → visitor `/` (upload or continue without a card; in-page camera needs HTTPS) → `/staff` with the bootstrap password → `/admin` as `admin@sarv.local` to list/create staff accounts **and** tag offline/portal supplier products for buyers. MySQL must not be reachable on the public IP.

**Staging host diagnose:** `cd C:\exhibition-portal-staging` then `.\verify-staging.ps1` (or `verify-staging.cmd`). One command only. Placeholder `change-me-*` still fails Deploy. **`net start` NET 2186** means the old powershell-only service registration — fixed by WinSW in `install-service.ps1`. Health Check must not use PowerShell `"$i: ..."` (drive parse error); use `-f` formatting. Manual Option A still works; close that window (or kill orphan java) before Jenkins `net start` so port 8082 is free.

## Reply footer (agents)

State lint/build/tests (pass/fail), specs touched, audit impact (`workflow_events` / `audit_events` / N/A), and **Verification** (browser / API / manual steps).
