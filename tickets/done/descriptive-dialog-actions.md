# Descriptive actions in dialogs

Captured: 2026-09-17

Related: `tickets/done/descriptive-confirmation-actions.md`

## Goal

Replace generic dialog buttons such as **OK** and **Cancel** with labels that describe the action the cashier is about to perform. The button text should make the result clear without requiring the user to infer it from the dialog title or the selected icon.

## Context

The POS currently uses shared generic resources in several dialogs:

- `src-pos` uses `AppLocal.getIntString("Button.OK")` and `Button.Cancel`.
- `src-beans` uses `button.ok` and `button.cancel` from `beans_messages`.
- `src-data` uses `button.ok` and `button.cancel` from `data_messages`.
- `src-data/com/openbravo/data/gui/JListData.java` hardcodes `Aceptar` and `Cancelar` instead of using localized resources.

The Catalan generic resources are currently `D'acord` and `Cancel·la`. The same resource is reused for different operations, so changing `Button.OK` globally would produce misleading labels.

## Scope

Review and update the following dialogs:

| File | Current result | Proposed primary CTA |
| --- | --- | --- |
| `src-pos/com/openbravo/pos/panels/JProductFinder.java` | Returns the selected product | `Selecciona producte` or `Afegeix producte` |
| `src-pos/com/openbravo/pos/customers/JCustomerFinder.java` | Returns the selected customer | `Selecciona client` |
| `src-pos/com/openbravo/pos/panels/JTicketsFinder.java` | Returns the selected ticket | `Selecciona tiquet` or `Obre tiquet` |
| `src-pos/com/openbravo/pos/payment/JPaymentSelect.java` | Executes and confirms a payment | `Confirma pagament` |
| `src-pos/com/openbravo/pos/sales/ReceiptSplit.java` | Confirms a ticket split | `Divideix tiquet` |
| `src-pos/com/openbravo/pos/sales/JProductLineEdit.java` | Saves changes to a ticket line | `Desa canvis` |
| `src-pos/com/openbravo/pos/sales/JProductAttEdit.java` | Saves product attributes | `Aplica atributs` |
| `src-pos/com/openbravo/pos/util/SelectPrinter.java` | Confirms the selected printer | `Selecciona impressora` |
| `src-pos/com/openbravo/pos/forms/JDlgChangePassword.java` | Changes the password | `Canvia contrasenya` |
| `src-beans/com/openbravo/beans/JNumberDialog.java` | Accepts a numeric value | `Aplica valor` |
| `src-beans/com/openbravo/beans/JCalendarDialog.java` | Accepts a date/time | `Selecciona data` |
| `src-data/com/openbravo/data/gui/JSort.java` | Applies the selected sort | `Aplica ordenació` |
| `src-data/com/openbravo/data/gui/JListData.java` | Returns the selected item | `Selecciona` |

The cancellation CTA should also be contextual where useful, for example `Cancel·la edició`, `Cancel·la pagament`, `Cancel·la devolució`, `Descarta canvis`, or `Tanca`.

## Button consistency

All dialogs should use the same action order and visual hierarchy. Following the product UX convention, the secondary action (cancel, close, or discard where appropriate) should be on the left and the primary CTA should be on the right. The primary action must be the default action when that is safe and unambiguous.

The current generated dialogs commonly add the OK button before the Cancel button to a right-aligned `FlowLayout`, which results in OK on the left and Cancel on the right. This order should be corrected consistently rather than fixed on a dialog-by-dialog basis.

## Implementation details

- Add action-specific resource keys instead of changing the shared `Button.OK` value.
- Update all available locale files for each new key: English, Spanish, and Catalan.
- Keep the existing event handlers and return values unchanged; this ticket changes presentation, not behavior.
- Keep the default-button and keyboard behavior unchanged.
- Standardize button order in every in-scope dialog: secondary action on the left, primary CTA on the right.
- Use the same alignment, spacing, margins, focus treatment, and primary/secondary visual styling across dialogs.
- Make the primary action the default button only when pressing Enter cannot cause an unexpected destructive action.
- Remove the hardcoded Spanish labels from `JListData.java` and use `LocalRes` resources.
- The form sections marked as NetBeans-generated should be changed through the form source where applicable, or regenerated consistently so the labels are not lost.
- Preserve icons only where they still communicate the action correctly; do not use the generic OK icon as the only indication of the operation.

### Product finder

`JProductFinder` is shared by the Till and inventory screens. In `JPanelTicket.m_jListActionPerformed()`, the returned product is passed to `buttonTransition(prod)` and added to the sale. Inventory flows only select a product. Therefore the dialog must receive a contextual action label, or use the neutral `Selecciona producte`; it must not globally become `Afegeix producte`.

## Acceptance criteria

- No in-scope dialog presents a generic `OK`, `D'acord`, or equivalent as its primary action.
- Each primary button describes the operation it performs.
- Cancellation labels clearly indicate whether the user is cancelling, discarding, or simply closing.
- The primary CTA is consistently placed on the right and the secondary action on the left in every in-scope dialog.
- Button alignment, spacing, focus behavior, and visual hierarchy are consistent across the dialogs.
- Product selection from the Till remains functionally unchanged and its CTA makes clear that the product will be added to the sale.
- All labels are localized in English, Spanish, and Catalan.
- Selecting a dialog action with the mouse, Enter key, or existing keyboard controls produces the same result as before.
- `JListData` no longer contains hardcoded `Aceptar` or `Cancelar` strings.

## Out of scope

Native confirmation dialogs using `JOptionPane` with `Yes/No/Cancel` buttons. They are tracked separately in `tickets/todo/descriptive-confirmation-actions.md`.
