# Observe fallback product creation

Captured: 2026-10-23

Related: `tickets/backlog/fallback-catalog-product-discovery.md`

## Goal

Make fallback catalog imports observable enough to distinguish a fallback
match from a product actually created or updated in the local catalogue.

## Context

The import dialog currently logs fallback matches and generic import success,
but those events cannot be correlated reliably or identify the local product
affected. Add a correlation ID per import attempt and structured events for
the resulting local product operation. Do not log names, prices, or other
catalogue fields beyond the existing barcode/family and stable product IDs.

## Done when

- A fallback import attempt has one correlation ID shared by its related logs.
- A successful single-product fallback import logs the local product ID and
  code with an explicit created/updated operation.
- A successful family fallback import logs the local product ID and code for
  each affected product with an explicit created/updated operation.
- Existing fallback match, cancellation, and success behaviour is preserved.
- Relevant tests and formatting pass.

## Shipped

- Added `import_id` correlation to catalog import and fallback events.
- Added `catalog_fallback_product_operation` events with the local product ID,
  code, and `created` or `updated` operation for single and family imports.
- Verified with `./gradlew spotlessApply`, `./gradlew compileJava`, and the
  `CatalogFileImporterIT` integration test.
