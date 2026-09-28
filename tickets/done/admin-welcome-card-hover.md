# Add hover feedback to administration welcome cards

Captured: 2026-09-29

## Goal

Show a clear hover state on workflow cards in the administration welcome
screen, including when the pointer is over the card's icon or text.

## Context

`JPanelWelcome.WorkflowButton` already provides the card actions and focus
state, but its custom painting keeps the same surface colour and nested child
components do not update the parent button's rollover model. Keep the existing
layout, labels, click actions, and keyboard behaviour unchanged.

## Done when

- Hovering any part of a workflow card visibly changes its surface colour.
- Hovering the icon, title, or hint behaves like hovering the card padding.
- Existing click and keyboard activation remain unchanged.

## Shipped

Updated `JPanelWelcome.WorkflowButton` with a visible rollover surface and
forwarded rollover events from nested icon and text components to the card.
`compileJava` and `spotlessCheck` pass. The focused integration test could not
compile because `StockReceivingPanel.java` is intentionally held in the stash
and an unrelated `StockReceivingLayoutTest` references it.

Follow-up adjustment: increased the hover contrast, added a hover border and
hand cursor, and explicitly repainted after nested hover transitions.

The implementation now uses the same `RetailPOSColors` secondary-button
pattern as the catalogue welcome cards: `surface-100` at rest, `surface-200`
on hover, `border` at rest, and `border-strong` for focus/hover feedback.

The custom `WorkflowButton` also disables Swing's default content fill so its
custom `surface-200` hover paint is not overwritten by the standard button UI.
