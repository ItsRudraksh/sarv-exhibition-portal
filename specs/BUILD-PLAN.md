# Exhibition Portal — project build plan

**Status:** Phases 1–8 complete; **business taxonomy v1** (Flyway V7) loaded. Live CRM/vendor/OCR and product catalogue remain open.  
**Updated:** 5 September 2026  
**Product SSOT:** [PLATFORM_CONTEXT.md](PLATFORM_CONTEXT.md)  
**Data SSOT:** [DATABASE-DESIGN.md](DATABASE-DESIGN.md), [exhibition_portal_schema.sql](exhibition_portal_schema.sql)  
**Applied schema:** `backend/src/main/resources/db/migration/` (Flyway V1–V6)  
**Verification:** [TESTING.md](TESTING.md)

This is the build sequence for a **Java Spring Boot** backend plus the existing **React + Vite** visitor prototype. It does not change product rules. Open business decisions in PLATFORM_CONTEXT §12 must be surfaced, not invented.

## 1. Current state

| Layer | State |
|---|---|
| Product / HLD | Approved scan-first visitor flow |
| Design | Alpine Blue; 11 mobile screens |
| PostgreSQL singleton DDL | Historical full DDL in this folder; **not applied**. POC applies a MySQL subset via Flyway (see §3) |
| Visitor UI | React 19 + TypeScript + Vite in `frontend/`; HTTP adapter to Java; **sessionStorage** draft-id pointer (no PII in `localStorage`) |
| Backend | **Running:** `backend/` Spring Boot 3.5, **Java 17**, JDBC, Flyway V1–V6, visitor API + staff + outbox + local card-QR assist |
| Admin UI | **POC:** `/staff` review queues and `/admin` staff-account CRUD (Alpine Blue After Dark), local Vite or same-origin from the packaged JAR. Not bolted into the visitor inquiry shell. |
| CRM / vendor / cloud OCR | Deferred until providers are chosen |

How to run the POC: [backend/README.md](../backend/README.md). API `http://localhost:8080`, UI `https://localhost:5173` (Vite proxies `/api`). Public Windows Server: [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md) (`http://43.225.195.200/` — Java 17 JAR + Jenkins, native MySQL 8 on 3306, no Docker, no Vite).

POC **does not** include: cloud OCR, voice assist, a real CRM product, or a real vendor ERP API. Outbox destinations are local stubs (`poc-mailbox`, `poc-vendor-stub`). Local card-QR assist (ZXing) is live.

## 2. Target stack

| Concern | Choice |
|---|---|
| Visitor UI | React 19 + TypeScript + Vite (existing `frontend/`) |
| Backend | **Java 17**, **Spring Boot 3.x** (Web, Validation, Security) — same JDK line as pharma-erp |
| Persistence | **MySQL 8** (same engine as pharma-erp); **Flyway** migrations. Logical entities still match [DATABASE-DESIGN.md](DATABASE-DESIGN.md); do not load `exhibition_portal_schema.sql`. |
| ORM | **POC uses JDBC** (`JdbcClient`). Spring Data JPA (or another Java ORM) remains replaceable later |
| API | JSON REST under `/api/v1` (HTTPS) |
| Files | Local private directory for now (`./var/exhibition-files`). MySQL holds `file_assets` metadata only. Object-storage provider is still an open decision. |
| Auth (visitors) | No visitor login for the pilot; draft identity is the confirmed contact + server-issued draft id |
| Auth (staff) | Internal users via `app_users` / roles (`ADMIN`, `SUPPLIER_REVIEWER`, `MARKETING`, `EXPORTER`, `TAXONOMY_MANAGER`) |
| Async | Outbox table `integration_deliveries` + scheduled worker |
| Frontend ↔ API | `inquiryApi` maps `InquiryDraft`; session pointer is resume only; offline is on-screen until reconnect |

Repo tree:

