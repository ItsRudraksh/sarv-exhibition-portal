# Specs index

**Documentation status:** Updated 1 Sep 2026 — Phases 1–5 implemented; specs under `specs/`.

Canonical product and delivery documents for the **Sarv Biolabs Exhibition Portal**. Historical evidence stays in `raw/`, `concepts/`, and `.stitch/`. Do not treat those as current implementation requirements.

## Read order

1. **[PLATFORM_CONTEXT.md](PLATFORM_CONTEXT.md)** — product, flow, non-negotiables, open decisions.
2. **[BUILD-PLAN.md](BUILD-PLAN.md)** — phased Java + React delivery plan (POC status lives here).
3. **[DATABASE-DESIGN.md](DATABASE-DESIGN.md)** and [exhibition_portal_schema.sql](exhibition_portal_schema.sql) — full data baseline; applied schema is Flyway V1–V5 in `backend/`.
4. **[TESTING.md](TESTING.md)** — how to verify after a change.
5. **[FRONTEND_BUILD_PROMPT.md](FRONTEND_BUILD_PROMPT.md)** — historical contract used to build the visitor prototype; still useful for screen list and visual rules.

When sources conflict, **PLATFORM_CONTEXT.md** decision precedence wins. Visual tokens: `.stitch/DESIGN.md`. Visitor code: `frontend/`. API: `backend/`.

## Inventory

| File | Role |
|---|---|
| `PLATFORM_CONTEXT.md` | Product SSOT and durable handoff |
| `BUILD-PLAN.md` | Implementation phases, Java stack, module layout, DoD, POC status |
| `DATABASE-DESIGN.md` | Approved PostgreSQL logical/physical design + POC deviations |
| `exhibition_portal_schema.sql` | Singleton DDL (full target; not Flyway V1) |
| `TESTING.md` | Lint, build, `mvn test`, browser smoke |
| `FRONTEND_BUILD_PROMPT.md` | Original visitor-frontend implementation contract |

## Not specs (do not move here)

- `raw/` — original HLD, brochure, superseded flow diagrams
- `.stitch/` — Alpine Blue design system and screen exports
- `frontend/` — React visitor app
- `backend/` — Spring Boot POC
- `.cursor/rules/` — agent workflow rules
