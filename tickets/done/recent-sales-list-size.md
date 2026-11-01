# Show more recent sales

Captured: 2026-11-01

## Goal

Show the most relevant recent completed sales on the edit-sales screen.

## Context

Keep the existing ordering, filters, permissions and sale-editing flow unchanged. The recent-sales table shows the payment method instead of the customer. Show at least 30 recent sales, unless today alone has more than 30 sales, in which case show all of today's sales.

## Done when

- Opening the edit-sales screen displays at least 30 recent sales.
- If today has more than 30 sales, the screen displays all of today's sales.
- The sales remain ordered from newest to oldest.
- The table shows the payment method and no longer shows the customer column.
- Existing sale editing and filtering behaviour remains unchanged.

## Shipped

- Made the edit-sales screen prefer today's sales, then yesterday's, with a 30-sale fallback in `JTicketsBagTicket`.
- Replaced the customer column with a localized payment-method column for cash and card sales.
