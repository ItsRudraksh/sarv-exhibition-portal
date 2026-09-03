# Testing and verification

**Updated:** 3 September 2026

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
mvn test              # rules + API tests including files/consent/audit/staff (embedded MariaDB; Docker not required)
mvn spring-boot:run   # needs MySQL 8 — see below
```

Local MySQL (not used by `mvn test`): native MySQL 8 on `localhost:3306` (see `deploy/windows/init-mysql.sql`). Docker is not required.

API tests cover: draft before route; buyer submit without company; supplier website-or-catalogue; taxonomy; card upload + consent; staff Add to production; outbox idempotency; vendor row only after Add to production; failed stub keeps the inquiry; SPA `/` and `/staff` are public; staff bootstrap password replaces `{noop}poc-staff`.

Flyway V1–V5 is the applied schema. Do not treat `exhibition_portal_schema.sql` as an applied production migration.

## Jenkins + public Windows host (`http://43.225.195.200/`)

Commands: [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md). **Java 17** only. Do not use `npm run dev` on the public IP.

Create a Jenkins Pipeline job from this repo’s **`Jenkinsfile`** (tools **`Java17`** and **`Maven3`**, same as pharma-erp). Node 22 must be visible to the **Jenkins Windows service** (`C:\Program Files\nodejs` or `NODE_HOME`); restart Jenkins after installing Node.

- **`poc` / `dev`:** Deploy staging → `C:\exhibition-portal-staging\exhibition-portal.jar`, Jenkins installs `exhibition-portal-staging` if missing, health on port **8082** (not 8081 — that is pharma-erp-staging). First run fails until `portal.env.ps1` has real `DATASOURCE_PASSWORD` and `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (not `change-me-*`) and MySQL `exhibition_portal` exists. Rebuild after editing.
- **`main`:** Deploy production → `C:\exhibition-portal\`, health on port **80**.

**Public smoke:** `http://43.225.195.200/actuator/health` → visitor `/` (upload or continue without a card; in-page camera needs HTTPS) → `/staff` with the bootstrap password. MySQL must not be reachable on the public IP.

## Reply footer (agents)

State lint/build/tests (pass/fail), specs touched, audit impact (`workflow_events` / `audit_events` / N/A), and **Verification** (browser / API / manual steps).
