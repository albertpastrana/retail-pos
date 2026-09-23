# Batch stock receiving

Captured: 2026-09-24

Related: `tickets/done/stock-orders.md`, `tickets/todo/improve-stock-diary-editor-layout.md`

## Goal

When a supplier order arrives, staff can add all received products and quantities
to stock quickly in one receiving session instead of creating individual stock
movements one at a time.

## Context

The current stock diary is useful for individual movements, but receiving a
delivery is repetitive when an order contains many products. Add a dedicated
batch mode to the stock area that works well with a barcode scanner and a
keyboard:

- Scan or search a product and enter the received quantity.
- Scanning the same product again increases its quantity in the current batch.
- Show the pending lines with product, reference/EAN, quantity, and any stock
  notes needed to resolve a mismatch.
- Allow a line to be edited or removed before posting the batch.
- Confirm once to create the corresponding stock-in movements and update
  current stock.
- Keep the batch reviewable until confirmation, and do not change stock for an
  unconfirmed batch.

The flow should optionally link the receipt to a replenishment entry or a free
text supplier/order reference, but it is not a supplier purchase-order module.
Received quantities may differ from what was expected; the user must be able
to adjust them before posting. Unknown barcodes should be clearly reported and
must not create an ambiguous stock movement.

## Done when

- A stock user can open a batch receiving session from the stock area.
- A scanner can add multiple products without leaving the receiving flow.
- Repeated scans of one product accumulate in its batch line.
- The user can change quantities, remove lines, and see the final review before
  confirmation.
- Confirming a batch creates stock diary entries and updates current stock for
  every accepted line.
- Cancelling or closing an unconfirmed batch leaves stock unchanged.
- Unknown products and invalid quantities are actionable errors and cannot be
  posted accidentally.
- A receipt can be identified by an optional supplier/order reference, and the
  resulting movements retain the receiving context where the data model allows.
- Automated coverage verifies an empty batch, repeated scans, edits/removals,
  unknown products, cancellation, and successful posting of multiple lines.
