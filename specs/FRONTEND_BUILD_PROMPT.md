# Fresh-chat prompt: build the frontend

> **Current implementation (1 Sep 2026):** The visitor app in `frontend/` talks to the Spring Boot API. Card and catalogue **uploads are live** (private disk + `file_assets`). OCR, QR decode, CRM, and vendor upsert are still not live. Use [PLATFORM_CONTEXT.md](PLATFORM_CONTEXT.md) and [BUILD-PLAN.md](BUILD-PLAN.md) as current product/delivery SSOT. The prompt below is the original client-only contract.

Copy everything below into a fresh Codex chat opened at `C:\Coding\Sarv\Exhibition-Portal`.

---

You are the implementation owner for the **Sarv Biolabs Exhibition Portal** frontend.

Your task is to build the working, high-fidelity visitor portal in `C:\Coding\Sarv\Exhibition-Portal\frontend`. Work end to end: establish the required context first, then implement, run, inspect, and fix the frontend. Do **not** stop after a plan or a visual mockup.

The existing `frontend/` directory is a React 19 + Vite + TypeScript scaffold with dependencies installed. Preserve that application and replace its default starter UI; do not create a second frontend elsewhere. There is no backend yet, so build a complete client-side prototype with a clean API seam, but do not pretend that OCR, QR decoding, uploads, AI, persistence, or integrations are live services.

## Working rules

- Work only within `C:\Coding\Sarv\Exhibition-Portal`, with implementation changes confined to `frontend/` unless a tiny frontend-specific note is genuinely needed outside it.
- Treat existing source/design files as user-owned. Inspect before changing; do not delete or overwrite unrelated work.
- Do not modify the historical `.stitch/`, `raw/`, `concepts/`, database, HLD, or context sources merely to make the frontend easier.
- Do not read or enumerate `frontend/node_modules`; use `rg --files frontend -g '!frontend/node_modules/**'` or equivalent to inspect project source.
- Use `apply_patch` for source-file edits.
- Use the **`stitch::react-components` skill**. Read its `SKILL.md` in full before writing implementation code, then follow its relevant instructions. It directly applies because this task converts approved Stitch screens into modular Vite/React components.
- If that skill explicitly routes you to another project-local instruction, read that full referenced instruction too. Do not use a skill simply to add unnecessary technology.
- Start with a concise commentary update describing the evidence you are reading, then proceed without waiting for a design/plan approval.

## Mandatory context pass before coding

Read/view the following completely before editing code. This is required to avoid rebuilding an obsolete version of the portal.

### Product, architecture, and data

1. Read `specs/PLATFORM_CONTEXT.md` **fully**, especially the decision-precedence, canonical visitor flow, non-regression rules, visual system, active-asset, database-baseline, and open-decision sections.
2. Read `raw/ABOUT-PLATFORM.TXT`, `raw/tentative-user-flow.md`, and `raw/tentative-system-design.md` fully. The diagrams are historical, so do not revive their old mandatory buyer steps.
3. Read the whole `raw/biotech-exhibition-inquiry-portal-hld.pdf`: extract text and render/view all pages, not just the text extraction. Use the PDF workflow/skill if it is available.
4. Read `specs/DATABASE-DESIGN.md` and `specs/exhibition_portal_schema.sql` fully. The frontend should honour their input, state, consent, file, taxonomy, and lifecycle constraints even though it will use mock data locally.
5. Read `raw/sarvbiolabs-brochure.pdf` fully and visually inspect it. Use it only for factual brand/domain orientation, never to invent claims, data, commitments, or contact information in the UI.

### Design, visual evidence, and history

6. Read `.stitch/DESIGN.md`, `.stitch/DESIGN.dark.md`, `.stitch/MASTER_TASKS.md`, and `.stitch/metadata.json` fully. `MASTER_TASKS.md` has stale historical entries; resolve conflicts according to the precedence in `specs/PLATFORM_CONTEXT.md`.
7. View every PNG in `.stitch/designs/final-flow-pngs/` at original resolution in numerical order (`001` through `011`). These are the canonical visual-flow references.
8. Read the matching 11 HTML exports in `.stitch/designs/` to understand content hierarchy and interactions. They are design references only, not production code and not a license to retain their hard-coded sample values.
9. View/read the original concept directions in `concepts/` and `raw/` (including `concepts/design-system-directions.png`, all three concept HTML pages, and the raw flow/system PNGs) so you understand what was selected versus superseded.
10. Inspect the Sarv logo source (`raw/sarv-bio-labs-logo-1.png` and the current local design logo) and use an appropriate local asset in the frontend. Do not hotlink remote assets.
11. Inspect the existing frontend source and `frontend/package.json` after the evidence pass. Keep the dependency footprint small and do not add a component library, router, or global state library unless it is genuinely necessary.