```text
sarv-exhibition-portal/
  specs/                 # this folder
  frontend/              # Vite visitor app
  backend/               # Spring Boot module (POC)
    pom.xml
    src/main/java/com/sarv/exhibitionportal/
    src/main/resources/application.properties
    src/main/resources/db/migration/   # Flyway V1+
    src/test/java/
  Jenkinsfile             # Checkout, npm, mvn (Java17/Maven3), Windows service deploy
  deploy/windows/        # Public Windows Server + Jenkins service scripts
```

Java package `com.sarv.exhibitionportal` is a placeholder until Sarv confirms the Maven `groupId`.

## 3. Schema work before the first Flyway migration

Do **not** load `exhibition_portal_schema.sql` as production V1. The singleton DDL is the full target. **POC Flyway V1** already applies these scan-first fixes on a **subset** of tables (inquiry + taxonomy + `workflow_events`; no consent, files, admin, or integrations):

| Issue | POC decision |
|---|---|
| `inquiries.route` is `NOT NULL` in the singleton DDL | **Nullable** until sell/buy. Required on submit (service rule). |
| `inquiry_parties.company_name_submitted` is `NOT NULL` | **Nullable**. Buyer company must not block submit. Supplier company still required in `InquiryRules`. |
| `EXHIBITION_QR` requires `qr_campaign_id` | Seeded campaign `22222222-2222-4222-8222-222222222222` (`POC-STALL-1`). |
| Supplier product types | Persisted as `(inquiry_id, department_id, product_type_id)`. |
| Consent uniqueness vs revocation | **Done in V3** — append-only `consent_records`; latest row wins |

POC seed also: one exhibition, IP/USP/BP/EP standards, and a temporary taxonomy. **V7** archives that POC taxonomy and inserts business-owned rows from [taxonomy/](taxonomy/).

## 4. Phased delivery

Do not start a later phase’s integrations until the earlier phase’s DoD is met. Admin and CRM can be designed in parallel with coding, but vendor upsert waits for human **Add to production**.

### Phase 0 — Specs and repo layout (done)

- Specs live in `specs/`.
- This build plan, testing notes, and index exist.
- Cursor rules point at `specs/` paths.

### Phase 1 — Java service skeleton (**POC done**)

- `backend/` Spring Boot app with health endpoint, Flyway, MySQL.
- Versioned migrations with §3 fixes (POC subset, not the full singleton DDL).
- Package layout started: `inquiry`, `taxonomy`, `api`, `config`. Remaining packages wait for later phases.
- Native MySQL 8 on **3306** for `mvn spring-boot:run` / Jenkins. `mvn test` uses **embedded MariaDB** (mariaDB4j); Docker is **not** required.

**DoD met:** `mvn test` passes; schema applies on a clean database.

### Phase 2 — Draft + submit API; wire the frontend (**POC done**)

Server-side draft after contact checkpoint; autosave on meaningful steps; submit is idempotent.

Visitor API:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/inquiries` | Create draft (optional card metadata) |
| `GET` | `/api/v1/inquiries/{id}` | Load draft or submitted inquiry |
| `PATCH` | `/api/v1/inquiries/{id}` | Autosave contact, route, taxonomy, buyer/supplier fields |
| `POST` | `/api/v1/inquiries/{id}/contact` | Confirm name / work email / mobile / country code |
| `POST` | `/api/v1/inquiries/{id}/submit` | Validate route rules; set `SUBMITTED`; keep `reference_code` |
| `GET` | `/api/v1/taxonomy/departments` | Active departments |
| `GET` | `/api/v1/taxonomy/product-types` | Optional `departmentIds` filter |

**DoD met for POC:** HTTP adapter + local fallback; buyer submit without company; supplier website-or-catalogue **on the server**; reload of a submitted draft stays on confirmation (`POC-` reference); CORS locked to localhost Vite origins. Shared-device isolation completed in Phase 7.

### Phase 3 — Files, consent, audit (**done**)

Visitor uploads are scoped to the draft id (no visitor login). Bytes go to a **local private directory**; MySQL stores `file_assets` only.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/inquiries/{id}/files` | Multipart upload (`purpose=BUSINESS_CARD\|CATALOGUE_ORIGINAL\|INQUIRY_ATTACHMENT`, card `side=front\|back`) |
| `GET` | `/api/v1/inquiries/{id}/files/{assetId}` | Serve a **CLEAN** file (not public listing; draft id required) |
| `DELETE` | `/api/v1/inquiries/{id}/files/{assetId}` | Soft-remove a draft supporting file (`PURGED`; bytes stay on disk) |
| `POST` | `/api/v1/inquiries/{id}/consents` | Append-only consent event |
| `GET` | `/api/v1/inquiries/{id}/consents` | List consent events, latest first |

