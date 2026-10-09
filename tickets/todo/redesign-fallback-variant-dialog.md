# Redesign fallback variant import dialog

Captured: 2026-10-09

## Goal

Make the scanned fallback-catalogue family quick to price and stock at the till, following the three supplied reference screenshots. Give the list one extra row of vertical space in the default dialog.

## Context

Keep factory cost and tax category persisted but hidden in the family editor. Use catalogue cost when available, otherwise zero; use the catalogue tax category when available, otherwise the configured/default available tax category. Only category, VAT-inclusive retail price and stock are directly editable; name and reference require an explicit edit toggle. The scanned variant remains selected. Preserve select-all, scanned-only, skip and receipt/stock/receiving semantics. Do not change the single-product importer.

## Done when

- The family dialog matches the supplied layout: EAN in header, editable stock per row and synchronized right-side stepper, scanned-row and missing-price text, optional name/reference editor, category, retail price and apply-price option.
- Receipt creation is disabled until the scanned variant has a valid price; the footer reports selection and price state.
- English, Spanish and Catalan labels are present; relevant tests and a manual check of the affected screens and flows pass.

## Implementation and verification

The family editor implementation and translations are in the task branch. Factory cost is persisted from the fallback catalogue when available, otherwise as zero; the catalogue tax category is used when valid, otherwise the configured default or first available tax category. `./gradlew spotlessApply`, compilation and `integrationTest --tests com.openbravo.pos.inventory.CatalogVariantModelTest` pass. Manual verification is still required for receipt, stock and receiving scans, focus and keyboard navigation, row/stepper synchronization, resizing and en/es/ca labels at supported display scales. Keep this ticket open until those checks pass.

The first screenshot review showed a duplicate native/window heading, an oversized two-column layout with a split-pane handle, incorrect checkbox appearance and inconsistent table foregrounds. Match the original reference's compact surface, selected-row outline, explicit checked boxes and sunken stock fields before marking visual verification complete.

The cashier's follow-up supersedes the unchecked reference state: start “Apply this price…” checked, so entering a price also prices the selected variants.

The compact two-column layout removes the split-pane divider, constrains the summary to a two-line block, and adds a footer divider. Theme tokens drive striped rows, the scanned-row outline, checked icons and stock wells. An earlier iteration removed native title chrome; cashier feedback below restores it. A new screenshot of the running application is still needed to confirm the visual result; verify both the collapsed and expanded identity layouts before closing.

Further cashier feedback: restore the native window frame and close control; identify the header barcode explicitly as the scanned EAN; focus the price field when the dialog opens; select the entire quantity on a table stock double-click; round its stock field outline; and stop the right-hand stepper from growing taller than the price field. Check both table and right-hand stock editing after this revision.

Implemented the follow-up controls and en/es/ca scanned-EAN label. The stock renderer now draws a rounded token-coloured well, and the editor requests a round FlatLaf text field. The right-hand stepper stays 48px high under an aligned label; the price field gains focus when the decorated window opens; table stock edit selects the current quantity. Compilation and targeted integration test pass; runtime visual/keyboard checks are still pending.

Latest cashier request: make the default dialog a little taller. Increased the variant list's preferred height by one 48px touch row (392 → 440) and reran `./gradlew spotlessApply` and the targeted integration test. The new height has not yet been visually checked in the running till; leave this ticket open for that check.

Code review identified a selection regression: full-table refreshes during editing could clear the selected row. Refresh only the edited row during keystrokes and use row-update events for family-wide changes so the cashier keeps the current variant selected.

Invalid catalogue prices are treated as missing while loading a variant, so the family editor remains open and the existing missing-price validation can guide the cashier instead of throwing on the Swing event thread.
