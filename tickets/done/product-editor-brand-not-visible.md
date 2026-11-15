# Show brand in product editor

Captured: 2026-11-04

Related: `tickets/done/redesign-products-screen.md`

## Goal

The product editor and the replenishment/customer-orders screen should show the
product's brand so an operator can see and confirm it while viewing or editing
a product.

## Context

Observed behaviour: the brand is not visible on the product editing screen or
in the product column of the replenishment/customer-orders table and editor.
The existing brand value and editing behaviour should be preserved; this task
is to make the brand visible in the editor without changing unrelated product
fields.

## Done when

- Opening a product in the product management screen shows its brand in the
  editor.
- The replenishment/customer-orders table shows the brand with the reference,
  using the `BRAND - REFERENCE` format when values are present.
- The replenishment/customer-orders editor shows the same brand and reference
  display for the selected product.
- A product with a brand displays the correct brand value.
- A product without a brand remains editable without errors and shows the
  appropriate empty state.
- The brand remains visible and correctly localized at supported screen sizes
  in English, Spanish, and Catalan.
- Searching replenishment/customer orders by brand returns matching products.

## Shipped

The product editor now displays the catalogue brand, and replenishment/customer
orders show `BRAND - REFERENCE` in the table and selected-product editor. Product
and replenishment data loaders provide the brand for both normal and export
flows. The replenishment/customer-orders search also matches the catalogue
brand.

Verification: `./gradlew spotlessApply`, `./gradlew compileJava`, and targeted
`ReplenishmentPanelLayoutTest` and `ProductBatchNavigatorUiTest` pass. The
targeted layout test was rerun after adding brand search.
