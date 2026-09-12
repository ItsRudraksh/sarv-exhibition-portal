# Code walkthrough (KT)

**Updated:** 11 September 2026  
**Audience:** Developers joining the exhibition portal. Pair this with [PLATFORM_CONTEXT.md](PLATFORM_CONTEXT.md) (product) and package/`file` Javadoc in source.

Walk **product first**, then **visitor UI**, then **API persistence**, then **staff/admin**. Do not start in DTOs.

## 1. What to hold in your head

| Choice | Meaning in code | Must not regress |
|---|---|---|
| **I want to sell** | `route = SUPPLIER` | Vendor create/update only after staff **Add to production**. AI never approves. |
| **I want to buy** | `route = PURCHASE` | Contact + one requirement (or Other details) is enough. Specs optional. |
| Scan-first | Card → confirm contact → durable draft → sell/buy | QR payloads are **internal only**. Never show or open the URL. |
| Standards | `IP`, `USP`, `BP`, `EP` | **PP** is a typo. Do not add it. |

Source of truth: `specs/PLATFORM_CONTEXT.md`. Applied schema: Flyway in `backend/src/main/resources/db/migration/` (not `exhibition_portal_schema.sql`).

## 2. Three apps, one JAR

`frontend/src/App.tsx` picks the shell from the path (`stripPublicBase` so `/exhibit/staff` still matches):

```text
/           InquiryApp     visitor sell/buy (no login)
/staff      StaffApp       review queues (HTTP Basic via in-app form)
/admin      AdminApp       staff CRUD + buyer catalogue tagging (ADMIN)
```

Vite (`npm run dev`) proxies `/api` to `http://localhost:8080`. Packaged `prod` serves `frontend/dist` from Spring Boot; `SpaForwardController` forwards `/staff`, `/admin`, `/web` to `index.html`.

