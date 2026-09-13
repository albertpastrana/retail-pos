# Loyalty balance on the customer

Captured: 2026-09-12, 2026-09-14

Related: `tickets/done/loyalty-stamps-on-ticket.md`, `tickets/todo/customer-on-ticket.md`

## Goal

Stamps and leftover euros live on the customer (card), not only on paper and not only on the current receipt. Close cash treats redemption as a discount, not a payment type.

## Context

First cut is shipped: this receipt’s eligible spend, stamps earned, −5 € redemption line (`tickets/done/loyalty-stamps-on-ticket.md`). The paper card still holds the running total.

Intended rules (unchanged):

- 1 stamp = 10 € of eligible base (floor). Store leftover euros on the customer (27 € → 2 stamps and 7 €). Paper loses the remainder; the till should not.
- 12 stamps → −5 € line. One redemption per ticket unless we say otherwise.
- Needs a customer with `CUSTOMERS.CARD`. No customer, no balance.
- On close: a movement (earn / redeem) tied to the ticket, not only a number. A refund can reverse it.
- Till copy: “this ticket: +N · total: X/12”. If the ticket reaches 12: offer redeem or apply it. First cut of this ticket: balance + calc + −5 € line + text. Not the brand-offer engine.
- Close cash: redemption lowers sales as a discount. Not a payment method.
- Still open: VAT on the 5 € (split by rate like a total discount, or a fixed category like the old product); auto-redeem vs button.

Do not implement as a catalogue product. Gift voucher (`ISVOUCHER`) is unrelated.

## Done when

A customer with a card carries stamps and leftover euros across tickets. The chip can show `X/12`. Closing cash books the 5 € as discount. A refund can undo the movement.
