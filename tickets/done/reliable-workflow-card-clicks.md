# Make workflow card clicks reliable

Captured: 2026-10-07

Related: `tickets/done/share-welcome-workflow-cards.md`

## Goal

Make workflow cards activate smoothly and consistently when the pointer is over
their nested icon or text components.

## Context

The shared card currently forwards activation from nested components only on
`mouseClicked`. Use the normal mouse press/release activation path instead,
without changing keyboard activation or causing duplicate actions.

## Done when

- A left-button press and release over the icon, title, hint, or card padding
  activates the card exactly once.
- Moving across nested card components does not leave the card stuck pressed or
  trigger an extra action.
- Keyboard activation and existing catalogue-specific actions remain unchanged.

## Shipped

Nested workflow-card components now forward left-button press and release to the
shared card model and activate once on release. Integration coverage exercises
the icon, title, and hint using press/release events.
