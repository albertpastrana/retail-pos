# Widen catalogue search field

Captured: 2026-10-07

Related: `tickets/done/clarify-catalogue-welcome-actions.md`

## Goal

Keep the catalogue and prices scan/search field visibly touch-sized at the
supported window widths.

## Context

The search well has a preferred height but no minimum height, so the enclosing
`GridBagLayout` can compress it. Preserve the existing search input, scanner
focus, hint, and translated copy.

## Done when

- The scan/search well does not render below its touch-sized height.
- The search field remains usable at the minimum supported window width.
- Existing catalogue welcome layout and search tests pass.

## Shipped

Added a minimum height to the scan/search well and fixed the shared edit icon
mapping for the product-editing card. The focused catalogue welcome test now
checks both the touch-sized search well and the visible edit icon.
