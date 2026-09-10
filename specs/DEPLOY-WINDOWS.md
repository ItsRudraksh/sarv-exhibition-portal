# Deploy on Windows Server (public IP)

**Updated:** 10 September 2026  
**Target:** `http://43.225.195.200/`  
**Runtime:** **Java 17** only (server is `17.0.18`). Same delivery shape as pharma-erp: Jenkins on the Windows agent, native database, **no Docker**.

The visitor UI and API ship as **one Spring Boot JAR** (`backend/target/exhibition-portal.jar`). Production does **not** use `npm run dev` or Vite port 5173.

## Honest limits

| Topic | Production behaviour |
|---|---|
| URL | `http://43.225.195.200/` (visitor), `http://43.225.195.200/staff` (review), `http://43.225.195.200/admin` (staff IDs, ADMIN) |
| Java | **17** (`javac`/`java` 17.0.x). Do not build with Java 21 bytecode. |
| Camera | In-page `getUserMedia` needs HTTPS. On HTTP, visitors **upload** a photo or use the phone file picker. |
| Auth | Visitor `/` is public (no browser login). Staff `/staff` and admin `/admin` use an in-app form; HTTP Basic is only for `/api/v1/staff/**` and must not send `WWW-Authenticate` (that pops a browser sign-in on the public URL). **Required:** `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (not `poc-staff` / `change-me-staff`). Prod refuses to start otherwise. |
| MySQL | Native MySQL 8 on **127.0.0.1:3306**. Do not publish 3306 on `0.0.0.0`. Docker is not used. |
| Cloud OCR / CRM / vendor API | Still not live. Local card-QR assist may propose fields. Outbox writes local stub files (`local-mailbox` / `local-vendor-stub`). |
| Receipts | Prod uses `EP-` prefix (`exhibition.reference-prefix`). |
| Lead export | Staff download is **Excel .xlsx** (expiring job). |
| Windows service | **WinSW** wraps `start-portal.ps1`. Bare `powershell -File` as the service binary causes **NET 2186**. |

## What must be on the server

- Windows Server with public NIC `43.225.195.200`
- **Java 17** (the installed `17.0.18` is the target)
- **Maven 3.9+** via Jenkins tool **`Maven3`**, and **Node 22+** on the **Jenkins service** PATH (not only your logged-in user). Default installer location: `C:\Program Files\nodejs`. After install, **restart the Jenkins Windows service**. Optional: set `NODE_HOME` on the agent.
- **MySQL 8** (Windows installer, same engine as pharma-erp). Create DB/user with `deploy/windows/init-mysql.sql`
- Jenkins with tools named **`Java17`** and **`Maven3`** (same ids as pharma-erp)
- Windows service **`exhibition-portal`** (once): `deploy/windows/install-service.ps1`
- Inbound **TCP 80** (Windows Firewall + cloud firewall)
- IIS stopped if it owns port 80 (`Stop-Service W3SVC`)

Do not open 5173 or 3306 on the public IP. Do not install Docker for this app.

## Two folders (do not mix them)

| Path | What it is |
|---|---|
| **`C:\exhibition-portal-staging`** | Jenkins **install dir**: JAR, `start-portal.ps1`, `portal.env.ps1`. **Not** a git clone. There is no `deploy\windows\deploy.ps1` here. |
| **Git repo** (clone, or Jenkins workspace `...\workspace\exibit-portal-pipeline_poc`) | Source of `deploy\windows\*.ps1` and `init-mysql.sql`. |

Do **not** `cd C:\exhibition-portal-staging` and run `.\deploy\windows\deploy.ps1`. That script only exists in the **repo**, and it copies the JAR to **`C:\exhibition-portal`** (production). Staging is already filled by Jenkins.

### Verify staging (paste the output)

Does **not** print passwords. The 15:33 Jenkins failure is this script’s FAIL on `change-me-db` / `change-me-staff`.

```powershell
Set-ExecutionPolicy -Scope Process Bypass
cd C:\exhibition-portal-staging
.\verify-staging.ps1
# or: C:\exhibition-portal-staging\verify-staging.cmd
```

Paste **one** command, then Enter. Do not paste the same line twice (PowerShell joins them into `verify-staging.ps1powershell` and fails). From a git clone: `.\deploy\windows\verify-staging.ps1`. Paste the full console (RESULT line included) into chat.

## One-time host setup

### 1. Create MySQL database (CLI is often not on PATH)

`mysql` as a bare command usually fails on this host. Use the 8.0 client (same engine as pharma-erp):

```powershell
Set-ExecutionPolicy -Scope Process Bypass
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (-not (Test-Path $mysql)) {
    Get-ChildItem 'C:\Program Files\MySQL' -Recurse -Filter mysql.exe -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
}