After this pass, state in one short update that you have reviewed the required evidence and name any real source conflict you will handle. Then begin the build immediately.

## Current product contract - do not change it

This is a single reusable responsive web inquiry portal. Exhibition QR entry and later normal-web entry use the same product.

### Public routes

- **I want to sell** means supplier/vendor intake for Sarv. It is not a marketplace seller flow.
- **I want to buy** means a potential Sarv customer is making a product inquiry. This route must be much faster because friction can lose a potential customer.

### Canonical visitor flow

```text
Entry
  -> business-card capture (front + back) OR manual continuation
  -> review/confirm name, work email, mobile number, country code
  -> create/update a resumable draft
  -> select I want to sell / I want to buy

Supplier
  -> select one or more departments
  -> select relevant product types filtered by departments
  -> smart details check: preview extracted/saved values; show only missing required fields
  -> review: at least a website OR a catalogue is required
  -> submit supplier inquiry -> confirmation / internal review

Buyer
  -> one required product-or-requirement description
  -> optional product-area search and collapsed optional specifications
  -> review need + saved contact
  -> submit inquiry -> confirmation / sales or marketing follow-up
```

### Non-negotiables

- The card scan, upload, voice assistance, and location evidence are optional assists. Manual fallback is mandatory.
- There is no live OCR/QR/camera/AI service. Create truthful frontend demo states only.
- A QR payload detected on a card is an **internal-only** saved value. Never expose it to the visitor, display a destination, or redirect/open it.
- The frontend must show that details are reviewable/correctable. AI or card-derived values cannot silently replace confirmed values.
- Use client-side state/draft persistence only as a mock of the planned server-side draft model. Make it robust enough to demonstrate resume/reload during this prototype, but do not claim it is secure production persistence.
- Buyer minimum path: confirmed contact plus one product/requirement statement. Product area, quantity, pack size, standards, needed-by date, notes, and buyer company information are optional/progressive.
- Supplier minimum path: company, contact, reliable contact method, department, product type, and website or catalogue. Use nonidentifying placeholder/mock data, never a fake real person/company.
- Do not force an obsolete mandatory buyer category/standard/profile form. `PP` is not a valid standard; controlled standards are IP, USP, BP, and EP when the user elects to provide one.
- Supplier submission only enters internal review. Do not claim approval, vendor creation, availability, a response SLA, a tracking number, or an external integration result.
- Location must remain consent-based. Do not implement invisible location collection or raw-IP collection.
- Do not invent phone numbers, email addresses, employees, customer data, product availability, product specifications, lead metrics, or regulatory claims.

## Build scope

Build the visitor-flow frontend now. Internal admin/queue screens are deliberately out of scope; leave a clean seam for later work but do not fabricate them.

### Required implementation behavior

Implement all eleven current visitor screens as actual React states/routes/views, with coherent forward/back navigation and keyboard support:

1. Business-card capture start.
2. Extracted/saved contact-details confirmation.
3. Intent selection.
4. Supplier department multi-select.
5. Supplier product-type selection filtered from chosen departments.
6. Supplier smart-details check, including a realistic missing-required-data state.
7. Supplier review/submit.
8. Supplier submission confirmation.
9. Buyer rapid need capture.
10. Buyer review/submit.
11. Buyer submission confirmation.

Use an explicit, typed inquiry-draft model and keep app state in a focused feature/module rather than one huge `App.tsx`. A reasonable shape is a `features/inquiry/` area for types, static taxonomy/mock data, validation, draft persistence adapter, reusable screen components, and journey orchestration. The exact file layout is your judgement, but the result must be readable and maintainable.

Implement these interactions:

