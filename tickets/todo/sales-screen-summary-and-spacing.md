# Sales screen summary, catalogue and spacing

Captured: 2026-10-08

## Goal

Make the sales screen's totals, product count and catalogue easy to read and operate at the till, with consistent top-row controls and a visible loyalty notice.

## Context

The summary has read-only amounts on one row and a quantity count, excluding auxiliary, discount and redemption lines. Keep ticket arithmetic and scanner/keyboard operation unchanged. Top buttons have equal heights with measured captions. Catalogue tiles have a separate photo/caption, wrapping and full-name tooltips; the subcategory header replaces the old empty sidebar.

The catalogue uses 144x100 minimum touch tiles and a 13pt-or-smaller caption. The full category breadcrumb includes root and intermediate categories and optional product detail with navigable ancestors; it scrolls if it cannot fit. The keypad notice has reduced padding.

Latest feedback: the ticket list should be a little shorter still. At the root level the selected category already appears in the sidebar, so the redundant breadcrumb/header is hidden altogether. The trail remains visible for subcategories and product details. The default `cat-height` is now 312 instead of 288 (explicit installation configurations remain authoritative), raising the catalogue boundary without leaving a root header gap.

## Verification so far

`./gradlew spotlessApply integrationTest --tests com.openbravo.pos.panels.SalesCatalogLayoutTest`, `./gradlew check` and `git diff --check` pass. The focused test checks root, subcategory and product-detail header visibility and root grid alignment in addition to nested navigation, parent IDs, tile text and summary alignment. Hands-on visual verification at compact sizes and both themes remains open; the user has not confirmed the new spacing yet.

## Done when

- Count, tax, subtotal and total remain aligned as values change; loyalty notice stays visible.
- Root catalogue has no redundant top row; a full breadcrumb appears in subcategories and product details, with navigable ancestors.
- The ticket list ends a little higher, and the product tiles remain readable and touch-sized.
- English, Spanish and Catalan, light/dark, resizing, keyboard/focus and scanner flows are visually checked.