Allowlist: JPEG/PNG/WebP for cards; those plus PDF for catalogue/supporting attachments. Supporting files: **5 MiB** each, max **10** (`exhibition.catalogue-max-bytes`, `exhibition.attachment-max-count`). Cards remain 10 MiB. Magic-byte check after write; **REJECTED** files stay on disk and in `file_assets` (not served). This is **not** an antivirus product.

Consent: `BUSINESS_CARD_EXTRACTION` granted on card upload, declined on continue-without-a-card. **Latest row wins**; never UPDATE a prior row. Location grant is rejected — GPS and raw IP are not collected.

Audit: `audit_events` on create, contact confirm, submit, file upload, scan reject, and consent. No PII in `metadata`. `workflow_events` still on create/submit.

**DoD met:** Catalogue/card bytes never in MySQL; failed content check does not delete the stored original; `INQUIRY_SUBMITTED` audit exists. Derived catalogue PDF and ClamAV are **not** in this phase.

### Phase 4 — Internal admin (**POC done**)

Staff UI is a **separate** route (`/staff`), Alpine Blue After Dark. HTTP Basic against seeded `app_users` (`{noop}poc-staff` locally — not SSO). **Do not send `WWW-Authenticate: Basic`** — that pops a browser login on the public visitor URL (card OCR `/tessdata/**` must stay `permitAll`). Staff/admin sign-in is the in-app form. **ADMIN** staff-account CRUD is a second route (`/admin`).

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/staff/me` | Current staff user |
| `GET` | `/api/v1/staff/suppliers` | Submitted supplier review queue |
| `POST` | `/api/v1/staff/suppliers/{id}/decisions` | `APPROVE` (Add to production), `REJECT`, `REQUEST_INFORMATION` |
| `GET` | `/api/v1/staff/buyers` | Submitted purchase-lead queue |
| `POST` | `/api/v1/staff/buyers/{id}/notes` | Internal marketing notes (never shown to visitors) |
| `POST` | `/api/v1/staff/exports` | Controlled purchase-lead export job (CSV in this POC) |
| `GET` | `/api/v1/staff/exports/{id}/file` | Download when `READY`; `GONE` after `expires_at` |
| `GET` | `/api/v1/staff/roles` | Role catalogue (`ADMIN` only) |
| `GET` | `/api/v1/staff/users` | List staff accounts (`ADMIN` only) |
| `POST` | `/api/v1/staff/users` | Create staff account |
| `PUT` | `/api/v1/staff/users/{id}` | Update email, name, roles, status, optional password |
| `DELETE` | `/api/v1/staff/users/{id}` | Deactivate (`INACTIVE`); does not hard-delete |

**DoD met:** Reviewer approve/reject has `decided_by_user_id`; `production_state` stays `NOT_REQUESTED` until **Add to production**. Phase 5 then enqueues `VENDOR_UPSERT`. Export is a job with expiry, not a raw table dump.

### Phase 5 — Outbox integrations (**POC done**)

- Purchase submit → `integration_deliveries` `MARKETING_LEAD` (`poc-mailbox` stub file). Duplicate submit reuses `idempotency_key` `MARKETING_LEAD:{inquiryId}`.
- **Add to production** → `VENDOR_UPSERT` (`poc-vendor-stub`). Visitor supplier submit does **not** enqueue vendor delivery.
- Scheduled worker (`exhibition.outbox.schedule-enabled`) claims due rows, retries with sanitized errors, never deletes the inquiry.

Stub payloads are JSON under `exhibition.storage-root/outbox/…` (reference code + kind only). This is **not** a CRM or vendor ERP API.

**DoD met:** Duplicate submit does not double-deliver. Vendor upsert is enqueued only after Add to production. Failed delivery leaves `lifecycle_state=SUBMITTED`.

### Phase 6 — Assistive OCR / QR / voice (**local QR + printed OCR**)

Optional, consented, reviewable field proposals (`ai_extracted_fields`). Manual fallback remains. AI must not approve vendors or overwrite confirmed contact fields.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/inquiries/{id}/extractions` | `BUSINESS_CARD_SCAN` (server re-scan) or `CLIENT_CARD_OCR` (browser Tesseract proposals). Voice returns validation error. |
| `GET` | `/api/v1/inquiries/{id}/extractions/latest` | Latest extraction + field proposals (`cardQrDetected` flag; **no raw QR payload**) |

