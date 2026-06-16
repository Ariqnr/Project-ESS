---
name: Industrial Precision
colors:
  surface: '#fbf9fa'
  surface-dim: '#dbd9db'
  surface-bright: '#fbf9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f4'
  surface-container: '#efedee'
  surface-container-high: '#eae7e9'
  surface-container-highest: '#e4e2e3'
  on-surface: '#1b1c1d'
  on-surface-variant: '#44474c'
  inverse-surface: '#303032'
  inverse-on-surface: '#f2f0f1'
  outline: '#74777c'
  outline-variant: '#c4c6cc'
  surface-tint: '#516071'
  primary: '#061625'
  on-primary: '#ffffff'
  primary-container: '#1c2b3a'
  on-primary-container: '#8392a5'
  inverse-primary: '#b8c8dc'
  secondary: '#3a656b'
  on-secondary: '#ffffff'
  secondary-container: '#bdeaf2'
  on-secondary-container: '#406b71'
  tertiary: '#1f1303'
  on-tertiary: '#ffffff'
  tertiary-container: '#362714'
  on-tertiary-container: '#a38d74'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d4e4f8'
  primary-fixed-dim: '#b8c8dc'
  on-primary-fixed: '#0d1d2b'
  on-primary-fixed-variant: '#394858'
  secondary-fixed: '#bdeaf2'
  secondary-fixed-dim: '#a2ced5'
  on-secondary-fixed: '#001f23'
  on-secondary-fixed-variant: '#204d53'
  tertiary-fixed: '#f8dec1'
  tertiary-fixed-dim: '#dbc3a6'
  on-tertiary-fixed: '#261907'
  on-tertiary-fixed-variant: '#55442e'
  background: '#fbf9fa'
  on-background: '#1b1c1d'
  surface-variant: '#e4e2e3'
typography:
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Hanken Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  gutter: 16px
  margin-mobile: 16px
  margin-tablet: 24px
  component-padding-x: 16px
  component-padding-y: 12px
---

## Brand & Style

The design system is engineered for the high-stakes environment of modern manufacturing. It targets factory operators, supervisors, and logistics personnel who require immediate, unambiguous data visualization and reliable control interfaces. 

The aesthetic is **Industrial Modern**, drawing heavily from high-end engineering branding. It balances the ruggedness of a factory floor with the technical sophistication of modern software. The UI leverages a "warm technical" feel—using cream and tan neutrals to offset the cool, authoritative deep navies and teals. The emotional goal is to evoke a sense of structural integrity, operational efficiency, and professional calm.

Key stylistic pillars include:
- **Utilitarian Precision:** Every element has a clear purpose and hierarchical priority.
- **Material 3 Foundation:** Adherence to Google’s latest design standards for reachability and motion.
- **Structural Depth:** Use of subtle shadows and layered surfaces to simulate physical control panels.

## Colors

The palette is anchored in **Primary Dark Navy (#1C2B3A)** to establish authority and legibility. This is complemented by a range of functional blues and industrial accents.

- **Primary & Secondary:** Dark Navy is used for headers, primary actions, and critical navigation. Accent Teal (#073A40) is reserved for focus states, active selections, and interactive high-precision elements.
- **Neutral Base:** The background uses a warm cream (#EDE8DC) to reduce eye strain under harsh factory lighting, while surfaces utilize a clean Light Gray (#F2F2F2) to differentiate content cards.
- **Status Indicators:** Success, Process, and Error colors are calibrated for high contrast against the cream background, ensuring safety alerts and production statuses are immediately recognizable.

## Typography

Typography prioritizes rapid information processing. 

- **Headlines:** Use **Hanken Grotesk** in bold weights. This provides a sharp, contemporary "engineered" look that remains legible even at a distance or in low-light environments.
- **Body:** **Work Sans** is selected for its neutral, versatile character and excellent legibility in data-dense lists and reports.
- **Technical Labels:** **JetBrains Mono** is used for serial numbers, timestamps, and technical IDs to provide a distinct "data-driven" aesthetic that separates machine data from human-readable prose.
- **Hierarchy:** Maintain a clear distinction between action-oriented headers and informational body text. Use tighter letter spacing for large headlines to emphasize the "bold industrial" feel.

## Layout & Spacing

This design system utilizes a **4px baseline grid** to ensure mathematical precision across all components, adhering to the 8dp rhythm of Material Design 3.

- **Grid Model:** A fluid 4-column grid for mobile and an 8-column grid for tablet. Gutters are fixed at 16px to maintain "breathing room" between industrial data points.
- **Density:** High density is permitted for data tables and monitoring screens, but interactive targets (buttons/inputs) must maintain a minimum height of 48dp for gloved-hand accessibility or rapid-fire interaction.
- **Reflow:** On wider screens (tablet), content cards should span multiple columns rather than stretching, preserving the readability of technical data.

## Elevation & Depth

Visual hierarchy is established through a combination of **Tonal Layering** and **Navy Ambient Shadows**.

- **Surface Tiers:** Background (#EDE8DC) is the lowest level. Content cards sit on Surface (#F2F2F2). Floating elements (FABs, Modals) occupy the highest elevation.
- **Shadow Profile:** Shadows are not neutral black; they are tinted with the Primary Dark Navy (#1C2B3A). This creates a "heavy" industrial feel. 
- **Character:** Use low-blur, medium-spread shadows (e.g., `0px 4px 12px rgba(28, 43, 58, 0.12)`) for cards to make them feel like physical plates mounted on a dashboard.
- **Focus States:** High-contrast Teal (#073A40) outlines (2dp) are used instead of shadows to indicate focus, ensuring clear visibility during navigation.

## Shapes

The shape language reflects the "built" nature of manufacturing components—solid but refined.

- **Primary Elements:** Buttons and major call-to-action elements use a **12px (rounded-lg)** radius. This makes them feel tactile and "pressable."
- **Structural Elements:** Data cards, input fields, and containers use a tighter **8px (rounded)** radius. This allows for more efficient screen utilization and creates a slightly more rigid, "machined" look.
- **Pill Shapes:** Reserved exclusively for status chips (e.g., "In Progress", "Completed") to differentiate them from interactive buttons.

## Components

### Buttons
- **Primary:** Solid #1C2B3A background with White text. 12px corner radius.
- **Secondary:** Outlined with #4E7391, 1.5dp border width.
- **Focus State:** 2dp #073A40 solid border with subtle inner glow.

### Cards
- **Style:** Background #F2F2F2, 8px radius.
- **Shadow:** 4dp elevation using Navy-tinted shadows.
- **Header:** Often includes a Gold/Tan (#A69076) accent line at the top to denote category.

### Input Fields
- **Style:** Outlined Material 3 style.
- **Border:** Neutral Gray (#A69F97) default; Teal (#073A40) when focused.
- **Label:** Small Hanken Grotesk Caps above the field.

### Chips & Status
- **Success:** Green background with dark green text.
- **Process/Warning:** Amber background with dark brown text.
- **Error:** Red background with white text.
- **Shape:** Fully rounded (pill) to distinguish from structural cards.

### Lists
- **Interaction:** Dividers use a very light version of Neutral Gray (#A69F97) at 0.5dp thickness.
- **Iconography:** Outlined icons only, using the Light Blue (#8AAFC4) for non-active states and Dark Navy (#1C2B3A) for active states.

### Specialized Components
- **Machine Gauges:** Radial or linear progress bars using Teal (#073A40) to track completion.
- **Barcode Scanner Overlay:** Transparent Dark Navy scrim with a Teal scanning reticle.