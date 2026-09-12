# Design System: Sarv Biolabs Exhibition Portal — marketing navy / cyan

**Stitch project:** `16252155655979346180`  
**Primary platform:** Mobile web, 390px first; desktop companion  
**Public host:** `https://welcome.sarvbiolabs.com/` (subdomain of [sarvbiolabs.com](https://sarvbiolabs.com/))  
**Variant:** Base / light

## 1. Visual Theme & Atmosphere

The visitor portal uses the same colour language as the public Sarv Biolabs website: deep navy chrome, cyan calls to action, white surfaces, and a restrained Himalayan/molecule illustration. It should feel like a branded microsite of sarvbiolabs.com.

Form screens stay single-column and task-first. Graphical treatment is concentrated on the entry hero, intent cards, confirmation mark, and a navy utility strip that links back to the marketing site.

**Design dials:** creativity 6, density 5, variance 5, motion intent 4.

## 2. Color Palette & Roles

Sampled from the live marketing site (11 Sep 2026):

- **Sarv Navy** (`#022D59`) — utility strip, desktop canvas, staff canvas, footer.
- **Navy Deep** (`#011C38`) — page backdrop gradient.
- **Sarv Cyan** (`#01AFEF`) — primary actions, focus, progress, links, hero accent. Alias `--color-sarv-blue`.
- **Cyan Deep** (`#0095CC`) — pressed/hover primary.
- **Himalayan Green** (`#4AA485`) — illustration ridges and the buy-route card accent only. Not a second primary button colour.
- **Paper** (`#F3F8FB`) — visitor column canvas.
- **Pure Surface** (`#FFFFFF`) — inputs, cards, header bar.
- **Ink** (`#2C2C2C`) — headings and form text (marketing body colour).
- **Slate** (`#69727D`) — supporting copy.
- **Glass Border** (`#C5DBE6`) — field outlines.
- **Cyan Mist** (`#E6F7FD`) — selected rows, notices, icon tiles.

Success, warning, and error colours remain reserved for real system states. They must never imply vendor approval or product availability.

Hero and primary-button gradients (navy→cyan, cyan→cyan-deep) are allowed on brand surfaces only. Do not flood form screens with decorative gradients.

## 3. Typography Rules

- **Display and interface:** Plus Jakarta Sans, 400–700. Display headings 700, track-tight (`-0.04em`).
- **Body:** Plus Jakarta Sans, `1rem` minimum, line-height `1.55–1.65`.
- **Technical metadata:** JetBrains Mono, 600, uppercase short labels only.
- **Banned:** Inter, generic serif stacks for UI, all-caps body copy, oversized uppercase headings. Fraunces is not used (the marketing site is sans-serif).

## 4. Component Stylings

### Brand chrome

A 36px navy strip is always visible: link to `https://sarvbiolabs.com/`, “Exhibition portal” kicker, restart / next-visitor control. White logo bar underneath with a 3px cyan rule — same hierarchy as the WordPress header (utility bar + white logo).

### Entry hero

Card capture opens with the logo on white, then a navy/cyan illustrated band (Himalayan ridge + molecule hexagons, no people). Title and lede sit on that band in white.

### Intent routes

Two equal-priority graphical cards:

- **“I want to sell”** — cyan left rail.
- **“I want to buy”** — Himalayan-green left rail.

Each card has a rounded icon tile, first-person label, one supporting sentence, and a chevron.

### Actions

- Primary actions use Sarv Cyan with white Plus Jakarta Sans 600 labels, 48–52px height, and 12px corners.
- Secondary actions are outlined in Glass Border with Ink text.
- Press feedback is `scale(0.98)` or `translateY(1px)`.

### Forms

- One-column fields, labels above fields, 12px label-to-control spacing, helper/error text below.
- Minimum control height 48px; selectable rows at least 44px.
- Inputs: white fill, 1px Glass Border, 12px radius, 2px cyan focus ring with 2px offset.
- Card scan is optional; extracted fields are always reviewable. Manual fallback is mandatory.

### Trust and consent

Left-bordered cyan note. Never preselect consent. Never block manual completion when a permission is declined. Card QR payloads are stored internally only — never preview or open.

### Upload and confirmation

Dashed cyan drop zone for capture; confirmation uses a cyan check mark and a summary card. Do not fabricate tracking numbers or response times.

### Admin surfaces

Staff `/staff` and admin `/admin` use navy `#022D59` with cyan `#01AFEF` accents. “Add to production” stays separate from ordinary edits.

## 5. Layout Principles

- Mobile-first at 390px with 18px page edges; visitor column max 720px on desktop, centred on the navy canvas.
- Public entry may use a full-width illustrated hero. Form screens stay one column.
- Do not copy sample people/companies from historic Stitch HTML.

## 6. Motion & Interaction Intent

- Press: `scale(0.98)` or `translateY(1px)`.
- Route cards: 1px lift on hover.
- Honour `prefers-reduced-motion`.

## 7. Content Rules

- Visitor-first copy: “I want to sell,” “I want to buy,” “Review my inquiry,” “Submit inquiry.”
- Precise domain language: API, intermediate, catalogue, supplier review, product inquiry.
- Do not make unsupported claims, present fabricated metrics, or imply automatic approval.

## 8. Anti-Patterns (Banned)

- No purple/neon, no consumer-wellness green UI, no generic dashboard metric cards, no invented statistics, no placeholder people.
- No emojis, no “next-gen” language.
- No overlapping copy or hidden manual fallbacks.
- Do not restyle from stale Alpine Paper Stitch HTML.