**DoD met for POC:** Flyway V6 AI tables. After a clean card upload with granted extraction consent, ZXing decodes a card QR locally (`zxing-qr-v1`). When QR yields no contact fields, the visitor app runs **local Tesseract.js OCR** (`tesseract-js-v1`) and POSTs proposals as `CLIENT_CARD_OCR`. Raw QR is stored only in `inquiry_ui_state.card_qr_payload_internal` and is **redacted** from visitor inquiry JSON. Proposals prefill **empty** fields only. Contact confirm marks ACCEPTED/CORRECTED/REJECTED. Cloud OCR and voice are **not** live (open decision on cloud AI provider).

**Chat-independent reference — Card OCR autofill (2026-09-06):** Camera/upload prepares one JPEG for preview+upload+OCR. Client **jsQR** + bundled **Tesseract.js** (`public/tessdata/eng.traineddata`, Vite `?url` worker/core) fill contact fields; server ZXing merges gaps; front-side success advances to contact-confirm. Failure shows a concrete notice instead of failing silently.
### Phase 7 — Exhibition pilot (**POC done**)

QR campaign codes, poor-network behaviour, staff-assisted capture, shared-device draft isolation (do not leave PII in `localStorage` on stall tablets). Same portal for website entry (`WEBSITE` / `DIRECT`).

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/campaigns/{code}` | Resolve an active stall QR campaign (`POC-STALL-1`) |
| `POST` | `/api/v1/inquiries` | Create draft with `entryChannel`, optional `campaignCode`, optional `staffAssisted` |

**Visitor entry URLs:** `?c=POC-STALL-1` (exhibition), `/web` or `?channel=website`, `?channel=direct`, `?assist=1` (staff-assisted). Shared-device mode is default for exhibition QR / assist.

**DoD met for POC:** Campaign attribution on create; WEBSITE/DIRECT without `qr_campaign_id`; visitor UI stores only a **sessionStorage draft id** (legacy full-draft `localStorage` cleared); offline banner + submit blocked without inventing a receipt; **Next visitor** clears the session; fonts self-hosted (`@fontsource/*`, no Google Fonts CDN). Real stall network/device certification remains an operational open decision.

### Phase 8 — Production hardening (**done 5 Sep 2026**)

Turn the POC into a production-deployable application **without inventing** CRM, vendor ERP, cloud OCR, or business taxonomy.

| Change | Behaviour |
|---|---|
| Prod fail-closed | `prod` profile refuses empty/`poc-staff`/`change-me-staff` bootstrap password, `exhibition.poc=true`, or outbox force-failure |
| Receipt codes | `exhibition.reference-prefix` — `POC-` in default profile, `EP-` in `prod` |
| Public meta | `GET /api/v1/meta` → `{ poc, referencePrefix, stage }` for visitor UI |
| Excel export | Purchase-lead export is **`.xlsx`** (Apache POI), not CSV |
| Visitor copy | Prototype banners only when `meta.poc` is true |
| Outbox | Local file destinations renamed `local-mailbox` / `local-vendor-stub` until live APIs are chosen |

**Still blocked (do not invent):** live CRM/vendor APIs, cloud OCR/voice provider, product catalogue, public HTTPS for in-page camera, SSO.

### Business taxonomy v1 (**done 5 Sep 2026**)

| Change | Behaviour |
|---|---|
| Content pack | [specs/taxonomy/](taxonomy/) CSVs + README (owner + change process) |
| Flyway V7 | Archives POC `1000…`/`2000…` rows (`poc_` codes, `is_active=0`); inserts active `a100…`/`a200…` rows |
| Frontend fallback | `taxonomy.ts` IDs match V7 |
| Standards | IP, USP, BP, EP unchanged (V2) |
| Deferred | `products` catalogue; admin CRUD UI |

## 5. Frontend work in the same programme

Keep Alpine Blue and the 11-screen journey. Phase 2 POC wiring is in place:

- `inquiryApi` maps `InquiryDraft` (including department-scoped product types). Taxonomy loads from GET `/api/v1/taxonomy` when the API is up; offline IDs in `taxonomy.ts` must match Flyway **V7** ([taxonomy/](taxonomy/)).
- Submit persists on the server; confirmation shows `reference_code` (`POC-` locally / `EP-` on prod). Offline fallback still does not invent a tracking number.
- Card upload loads `/extractions/latest` and prefills empty contact fields from PENDING proposals. Shared-device mode uses a session pointer only; fonts are self-hosted. Camera permission copy runs **before** `getUserMedia`.

Admin UI is a separate `/staff` review route plus `/admin` for staff IDs (not inside `InquiryApp`).

## 6. Testing and quality gates

See [TESTING.md](TESTING.md). Every phase: automated tests for new server rules; browser smoke for visitor paths that persist.

## 7. Explicitly out of scope until decided

Do not invent: visitor accounts/OTP, CRM product, vendor ERP API, AI vendor, location legal copy, Stitch public/private, response SLAs, or a new department list outside [taxonomy/](taxonomy/).

## 8. Next implementation ticket

**Done:** Phases 1–8 + business taxonomy v1 (content pack, V7 archive/insert, `TaxonomyApiTest`).

**Next (needs decisions / build):** buyer FG periodic sync from pharma-erp; HTTPS for public camera; live CRM/vendor destinations; cloud OCR/voice (PLATFORM_CONTEXT §12).

---

**Chat-independent reference — Buyer FG per-item qty (2026-09-07):** Flyway V9 adds `purchase_inquiry_finished_goods.quantity_text`. API `buyer.finishedGoods[]` is `{ finishedGoodId, quantity }` (replaces `finishedGoodIds`). Each selected FG requires quantity (client + `InquiryRules`). UI: name-only list with strong selected highlight + per-item qty fields.

**Chat-independent reference — application.properties (2026-09-07):** Backend config is `.properties` only (`application.properties`, `application-prod.properties`, `application-ci.properties`). YAML removed so env/`exhibition.*` keys have one source of truth. Tests use `@ActiveProfiles("test")` + `application-test.properties` (never a test `application.properties`, which would shadow main). See `backend/README.md` § Configuration.

**Chat-independent reference — Buyer FG sync (2026-09-07):** Flyway V8 `finished_goods` + `purchase_inquiry_finished_goods`. `GET /api/v1/finished-goods?q=` for visitor buy path. Staff `POST /api/v1/staff/finished-goods/sync` reads `pharmadb.products` (active). Config: `exhibition.pharma-erp.*`. UI: BuyerNeedScreen flat multi-select; Staff Buyers tab sync panel. Tests: `FinishedGoodsApiTest`.

**Chat-independent reference — Buyer FG source (2026-09-06):** Buyer picks a **flat multi-select list** of Sarv finished goods from pharma-erp (`OUTPUT_PRODUCTS` / product catalog). Sync = **weekly DB sync** (pharma-erp MySQL → exhibition portal upsert); **manual first**, **cron** later. Not CSV/file. Seller path stays departments/product types.

**Chat-independent reference — Phase 8 (2026-09-05):** ProductionStartupGuard; `GET /api/v1/meta`; reference prefix `EP-` in prod; purchase-lead **xlsx** export; visitor UI hides prototype banners when `poc=false`. Tests: `ProductionStartupGuardTest`, `MetaApiTest`; export assertions updated in `StaffReviewApiTest`.

**Chat-independent reference — Phase 7 (2026-09-04):** Campaign GET + create with `entryChannel`/`campaignCode`/`staffAssisted`. Frontend `entryContext` + session pointer (no PII localStorage). Self-hosted fonts. Tests: `CampaignEntryApiTest`, Spa `/web`.

**Chat-independent reference — Public URL must not prompt Basic (2026-09-10):** Visitor `http://43.225.195.200:8082/` is unauthenticated. Card OCR fetches `/tessdata/eng.traineddata`; that path was `denyAll`, so Spring sent `WWW-Authenticate: Basic` and Chrome showed a username/password dialog over the card-capture screen. Fix: permit `/tessdata/**` and other public GET assets; staff APIs still 401 but **without** `WWW-Authenticate` (staff/admin use the in-app form). Tests: `SpaRoutingTest` (`staffApiStillRequiresAuth`, `cardOcrLangDataIsNotALoginChallenge`).

**Chat-independent reference — Phase 6 (2026-09-04):** Flyway V6 `ai_assistance_sessions` / `ai_extractions` / `ai_extracted_fields`. Card upload runs local ZXing; visitor never receives raw QR text. Tests: `LocalCardScanEngineTest`, `CardExtractionApiTest`.

**Chat-independent reference — Phase 5 (2026-09-01):** Flyway V5 `integration_deliveries`. Buyer submit enqueues `MARKETING_LEAD`; Add to production enqueues `VENDOR_UPSERT`. Worker writes stub JSON; retries; never drops the inquiry. Tests: `OutboxApiTest`, `OutboxRetryApiTest`.

**Chat-independent reference — Supplier Other + attachments (2026-09-09):** Supplier submit no longer requires a listed department+product-type pair. Categories (departments) **or Other**; required free-text `capabilityNotes`; product types optional when Other/notes cover the offering. Flyway V11: `supplier_inquiries.other_category` / `other_product_type` / `capability_notes`; `file_assets.purpose` adds `INQUIRY_ATTACHMENT`. Both routes: multiple supporting files, 5 MiB each, max 10. `DELETE` draft supporting files (soft `PURGED`). Cards stay 10 MiB. Audit: `FILE_REMOVED` (purpose only). Tests: `InquiryRulesTest`, `FileConsentAuditApiTest`, `FileContentRulesTest`. UI: supplier category/subcategory + Other + notes; attachments on supplier review and buyer need.

**Chat-independent reference — Buyer multi-source catalogue (2026-09-09):** Buyer list is `GET /api/v1/buyer-products` = active pharma-erp `finished_goods` **union** trading products with `listed_for_buyers`. Flyway V10 `trading_suppliers` / `trading_products` / `purchase_inquiry_trading_products`. `/admin` → Buyer catalogue: create offline suppliers; link submitted portal suppliers (does not Add to production); tag product names for buyers. Inquiry `buyer.tradingProducts[]` `{ tradingProductId, quantity }`. Tests: `TradingCatalogueApiTest`, `InquiryRulesTest`. UI: BuyerNeedScreen names only; Admin `TradingCataloguePanel`.

**Chat-independent reference — Admin staff CRUD (2026-09-09):** `/admin` is an ADMIN-only Alpine Blue After Dark panel for staff-account CRUD (`app_users` + `user_roles`). API: `GET/POST /api/v1/staff/users`, `GET/PUT/DELETE /api/v1/staff/users/{id}`, `GET /api/v1/staff/roles`. Delete deactivates (`INACTIVE`); last active ADMIN and self-deactivation are rejected. Audit: `STAFF_USER_CREATED` / `UPDATED` / `DEACTIVATED` on `APP_USER` (no email/password in metadata). `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` rotates only seeded `poc-*` accounts; `/admin`-created users keep their password. Tests: `StaffAdminApiTest`, `SpaRoutingTest` `/admin`. UI: `frontend/src/features/admin/`.

**Chat-independent reference — Windows public IP (2026-09-02):** Visitor UI is packaged into the Spring Boot JAR (`with-frontend` when `frontend/dist` exists). Prod profile binds port 80, CORS origin `http://43.225.195.200`, SPA forward for `/staff` and `/admin`. `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` rotates seeded `poc-*` `app_users` hashes. Runbook: [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md). In-page camera still needs HTTPS.

**Chat-independent reference — Java 17 + Jenkins (2026-09-03):** Target JDK is **17** (Windows Server `17.0.18`), same Jenkins tool ids as pharma-erp (`Java17`, `Maven3`). Pipeline: npm in `frontend/` then `mvn` in `backend/`; **`dev` and `poc`** → `C:\exhibition-portal-staging` (port **8082**, service `exhibition-portal-staging`); **`main`** → `C:\exhibition-portal` (port 80). **8081 is pharma-erp-staging** on this host. Docker is not part of deploy. A Maven SUCCESS on `poc` is not a deploy.

**Chat-independent reference — MySQL 8 (2026-09-03):** Applied store is **MySQL 8** on **3306** (same engine as pharma-erp). Flyway V1–V5 is MySQL/MariaDB DDL (`CHAR(36)` UUIDs, `DATETIME(6)`, `JSON`). `mvn test` uses embedded MariaDB (mariaDB4j). `exhibition_portal_schema.sql` remains a historical PostgreSQL singleton and must not be loaded. Create the host database with `deploy/windows/init-mysql.sql`.

**Chat-independent reference — Jenkins npm PATH (2026-09-03):** Frontend failed with `'npm' is not recognized` because the Jenkins Windows service (SYSTEM, workspace under `systemprofile`) does not use an interactive user PATH. Java17/Maven3 are Jenkins tools; Node is not. The Frontend stage prepends `NODE_HOME`, `C:\Program Files\nodejs`, and `C:\nodejs`. Install Node 22 for all users and restart the Jenkins service.

**Chat-independent reference — poc staging folder (2026-09-03):** Job `exibit-portal-pipeline_poc` commit `9fc4d710` ran **Deploy to Staging**: created `C:\exhibition-portal-staging`, copied JAR + `start-portal.ps1`, seeded `portal.env.ps1`, then failed because service `exhibition-portal-staging` was not installed. Jenkinsfile now runs `install-service.ps1 -Staging` when that service is missing. Exhibition staging listens on **8082** because **8081 is pharma-erp-staging**. The first seed used 8081; the next deploy pins `SERVER_PORT=8082`. `start-portal.ps1` still refuses `change-me-db` / `change-me-staff` — edit `C:\exhibition-portal-staging\portal.env.ps1` (and create MySQL via `init-mysql.sql`), then rebuild. Do not bind port 80 for staging.

**Chat-independent reference — WinSW / NET 2186 (2026-09-04):** Manual `start-portal.ps1` (Option A) reached `{"status":"UP"}` on **8082**, but Jenkins `net start exhibition-portal-staging` failed with **NET HELPMSG 2186**. Root cause: `install-service.ps1` used `New-Service` with `powershell.exe -File start-portal.ps1`, which is not a Windows service binary. Fix: WinSW wrapper (`exhibition-portal-staging.exe` + `.xml` under the install dir); Jenkins always re-runs `install-service.ps1` to replace the broken registration and stop orphan java from Option A before `net start`.

**Chat-independent reference — Health Check `$i:` (2026-09-07):** After WinSW deploy, `net start` succeeded but Health Check failed with PowerShell `InvalidVariableReferenceWithDrive` on `Write-Host "health wait attempt $i: ..."`. In PowerShell `$i:` is a drive-scoped variable. Fixed to use `-f` formatting (`"health wait attempt {0}: ..." -f $i, ...`).

**Chat-independent reference — green pipeline, dead app (2026-09-08):** Removing Health Check made Jenkins SUCCESS while `exhibition-portal-staging` often exits after Spring Boot starts (Flyway / DB / `ProductionStartupGuard`). `net start` only proves WinSW launched `java.exe`. Deploy stage now waits up to ~120s for `/actuator/health` and dumps WinSW `*.err.log` / `*.out.log` on failure. On the host, always check: `Get-Service exhibition-portal-staging` and `Get-Content C:\exhibition-portal-staging\exhibition-portal-staging.err.log -Tail 80`.

**Chat-independent reference — staging Flyway failed V1 (2026-09-08):** Host logs showed WinSW crash-looping java every ~5–10s. Root cause: `FlywayValidateException` — `Detected failed migration to version 1 (poc core schema)`. Same class of issue as local `backend/README.md` recovery: do **not** `flyway repair` a half-applied V1. Staging with no valuable data: `net stop exhibition-portal-staging`, drop/recreate `exhibition_portal`, `net start`. See [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md) § Flyway failed V1 on staging.

**Chat-independent reference — mysql not on PATH / wrong cwd (2026-09-03):** `C:\exhibition-portal-staging` is the install dir (JAR + env), not the repo. `mysql` is not on PATH; use `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`. `.\deploy\windows\deploy.ps1` does not exist there and must not be run for staging (it targets production `C:\exhibition-portal`). Run `init-mysql.sql` from a git clone or Jenkins workspace. Jenkins now also copies `init-mysql.sql` into the install dir.

**Chat-independent reference — staging empty buy FG list (2026-09-08):** Local Vite shows Colchicine etc. because local `finished_goods` was synced from `pharmadb`. Staging `GET /api/v1/finished-goods` returned `[]`. `prod` defaults `exhibition.pharma-erp.enabled=false`. Git does not copy local MySQL rows. Follow-up: Java loads `portal.env.ps1` from the WinSW working dir (so `net start` picks JDBC flags without re-baking XML); startup auto-sync when enabled; `/api/v1/meta` reports `pharmaErpEnabled` + `finishedGoodsActive`; Staff sync panel is on `/staff` after sign-in. Tests: `PortalEnvFileEnvironmentPostProcessorTest`, `MetaApiTest`.

**Chat-independent reference — Jenkins staging port-in-use (2026-09-10):** Job `exibit-portal-pipeline_poc` commit `9ba0e85` compiled and tested (81 tests) then Deploy failed: `Port 8082 was already in use`. Killing WinSW’s `java.exe` while the service was still Running let `onfailure restart` rebind 8082. Deploy now stops `exhibition-portal-staging` first and waits for the listen port to be free. Failure dumps redact WinSW XML `value=` (rotate host DB/staff passwords if an older log leaked them). See [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md) § Staging deploy: port 8082 already in use.

**Chat-independent reference — Jenkinsfile Groovy backslash-s (2026-09-10):** Commit `4b9781b` never started: `unexpected char: '\\'` on `Select-String ...` plus a `\s` regex inside `powershell """`. Groovy GStrings reject `\s` / `\d`. The netstat fallback now splits on spaces. Push and rebuild `poc`.

**Chat-independent reference — local Flyway failed V1 (2026-09-07):** After creating MySQL user `exhibition`, a first `.\run.ps1` can still fail if `exhibition_portal` already has tables but `flyway_schema_history` has `success=0` for V1 (tables existed; CREATE TABLE aborted in ~14ms). **Do not `flyway repair` that local state** — V2/V7 seed never applied, and later CREATE TABLE versions would collide. Drop empty local tables, then re-run so Flyway applies V1–V7. SQL: `backend/README.md`. `'exhibition'@'127.0.0.1'` is optional for `localhost` JDBC.

**Chat-independent reference — same-Wi-Fi phone (2026-09-07):** Local visitor UI is Vite HTTPS on **0.0.0.0:5173** (`npm run dev`). Phone uses the printed **Network** URL (`https://<lan-ip>:5173/?c=POC-STALL-1`). Firewall: elevated `scripts/allow-vite-lan.ps1` (TCP 5173/4173 Private+Public). Home Wi‑Fi is often **Public**; that rule is required. `/api` is proxied to `localhost:8080`, so the phone does not need 8080. Default CORS uses `https://192.168.*.*:5173` / `https://10.*.*.*:5173` instead of a hard-coded DHCP IP. Do not expose 5173 on the public host.
