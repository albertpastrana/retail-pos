# Select rows by clicking the checkbox column

Captured: 2026-11-01

## Goal

Clicking a row's checkbox in the replenishment and customer-order list selects or
deselects that row without requiring `Shift`, `Cmd`, or `Ctrl`.

## Context

Keep the existing modifier-based multi-selection behaviour for clicks outside the
checkbox column. The checkbox column is the explicit touch-friendly selection
control and must toggle only the clicked row.

## Done when

- A modifier-free click on an unchecked checkbox selects its row.
- A modifier-free click on a checked checkbox deselects its row.
- Existing `Shift` and `Cmd/Ctrl` selection behaviour remains unchanged.
- The behaviour is covered by an automated regression test.
- Focused replenishment tests and formatting pass.

## Shipped

Added modifier-free checkbox-column interaction coverage and made checkbox hit
detection use the column's model index. Existing selection behaviour with
`Shift` and `Cmd/Ctrl` remains unchanged.
