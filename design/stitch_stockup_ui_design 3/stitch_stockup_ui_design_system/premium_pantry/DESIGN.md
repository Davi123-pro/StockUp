---
name: Premium Pantry
colors:
  surface: '#101411'
  surface-dim: '#101411'
  surface-bright: '#363a36'
  surface-container-lowest: '#0b0f0c'
  surface-container-low: '#181d19'
  surface-container: '#1c211d'
  surface-container-high: '#272b27'
  surface-container-highest: '#313632'
  on-surface: '#e0e4dd'
  on-surface-variant: '#bfc9bf'
  inverse-surface: '#e0e4dd'
  inverse-on-surface: '#2d312d'
  outline: '#89938a'
  outline-variant: '#3f4941'
  surface-tint: '#88d7a2'
  primary: '#88d7a2'
  on-primary: '#00391e'
  primary-container: '#2e7d50'
  on-primary-container: '#ceffda'
  inverse-primary: '#196c41'
  secondary: '#a0d2ad'
  on-secondary: '#06381f'
  secondary-container: '#245236'
  on-secondary-container: '#93c4a0'
  tertiary: '#ffb2bb'
  on-tertiary: '#5b1826'
  tertiary-container: '#a95562'
  on-tertiary-container: '#fff0f0'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#a4f4bd'
  primary-fixed-dim: '#88d7a2'
  on-primary-fixed: '#00210f'
  on-primary-fixed-variant: '#00522d'
  secondary-fixed: '#bcefc8'
  secondary-fixed-dim: '#a0d2ad'
  on-secondary-fixed: '#00210f'
  on-secondary-fixed-variant: '#224f34'
  tertiary-fixed: '#ffd9dd'
  tertiary-fixed-dim: '#ffb2bb'
  on-tertiary-fixed: '#3e0212'
  on-tertiary-fixed-variant: '#782e3b'
  background: '#101411'
  on-background: '#e0e4dd'
  surface-variant: '#313632'
typography:
  display-lg:
    fontFamily: Outfit
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Outfit
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Outfit
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-md:
    fontFamily: Outfit
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 20px
---

## Brand & Style
The design system is engineered for a high-end pantry management experience, blending the utility of Material Design 3 with a "Cyberpunk Clean" aesthetic. It targets tech-savvy home cooks and organization enthusiasts who value precision and premium aesthetics. 

The visual narrative is built on high-contrast surfaces, vibrant mint accents, and glassmorphic depth. By utilizing an OLED black foundation, the UI minimizes eye strain and creates a canvas where food photography and semantic status indicators pop with intentionality. The emotional response should be one of "effortless control"—transforming the mundane task of inventory management into a sophisticated, tactile experience.

## Colors
The palette is centered around **Deep Mint** and **OLED Black**. The primary green (#2E7D50) serves as the anchor for action, while the lighter Mint Accent (#A8DAB5) is used for high-visibility highlights and interactive states.

- **Foundational Neutrals:** The background is true black (#121212) to ensure maximum contrast and battery efficiency on mobile displays. Secondary surfaces use a tiered grayscale to establish hierarchy.
- **Semantic Logic:** Status colors are "Pastel-Punch"—high enough in saturation to be seen quickly against dark backgrounds, but soft enough to remain premium. Use these strictly for expiration logic: Red for expired, Yellow for expiring within 3 days, and Green for fresh stock.

## Typography
The system employs a dual-font strategy:
- **Outfit** is used for headlines and display text. Its rounded, geometric construction echoes the "Fresh" brand pillar while providing a modern, premium feel.
- **Inter** handles all functional UI and body text. It is chosen for its exceptional legibility at small sizes, crucial for reading ingredient lists or expiration dates on mobile screens.

Use `label-sm` in all-caps for categories or metadata to create a clear architectural distinction from body content.

## Layout & Spacing
This design system follows a strict **4px baseline grid**. 
- **Margins:** Use a 20px side margin for mobile views to give the content room to breathe against the screen edges.
- **Grid:** Use a 4-column fluid grid for mobile.
- **Rhythm:** Vertical spacing between cards should be `md` (16px). Internal card padding should also be `md` (16px) to ensure touch targets remain accessible.
- **Photography:** Images should occupy full-width containers or specific 24px-rounded placeholders to maintain the premium, editorial feel.

## Elevation & Depth
Depth is created through **Glassmorphism** and **Tonal Layering** rather than traditional drop shadows.
- **Level 0:** Background (#121212).
- **Level 1 (Cards):** Surface (#1E1E1E) with a subtle 1px inner stroke (White @ 5% opacity).
- **Level 2 (Floating/Modals):** Glassmorphic surfaces. Use a backdrop blur of 20px and a 15% opacity white fill. These surfaces should have a slightly brighter 1px border to simulate a "glass edge."
- **Overlays:** Navigation bars and sticky headers should use the glass effect to allow content to scroll subtly beneath them, maintaining a sense of space.

## Shapes
The shape language is modern and approachable.
- **Standard UI (Buttons, Cards, Inputs):** 16px radius (`rounded-lg`).
- **Feature Containers (Hero images, Camera viewports):** 24px radius (`rounded-xl`).
- **Interactive Small Elements (Chips, Tags):** Fully rounded (Pill-shaped) to distinguish them from structural cards.

## Components
- **Buttons:** Primary buttons use the Mint (#2E7D50) fill with white text/icons. Secondary buttons are "Ghost" style with a 1px Mint stroke. All icons within buttons must use a 2.0pt stroke weight for clarity.
- **Inventory Cards:** Must feature a 4px vertical "status strip" on the left edge using semantic colors (Expired/Expiring/OK) to allow quick scanning.
- **Inputs:** Fields use the #262626 surface with a bottom-only 2px stroke that highlights in Mint (#A8DAB5) when focused.
- **Glass Chips:** Used for categories (e.g., "Dairy," "Frozen"). These should have a subtle blur and be semi-transparent to sit lightly on top of product images.
- **Progress Bars:** Used for "Stock Levels." These should be thin (4px height) with a rounded track, using the Primary Mint color for the fill.