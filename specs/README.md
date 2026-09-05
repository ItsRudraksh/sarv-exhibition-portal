# Specs index

**Documentation status:** Updated 5 Sep 2026 — MySQL 8; Java 17 + Jenkins Windows deploy; Phases 1–8 + business taxonomy v1 (Flyway V7).

Canonical product and delivery documents for the **Sarv Biolabs Exhibition Portal**. Historical evidence stays in `raw/`, `concepts/`, and `.stitch/`. Do not treat those as current implementation requirements.

## Read order

1. **[PLATFORM_CONTEXT.md](PLATFORM_CONTEXT.md)** — product, flow, non-negotiables, open decisions.
2. **[BUILD-PLAN.md](BUILD-PLAN.md)** — phased Java + React delivery plan (POC status lives here).
3. **[DATABASE-DESIGN.md](DATABASE-DESIGN.md)** and [exhibition_portal_schema.sql](exhibition_portal_schema.sql) — full data baseline; applied schema is Flyway V1–V7 in `backend/`.
4. **[taxonomy/](taxonomy/)** — business-owned departments, product types, mappings (v1).
5. **[TESTING.md](TESTING.md)** — how to verify after a change.
6. **[DEPLOY-WINDOWS.md](DEPLOY-WINDOWS.md)** — public Windows Server (`http://43.225.195.200/`).
7. **[FRONTEND_BUILD_PROMPT.md](FRONTEND_BUILD_PROMPT.md)** — historical contract used to build the visitor prototype; still useful for screen list and visual rules.

When sources conflict, **PLATFORM_CONTEXT.md** decision precedence wins. Visual tokens: `.stitch/DESIGN.md`. Visitor code: `frontend/`. API: `backend/`.

## Inventory

| File | Role |
|---|---|
| `PLATFORM_CONTEXT.md` | Product SSOT and durable handoff |
| `BUILD-PLAN.md` | Implementation phases, Java stack, module layout, DoD, POC status |
| `DATABASE-DESIGN.md` | Logical/physical design + POC deviations; **applied store is MySQL 8** |
| `exhibition_portal_schema.sql` | Historical PostgreSQL singleton DDL (full target; **not** Flyway V1; not applied) |
| `TESTING.md` | Lint, build, `mvn test`, browser smoke |
| `DEPLOY-WINDOWS.md` | Windows Server + Jenkins public-IP runbook (`http://43.225.195.200/`, Java 17, no Docker) |
| `taxonomy/` | Business-owned departments, product types, mappings (v1); change process |

## Not specs (do not move here)

- `raw/` — original HLD, brochure, superseded flow diagrams
- `.stitch/` — Alpine Blue design system and screen exports
- `frontend/` — React visitor app
- `backend/` — Spring Boot POC
- `.cursor/rules/` — agent workflow rules
