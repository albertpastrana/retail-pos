# Show line discounts on receipts

Captured: 2026-11-08

Related: `tickets/done/catalog-sale-percent.md`

## Goal

The standard printed receipt and preview show a discounted item's original unit price, final line total and a separate line identifying the percent and total savings, within the existing 42 columns.

## Context

Use the agreed format: original price in the Preu column, discounted amount in Total, and `Descompte aplicat (-10%)` with `-1,99` below the item. Calculate the amount including tax and quantity, including returns. Leave non-discounted and total-discount lines as they are. Shipped XML templates are the default for new installations and available as originals to restore manually; existing database-backed resources must not be overwritten. No database migration or direct database edit.

## Done when

- A discounted product line prints and previews with the original unit price, discounted line total, percent and actual savings, correctly aligned in 42 columns.
- Ordinary lines, total-discount lines and refunds still show correct amounts without spurious discount detail.
- A fresh installation uses the shipped templates; an existing installation retains its database resource until an operator chooses to restore the original.
- Targeted tests pass and printed/preview receipt layout is checked where possible.

## Shipped

- `TicketLineInfo` supplies the original tax-included unit price and signed total savings; both shipped printer templates display the additional 42-column discount row without changing regular or total-discount rows.
- Existing database resources stay in place; the bundled defaults are available through Restore original in Resources. See the English and Catalan receipt-template guides for opt-in steps.
- Velocity rendering, XML parsing, discount arithmetic, refund sign, and related ticket tests passed. A physical printer test remains for a shop till.
