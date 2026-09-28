# Make catalogue welcome cards easier to click

Captured: 2026-09-29

## Goal

Make the job cards in the catalogue and prices welcome screen respond
consistently when the cashier points at or clicks their text and icon, not only
the empty part of the card.

## Context

The cards are `JButton` instances containing nested labels and text areas. Those
children can take the mouse events, so the parent button's rollover state and
action are unreliable over the visible card content. Keep the existing card
layout, labels, actions, and touch-sized dimensions unchanged.

The administration welcome screen uses a different card implementation and is
out of scope; its missing hover treatment is a separate follow-up.

## Done when

- Moving over any visible part of a top job card shows its hover treatment.
- Clicking its icon, title, hint, or padding performs the same action.
- Keyboard activation and the lower catalogue navigation links still work.
- English, Spanish, and Catalan labels remain unchanged.

## Shipped

Updated `JPanelStockWelcome` so the top job-card children forward hover and
click behaviour to the containing button, and enabled rollover tracking for the
rounded button implementation. `spotlessCheck` passes. Full compilation and
manual UI verification are blocked by the unrelated pre-existing
`StockReceivingPanel` call to `ReplenishmentPanel.startManualEntry` with two
arguments instead of one.

The card hover now follows the shared design-system secondary-button pattern:
`surface-100` at rest, `surface-200` on hover, `border` at rest, and
`border-strong` for focus/hover feedback.
