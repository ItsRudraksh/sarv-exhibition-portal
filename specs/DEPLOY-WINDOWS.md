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
- **Maven 3.9+** and **Node 22+** on the Jenkins agent PATH (same box as pharma-erp)
- **MySQL 8** (Windows installer, same engine as pharma-erp). Create DB/user with `deploy/windows/init-mysql.sql`
- Jenkins with tools named **`Java17`** and **`Maven3`** (same ids as pharma-erp)
- Windows service **`exhibition-portal`** (once): `deploy/windows/install-service.ps1`
- Inbound **TCP 80** (Windows Firewall + cloud firewall)
- IIS stopped if it owns port 80 (`Stop-Service W3SVC`)

Do not open 5173 or 3306 on the public IP. Do not install Docker for this app.

## One-time host setup

Elevated PowerShell, repo checkout on the server:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
cd C:\path\to\sarv-exhibition-portal

# Native MySQL 8 (PowerShell example)
Get-Content deploy\windows\init-mysql.sql | mysql -u root -p
# then: ALTER USER 'exhibition'@'localhost' IDENTIFIED BY 'your-db-password';

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
| **Frontend** | `frontend/`: `npm ci` and `npm run build` (skipped if `SKIP_MAVEN_BUILD=true`) |
| **Maven** | `backend/`: `mvn clean compile`, `mvn test`, `mvn package -DskipTests` — JDK **Java17** |
| **Run (Smoke) / Validate** | Unix only (same skip as pharma-erp on Windows) |
| **Deploy to Staging** | Branch **`dev`**, Windows: `net stop exhibition-portal-staging`, copy JAR to `C:\exhibition-portal-staging\`, `net start` |
| **Deploy to Production** | Branch **`main`**, Windows: `net stop exhibition-portal`, copy JAR to `C:\exhibition-portal\`, `net start` |
| **Health Check** | **`main`**: wait for HTTP 200 on `http://127.0.0.1/actuator/health` |

Parameters (same idea as pharma-erp):

- **`SKIP_MAVEN_BUILD`** default **false**. Set **true** only to deploy a JAR already on disk.
- **`JAR_SOURCE`** optional absolute path on the agent (like pharma **`WAR_SOURCE`**).

Create the Jenkins job as a **Pipeline from SCM** pointing at this repo, same as pharma-erp. Staging service (optional): copy `start-portal.ps1` + `portal.env.ps1` to `C:\exhibition-portal-staging` and run `.\deploy\windows\install-service.ps1 -Staging` with `SERVER_PORT=8081` in that env file.

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
