# Sales screen summary, catalogue and spacing

Captured: 2026-10-08

## Goal

Make the sales screen's totals, product count and catalogue easy to read and operate at the till, with consistent top-row controls and a visible loyalty notice.

## Context

The summary has read-only amounts on one row and a quantity count, excluding auxiliary, discount and redemption lines. Keep ticket arithmetic and scanner/keyboard operation unchanged. Top buttons have equal heights with measured captions. Catalogue tiles have a separate photo/caption, wrapping and full-name tooltips; the subcategory header replaces the old empty sidebar.

The catalogue uses 144x100 minimum touch tiles and a 13pt-or-smaller caption. The full category breadcrumb includes root and intermediate categories and optional product detail with navigable ancestors; it scrolls if it cannot fit. The keypad notice has reduced padding.

The selected root category already appears in the sidebar, so its redundant breadcrumb/header is hidden altogether. The trail remains visible for subcategories and product details. The default `cat-height` is 312 (explicit installation configurations remain authoritative), raising the catalogue boundary without leaving a root header gap.

CodeRabbit review on PR #97 identified three edge cases, now addressed: product detail opened from a ticket line resolves that product's category and any uncached ancestors without changing the browsing category used by Back; long customer names/debt are capped in the toolbar with the full caption in a tooltip; and exceptionally large amounts/counts grow only their readout widths beyond the normal fixed minimums. A focused test checks the quantity aggregation and clearing as well.

## Verification so far

`./gradlew spotlessApply integrationTest --tests com.openbravo.pos.panels.SalesCatalogLayoutTest`, `./gradlew check` and `git diff --check` pass after the CodeRabbit fixes. CI checks were green for the original PR commit and must be rechecked after the review fix. Hands-on visual verification at compact sizes and both themes remains open; the user has not confirmed the new spacing yet.

## Done when

- Count, tax, subtotal and total remain aligned as values change; loyalty notice stays visible.
- Root catalogue has no redundant top row; a full breadcrumb appears in subcategories and product details, with navigable ancestors and correct product category.
- The ticket list ends a little higher, product tiles remain readable and touch-sized, long captions and amounts remain accessible.
- English, Spanish and Catalan, light/dark, resizing, keyboard/focus and scanner flows are visually checked.
