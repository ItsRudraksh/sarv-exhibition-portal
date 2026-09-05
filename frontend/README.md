# Sarv Biolabs Exhibition Portal — Frontend

Visitor inquiry UI for the scan-first exhibition portal, plus a **separate** staff app at `/staff`. Talks to the Java POC at `/api/v1` (Vite proxies to `http://localhost:8080`). Shared stall tablets keep only a **sessionStorage draft id** — not contact PII in `localStorage`.

## Commands

```bash
npm install   # if needed
npm run dev   # start dev server — exposes LAN URL for mobile testing
npm run build # production build
npm run lint  # ESLint
npm run preview -- --host  # preview production build on LAN
```

Start native MySQL 8 on `localhost:3306`, then the API (`.\run.ps1` in `backend/`). See [backend/README.md](../backend/README.md). Docker is not required.

**Public Windows Server** (`http://43.225.195.200/`): Java 17 + Jenkins. Do not run this Vite dev server on the public IP. `npm run build` is copied into the Spring Boot JAR. See [DEPLOY-WINDOWS.md](../specs/DEPLOY-WINDOWS.md). In-page camera still needs HTTPS; upload still works on HTTP.

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

Local **card-QR assist** is live when the API is up: ZXing may propose contact fields from a vCard/MECARD QR for the visitor to review. Cloud OCR, voice, geolocation, and CRM/vendor integrations are **not** implemented. Card/catalogue uploads are live when the API is up. Confirmation shows a server `POC-` reference when the API accepts submit.

**Pilot entry:** `/?c=POC-STALL-1` (exhibition campaign), `/web` or `?channel=website`, `?channel=direct`, `?assist=1` (staff-assisted). Shared devices show **Next visitor**. Fonts are self-hosted (no Google Fonts CDN).

## Persistence

- Server draft: `inquiryApi` in `features/inquiry/api.ts` (`POST` create with channel/campaign, `PATCH/GET`, contact confirm, submit, files, consents, extractions).
- Session pointer: `sarv-inquiry-pointer-v1` in **sessionStorage** (draft id only). Legacy full-draft `localStorage` key is cleared on load.
- Entry parsing: `features/inquiry/entryContext.ts`.
- Card and catalogue files are stored privately when the API is up. Offline: on-screen only; submit requires the API for a receipt.
- Taxonomy loads from `GET /api/v1/taxonomy` when the API is up. Fallback IDs in `features/inquiry/taxonomy.ts` must match Flyway `V7__business_taxonomy.sql` ([specs/taxonomy/](../specs/taxonomy/)).
- Card QR payloads are stored server-side only; visitor GET never returns the raw payload.
- Buyer company name is optional; the server allows buyer submit without a company.
- Card capture requires an affirmative store-images consent, or continue without a card (decline). Camera permission copy is shown before `getUserMedia`.

Use **Next visitor** / **Restart demo** (top-right) to clear the session and create a new server draft.

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
    api.ts            # HTTP client + create/campaign APIs
    entryContext.ts   # URL entry + session pointer (no PII localStorage)
    taxonomy.ts       # Offline fallback departments / product types (V7 / specs/taxonomy)
    validation.ts     # Form validation
    copy.ts           # Visitor-facing strings
    useInquiryJourney.ts
    InquiryApp.tsx    # Step orchestration
    screens/          # One component per screen
  features/staff/     # Internal review UI at /staff (not the visitor shell)
  components/ui.tsx   # Shared UI primitives
  styles/             # Alpine Blue tokens, visitor layout, staff dark theme
```
