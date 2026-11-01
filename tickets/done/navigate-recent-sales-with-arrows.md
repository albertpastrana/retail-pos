# Navigate recent sales with arrows

Captured: 2026-11-01

## Goal

Allow the operator to move through the recent-sales table with the keyboard arrow keys.

## Context

The selected sale should continue to load through the existing selection listener. Preserve mouse selection and the single-row selection mode.

## Done when

- The recent-sales table can receive keyboard focus.
- Up and down arrows change the selected sale and load it through the existing flow.
- Mouse selection and sale editing remain unchanged.

## Shipped

- Enabled keyboard focus on the recent-sales table so the up and down arrows can move between sales.
