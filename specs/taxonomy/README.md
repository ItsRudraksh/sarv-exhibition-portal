# Business-owned taxonomy (v1)

**Status:** Active content pack for supplier department / product-type selection.  
**Updated:** 5 September 2026  
**Owner:** Sarv exhibition programme owner (assign a named person before each stall; request changes via this folder + a Flyway migration).  
**Change process:** Edit the CSVs below → add/adjust `backend/src/main/resources/db/migration/V*_*.sql` → update `frontend/src/features/inquiry/taxonomy.ts` offline fallback → `mvn test` + supplier smoke.

## Scope of v1

| Included | Deferred |
|---|---|
| Departments, product types, dept↔type mappings | Full Sarv `products` / `product_standards` catalogue |
| Pharmacopoeial standards **IP, USP, BP, EP** only (no **PP**) | Extra standards (e.g. FP) unless business elects |
| Supplier minimum: ≥1 department + ≥1 mapped product type | Admin CRUD UI (`TAXONOMY_MANAGER` role exists; no screens yet) |

Source of labels: approved Stitch supplier department screen + the prior POC seed mapping, promoted to production configuration so agents stop treating it as disposable sample data.

## Files

1. [`departments.csv`](departments.csv) — `code`, `name`, `display_order`
2. [`product_types.csv`](product_types.csv) — `code`, `name`, `display_order`
3. [`mappings.csv`](mappings.csv) — `department_code`, `product_type_code`, `display_order`

Stable UUIDs for these rows live in Flyway **V7** (`a100…` departments, `a200…` product types). Historical POC rows (`1000…` / `2000…`) are archived (`is_active = 0`, codes prefixed `poc_`).

## Pharmacopoeial standards

Confirmed for buyer optional specs: **IP**, **USP**, **BP**, **EP**. Seeded in V2; unchanged by V7.
