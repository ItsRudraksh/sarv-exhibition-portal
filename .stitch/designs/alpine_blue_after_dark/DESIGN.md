---
name: Alpine Blue After Dark
colors:
  surface: '#06151b'
  surface-dim: '#06151b'
  surface-bright: '#2c3b42'
  surface-container-lowest: '#021016'
  surface-container-low: '#0e1e24'
  surface-container: '#122228'
  surface-container-high: '#1d2c33'
  surface-container-highest: '#28373e'
  on-surface: '#d4e5ee'
  on-surface-variant: '#bcc9cb'
  inverse-surface: '#d4e5ee'
  inverse-on-surface: '#233339'
  outline: '#869395'
  outline-variant: '#3d494b'
  surface-tint: '#5ed7e7'
  primary: '#75ebfc'
  on-primary: '#00363c'
  primary-container: '#55cfdf'
  on-primary-container: '#00565f'
  inverse-primary: '#006973'
  secondary: '#a8ccd9'
  on-secondary: '#0f353f'
  secondary-container: '#2b4e58'
  on-secondary-container: '#9abeca'
  tertiary: '#b7e1f0'
  on-tertiary: '#043541'
  tertiary-container: '#9bc5d4'
  on-tertiary-container: '#29535f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#92f1ff'
  primary-fixed-dim: '#5ed7e7'
  on-primary-fixed: '#001f23'
  on-primary-fixed-variant: '#004f57'
  secondary-fixed: '#c4e8f5'
  secondary-fixed-dim: '#a8ccd9'
  on-secondary-fixed: '#001f27'
  on-secondary-fixed-variant: '#294c56'
  tertiary-fixed: '#bfe9f8'
  tertiary-fixed-dim: '#a3cddc'
  on-tertiary-fixed: '#001f27'
  on-tertiary-fixed-variant: '#224c58'
  background: '#06151b'
  on-background: '#d4e5ee'
  surface-variant: '#28373e'
  midnight-mineral: '#07171D'
  deep-lab-surface: '#0D2932'
  raised-current: '#143943'
  ice-ink: '#E7F5F6'
  cool-metadata: '#A4BEC4'
  night-border: '#274B55'
  sarv-process-blue: '#55CFDF'
  blue-shadow: '#123F4B'
typography:
  display-hero:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '600'
    lineHeight: 38px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-mono:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  metadata-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  gutter: 16px
  margin-mobile: 20px
  margin-desktop: 40px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

The design system is a high-precision, low-light environment engineered for Sarv Biolabs event operations and internal review. The aesthetic is defined by a "Calm Lab" philosophy—prioritizing focus and clarity over decorative flair. It rejects the typical "gamer" or "cyberpunk" tropes of dark modes in favor of a sophisticated, mineral-inspired palette.

The style is **Corporate / Modern** with a lean toward **Minimalism**. It utilizes a systematic layering of deep teals and cyans to create a sense of depth and specialized utility. The atmosphere is quiet and professional, ensuring that users can engage with complex biological and logistical data for extended periods without visual fatigue. Color is reserved strictly for state indication and primary path navigation.

## Colors

The color palette is built on a foundation of "Midnight Mineral," providing a deep, stable canvas. High-contrast "Ice Ink" ensures optimal readability for all primary content, while "Cool Metadata" handles secondary information.

**Sarv Process Blue** acts as the singular, high-visibility accent. It is the only color used for primary actions, active progress, and focus states. 

Surfaces are tiered by lightness:
- **Level 0 (Base):** Midnight Mineral for the global background.
- **Level 1 (Default Surface):** Deep Lab Surface for cards, inputs, and sheets.
- **Level 2 (Active/Raised):** Raised Current and Blue Shadow for selection states and interaction feedback.
- **Borders:** Night Border provides structural definition without creating harsh visual breaks.

## Typography

The typography system uses **Plus Jakarta Sans** for the vast majority of the interface to maintain a welcoming but professional tone. To emphasize its role as a technical tool, **JetBrains Mono** is employed for compact metadata, IDs, and secondary labels.

- **Headings:** Set in Ice Ink with tight letter spacing for a modern, architectural feel.
- **Body:** Prioritize legibility. Paragraphs use Ice Ink for high contrast, while supporting copy uses Cool Metadata.
- **Technical Data:** Use JetBrains Mono for any data-heavy strings or administrative counters.
- **Visual Weight:** Avoid bold weights except for high-level headings. Rely on color (Ice Ink vs. Cool Metadata) to establish hierarchy.

## Layout & Spacing

This design system follows a **Fixed Grid** approach for mobile (390px base) and a structured 12-column system for desktop. 

- **Mobile First:** Content is arranged in a single-column flow to facilitate ease of use for event operators on the move.
- **Rhythm:** A 4px baseline grid ensures vertical consistency.
- **Margins:** Standard 20px margins on mobile, expanding to 40px on tablet/desktop.
- **Structure:** Content is organized into functional blocks. Administrative queues use dense, divider-led rows rather than card grids to maximize information density.

## Elevation & Depth

In a dark environment, depth is communicated through **Tonal Layers** rather than heavy drop shadows. Surfaces closer to the user are rendered in lighter shades of teal-gray.

- **Base Layer:** Midnight Mineral (`#07171D`).
- **Surface Layer:** Deep Lab Surface (`#0D2932`) for interactive elements.
- **Interactive Layer:** Raised Current (`#143943`) for elements that can be clicked or are currently hovered.
- **Focus/Selection Layer:** Blue Shadow (`#123F4B`) provides a subtle, tinted glow behind selected items.
- **Outlines:** Use Night Border (`#274B55`) for structural containment. Avoid vibrant outlines unless they indicate a focused state (which uses Sarv Process Blue).

## Shapes

The shape language is **Soft** and precise. A uniform radius of 0.25rem (4px) is applied to most UI components to suggest a modern, engineered feel without the softness of consumer apps or the harshness of brutalism.

- **Buttons & Inputs:** 4px radius (Soft).
- **Cards & Sheets:** 8px radius (Large).
- **Route Markers:** Circular (Fully rounded) for specific action indicators.

## Components

### Buttons
- **Primary:** Filled with Sarv Process Blue, text in Midnight Mineral. Used for the single most important action on a screen.
- **Secondary:** Transparent fill with a 1px Night Border and Ice Ink text.
- **Tertiary:** Ghost style; Ice Ink text with no border or fill until interacted with.

### Inputs
- **Field:** Deep Lab Surface background with 1px Night Border.
- **Focus State:** 2px solid ring of Sarv Process Blue.
- **Label:** Cool Metadata in JetBrains Mono above the field.

### Route Rows & Selection
- **Standard:** Deep Lab Surface background with Night Border dividers.
- **Active State:** Background shifts to Blue Shadow.
- **Indicator:** A circular Sarv Process Blue marker designates active routes or markers.

### Administrative Queues
- Use dense, horizontal rows separated by Night Border. 
- Avoid cards in admin views to maintain a tabular, data-first hierarchy.
- **Selected Row:** Highlighting uses Blue Shadow to maintain low-light comfort.

### Cards & Containers
- Containers use Deep Lab Surface. For elevated modals or sheets, add a subtle 1px border using Night Border to ensure separation from the background.