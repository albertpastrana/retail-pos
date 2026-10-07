# Share welcome workflow cards

Captured: 2026-10-07

## Goal

Use one reliable, shared workflow-card component in the administration welcome
screen and the catalogue and prices welcome screen, with the administration
welcome card visual treatment.

## Context

Both screens render horizontal action cards containing nested icon and text
components. The catalogue cards forward nested mouse clicks, while the
administration cards only forward hover state, so clicking visible text or the
icon can fail. Keep the catalogue actions that create a product or start stock
receiving.

## Done when

- Clicking the icon, title, hint, or padding activates either screen's card.
- Both screens use the same shared card implementation and administration card
  visual treatment.
- Keyboard activation, focus, permissions, and catalogue-specific actions keep
  working.
- English, Spanish, and Catalan labels remain visible and unchanged.

## Shipped

Added `WorkflowCard` and migrated the administration and catalogue welcome
screens to it. Nested icon and text components now forward mouse activation to
the card action. Focused integration tests pass for both welcome screens in the
supported locales.

Follow-up: the catalogue job cards retained a screen-specific 112px preferred
height after the shared component was introduced. Remove that override so they
use the shared administration card height.

Follow-up shipped: removed the catalogue-only preferred-height override and
re-ran the focused welcome-screen integration tests.
