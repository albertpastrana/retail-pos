# Loyalty stamps on this ticket

Captured: 2026-09-12

Related: `tickets/todo/loyalty-customer-balance.md`

## Goal

First cut: the till shows how many stamps this ticket earns and can add a −5 € redemption line. Not a catalogue product. Paper card still holds the running total.

## Context

**Decision:** not a product. 5 € is a redemption, not a sale (it would distort catalogue, sales, and VAT). Negative ticket line, like a total discount. Gift voucher (`ISVOUCHER`) is separate.

Rules implemented on the **current ticket** only:

- 1 stamp per 10 € of eligible spend (floor).
- Do not count: catalogue sale (`SALE_PERCENT`), line/total discount, gift vouchers, redemption lines.
- Redemption is a −5 € line (button). Several redemptions allowed if the ticket qualifies more than once; customer `X/12` balance is the follow-up ticket.

VAT split, leftover euros on the customer, and close-cash booking are `tickets/todo/loyalty-customer-balance.md`.

## Done when

A receipt with eligible spend shows stamps earned. A cashier can add a −5 € loyalty line. Sale, discount, voucher, and redemption lines do not earn stamps.

## Shipped

`LoyaltyStamps`, ticket properties `loyalty.enabled` / `loyalty.name` (default `victorines`), redemption line property, till copy and redemption button in `JPanelTicket`. Tests in `LoyaltyStampsTest`. Migrations `V13`–`V15` for ticket wording.
