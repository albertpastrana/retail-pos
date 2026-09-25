# Replace legacy numeric keypads

Captured: 2026-09-19

## Goal

Use the sales-screen `JNumberKeys` component consistently wherever a cashier enters numeric data, instead of the legacy `JEditorKeys` keypad.

## Context

The following screens already use `JNumberKeys`:

- `JPaymentCashPos` for cash payment.
- `JPaymentDebt` for customer-debt payment.
- `JPanelPayments` for cash-drawer movements.
- `JTicketsBagTicket`.
- `JProductLineEdit`, reached through Sales > Edit line, now uses regular
  `JTextField` controls with `JNumberKeys` for its numeric fields. Its dialog
  uses content-driven Swing layouts and `pack()` rather than fixed bounds.
- `JPaymentCashPos` now uses one editable `JTextField` for tendered cash,
  numbers-only `JNumberKeys`, and no denomination shortcut buttons. Its cash
  status is top-aligned with natural field sizing so the partial-payment action
  does not move the entry controls.

The remaining `JEditorKeys` usages are:

- `PaymentPanelType`, reached through card payment, where the card number and
  expiration date are numeric but the cardholder name is free-form text.
- `JPasswordDialog`, used for passwords and supervisor authorization. It now
  uses `JNumberKeys` for touch-entered digits and a masked `JPasswordField`
  for physical-keyboard text input.

There is no `JProductAttEdit.java` in the current source tree.

Keep a legacy text-entry keypad only where the field genuinely needs free-form text or password input. Preserve decimal, negative, clear, focus, and keyboard-input behaviour for every migrated field.

## Done when

- Numeric-entry screens use the same visual keypad and key ordering as the sales screen.
- Text and password entry still support the required character set and editing behaviour.
- Decimal, negative, clear, focus, and physical-keyboard input are verified for each migrated screen.
- No remaining `JEditorKeys` usage is numeric-only without a documented reason.
- The relevant screen flows and automated checks pass.
