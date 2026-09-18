# Descriptive actions in confirmation dialogs

Captured: 2026-09-17

Related: `tickets/todo/descriptive-dialog-actions.md`

## Goal

Replace native confirmation buttons such as **Yes**, **No**, and **Cancel** with explicit, localized actions that describe what will happen. This is a second phase after replacing generic actions in regular dialogs.

## Context

The POS uses `JOptionPane.showConfirmDialog(...)` in multiple destructive or state-changing flows. Native option labels do not explain the consequence of the choice and can be especially ambiguous when the dialog message asks whether the user wants to continue.

## Button consistency

Custom confirmation dialogs must follow the same product-wide order as regular dialogs: the safe secondary action (`Cancel·la`, `Tanca`, or `Conserva`) goes on the left and the action that applies the change goes on the right. Destructive actions should remain visually distinct, but their position must not change between dialogs.

## Scope

Review these confirmation flows:

- Delete the current ticket: `JTicketsBagTicketBag`, `JTicketsBagSimple`, and `JTicketsBagShared`.
- Close the cash: `JPanelCloseMoney`.
- Save changes before leaving: `JPanelConfiguration` and `BrowsableEditableData`.
- Restore configuration values: `JPanelConfiguration`.
- Remove card values: `PeopleView`.
- Bulk price-rule operations: `PriceRulesPanel`.
- Sale-mark operations: `SaleMarkPanel`.
- Resize an image: `src-data/com/openbravo/data/gui/JImageEditor.java`.

## Implementation details

- Replace `JOptionPane.showConfirmDialog(...)` with an option dialog or a small shared confirmation helper that supports custom button labels.
- Add localized resource keys for each action, for example `Suprimeix`, `Tanca caixa`, `Desa`, `Descarta`, `Continua editant`, `Restaura valors`, and `Cancel·la`.
- Keep the current result mapping exactly equivalent. In particular, the save-before-exit dialogs have three outcomes: save, discard changes and continue, or remain in the current screen.
- Use a destructive visual treatment for irreversible actions such as deleting a ticket or removing a card value.
- Ensure the cancel/close option leaves the current data unchanged.
- Use a consistent button order: secondary/safe action on the left, primary action on the right.
- Use consistent spacing, alignment, focus treatment, and destructive styling across all confirmation dialogs.
- Do not make a destructive action the default keyboard action; the default must be the safest expected choice.
- Update English, Spanish, and Catalan locale files.
- Verify keyboard focus and default-button behavior for every custom confirmation dialog.

## Acceptance criteria

- Confirmation actions state the consequence instead of displaying generic `Yes`, `No`, or `Cancel` labels.
- Destructive actions are distinguishable from safe alternatives.
- The action order is consistent with regular dialogs: secondary/safe action left, primary action right.
- Destructive actions are never the default action when triggered by Enter.
- Save, discard, continue-editing, and cancel outcomes retain their existing behavior.
- All confirmation text and button labels are localized in English, Spanish, and Catalan.
- Closing a confirmation dialog through the window close control has the same safe behavior as cancelling.
- All listed flows are manually verified, including delete ticket, close cash, save-before-exit, restore configuration, and remove card value.

## Dependency

Implement after `tickets/todo/descriptive-dialog-actions.md`, so the two phases can be reviewed and released independently.

## Shipped

Confirmation flows now use localized contextual actions with safe defaults, destructive styling, and consistent button ordering. Covered flows include ticket deletion, cash closing, save-before-exit, configuration restore, card values, price rules, sale marks, and image resizing. Main paths: `src-data/com/openbravo/data/gui/JConfirmationDialog.java` and the affected sales, configuration, inventory, admin, and data GUI classes.
