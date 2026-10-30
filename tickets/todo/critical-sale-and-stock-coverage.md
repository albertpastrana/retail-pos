# Protect critical sale and stock paths

Captured: 2026-10-30

Related: `tickets/in-progress/improve-testing-foundation.md`

## Goal

Protect the POS behaviours that can lose money or stock with representative service and repository tests, plus a small number of stable end-to-end till scenarios.

## Context

The existing suite covers selected taxes, payments, cash closing, persistence, and stock behaviour, but does not yet provide a complete regression path from ticket creation through payment, persistence, and stock mutation.

## Done when

- Ticket creation, taxes, line and total discounts, cash/card/mixed payments, and change are covered.
- Refunds, voids, cash closing, sale persistence, stock mutation, and transaction rollback are covered.
- Invalid payment and rollback cases assert both the returned error and unchanged persistent state.
- At least one representative till flow runs end to end without depending on a physical device.
