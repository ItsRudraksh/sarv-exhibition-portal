# Sarv Biolabs Exhibition Portal — Frontend

Visitor inquiry UI for the scan-first exhibition portal, plus a **separate** staff app at `/staff`. Talks to the Java POC at `/api/v1` (Vite proxies to `http://localhost:8080`). If the API is down, visitor drafts fall back to `localStorage`.

## Commands

```bash
npm install   # if needed
npm run dev   # start dev server — exposes LAN URL for mobile testing
npm run build # production build
npm run lint  # ESLint
npm run preview -- --host  # preview production build on LAN
```

Start Postgres + the API first (`docker compose up -d db` then `mvn spring-boot:run` in `backend/`). See [backend/README.md](../backend/README.md).

### Test on your phone (same Wi‑Fi)

1. Run `npm run dev` in `frontend/`. Vite prints a **Network** URL, e.g. `https://192.168.1.12:5173/`.
2. **Windows Firewall** usually blocks that port when Wi‑Fi is a **Public** network (common at home). From an **elevated** PowerShell at the repo root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\allow-vite-lan.ps1
```

That opens inbound TCP **5173** (and preview **4173**) on Private and Public. You can instead set the Wi‑Fi profile to **Private** in Windows Settings → Network.
3. On the phone, use **https** (not http). You will see a self-signed certificate warning:
   - **Android Chrome:** Advanced → Proceed to 192.168.x.x (unsafe).
   - **iPhone Safari:** Show Details → visit this website. If there is no proceed button, the cert cannot be bypassed on that iOS version — use Android or desktop for camera smoke.
4. Phone and PC must be on the same LAN. Guest Wi‑Fi / AP isolation (client isolation) will still fail even with the firewall rule.

HTTPS is required for in-page camera. The Vite `/api` proxy talks to the API on the PC, so the phone does not need port 8080.

### Staff review (`/staff`)

Open `https://localhost:5173/staff`. This is not part of the visitor inquiry shell. Seeded POC logins (password `poc-staff`): `reviewer@sarv.local`, `marketing@sarv.local`, `admin@sarv.local`. Add to production enqueues a vendor outbox stub; it does not call a vendor API.

## What this is

A **mobile-first React + Vite + TypeScript** app of the 11-screen visitor journey:

1. Business card capture (or manual continuation)
2. Contact details confirmation
3. Intent selection (`I want to sell` / `I want to buy`)
4–8. Supplier path (departments → product types → smart details → review → confirmation)
9–11. Buyer path (need capture → review → confirmation)

OCR, QR decoding, AI, geolocation, admin, and CRM/vendor integrations are **not implemented**. Card/catalogue uploads are live when the API is up. Confirmation shows a server `POC-` reference when the API accepts submit.

## Persistence

- Server draft: `inquiryApi` in `features/inquiry/api.ts` (`POST/PATCH/GET /inquiries`, contact confirm, submit, **multipart file upload**, consents).
- Local resume key: `sarv-inquiry-draft-v2`. This is demo/resume only — not a production privacy solution for shared stall tablets.
- Card and catalogue files are stored privately when the API is up. Rejected content checks keep the original on disk and do not serve it. Offline fallback is metadata-only.
- Taxonomy loads from `GET /api/v1/taxonomy` when the API is up. Fallback IDs in `features/inquiry/taxonomy.ts` must match Flyway `V2__poc_seed.sql`.
- QR payloads would be stored internally only in production; this app does not open card QR destinations.
- Buyer company name is optional; the server allows buyer submit without a company.
- Card capture requires an affirmative store-images consent, or continue without a card (decline). Camera permission copy is shown before `getUserMedia`.

Use **Restart demo** (top-right) to clear the saved draft and create a new server draft.

## Design and product sources

- [PLATFORM_CONTEXT.md](../specs/PLATFORM_CONTEXT.md) — product rules and canonical flow
- [BUILD-PLAN.md](../specs/BUILD-PLAN.md) — Java + React delivery phases
- [FRONTEND_BUILD_PROMPT.md](../specs/FRONTEND_BUILD_PROMPT.md) — implementation contract
- [.stitch/DESIGN.md](../.stitch/DESIGN.md) — Alpine Blue design system
- [.stitch/designs/](../.stitch/designs/) — screen HTML references (001–011)
- Logo: copied from [raw/sarv-bio-labs-logo-1.png](../raw/sarv-bio-labs-logo-1.png) into `src/assets/`

## Project structure

```
src/
  features/inquiry/
    types.ts          # InquiryDraft model and step types
    api.ts            # HTTP client + localStorage fallback
    taxonomy.ts       # Fallback departments / product types (POC seed IDs)
    validation.ts     # Form validation
    copy.ts           # Visitor-facing strings
    useInquiryJourney.ts
    InquiryApp.tsx    # Step orchestration
    screens/          # One component per screen
  features/staff/     # Internal review UI at /staff (not the visitor shell)
  components/ui.tsx   # Shared UI primitives
  styles/             # Alpine Blue tokens, visitor layout, staff dark theme
```
