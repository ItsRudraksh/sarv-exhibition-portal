# Sarv Biolabs Exhibition Portal

Reusable QR and website inquiry portal: **I want to sell** (supplier intake) and **I want to buy** (customer lead).

**Start here:** [specs/README.md](specs/README.md) · product [specs/PLATFORM_CONTEXT.md](specs/PLATFORM_CONTEXT.md) · delivery [specs/BUILD-PLAN.md](specs/BUILD-PLAN.md) · public Windows host [specs/DEPLOY-WINDOWS.md](specs/DEPLOY-WINDOWS.md)

| Path | Role |
|---|---|
| `specs/` | Product, database, build plan, testing, Windows/Jenkins deploy |
| `frontend/` | React + Vite visitor app + `/staff` review (HTTP to Java, `localStorage` fallback for visitors) |
| `backend/` | Java 17 Spring Boot POC (Flyway V1–V5) |
| `Jenkinsfile` | Same agent flow as pharma-erp: npm + Maven (Java17) + Windows service |
| `deploy/windows/` | Native MySQL 3306, Windows service, `http://43.225.195.200/` |
| `raw/`, `.stitch/`, `concepts/` | Historical HLD, brochure, and design exports |

## POC (Phases 1–5)

```bash
cd backend
mvn test
.\run.ps1
```

In another terminal: `cd frontend && npm run dev`.

Needs **native MySQL 8** on `localhost:3306` (user/db `exhibition`). `mvn test` uses embedded MariaDB. Docker is not required.

UI: `https://localhost:5173` · staff: `https://localhost:5173/staff` · API: `http://localhost:8080`.

**Public Windows Server:** Java 17 JAR + Jenkins — [specs/DEPLOY-WINDOWS.md](specs/DEPLOY-WINDOWS.md).

Out of this slice: OCR, live CRM, live vendor API. Details: [backend/README.md](backend/README.md).
