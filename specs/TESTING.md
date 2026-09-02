# Testing and verification

**Updated:** 1 September 2026

After every substantive change: reproduce (if a bug) → fix → run the commands below → update specs. Runtime parity: lint/build is not a browser walkthrough. See `.cursor/rules/runtime-parity-definition-of-done.mdc`.

## Frontend (`frontend/`)

```bash
npm install    # if node_modules is missing
npm run lint
npm run build
npm run dev    # https://localhost:5173 — HTTPS required for in-page camera on phones
```

Vite proxies `/api` to `http://localhost:8080`. Start the Java API for a live draft/submit/upload; without it the UI falls back to `localStorage`.

**Phone on the same Wi‑Fi:** Vite’s Network URL (e.g. `https://192.168.1.12:5173/`) is often blocked by Windows Firewall when the Wi‑Fi profile is **Public**. Run `scripts/allow-vite-lan.ps1` as Administrator (see `frontend/README.md`). Then accept the self-signed HTTPS warning on the phone. Guest/AP isolation still blocks device-to-device traffic.

**Visitor UI smoke** (when screens, journey, validation, or persistence change):

1. Card capture: agree to store images → upload or camera (permission copy first) → contact. Or continue without a card (decline recorded).
2. Supplier: departments → product types → smart details → website or **uploaded** catalogue → submit → confirmation with `POC-` reference. Reload must stay on confirmation when the API is up.
3. Buyer: requirement only (no company) → review → submit. Optional specs must not be required.
4. Back/edit, Restart demo, and resume after reload (card preview from `GET /files/{assetId}` when an `assetId` exists).

**Staff UI smoke** (`https://localhost:5173/staff`, API must be up):

1. Sign in as `reviewer@sarv.local` / `poc-staff`.
2. After a visitor supplier submit: **Add to production** → `production_state` QUEUED and outbox `VENDOR_UPSERT` PENDING (worker then stub-succeeds). Reject another → stays `NOT_REQUESTED` with no vendor row.
3. Sign in as `marketing@sarv.local`: buyers list shows marketing-lead outbox state + export CSV. Reviewer must not see export.

If the browser was not run, write **Manual smoke required** with those screens.

## Backend (`backend/`)

```bash
mvn test              # rules + API tests including files/consent/audit/staff (embedded Postgres; Docker not required)
mvn spring-boot:run   # needs Postgres — see below
```

Local Postgres (not used by `mvn test`):

```bash
docker compose up -d db   # from repo root; host port 5433, user/db/password exhibition
```

API tests cover: draft before route; buyer submit without company; supplier website-or-catalogue; taxonomy; card upload + consent; staff Add to production; outbox idempotency; vendor row only after Add to production; failed stub keeps the inquiry.

Flyway V1–V5 is the applied schema. Do not treat `exhibition_portal_schema.sql` as an applied production migration.

## Reply footer (agents)

State lint/build/tests (pass/fail), specs touched, audit impact (`workflow_events` / `audit_events` / N/A), and **Verification** (browser / API / manual steps).
