# Pending barcodes queue

Captured: 2026-09-11, 2026-09-14

Related: `tickets/done/unknown-barcode-notice.md`, `tickets/todo/stock-orders.md`, `tickets/done/postgres-lan.md`

## Goal

Unknown EANs are a shared queue on the database, not a log line on one PC. Stock staff can create the product or discard the row. Creating the product closes the row. Every till sees the same queue.

## Context

Till scan of a code that is in neither `PRODUCTS` nor the import TSV already skips the create dialog and shows a notice under the keypad (`tickets/done/unknown-barcode-notice.md`). That path only logs locally.

Intended table (`PENDING_BARCODES`): code, first seen, last seen, count, user.

Stock panel: list → create or discard. Same queue on all PCs once Postgres is shared.

“Not in the TSV” and “not in `PRODUCTS`” are different; the till already treats “not in either” as unknown.

## Done when

Scanning an unknown EAN writes or updates a pending row. Stock can resolve it. A second till on the same database sees the scan without copying files.
