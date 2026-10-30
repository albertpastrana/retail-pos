# Use one fallback import screen

Captured: 2026-10-30

## Goal

Use the fallback variant screen for every import, including a fallback family
with one product and an unknown barcode without family data.

## Context

The fallback flow currently has a family editor and a separate single-product
editor. The family editor already contains the complete product fields and the
selection table, so keeping both screens makes the cashier learn two layouts
for the same operation.

The single-product case must appear as one selected variant. When no catalogue
row exists, the screen must still provide one editable row using the scanned
barcode as its initial code and reference.

## Done when

- A fallback family with multiple products opens the variant screen.
- A fallback family with one product opens that same variant screen with one row.
- An unknown stock barcode opens that same variant screen with one editable row.
- Cancelling, validation, stock application, and receiving handoff keep their
  existing behaviour.
- The old single-product dialog and its locale-dependent action path are gone.

## Shipped

The fallback import now always uses `CatalogImportDialog`'s variant screen.
Single products and unknown stock barcodes are represented by one editable row;
the former single-product JOptionPane flow was removed. Coverage is in
`CatalogVariantModelTest`.