# SQL file: repo clone, or after the next Jenkins copy: C:\exhibition-portal-staging\init-mysql.sql
$sql = 'C:\path\to\sarv-exhibition-portal\deploy\windows\init-mysql.sql'
Get-Content $sql | & $mysql -u root -p
```

Then set a real password (must match `portal.env.ps1`):

```sql
ALTER USER 'exhibition'@'localhost' IDENTIFIED BY 'your-db-password';
ALTER USER 'exhibition'@'127.0.0.1' IDENTIFIED BY 'your-db-password';
FLUSH PRIVILEGES;
```

MySQL Workbench connected as root can paste the same `init-mysql.sql` if you prefer a GUI.

### 2. Staging (Jenkins already built the JAR)

```powershell
notepad C:\exhibition-portal-staging\portal.env.ps1
# DATASOURCE_PASSWORD = the exhibition user password from step 1
# EXHIBITION_STAFF_BOOTSTRAP_PASSWORD = a real staff password
# SERVER_PORT = '8082'   (8081 is pharma-erp-staging; 80 is production)

# Service install is in the repo, not the install dir:
cd C:\path\to\sarv-exhibition-portal
.\deploy\windows\install-service.ps1 -Staging
```

Or skip the manual `install-service.ps1` and rebuild `exibit-portal-pipeline_poc` after passwords are set (Jenkins installs the service if missing).

### 2b. Buyer finished goods on staging (empty buy list)

Git being clean/pushed does **not** copy the local `finished_goods` snapshot. Staging `GET /api/v1/finished-goods` is `[]` until this host pulls `pharmadb`. Profile **`prod`** defaults `exhibition.pharma-erp.enabled=false`. Diagnose without secrets: `http://43.225.195.200:8082/api/v1/meta` (`pharmaErpEnabled`, `finishedGoodsActive`).

On the Windows Server, uncomment in `C:\exhibition-portal-staging\portal.env.ps1` (do not paste the password into chat):

```powershell
$env:EXHIBITION_PHARMA_ERP_ENABLED = 'true'
$env:EXHIBITION_PHARMA_ERP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3306/pharmadb?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8'
$env:EXHIBITION_PHARMA_ERP_USERNAME = 'botuser'
$env:EXHIBITION_PHARMA_ERP_PASSWORD = 'the-pharma-db-password'
$env:EXHIBITION_PHARMA_ERP_SCHEDULE = 'false'
```

Then **rebuild `exibit-portal-pipeline_poc`** so the JAR that reads `portal.env.ps1` on boot is installed. After that JAR is live, `net stop` / `net start exhibition-portal-staging` is enough — Java loads the ps1 from `%BASE%` and auto-syncs on startup when enabled. Staff **Sync finished goods** is also on `/staff` immediately after sign-in (ADMIN / MARKETING), not only the Buyers tab.

Until that Jenkins deploy, WinSW XML is stale unless you re-run `.\deploy\windows\install-service.ps1 -Staging` from the git clone.

Confirm: `finishedGoodsActive` > 0 on `/api/v1/meta` and `/api/v1/finished-goods` is a non-empty array.

Host check: `.\deploy\windows\verify-staging.ps1`.

### 3. Production only (`main` / `C:\exhibition-portal`)

Elevated, **from the repo**, not from the staging folder:

```powershell
cd C:\path\to\sarv-exhibition-portal
.\deploy\windows\deploy.ps1
# Edit C:\exhibition-portal\portal.env.ps1
.\deploy\windows\open-http-80.ps1
.\deploy\windows\install-service.ps1
net start exhibition-portal
```

