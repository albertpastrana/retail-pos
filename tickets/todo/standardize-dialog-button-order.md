# Standardize dialog button order

Captured: 2026-10-04

Related: `tickets/done/descriptive-dialog-actions.md`, `tickets/done/descriptive-confirmation-actions.md`

## Goal

Make the placement of cancel/safe and accept/primary actions predictable across the entire application. A cashier should not need to relearn which side of a dialog performs or abandons an operation.

## Context

The regular-dialog and confirmation-dialog work established the intended convention for the flows they covered: the secondary or safe action is on the left, and the primary action that applies the change is on the right. This ticket audits the rest of the application and closes any remaining inconsistencies, including dialogs built through shared helpers, legacy panels, custom action bars, and native confirmation replacements where the product controls the layout.

Use action meaning rather than literal labels: cancel, close, keep editing, or discard belong to the secondary/safe side; accept, save, apply, continue, or the intended destructive operation belong to the primary side. Preserve existing behavior, default-button safety, keyboard shortcuts, and localized labels.

## Done when

- Every user-facing dialog and dialog-like action bar in the application follows one documented order: secondary/safe action on the left and primary action on the right.
- The audit covers shared dialog helpers and all important call sites, including sales, payment, inventory, administration, configuration, reports, and update flows.
- No flow changes its action position based on language, window size, or whether it is opened from the till or a management screen.
- Enter, Escape, Tab, and existing accelerator behavior still produce the same safe result as before; destructive actions are not accidental default actions.
- English, Spanish, and Catalan labels remain visible and correctly associated with their actions at supported window sizes.
- Automated checks cover the shared layout/order rule and representative dialogs from each major area; manual checks record the concrete screens and flows reviewed.
- Existing contextual action labels remain contextual; this ticket changes placement and shared presentation, not the operation performed.
