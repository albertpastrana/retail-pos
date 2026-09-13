# Unknown barcode notice at the till

Captured: 2026-09-10, 2026-09-11

Related: `tickets/todo/pending-barcodes.md`

## Goal

If a scanned code is not in `PRODUCTS` and not in the import TSV, do not open the create dialog. Show a notice under the keypad. Record that it was not found.

## Context

Create-from-TSV still uses `CatalogImportDialog` when the code is in the file. Unknown means neither catalogue nor TSV.

Shared `PENDING_BARCODES` and the Stock list are a later ticket.

## Done when

The till beeps, the keypad area says the code was not found and was noted, and no create dialog appears.

## Shipped

`JPanelTicket.productNotFound`: log `event=unknown_barcode`, set `m_jScanStatus` with `message.unknownbarcode.logged`. Create dialog only via import-if-in-TSV.
