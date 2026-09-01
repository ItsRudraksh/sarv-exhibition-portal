# Sarv Biolabs Exhibition Portal — Master Task List

**Stitch project:** `16252155655979346180`  
**Active visitor design system:** Alpine Blue (`assets/ec49fa42191d45048964a70d670003ba`)  
**Active admin design system:** Alpine Blue After Dark (`assets/30f2e48d1564477bbfa471de0bd717fe`)  
**Workflow rule:** Generate one screen at a time. Do not begin the next screen until the current screen is explicitly approved.

## Flow revision — scan-first, auto-saved

- [x] 01. Business-card capture start — capture front and back, provide camera/upload/manual paths, and save a detected QR payload with the inquiry without redirecting the visitor. Approved: `b792a9f8114c43ef9c47b92565a13bc9`.
- [x] 02. Extracted-details confirmation — name, email, and phone with country code establish the resumable partial record. Approved: `81eacb82ff39448ca948348d5a7b33f1`.
- [x] 03. Intent selection — “I want to sell” / “I want to buy”. Approved: `f6a79d52e755464686ef2dcdf0583104`.
- [x] 04. Supplier product types — refined replacement with corrected Step 2 of 4 progress, automatic-save cue, and “Review my details” handoff. Approved: `ff747fafbf0e4254a70d0bd3f439d9d8`.
- [x] 05. Supplier smart details check — preview extracted details and selections; request only required data that could not be read. Approved: `9f0df261d9784d029fcdcc67ce4dc390`.
- [x] 06. Buyer rapid need capture — one required product-or-need entry, with search and specifications kept optional. Approved: `a07014409a4947a38990bdf1e996fb86`.
- [x] 07. Buyer final review and submit — review one need summary and saved contact details before submission. Approved: `193bb0d77cdd4ed18e4fb52b73bd9cdf`.
- [~] 08. Buyer confirmation — revised to acknowledge the short inquiry without assumed categories or specifications. Candidate: `94465f5f179a4aae84290e12019ab4a8`.
- [ ] Supplier path — department → product type → smart missing-details/preview → review and submit → confirmation.
- [ ] Buyer path — product or exact requirement → smart missing-details/preview → review and submit → confirmation; product specifications remain progressive and optional.

## Desktop counterparts — Alpine Blue (light)

- [x] 01. Entry choice — `35d8eb26541b4ce9b9715755b8062606`
- [x] 02. Supplier departments — `db689e500ce34fcd8b9581b6c430acb0`
- [x] 03. Supplier product types — `62a6930cb8fb4e50a4e92bfcabd7e992`
- [x] 04. Supplier profile — `79caa44945aa4e2b9ce5ae1ccb35dccd`
- [x] 05. Supplier review and submit — `442630bc11934723aa9b20ebc3c99f28`
- [x] 06. Supplier confirmation — `5e1ab90c6c194a378324d7a2f248709c`
- [x] 07. Buyer product inquiry — `189f356296354862909b80647d52f9b1`
- [x] 08. Buyer requirement details — `abddbaab2dfe451dbc37ae22989f4564`
- [x] 09. Buyer profile and consent — `93a4284d64524c9fac2771cbe735f439`
- [x] 10. Buyer confirmation — `2f6f63492e5748f5b4832ebdbf7ad0c3`

## Visitor portal — Alpine Blue (light)

- [x] 01. Entry choice — “I want to sell” / “I want to buy”; QR-exhibition context and optional assistance.
- [x] 02. Supplier departments — multi-select, searchable department list. Approved direction: Search & Taxonomy Focus.
- [x] 03. Supplier product types — filtered by the selected departments. Approved current Stitch screen: `3378382deca54c13b0700fbd58cf53a1`.
- [x] 04. Supplier profile — finalized company, contact, alternate-contact, location, and product-classification fields; optional voice or visiting-card scan. Approved current Stitch screen: `2f9cf531803c4adb9fbb3ad6bd592eb2`.
- [x] 05. Supplier review — approved Fast Scan direction; catalogue upload and website URL are accepted together or singly, with at least one required. Location evidence is automatic in code and absent from this screen. Approved current Stitch screen: `0071a004fa7243479a1d1d34c5b0d860`.
- [x] 06. Supplier confirmation — approved production-safe acknowledgement, review summary placeholders, next-step explanation, and exhibition-staff assistance. Approved current Stitch screen: `16bbb11987a54bbc80405eff7b91aab0`.
- [x] 07. Buyer product inquiry — approved base direction; product selection or exact-requirement entry. Approved current Stitch screen: `fd38f8f196404cf8a7acc2fedade7c52`.
- [x] 08. Buyer category — pharmacopeial category and product requirement detail. Kept live Stitch screen after variation cleanup: `6af956a24a4d464383ed8df144f6a209` (Buyer Inquiry: Minimalist Card Variant).
- [x] 09. Buyer profile — buyer/company details and consent. Approved refined consistency update: `69fae3abe9c049d08ce2e16ab84ec038`.
- [x] 10. Buyer confirmation — approved base acknowledgement with placeholder-only inquiry summary, follow-up expectation, exhibition-staff correction route, and restart actions. Stitch canvas audit confirms this is the sole non-hidden Screen 10 item; its alternatives are hidden. Approved current Stitch screen: `979563a272a24d51b266578abeeb689d`.

## Internal workspace — Alpine Blue After Dark

- [ ] 11. Supplier-review queue — assignment, genuine workflow state, and search/filter controls.
- [ ] 12. Supplier record review — identity, catalogue, duplicate review, and explicit Add to production decision.
- [ ] 13. Buyer-lead queue — lead detail, marketing-routing state, and governed export action.

## Design operations

- [x] Base and dark design systems documented locally and imported into Stitch.
- [x] Brand logo imported into Stitch.
- [x] Download and visually audit each approved Stitch screen before code implementation.
- [x] Update `.stitch/metadata.json` and this task list after each approved screen. Last updated: all ten approved visitor screens have desktop counterparts generated and visually audited.
