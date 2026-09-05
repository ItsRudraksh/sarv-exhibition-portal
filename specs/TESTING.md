# Testing and verification

**Updated:** 5 September 2026 (business taxonomy v1 — Flyway V7)

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

**Visitor UI smoke:** campaign entry → card/contact → buy or sell → submit → receipt (`POC-` locally / `EP-` on prod) → Next visitor. Staff: export downloads `.xlsx`.

## Backend (`backend/`)

```bash
mvn test
mvn spring-boot:run                          # default profile (poc=true)
# prod checks (expect fail without real password):
# mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Flyway V1–V7. Phase 8: `ProductionStartupGuardTest`, `MetaApiTest`; export is xlsx. Taxonomy: `TaxonomyApiTest` (active business rows only; supplier submit with mapped V7 IDs).


## Jenkins + public Windows host (`http://43.225.195.200/`)

Commands: [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md). **Java 17** only. Do not use `npm run dev` on the public IP.

Create a Jenkins Pipeline job from this repo’s **`Jenkinsfile`** (tools **`Java17`** and **`Maven3`**, same as pharma-erp). Node 22 must be visible to the **Jenkins Windows service** (`C:\Program Files\nodejs` or `NODE_HOME`); restart Jenkins after installing Node.

- **`poc` / `dev`:** Deploy staging → `C:\exhibition-portal-staging\exhibition-portal.jar`, Jenkins installs `exhibition-portal-staging` if missing, health on port **8082** (not 8081 — that is pharma-erp-staging). First run fails until `portal.env.ps1` has real `DATASOURCE_PASSWORD` and `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (not `change-me-*`) and MySQL `exhibition_portal` exists. Rebuild after editing. **`C:\exhibition-portal-staging` is not the git repo** (`mysql` and `deploy.ps1` are not there). Create the DB with `mysql.exe` full path + `init-mysql.sql` from the repo (see [DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md)).
- **`main`:** Deploy production → `C:\exhibition-portal\`, health on port **80**.

**Public smoke:** `http://43.225.195.200/actuator/health` → visitor `/` (upload or continue without a card; in-page camera needs HTTPS) → `/staff` with the bootstrap password. MySQL must not be reachable on the public IP.

**Staging host diagnose:** `cd C:\exhibition-portal-staging` then `.\verify-staging.ps1` (or `verify-staging.cmd`). One command only. Placeholder `change-me-*` still fails Deploy. **`net start` NET 2186** means the old powershell-only service registration — fixed by WinSW in `install-service.ps1` (push that change, then rebuild). Manual Option A (`start-portal.ps1` in a console) still works; close that window (or kill orphan java) before Jenkins `net start` so port 8082 is free.

## Reply footer (agents)

State lint/build/tests (pass/fail), specs touched, audit impact (`workflow_events` / `audit_events` / N/A), and **Verification** (browser / API / manual steps).
