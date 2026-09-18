# Shared fallback catalogue in the database

Captured: 2026-09-11, 2026-09-18

Related: `tickets/todo/postgres-lan.md`, `tickets/todo/pending-barcodes.md`

## Goal

Keep `PRODUCTS` as the sell catalogue, but move the fallback catalogue used to pre-fill unknown-product creates into the shared PostgreSQL database. Every till must be able to look up the same fallback data without a local or network-mounted TSV.

## Context

The current fallback lookup reads `import-products.tsv` and overlays matching rows from `import-prices.tsv`. `import-categories.tsv` supplies the category labels shown by the dialog. When a scanned barcode is not in `PRODUCTS` but is in the TSV catalogue, the till opens the editable create-product dialog with the catalogue data. Saving creates the real `PRODUCTS` row; the fallback row is not itself sellable.

Add database tables for the fallback products and their price overlay, keyed by barcode and retaining the reference lookup used by the current implementation. Store the fields needed by the existing create flow: reference, name, category, purchase price, sale price, brand, and price source. Keep category names available without requiring a local `import-categories.tsv`. The tables are shared by all tills and are populated or refreshed by an explicit TSV import/migration step; the application does not generate the source catalogue.

### Price behaviour

Preserve the current price semantics rather than treating the two product files as interchangeable:

- `import-products.tsv` provides the base product row, including its purchase price and source sale price.
- `import-prices.tsv` is an overlay indexed by barcode and reference. When it matches, its purchase cost replaces the base purchase cost. A price-only row can also make an otherwise absent barcode available for scan-to-import.
- The scan-to-import dialog does not blindly copy `import-prices.tsv`'s sale price. It uses the resulting purchase cost and the database price rules to offer the sale price, with the cashier able to edit it. Prices are before tax; tax category `001` and the existing tax-included till display remain unchanged.
- `import-reference-prices.tsv` is not part of the barcode fallback lookup; unresolved invoice-reference data remains a separate later import concern.

The database implementation must produce the same result for barcode and reference matches, including zero-padded barcode lookup, missing-price handling, brand/source propagation, and the distinction between purchase cost and offered sale price.

The import must define deterministic behaviour for repeated barcodes and refreshes, and must not overwrite an existing `PRODUCTS` row. Existing TSV import commands may remain as the initial loading mechanism, but the sales screen must read the database table once it is populated. `PENDING_BARCODES` remains separate for codes found in neither `PRODUCTS` nor the fallback catalogue.

## Slices

1. Add the schema and migration for the shared fallback catalogue.
2. Populate the empty tables as a separate operational step from the database; the migration must not load the current TSV files.
3. Change the till lookup and create dialog to use the database table instead of reading the TSV.
4. Remove the requirement for a local TSV from satellite tills and document the one-time/import refresh operation.

## Done when

The fallback catalogue tables exist in PostgreSQL and can be populated directly in the shared database. The migration does not insert or transform the current TSV files.

Scanning a barcode that is absent from `PRODUCTS` but present in the database fallback catalogue opens the same editable create-product dialog with the expected data. Saving creates `PRODUCTS`; cancelling changes neither table nor the ticket.

All tills connected to the shared database see the same fallback data. Satellite tills do not need access to a local or network-mounted TSV to pre-fill a product, and codes absent from both tables continue through the pending-barcode/unknown-barcode flow.

## Shipped

`V27__shared_fallback_catalog` creates the empty shared tables. `DataLogicSales` reads them for till and Stock scan-to-import, including price overlays by barcode or reference. The application no longer reads the configured catalogue TSV paths for this flow.

The shared PostgreSQL database at the shop LAN address was populated from the current categories, products, and prices TSVs on 2026-09-18: 44,169 fallback products and 24,084 price lookup keys.
