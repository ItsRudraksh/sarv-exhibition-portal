# Deploy on Windows Server (public IP)

**Updated:** 3 September 2026  
**Target:** `http://43.225.195.200/`  
**Runtime:** **Java 17** only (server is `17.0.18`). Same delivery shape as pharma-erp: Jenkins on the Windows agent, native database, **no Docker**.

The visitor UI and API ship as **one Spring Boot JAR** (`backend/target/exhibition-portal.jar`). Production does **not** use `npm run dev` or Vite port 5173.

## Honest limits

| Topic | Production behaviour |
|---|---|
| URL | `http://43.225.195.200/` (visitor) and `http://43.225.195.200/staff` (internal) |
| Java | **17** (`javac`/`java` 17.0.x). Do not build with Java 21 bytecode. |
| Camera | In-page `getUserMedia` needs HTTPS. On HTTP, visitors **upload** a photo or use the phone file picker. |
| Auth | Set `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` on first start. Do not expose `{noop}poc-staff`. |
| MySQL | Native MySQL 8 on **127.0.0.1:3306**. Do not publish 3306 on `0.0.0.0`. Docker is not used. |
| OCR / CRM / vendor API | Still not live. Outbox writes local stub files. |

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
# After the next Jenkins copy:
powershell -NoProfile -ExecutionPolicy Bypass -File C:\exhibition-portal-staging\verify-staging.ps1
# Or from a git clone:
.\deploy\windows\verify-staging.ps1
```

Paste the full console (RESULT line included) into chat.

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

Local run without Jenkins: `backend\run.ps1` (port 8080, needs MySQL 3306). `mvn test` uses embedded MariaDB (mariaDB4j); no Docker.

## Jenkins (copy of pharma-erp flow)

Root **`Jenkinsfile`**. One agent: **Checkout → Frontend (npm) → Maven → Deploy**.

| Stage | What it does |
|---|---|
| **Checkout** | Clone repo |
| **Frontend** | `frontend/`: `npm ci` and `npm run build` (skipped if `SKIP_MAVEN_BUILD=true`). On Windows, PATH is prefixed with `C:\Program Files\nodejs` / `NODE_HOME` because the Jenkins service does not see an interactive user PATH. |
| **Maven** | `backend/`: `mvn clean compile`, `mvn test`, `mvn package -DskipTests` — JDK **Java17** |
| **Run (Smoke) / Validate** | Unix only (same skip as pharma-erp on Windows) |
| **Deploy to Staging** | Branches **`dev`** and **`poc`**: create `C:\exhibition-portal-staging\`, copy JAR + `start-portal.ps1`, seed `portal.env.ps1`, pin **`SERVER_PORT=8082`** (8081 is **pharma-erp-staging**), install service `exhibition-portal-staging` if missing, then `net start` |
| **Deploy to Production** | Branch **`main`** only: same copy into `C:\exhibition-portal\`, then `net start exhibition-portal` (port **80**). **`poc` never deploys production.** |
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

PowerShell `$` in the Jenkinsfile is escaped as `\$` so Groovy does not treat it as a Jenkins binding (same pharma-erp rule). The file is a Groovy script: comments must be `//` or `/* */`. A leading `#` is parsed as a shebang and Jenkins fails with `expecting '!', found ' '`.

## Files

| Path | Role |
|---|---|
| `Jenkinsfile` | Build + Windows service deploy |
| `backend/pom.xml` | Java **17**, `finalName` `exhibition-portal` |
| `backend/src/main/resources/application-prod.yml` | Port 80, MySQL 3306, public CORS |
| `deploy/windows/deploy.ps1` | Manual `npm` + `mvn` + copy JAR |
| `deploy/windows/install-service.ps1` | Windows service for `net stop` / `net start` |
| `deploy/windows/init-mysql.sql` | Create database + user |
| `backend/run.ps1` | Local `spring-boot:run` |

Maven copies `frontend/dist` into the JAR when `frontend/dist/index.html` exists (`with-frontend` profile).

## After first boot

Staff passwords for ACTIVE `app_users` are rotated to `EXHIBITION_STAFF_BOOTSTRAP_PASSWORD` (bcrypt). You can unset that env later; hashes stay in MySQL.