Local run without Jenkins: `backend\run.ps1` (port 8080, needs MySQL 3306, user `exhibition` / `exhibition`). `mvn test` uses embedded MariaDB (mariaDB4j); no Docker. If Flyway reports a failed V1 on an empty local DB, drop the leftover tables and re-run — see `backend/README.md`. Do not `flyway repair` that state.

### Flyway failed V1 on staging (WinSW crash loop)

If `exhibition-portal-staging.err.log` shows `Detected failed migration to version 1 (poc core schema)`, Java exits and WinSW restarts every few seconds. **Stop the service first.** Staging with no valuable data — drop and recreate the DB (same advice as local; do **not** `flyway repair`):

```powershell
net stop exhibition-portal-staging
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
# As root (or any admin MySQL user):
& $mysql -u root -p -e "DROP DATABASE IF EXISTS exhibition_portal; CREATE DATABASE exhibition_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON exhibition_portal.* TO 'exhibition'@'localhost'; GRANT ALL PRIVILEGES ON exhibition_portal.* TO 'exhibition'@'127.0.0.1'; FLUSH PRIVILEGES;"
net start exhibition-portal-staging
Start-Sleep -Seconds 25
Invoke-WebRequest http://127.0.0.1:8082/actuator/health -UseBasicParsing
```

Do not paste `portal.env.ps1` / WinSW XML password lines into chat.

### Staging deploy: port 8082 already in use (2026-09-10)

Jenkins `exibit-portal-pipeline_poc` on commit `9ba0e85` built frontend + Maven (81 tests) and then **Deploy to Staging failed**. Flyway on host MySQL was already at **v11**. The new Java process logged:

`Web server failed to start. Port 8082 was already in use.`

`install-service.ps1` used to **kill the running `java.exe` while WinSW was still Running**. WinSW `onfailure restart` (10s) can bind 8082 again before Jenkins `net start`. Deploy now **stops the service first**, waits until TCP **8082** has no LISTEN, then starts. Rebuild `poc` after this change.

If a leftover listener remains on the host:

```powershell
net stop exhibition-portal-staging
Get-NetTCPConnection -LocalPort 8082 -State Listen |
  ForEach-Object { Get-Process -Id $_.OwningProcess | Format-List Id, ProcessName, Path }
```

**Secrets:** an older failure dump printed WinSW `<env value="...">` lines. Jenkins now redacts `value="***"`. Rotate `DATASOURCE_PASSWORD` and `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` on the host (MySQL user + `portal.env.ps1`) if those values were copied into a chat or log archive. Do not paste the new passwords here.

**`$PID` is read-only:** PowerShell’s automatic `$PID` is the current process id. A `foreach ($pid in …)` in `install-service.ps1` crashed Jenkins (`Cannot overwrite variable PID`). Use `$owningPid` (never `$pid`).

## Jenkins (copy of pharma-erp flow)

Root **`Jenkinsfile`**. One agent: **Checkout → Frontend (npm) → Maven → Deploy**.

| Stage | What it does |
|---|---|
| **Checkout** | Clone repo |
| **Frontend** | `frontend/`: `npm ci` and `npm run build` (skipped if `SKIP_MAVEN_BUILD=true`). On Windows, PATH is prefixed with `C:\Program Files\nodejs` / `NODE_HOME` because the Jenkins service does not see an interactive user PATH. |
| **Maven** | `backend/`: `mvn clean compile`, `mvn test`, `mvn package -DskipTests` — JDK **Java17** |
| **Run (Smoke) / Validate** | Unix only (same skip as pharma-erp on Windows) |
| **Deploy to Staging** | Branches **`dev`** and **`poc`**: create `C:\exhibition-portal-staging\`, copy JAR + `start-portal.ps1`, seed `portal.env.ps1`, pin **`SERVER_PORT=8082`** (8081 is **pharma-erp-staging**), run **`install-service.ps1 -Staging`** (WinSW wrapper; **net stop first**, wait until 8082 is free, then `net start`). Failure dumps redact WinSW env values. |
| **Deploy to Production** | Branch **`main`** only: same copy into `C:\exhibition-portal\`, WinSW install, then `net start exhibition-portal` (port **80**). **`poc` never deploys production.** |
| **Health Check** | **`main`**: `http://127.0.0.1/actuator/health`. **`dev`/`poc`**: `http://127.0.0.1:8082/actuator/health` |

