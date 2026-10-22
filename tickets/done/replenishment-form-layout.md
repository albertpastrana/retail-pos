# Replenishment form layout

Captured: 2026-10-22

Related: `tickets/done/stock-orders.md`
Related: `tickets/done/replenishment-customer-terminology.md`
Related: `tickets/done/replenishment-row-status-colors.md`

## Goal

The replenishment and customer orders screen keeps the right-hand form readable
at the supported counter layout, including the manual product description field.
The form must not expose literal `null` values when older or incomplete entries
are opened.

## Context

The right form was constrained to a width that could be smaller than its
controls, and its manual description scroll pane could collapse to one line.
The list and detail labels also assembled database values directly.

## Done when

- The right-hand form can be laid out without clipping its text fields or
  buttons at the normal application width.
- The manual description field retains a readable multi-line height.
- Missing seller, product, customer, reference, EAN, note, and creator values
  render as empty or the existing translated placeholder, never as `null`.
- `./gradlew spotlessApply` and `./gradlew compileJava` pass.

## Shipped

The form now scrolls vertically, derives a smaller minimum width, preserves
stable field heights, and normalizes nullable display values in
`ReplenishmentPanel`.
