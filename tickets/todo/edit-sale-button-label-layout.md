# Fix edit-sale button label layout

Captured: 2026-09-19

## Goal

Make the complete `Suprimeix` and `Cancel·la` labels readable in the completed-sale editor at the configured desktop and HiDPI sizes.

## Context

The two editor buttons currently truncate their Catalan labels with an ellipsis. The buttons should keep the normal application font and use their natural layout size rather than relying on a fixed width or clipping text.

## Done when

- `Suprimeix` is fully visible.
- `Cancel·la` is fully visible.
- The normal application font and touch-friendly button height are preserved.
- The layout remains usable at the supported window sizes and display scales.
- The edit, delete, and cancel actions are unchanged.
