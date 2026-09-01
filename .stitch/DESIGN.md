# Design System: Sarv Biolabs Exhibition Portal — Alpine Blue

**Stitch project:** `16252155655979346180`  
**Primary platform:** Mobile web, 390px first  
**Variant:** Base / light

## 1. Visual Theme & Atmosphere

Alpine Blue combines Sarv Biolabs' Himalayan-origin story with the clarity expected of a pharmaceutical B2B workflow. It feels measured, assured, and quietly human: a research notebook that has been engineered for a busy exhibition floor.

The system is deliberately calm rather than decorative. Blue carries brand recognition and action; pale mineral surfaces and botanical-blue landscape illustrations give the experience a sense of origin without becoming rustic, medicinal, or consumer-wellness oriented. The visitor sees one clear decision at a time and always has a manual path forward.

**Design dials:** creativity 5, density 5, variance 5, motion intent 4. Use asymmetric editorial composition only in the opening context panel; use direct, single-column task layouts for all form screens.

## 2. Color Palette & Roles

- **Alpine Paper** (`#F7F8F5`) — page canvas; a calm mineral off-white, never stark white.
- **Pure Surface** (`#FFFFFF`) — inputs, sheets, and elevated confirmation surfaces.
- **Research Ink** (`#113944`) — primary headings, selected-route text, and dense information.
- **Measured Slate** (`#5C747C`) — body copy, descriptions, and secondary metadata.
- **Glass Border** (`#C9D9DF`) — 1px dividers, field outlines, and low-emphasis structure.
- **Sarv Process Blue** (`#147A9A`) — the single brand accent: primary actions, active progress, focus rings, and route markers.
- **Blue Mist** (`#D7F0F7`) — selected-row tint, low-risk informational surfaces, and route icon discs.
- **Clear Blue** (`#009EC5`) — light-facing illustration plane and restricted hover/pressed variation of the primary accent.

Do not introduce green, purple, neon, gradients, or additional marketing accent colors. Success, warning, and error colours are reserved for real system states only; they must never imply a vendor approval or product availability that has not occurred.

## 3. Typography Rules

- **Display:** Plus Jakarta Sans, 600–700, track-tight (`-0.045em`), `clamp(2.5rem, 11vw, 4.25rem)`, line-height `0.96`. Use only for the entry screen and major confirmation headline.
- **Editorial display:** Fraunces, 500, track-tight (`-0.055em`), reserved for the public entry headline. It is never used in the admin workspace or form labels.
- **Interface and body:** Plus Jakarta Sans, 400–600, `1rem` minimum body size, line-height `1.55–1.65`, body measures capped at 32ch on mobile.
- **Technical metadata:** JetBrains Mono, 600, `0.6875rem–0.75rem`, uppercase only for short labels such as route or step markers.
- **Banned:** Inter, generic serif fonts, browser-default font stacks, all-caps body copy, and oversized uppercase headings.

## 4. Component Stylings

### Entry routes

The public portal begins with two equal-priority, vertically stacked route rows:

- **“I want to sell”** — supplier/vendor intake.
- **“I want to buy”** — product inquiry from Sarv's portfolio.

Each row uses a 48px pale-blue circular direction marker, a direct first-person label, one precise supporting sentence, and a right arrow. Use hairline dividers rather than floating card grids. The first-person wording is intentional: it describes the employee's immediate task, not Sarv's marketing goal.

### Actions

- Primary actions use Sarv Process Blue with white Plus Jakarta Sans 600 labels, 48–52px height, and 10px corners.
- Secondary actions are outlined in Glass Border with Research Ink text.
- Text actions are simple blue links, never underlined by default.
- Press feedback is a subtle `scale(0.98)` or `translateY(1px)`; never use glows or bouncy animation.

### Forms

- Use one-column fields, labels above fields, 12px label-to-control spacing, and helper/error text below.
- Minimum control height is 48px; each selectable department or product row has a 44px tap target.
- Inputs have white fill, a 1px Glass Border, 10px corner radius, and a 2px Sarv Process Blue focus ring with 2px offset.
- Dynamic taxonomy selection uses searchable checklists or bottom sheets; never force a dense multi-select dropdown on mobile.
- AI-assisted card scan and voice entry are optional utility actions, visually secondary to manual entry. Extracted fields are always marked as reviewable.

### Trust and consent

Use a compact left-bordered information note with Sarv Process Blue. Explain why location evidence, card scanning, or microphone use is requested before invoking permission. Never preselect consent and never block manual completion when a permission is declined.

### Upload and confirmation

- Catalogue upload uses a dashed or hairline drop zone with plain file-type guidance and a compact file list.
- Confirmation uses a single substantial surface with a clear state title, what happens next, and a reference placeholder such as `[submission reference]`. Do not fabricate tracking numbers or response times.

### Admin surfaces

Admin screens are operationally denser but retain the same blue/ink system. Use table-like vertical lists, status chips for genuine workflow states, and a persistent review summary. The “Add to production” action must remain separate from ordinary edits and require explicit confirmation in the implementation.

## 5. Layout Principles

- Mobile-first at 390px with `18px` page edges, a single content column, and no horizontal overflow.
- Major content gaps use `clamp(1.5rem, 6vw, 2.5rem)`; form sections use 24px gaps; field stacks use 16px gaps.
- The public entry screen may include a compact, non-overlapping Alpine Blue landscape illustration. It is a contextual brand marker, not a decorative hero image.
- Use full-width route rows and full-width primary actions on mobile. Tablet and desktop increase whitespace and can place supporting context beside form content, but the task sequence remains vertically legible.
- Keep all screen content in normal document flow. No overlapping copy, absolute-positioned headline treatments, floating decorative gradients, or three-card feature rows.

## 6. Motion & Interaction Intent

- Use a restrained spring: stiffness 170, damping 24.
- Route rows and controls receive only opacity/background/transform feedback.
- Step transitions use a short fade and 8px vertical movement; do not animate layout dimensions.
- Voice visualisation, if implemented, should respond only while voice capture is actively requested and must have a reduced-motion alternative.
- Skeletons mirror final form geometry. Never use a generic circular loader.

## 7. Content Rules

- Copy from the employee/visitor perspective: “I want to sell,” “I want to buy,” “Review my details,” and “Submit my inquiry.”
- Use precise domain language: API, intermediate, catalogue, department, pharmacopeial category, supplier review, and product inquiry.
- Keep assistance concrete: “Scan visiting card,” “Use voice to fill this section,” and “Enter manually instead.”
- Do not make unsupported claims, present fabricated metrics, use generic AI copy, or imply automatic approval.

## 8. Anti-Patterns (Banned)

- No purple/blue neon aesthetic, no gradients, no outer glows, no pure black.
- No generic dashboard metric cards, invented statistics, placeholder people, or made-up lead values.
- No emojis, generic default iconography, or “next-gen” language.
- No three equal feature cards, nested cards, overlapping content, or desktop UI shrunk into a phone screen.
- No hidden manual fallback for voice, camera, network, or location features.