Parameters (same idea as pharma-erp):

- **`SKIP_MAVEN_BUILD`** default **false**. Set **true** only to deploy a JAR already on disk.
- **`JAR_SOURCE`** optional absolute path on the agent (like pharma **`WAR_SOURCE`**).

Create the Jenkins job as a **Pipeline from SCM** (or Multibranch) pointing at this repo, same as pharma-erp. Job `exibit-portal-pipeline_poc` tracks branch **`poc`** and deploys **staging**, not production.

**First staging deploy:** Job `exibit-portal-pipeline_poc` on **`poc`** now runs Deploy. It creates `C:\exhibition-portal-staging`, copies the JAR, and Jenkins LocalSystem runs `deploy\windows\install-service.ps1 -Staging` if the service is missing. **`start-portal.ps1` refuses placeholder passwords.** After the first copy, edit secrets, then rebuild:

```powershell
notepad C:\exhibition-portal-staging\portal.env.ps1
# DATASOURCE_PASSWORD + EXHIBITION_STAFF_BOOTSTRAP_PASSWORD; SERVER_PORT=8082 (8081 is pharma-erp)
# MySQL: full path to mysql.exe — see “One-time host setup”. Do not run deploy.ps1 from this folder.
```

Staging must **not** bind port 80 (production) or **8081** (**pharma-erp-staging** on this host). Exhibition staging is **8082**. The first Jenkins copy seeded `portal.env.ps1` with 8081; the next staging deploy rewrites `SERVER_PORT` to 8082. You can also edit it now.

**Node on the Windows agent:** Pharma-erp does not run npm. This pipeline does. The Jenkins Windows service runs as SYSTEM (`...\systemprofile\...`) and does **not** inherit PATH from a logged-in admin. If the log says `'npm' is not recognized`, install Node 22 into `C:\Program Files\nodejs` (all users), or set agent env `NODE_HOME` to the folder that contains `npm.cmd`, then **restart Jenkins**. The Frontend stage also prepends those folders to PATH.

**Branch vs deploy:** Staging = **`dev`** or **`poc`** → `C:\exhibition-portal-staging` (port **8082**). Production = **`main`** only → `C:\exhibition-portal` (port 80). **8081 is pharma-erp-staging.** A green Maven stage is not a deploy.

PowerShell `$` in the Jenkinsfile is escaped as `\$` so Groovy does not treat it as a Jenkins binding (same pharma-erp rule). The file is a Groovy script: comments must be `//` or `/* */`. A leading `#` is parsed as a shebang and Jenkins fails with `expecting '!', found ' '`. Inside the deploy `powershell """ ... """` GString, PowerShell regex such as `\s` is an **invalid Groovy escape** and the job fails at parse (`unexpected char: '\'`) before any stage runs. Use `Get-NetTCPConnection` or split `netstat` on spaces instead.

## Files

| Path | Role |
|---|---|
| `Jenkinsfile` | Build + Windows service deploy |
| `backend/pom.xml` | Java **17**, `finalName` `exhibition-portal` |
| `backend/src/main/resources/application-prod.properties` | Port 80, MySQL 3306, public CORS |
| `deploy/windows/deploy.ps1` | Manual `npm` + `mvn` + copy JAR |
| `deploy/windows/install-service.ps1` | **WinSW** service: runs **`java.exe -jar`** directly with env from `portal.env.ps1` (not powershell wrapper — that exited and left Status=Stopped). Downloads WinSW-x64 once. Resolves Java 17 / `JAVA_HOME`. |
| `deploy/windows/init-mysql.sql` | Create database + user |
| `backend/run.ps1` | Local `spring-boot:run` |

Maven copies `frontend/dist` into the JAR when `frontend/dist/index.html` exists (`with-frontend` profile).

## After first boot

Staff passwords for seeded `poc-*` `app_users` are rotated to `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (bcrypt). Accounts created in `/admin` keep the password set there. You can unset that env later; hashes stay in MySQL.
