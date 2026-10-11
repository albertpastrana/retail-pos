# Select lines for gift receipts

Captured: 2026-10-11

## Goal

When printing a gift receipt from a completed sale, the operator can choose which sale lines belong to that receipt and print different selections for different recipients.

## Context

The existing gift-receipt action always prints every line from `Printer.TicketGift.xml`. Selection is per complete ticket line. A selected receipt is rendered from an in-memory copy and is not saved as a new sale or receipt.

The original sale, its lines, payments, stock, and totals must remain unchanged.

## Done when

- The gift-receipt action shows the sale lines with touch-sized selection controls.
- The operator can print a receipt containing only the selected lines.
- The operator can print another selection from the same sale without changing the sale.
- Printing with no selected lines is prevented with a clear message.
- The Victorines redemption line never appears on a gift receipt.
- The flow is available in English, Spanish, and Catalan.
