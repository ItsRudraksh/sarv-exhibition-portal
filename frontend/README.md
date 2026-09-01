# Sarv Biolabs Exhibition Portal — Frontend

Client-only visitor inquiry prototype for the scan-first exhibition portal flow.

## Commands

```bash
npm install   # if needed
npm run dev   # start dev server — exposes LAN URL for mobile testing
npm run build # production build
npm run lint  # ESLint
npm run preview -- --host  # preview production build on LAN
```

### Test on your phone (same Wi‑Fi)

1. Run `npm run dev` in `frontend/`.
2. Vite prints a **Network** URL, e.g. `http://192.168.x.x:5173/`.
3. Open that URL on your mobile browser (phone and PC must be on the same network).
4. Allow the port through Windows Firewall if prompted.

If the Network line does not appear, find your PC’s IPv4 address (`ipconfig` on Windows) and use `http://<your-ip>:5173/`.

## What this is

A **mobile-first React + Vite + TypeScript** prototype of the 11-screen visitor journey:

1. Business card capture (or manual continuation)
2. Contact details confirmation
3. Intent selection (`I want to sell` / `I want to buy`)
4–8. Supplier path (departments → product types → smart details → review → confirmation)
9–11. Buyer path (need capture → review → confirmation)

There is **no backend**. OCR, QR decoding, file upload, AI, geolocation, and CRM/vendor integrations are **not implemented**. The UI is truthful about prototype limits.

## Prototype boundaries

- Draft state persists in **`localStorage`** via `features/inquiry/api.ts` (`localStorageDraftPort`). This is for demo/resume only — not a production privacy or security solution.
- Selected catalogue files store **metadata only** (name, size, type). File bytes and image previews do not survive a full page reload.
- Taxonomy (departments, product types) is **prototype mock data** in `features/inquiry/taxonomy.ts`, not production configuration.
- QR payloads would be stored internally only in production; this prototype does not read QR codes and never displays card QR destinations.
- Buyer company name is optional in the UX; backend policy may differ (see `DATABASE-DESIGN.md`).

Use **Restart demo** (top-right) to clear the saved draft.

## Design and product sources

- [PLATFORM_CONTEXT.md](../PLATFORM_CONTEXT.md) — product rules and canonical flow
- [FRONTEND_BUILD_PROMPT.md](../FRONTEND_BUILD_PROMPT.md) — implementation contract
- [.stitch/DESIGN.md](../.stitch/DESIGN.md) — Alpine Blue design system
- [.stitch/designs/](../.stitch/designs/) — screen HTML references (001–011)
- Logo: copied from [raw/sarv-bio-labs-logo-1.png](../raw/sarv-bio-labs-logo-1.png) into `src/assets/`

## Project structure

```
src/
  features/inquiry/
    types.ts          # InquiryDraft model and step types
    api.ts            # Draft persistence port (future HTTP seam)
    taxonomy.ts       # Mock departments / product types
    validation.ts     # Form validation
    copy.ts           # Visitor-facing strings
    useInquiryJourney.ts
    InquiryApp.tsx    # Step orchestration
    screens/          # One component per screen
  components/ui.tsx   # Shared UI primitives
  styles/             # Alpine Blue tokens and layout
```

## Future integration

Replace `localStorageDraftPort` with an HTTP adapter that maps `InquiryDraft` to the API backed by `exhibition_portal_schema.sql`. Internal admin queues remain out of scope for this package.
