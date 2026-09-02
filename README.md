# Sarv Biolabs Exhibition Portal

Reusable QR and website inquiry portal: **I want to sell** (supplier intake) and **I want to buy** (customer lead).

**Start here:** [specs/README.md](specs/README.md) · product [specs/PLATFORM_CONTEXT.md](specs/PLATFORM_CONTEXT.md) · delivery [specs/BUILD-PLAN.md](specs/BUILD-PLAN.md)

| Path | Role |
|---|---|
| `specs/` | Product, database, build plan, testing |
| `frontend/` | React + Vite visitor app + `/staff` review (HTTP to Java, `localStorage` fallback for visitors) |
| `backend/` | Java 21 Spring Boot POC (Flyway V1–V5) |
| `docker-compose.yml` | Postgres 16 on host port 5433 |
| `raw/`, `.stitch/`, `concepts/` | Historical HLD, brochure, and design exports |

## POC (Phases 1–5)

```bash
docker compose up -d db
cd backend && mvn spring-boot:run
cd frontend && npm run dev
```

UI: `https://localhost:5173` · staff: `https://localhost:5173/staff` · API: `http://localhost:8080` · tests: `cd backend && mvn test` (embedded Postgres; Docker not required).

Out of this slice: OCR, live CRM, live vendor API. Details: [backend/README.md](backend/README.md).
