# Offer family import during receiving

Captured: 2026-10-04

Related: `tickets/done/batch-stock-receiving.md`

## Goal

When a scanned receiving barcode matches a fallback catalogue family, the
operator can choose which family variants to create in the local catalogue.
Only the scanned variant is added to the open receipt; later scans add the
other variants when they are physically received.

## Context

Sales already uses the family import dialog. Receiving currently bypasses it
and only offers the single-product editor, even though the receiving dialog
already has labels and behaviour for saving selected variants. The family
selector must keep the scanned variant selected and offer a scanned-only
choice, while catalogue variants must not create stock or receipt lines.

The receiving path must use the same family choice as sales. The receiving
session still receives only the scanned variant; selected siblings are
catalogue preparation for later scans.

## Done when

- A receiving scan with multiple fallback variants opens the family selector.
- The operator can create all selected variants or keep only the scanned one.
- Only the scanned variant is added to the open receipt.
- No stock is changed before the receipt is posted.
- English, Spanish, and Catalan labels describe the resulting behaviour.
- Automated coverage verifies family import from receiving and the receipt line
  scope.

## Shipped

Receiving now uses the family selector from `CatalogImportDialog`. Selected
variants are created in the catalogue, while the receiving handoff returns only
the scanned variant. Family loading and scanned-variant selection are covered
by `CatalogVariantModelTest`; receipt line and stock scope remain covered by
`StockSessionRepositoryTest`.
