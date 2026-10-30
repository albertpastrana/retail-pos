# Cover UI and hardware test boundaries

Captured: 2026-10-30

Related: `tickets/in-progress/improve-testing-foundation.md`

## Goal

Test critical Swing and peripheral boundaries with operator-focused assertions and fakes instead of physical devices.

## Context

The repository contains layout and permission tests, but important cashier workflows and configured hardware boundaries still lack focused, stable coverage.

## Done when

- Permission-driven UI behaviour is verified as observed by the operator.
- Receipt rendering, preview, and reprint are tested structurally.
- Drawer, printer, barcode scanner, and scale adapters are tested with fakes, including error handling.
- Important flows are safe on the EDT and include keyboard, focus, resize, English, Spanish, and Catalan checks.
