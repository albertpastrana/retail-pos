# Loyalty stamps on this ticket

Captured: 2026-09-12

Related: `tickets/backlog/loyalty-customer-balance.md`

## Goal

First cut: the till shows how many stamps this ticket earns and can add a −5 € redemption line. Not a catalogue product. Paper card still holds the running total.

## Context

**Decision:** not a product. 5 € is a redemption, not a sale (it would distort catalogue, sales, and VAT). Negative ticket line, like a total discount. Gift voucher (`ISVOUCHER`) is separate.

Rules implemented on the **current ticket** only:

- 1 stamp per 10 € of eligible spend (floor), on the amount with VAT.
- A **total discount** (`Dte. total`) makes the whole receipt ineligible: 0 stamps.
- A **line percentage** off (typed at the till or catalogue `SALE_PERCENT`) drops that line from the count. Gift vouchers never count, in either direction: handing one over is a payment, not a discount.
- Redemptions and any other **negative line** come off the eligible spend, so 82 € with a −5 € redemption earns 7 stamps, not 8. Never below zero.
- Redemption is a −5 € line (button). Several redemptions allowed if the ticket qualifies more than once; customer `X/12` balance is the follow-up ticket.

**Decision (2026-09-14):** stamps follow what the customer actually pays. The first cut ignored negative lines instead of subtracting them, so a redeemed receipt still earned stamps on the pre-discount amount. Total discounts were made all-or-nothing to avoid splitting a ticket-wide discount across eligible and non-eligible lines. On a receipt with a total discount the till says why instead of showing a bare zero (`label.loyalty.totaldiscount`).

There is no fixed-amount ticket discount at the till: `Dte. total` only takes a percentage. A discount in euros has to be typed as a negative line, which comes off the eligible spend like a redemption.

VAT split, leftover euros on the customer, and close-cash booking are `tickets/backlog/loyalty-customer-balance.md`.

## Done when

A receipt with eligible spend shows stamps earned. A cashier can add a −5 € loyalty line. Sale, line discount and voucher lines do not earn stamps; redemptions and negative lines lower the count; a total discount leaves the receipt at zero with the reason on screen.

## Shipped

`LoyaltyStamps`, ticket properties `loyalty.enabled` / `loyalty.name` (default `victorines`), redemption line property, till copy and redemption button in `JPanelTicket`. Tests in `LoyaltyStampsTest`. Migrations `V13`–`V15` for ticket wording.