Public hostname: **`https://welcome.sarvbiolabs.com/`** (IIS reverse-proxy; Java stays on 8083). Default build `VITE_BASE=/`. Path URL `https://sarvbiolabs.com/exhibit` needs `VITE_BASE=/exhibit/` plus `SERVER_SERVLET_CONTEXT_PATH=/exhibit` (`frontend/src/lib/publicPath.ts`). Marketing [sarvbiolabs.com](https://sarvbiolabs.com/) is a different host (`209.42.22.88`). Visitor chrome/tokens match that site (navy `#022D59`, cyan `#01AFEF`).

## 3. Visitor journey (frontend)

Start: `frontend/src/features/inquiry/InquiryApp.tsx` → `useInquiryJourney.ts`.

```text
card-capture → contact-confirm → intent-selection
    ├─ SUPPLIER: departments → product-types → smart-details → review → confirmation
    └─ PURCHASE: need → review → confirmation
```

| File | Walkthrough role |
|---|---|
| `types.ts` | Draft shape. `currentStep` is the screen. `ReviewEditSession` is in-memory only (Edit from review). |
| `copy.ts` | Visitor-facing strings. Change labels here, not inline in JSX. |
| `validation.ts` | Client rules; keep aligned with `InquiryRules` on the server. |
| `api.ts` | `inquiryApi` REST adapter. `asDraft` fills defaults when the API omits fields. Paths use `withPublicBase`. |
| `entryContext.ts` | `?c=`, `/web`, `?channel=direct`, `?assist=1`. Session pointer = draft **id** only. Strips public base before `/web`. |
| `taxonomy.ts` | Live taxonomy from API, with V7 UUID fallback when offline. |
| `cardOcr.ts` / `cardContactPayload.ts` | Client jsQR + Tesseract; proposals only until contact confirm. |
| `OtherDetailsField.tsx` | Shared Other checkbox + required Describe Other box (sell and buy). |
| `screens/*` | One file per step. Footer Continue/Save lives with the screen. |

Autosave: `useInquiryJourney` PATCHes the draft (debounced) while `lifecycleState === DRAFT`. Submit needs the API for a receipt reference.

## 4. Visitor API (backend)

HTTP: `InquiryController` `/api/v1/inquiries`.

```text
POST   create draft (campaign/channel)
PATCH  save draft (visitor body; card QR is stripped/ignored)
POST   /contact   confirm contact checkpoint
POST   /submit    InquiryRules then lifecycle SUBMITTED + outbox
```

| Package | Walkthrough role |
|---|---|
| `inquiry` | `InquiryService` orchestrates; `InquiryRules` is the submit contract; `InquiryRepository` JDBC persist. |
| `taxonomy` | Departments / product types (V7 business pack). |
| `extraction` | Server ZXing on card upload. Raw QR stays in `inquiry_ui_state`, never in visitor GET. |
| `fileasset` | Private files + `file_assets` metadata. Cards 10 MiB; supporting files 5 MiB / max 10. |
| `consent` | Append-only GRANT/DECLINE. Latest row wins; never UPDATE a prior decision. |
| `campaign` | Stall QR campaigns (`POC-STALL-1`). |
| `finishedgoods` | Snapshot of pharma-erp products for the buy list. |
| `trading` | Offline/portal supplier products tagged `listed_for_buyers`. |
| `outbox` | `integration_deliveries` + worker. Buyer submit → marketing stub; Add to production → vendor stub. Never drops the inquiry. |
| `review` | Staff supplier queue + buyer leads. Approve ≠ vendor upsert until **Add to production**. |
| `audit` | Operational events (no secrets in metadata). |
| `staff` | `app_users` + roles; HTTP Basic for `/api/v1/staff/**`. |
| `exportjob` | Controlled Excel export of buyer leads. |
| `config` | Security (no browser Basic prompt on visitor URLs), CORS, prod fail-closed guard. |

Submit rules (both UI and API): `InquiryRules` / `frontend/.../validation.ts`. Other checked ⇒ details required. Sell: category signal = listed departments **or** Other **or** capability notes (same idea for product types). Buy: requirement **or** Other+details.

## 5. Suggested live walkthrough (30–40 min)

1. Open `App.tsx` and `InquiryApp.tsx` — three shells, step switch.
2. Trace `useInquiryJourney` create → PATCH → contact confirm → submit.
3. Sell: `SupplierDepartmentsScreen` → `SupplierProductTypesScreen` → `InquiryRules.assertSupplierSubmit`.
4. Buy: `BuyerNeedScreen` (catalogue union + Other) → `BuyerProductsController`.
5. `InquiryRepository.saveDraft` — party, supplier/purchase extension tables, UI state.
6. `SecurityConfig` — what is public vs role-gated.
7. `StaffApp` approve vs `ReviewService` Add to production → `OutboxService`.
8. `AdminApp` trading catalogue — tagging does **not** Add to production.

## 6. Tests that document behaviour

`mvn test` (embedded MariaDB). Highest signal for KT:

| Test | What it proves |
|---|---|
| `InquiryRulesTest` | Sell/buy submit contract, Other details, quantities |
| `InquiryApiTest` | Create / save / submit HTTP slice |
| `TaxonomyApiTest` | V7 departments/types |
| `StaffReviewApiTest` | Queues and decisions |
| `TradingCatalogueApiTest` | Buyer list union + admin tagging |
| `OutboxApiTest` / `OutboxRetryApiTest` | Delivery stubs and retry |
| `SpaRoutingTest` | `/staff` `/admin` `/web` + no Basic prompt on tessdata |
| `PublicPathPrefixTest` | `server.servlet.context-path=/exhibit` serves `/exhibit/api/v1/meta` |
| `FileConsentAuditApiTest` | Files, consent, audit |

Frontend has no unit-test runner; `npm run lint` + `npm run build` + browser smoke (`specs/TESTING.md`).

## 7. Where comments live

- Java: `package-info.java` per package, then class Javadoc on types.
- Frontend: file-level block comment at the top of each `src` module.
- Product copy and flow still belong in `PLATFORM_CONTEXT.md` and `copy.ts`, not duplicated in every screen.
