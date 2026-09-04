# Testing and verification

**Updated:** 4 September 2026 (Phase 7 pilot entry)

After every substantive change: reproduce (if a bug) → fix → run the commands below → update specs. Runtime parity: lint/build is not a browser walkthrough. See `.cursor/rules/runtime-parity-definition-of-done.mdc`.

## Frontend (`frontend/`)

```bash
npm install    # if node_modules is missing
npm run lint
npm run build
npm run dev    # https://localhost:5173 — HTTPS required for in-page camera on phones
```

Vite proxies `/api` to `http://localhost:8080`. Start the Java API for a live draft/submit/upload. Without it the UI stays on-screen only (no PII written to `localStorage`).

**Entry URLs:** `https://localhost:5173/?c=POC-STALL-1` (stall), `https://localhost:5173/web` (website), `?channel=direct`, `?assist=1` (staff-assisted). Shared tablets show **Next visitor**.

**Phone on the same Wi‑Fi:** Vite’s Network URL (e.g. `https://192.168.1.12:5173/`) is often blocked by Windows Firewall when the Wi‑Fi profile is **Public**. Run `scripts/allow-vite-lan.ps1` as Administrator (see `frontend/README.md`). Then accept the self-signed HTTPS warning on the phone. Guest/AP isolation still blocks device-to-device traffic.

**Visitor UI smoke** (when screens, journey, validation, or persistence change):

1. Open with `?c=POC-STALL-1` → campaign label visible → agree to store images → upload or camera → contact. Or continue without a card. Confirm **Next visitor** clears the session (no contact left in localStorage).
2. Toggle airplane mode mid-form: connection banner; submit blocked until reconnect.
3. Supplier and buyer paths as before; confirmation shows `POC-` reference only when online submit succeeds.
4. `/web` creates `WEBSITE` channel draft (no campaign required).

**Staff UI smoke** (`https://localhost:5173/staff`, API must be up): unchanged from Phase 5.

If the browser was not run, write **Manual smoke required** with those screens.

## Backend (`backend/`)

```bash
mvn test              # includes CampaignEntryApiTest + Spa `/web`
mvn spring-boot:run   # needs MySQL 8
```

API tests also cover campaign resolve, website/direct create without campaign, staffAssisted audit metadata (no PII), invalid campaign on website.

Flyway V1–V6 is the applied schema (Phase 7 needed no new migration).


## Jenkins + public Windows host (`http://43.225.195.200/`)

Commands: [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md). **Java 17** only. Do not use `npm run dev` on the public IP.

Create a Jenkins Pipeline job from this repo’s **`Jenkinsfile`** (tools **`Java17`** and **`Maven3`**, same as pharma-erp). Node 22 must be visible to the **Jenkins Windows service** (`C:\Program Files\nodejs` or `NODE_HOME`); restart Jenkins after installing Node.

- **`poc` / `dev`:** Deploy staging → `C:\exhibition-portal-staging\exhibition-portal.jar`, Jenkins installs `exhibition-portal-staging` if missing, health on port **8082** (not 8081 — that is pharma-erp-staging). First run fails until `portal.env.ps1` has real `DATASOURCE_PASSWORD` and `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (not `change-me-*`) and MySQL `exhibition_portal` exists. Rebuild after editing. **`C:\exhibition-portal-staging` is not the git repo** (`mysql` and `deploy.ps1` are not there). Create the DB with `mysql.exe` full path + `init-mysql.sql` from the repo (see [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md)).
- **`main`:** Deploy production → `C:\exhibition-portal\`, health on port **80**.

**Public smoke:** `http://43.225.195.200/actuator/health` → visitor `/` (upload or continue without a card; in-page camera needs HTTPS) → `/staff` with the bootstrap password. MySQL must not be reachable on the public IP.

**Staging host diagnose:** `cd C:\exhibition-portal-staging` then `.\verify-staging.ps1` (or `verify-staging.cmd`). One command only. The 15:33 and later pipeline FAIL is placeholder `change-me-*` in `portal.env.ps1` while the service exists and is Stopped.

## Reply footer (agents)

State lint/build/tests (pass/fail), specs touched, audit impact (`workflow_events` / `audit_events` / N/A), and **Verification** (browser / API / manual steps).
