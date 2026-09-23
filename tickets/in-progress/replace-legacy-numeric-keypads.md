# Replace legacy numeric keypads

Captured: 2026-09-19

## Goal

Use the sales-screen `JNumberKeys` component consistently wherever a cashier enters numeric data, instead of the legacy `JEditorKeys` keypad.

## Context

Payment cash and customer-debt screens now use `JNumberKeys`. Remaining legacy keypad usages need to be reviewed and migrated where the interaction is numeric:

- `PaymentPanelType` for card payment fields.
- `JPanelPayments` for cash-drawer movements.
- `JTicketsBagTicket`.
- `JProductLineEdit`.
- `JProductAttEdit`.
- `JNumberDialog`.
- `JPasswordDialog`.

Keep a legacy text-entry keypad only where the field genuinely needs free-form text or password input. Preserve decimal, negative, clear, focus, and keyboard-input behaviour for every migrated field.

## Done when

- Numeric-entry screens use the same visual keypad and key ordering as the sales screen.
- Text and password entry still support the required character set and editing behaviour.
- Decimal, negative, clear, focus, and physical-keyboard input are verified for each migrated screen.
- No remaining `JEditorKeys` usage is numeric-only without a documented reason.
- The relevant screen flows and automated checks pass.
