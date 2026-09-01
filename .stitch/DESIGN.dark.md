# Design System: Sarv Biolabs Exhibition Portal — Alpine Blue After Dark

**Stitch project:** `16252155655979346180`  
**Primary platform:** Mobile web, 390px first  
**Variant:** Dark

## 1. Visual Theme & Atmosphere

The dark variation is a low-light companion for event operators and internal review. It retains the calm Alpine Blue structure rather than becoming a neon technical dashboard. The mood is focused, precise, and quiet; colour is used to clarify state, not to decorate it.

Use this as the dark-mode equivalent of the base system and as the stronger candidate for admin workspaces. Do not treat it as a separate brand.

## 2. Color Palette & Roles

- **Midnight Mineral** (`#07171D`) — primary canvas.
- **Deep Lab Surface** (`#0D2932`) — fields, sheets, and elevated containers.
- **Raised Current** (`#143943`) — selected or raised secondary surfaces.
- **Ice Ink** (`#E7F5F6`) — primary text and headings.
- **Cool Metadata** (`#A4BEC4`) — supporting copy and disabled labels.
- **Night Border** (`#274B55`) — structural lines, field borders, and dividers.
- **Sarv Process Blue** (`#55CFDF`) — the single bright accent; focus, active progress, primary action, and route marker.
- **Blue Shadow** (`#123F4B`) — accent-tinted active surfaces and selected rows.

No violet, electric-blue gradients, green success fills, or harsh white surfaces. Genuine success/warning/error tokens remain semantic-only and must maintain accessible contrast against Midnight Mineral.

## 3. Typography Rules

Use the same type architecture as the base theme: Plus Jakarta Sans for interface, Fraunces only for the public editorial entry headline, and JetBrains Mono for compact metadata. Display text changes colour to Ice Ink, not font family or scale. Never use glowing or outlined text to create contrast.

## 4. Component Stylings

- Route rows are Deep Lab Surface blocks separated by Night Border. The active/pressed state receives Blue Shadow, while the circular marker and primary action use Sarv Process Blue.
- Primary buttons use Sarv Process Blue with Midnight Mineral text for maximum clarity. Secondary actions use transparent fill, Night Border, and Ice Ink text.
- Inputs use Deep Lab Surface with a 1px Night Border. Focus is a 2px Sarv Process Blue ring. Error/help text stays inline and readable; never rely on colour alone.
- Context panels use a restrained blue-tinted illustration or subtle linework. No glowing particles, aurora effects, glassmorphism, or faux code backgrounds.
- Admin queues use dense, divider-led rows rather than bright card grids. Selected rows use Blue Shadow; irreversible actions remain visually separated and explicit.

## 5. Layout, Motion, Content, and Bans

Retain the base system's single-column mobile layout, touch targets, normal-flow composition, consent handling, manual fallbacks, motion settings, employee-first copy, and anti-patterns exactly. Dark mode is a colour-system shift, not a different information architecture.

The first route labels remain **“I want to sell”** and **“I want to buy.”** Keep the public workflow direct and avoid operational jargon until a visitor reaches the relevant form.
