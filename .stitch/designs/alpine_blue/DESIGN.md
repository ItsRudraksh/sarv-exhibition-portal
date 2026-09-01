---
name: Alpine Blue
colors:
  surface: '#f9faf7'
  surface-dim: '#d9dad8'
  surface-bright: '#f9faf7'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f1'
  surface-container: '#edeeeb'
  surface-container-high: '#e7e8e6'
  surface-container-highest: '#e2e3e0'
  on-surface: '#191c1b'
  on-surface-variant: '#3f484d'
  inverse-surface: '#2e312f'
  inverse-on-surface: '#f0f1ee'
  outline: '#6f787e'
  outline-variant: '#bec8ce'
  surface-tint: '#006783'
  primary: '#00607b'
  on-primary: '#ffffff'
  primary-container: '#147a9a'
  on-primary-container: '#ecf8ff'
  inverse-primary: '#7fd1f4'
  secondary: '#3f646f'
  on-secondary: '#ffffff'
  secondary-container: '#c2e9f7'
  on-secondary-container: '#456a76'
  tertiary: '#00617a'
  on-tertiary: '#ffffff'
  tertiary-container: '#007b9a'
  on-tertiary-container: '#edf9ff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#bce9ff'
  primary-fixed-dim: '#7fd1f4'
  on-primary-fixed: '#001f2a'
  on-primary-fixed-variant: '#004d63'
  secondary-fixed: '#c2e9f7'
  secondary-fixed-dim: '#a7cdda'
  on-secondary-fixed: '#001f27'
  on-secondary-fixed-variant: '#274c57'
  tertiary-fixed: '#baeaff'
  tertiary-fixed-dim: '#60d4fd'
  on-tertiary-fixed: '#001f29'
  on-tertiary-fixed-variant: '#004d62'
  background: '#f9faf7'
  on-background: '#191c1b'
  surface-variant: '#e2e3e0'
  pure-surface: '#FFFFFF'
  measured-slate: '#5C747C'
  glass-border: '#C9D9DF'
  blue-mist: '#D7F0F7'
typography:
  display-hero:
    fontFamily: Fraunces
    fontSize: 48px
    fontWeight: '500'
    lineHeight: '1.1'
    letterSpacing: -0.055em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 42px
    fontWeight: '700'
    lineHeight: '0.96'
    letterSpacing: -0.045em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.045em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.2'
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.55'
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '600'
    lineHeight: '1.4'
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  edge-margin: 18px
  section-gap: 24px
  stack-gap: 16px
  label-gap: 12px
  action-height: 48px
---

## Brand & Style

The design system embodies a **Corporate / Modern** aesthetic tailored for the pharmaceutical B2B sector. It draws inspiration from the Himalayan origins of Sarv Biolabs, translating those roots into a "mineral" visual language that prioritizes clarity, precision, and professional calm. 

The personality is measured and assured—functioning like a high-end research notebook engineered for efficiency. The UI avoids decorative excess, using a botanical-blue palette to evoke trust without lapsing into consumer-wellness tropes. Every design decision is task-oriented, ensuring that visitors on a busy exhibition floor encounter one clear decision at a time through direct, single-column layouts and purposeful interaction.

## Colors

The color strategy uses a hierarchy of "mineral" tones to establish a professional pharmaceutical atmosphere.

- **Primary (Sarv Process Blue):** Reserved for high-priority actions, focus states, and active progress.
- **Secondary (Research Ink):** Used for headlines and core information density to ensure high legibility.
- **Neutral (Alpine Paper):** The foundational canvas color. Avoid using stark white for backgrounds; keep it for inputs and cards.
- **Supportive Tones:** 
    - **Blue Mist** is used for subtle row highlights and icon backdrops.
    - **Glass Border** provides structural definition without visual noise.
    - **Clear Blue** is strictly for illustration planes and interactive feedback states.

## Typography

The system utilizes a sophisticated typographic stack to balance brand heritage with technical precision.

- **Plus Jakarta Sans** is the primary workhorse, used for both display and interface elements. It should be tracked tightly in headlines to maintain a modern, engineered feel.
- **Fraunces (500)** is the "Editorial" voice, reserved exclusively for the public-facing entry headline to inject a touch of humanistic character.
- **JetBrains Mono** is employed for technical metadata and step markers. Use it in uppercase for small labels to denote a "laboratory" or "logistics" quality.
- **Constraints:** Maintain a 32-character line length on mobile for body copy to ensure rapid scanning during exhibition use.

## Layout & Spacing

This design system uses a **Fixed Grid** approach optimized for mobile-first delivery (390px base).

- **Grid & Margins:** Use an 18px safe area at the page edges. 
- **Task Alignment:** All form screens must follow a strict single-column layout. Avoid side-by-side inputs on mobile to maintain vertical task focus.
- **Responsive Scaling:** On larger screens, the content width remains constrained to maintain readability, while whitespace expands to emphasize the "clean room" aesthetic.
- **Rhythm:** Use a consistent vertical scale: 24px for separating major functional groups and 16px for related field elements.

## Elevation & Depth

Hierarchy is established primarily through **Low-contrast outlines** and tonal layering rather than heavy shadows.

- **Surface Tiers:** The `Alpine Paper` base layer serves as the foundation. Surfaces that require focus or confirmation (like inputs and modal sheets) use `Pure Surface` (White).
- **Glass Boundaries:** Use 1px `Glass Border` outlines for all interactive containers and dividers. This provides structure without the "weight" of traditional borders.
- **Focus States:** Depth is signaled through a 2px `Sarv Process Blue` ring with a 2px offset, making the active element pop against the mineral background.
- **Shadows:** Avoid ambient shadows. If depth is required for a floating element, use a single, highly-diffused tint of `Research Ink` at very low opacity (3-5%).

## Shapes

The shape language is precise and systematic. A standard 10px corner radius is applied to all primary UI elements, including buttons, input fields, and cards. This specific radius balances the approachability of a rounded UI with the rigid precision required for a pharmaceutical B2B tool. Smaller elements like chips or markers should maintain this proportion, while directional discs (like route markers) are kept as perfect circles.

## Components

### Actions
- **Primary Buttons:** Solid `Sarv Process Blue` background, white text, 48-52px height. 
- **Secondary Buttons:** 1px `Glass Border` outline, `Research Ink` text.
- **Interaction:** Use a subtle `scale(0.98)` on press. No bouncy animations or glows.

### Entry Routes
- Designed as full-width rows with hairline dividers. Each row contains a 48px `Blue Mist` circular marker and a right-pointing chevron. 

### Forms
- **Input Fields:** White fill, `Glass Border` outline, 10px radius. Labels sit 12px above the field in `Research Ink`.
- **Selection:** Use bottom sheets or searchable checklists for taxonomy; avoid standard dropdowns on mobile.
- **Helper Text:** Positioned below the field in `Measured Slate`.

### Notes & Trust
- **Consent Info:** Small container with a 2px left border in `Sarv Process Blue`. This is used to explain data requests (camera/location) before the system prompt appears.

### Status Indicators
- **Workflow Chips:** Use `Blue Mist` backgrounds with `Research Ink` text for standard states. Reserve semantic colors (success/error) for verified system results only.