- Card front/back capture UI with file inputs and preview metadata. Since camera/OCR is not real, make the UI truthful and preserve a manual-continuation path. Do not run fake OCR or pretend files were uploaded remotely.
- Contact form with proper labels, validation, country-code selection, editable values, and saved-draft feedback.
- Persist non-sensitive prototype draft state to `sessionStorage` or `localStorage` through an isolated adapter; include a visible, accessible way to restart/clear the demo. Explain in the README that this is a prototype only, not a production privacy solution.
- Intent selection after a valid contact checkpoint.
- Supplier department checkboxes/search, then product types derived from selected departments. Do not hard-code all choices into JSX; use typed mock taxonomy data.
- Supplier smart details page that can show both an extracted-data preview and an incomplete-data state which asks only for the required missing fields. Keep optional company details collapsed.
- Supplier review validation: website URL and/or a local catalogue file selection. Treat a selected file as local-only and clearly avoid a claim that it was uploaded/scanned.
- Buyer requirement textarea with one required value; optional product-area search and progressive specification disclosure. Validate only what is genuinely required.
- Buyer/supplier review pages with edit-back navigation that preserves the draft.
- Confirmation pages that use dynamic, nonidentifying submitted values and restrained, accurate “what happens next” copy.
- Responsive behavior from a 390px mobile layout upward. Desktop must be a thoughtful responsive expansion of the new scan-first flow, not a resurrection of the old desktop mockups.

### Visual and accessibility requirements

Implement Alpine Blue faithfully from `.stitch/DESIGN.md`:

- Alpine Paper `#F7F8F5`, Pure Surface `#FFFFFF`, Research Ink `#113944`, Measured Slate `#5C747C`, Glass Border `#C9D9DF`, Sarv Process Blue `#147A9A`, and Blue Mist `#D7F0F7`.
- Plus Jakarta Sans for interface/body if it can be loaded reliably without an unnecessary dependency; Fraunces only for the public editorial entry headline; JetBrains Mono only for compact metadata. Use a sensible local/system fallback chain.
- Use a single-column mobile task layout, 18px page edges, full-width primary actions, 48px minimum control height, clear labels, 10px corners, hairline structure, and strong visible focus states.
- Preserve the calm, high-trust pharmaceutical B2B character. Do not add gradients, purple, neon, glassmorphism, generic dashboard cards, fake statistics, emoji, marketing-card grids, or generic Vite/React visuals.
- Use native semantic controls whenever possible. Every input needs a label; errors need text and programmatic association; buttons need visible focus; route/status messaging should work with keyboard and screen readers.
- Respect `prefers-reduced-motion`. Keep motion restrained: subtle opacity/transform feedback only.
- Use the actual logo asset locally with meaningful alt text. Do not use remote image URLs.

## Engineering constraints

- TypeScript must remain strict and `npm run build` must pass.
- `npm run lint` must pass, or any existing lint limitation must be clearly identified and corrected if within scope.
- Do not leave the Vite default starter UI, default styles, fake counter, React/Vite logos, or unused starter assets in the rendered product.
- Do not add a backend, fake HTTP endpoints, auth, analytics, payment, external tracking, or external SaaS calls.
- Keep mock taxonomy and draft state clearly labelled as local/prototype data. Do not bake sample screen text into domain logic.
- Do not make design assets or historical `.stitch` HTML files the runtime UI by embedding them in iframes.
- Do not delete `node_modules` or alter lockfiles unless a necessary, deliberate dependency change requires it.

## Verification and finish criteria

Before calling the work complete:

1. Run `npm run lint` and `npm run build` in `frontend/` and fix failures.
2. Start the Vite app and visually inspect the implemented flow in a browser at mobile width (390px) and a desktop width. Use the browser/computer workflow available to you rather than relying only on code review.
3. Walk both paths end to end, including:
   - manual card fallback;
   - card-first/contact-confirmation flow;
   - valid and invalid contact input;
   - supplier selection/filtering, missing detail state, and website-or-catalogue validation;
   - buyer minimum path and optional specification path;
   - edit/back navigation, reload/resume behavior, and restart/clear behavior.
4. Compare the visual result against all 11 ordered final-flow references. Correct material hierarchy, spacing, typography, or interaction mismatches.
5. Add/update `frontend/README.md` with: start/build/lint commands; client-only prototype boundaries; mock draft persistence; and the source design/context files used.

In your final response, report:

- what was implemented;
- the validation commands and outcomes;
- any deliberately mocked/unimplemented behavior because the backend/provider/policy is not yet available;
- the key changed files as clickable absolute paths;
- any genuine blocker or decision that needs the user's input.

Do not ask the user to repeat product context that is already available in this workspace. Start the evidence pass, then build.

