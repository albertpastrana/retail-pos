# Manual loyalty card decision

Captured: 2026-09-17

Related: `tickets/done/loyalty-stamps-on-ticket.md`, `tickets/todo/loyalty-customer-balance.md`, `tickets/done/customer-on-ticket.md`

## Goal

Keep loyalty manual while the shop does not need a stored customer balance. This is an operational decision, not a pending implementation task. The paper card is the source of truth and the cashier records stamps and redemptions manually.

## Context

Do not add loyalty columns, balances, movements, or customer history to the database. A customer can still be attached to a ticket for customer information and debt, but loyalty must not depend on having a customer record.

The existing ticket-only stamp calculation and redemption line from `tickets/done/loyalty-stamps-on-ticket.md` may remain available as a cashier aid. It must not imply that the customer’s running balance is persisted.

The future stored balance is parked in `tickets/todo/loyalty-customer-balance.md`.

## Done when

The cashier can use the paper card to decide when to add stamps or apply the manual loyalty redemption. Closing a ticket does not create or update any loyalty data in the database.

## Shipped

The shop keeps the loyalty balance on paper. The POS may calculate the current ticket's stamps and redemption as a cashier aid, but it does not persist customer loyalty data.
