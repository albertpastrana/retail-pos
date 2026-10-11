# Classify exchanges by balance

Captured: 2026-10-11

## Goal

Allow a return-and-replacement flow to produce the correct receipt type and cash-closing classification based on its final balance.

## Context

The return screen creates a refund ticket before the operator adds replacement products. The ticket therefore always uses the refund number sequence, refund payment dialog, refund receipt wording, and refund cash-closing totals.

Keep the existing single-screen flow and stock movements. At payment time, classify a return containing a replacement item by its final balance: positive balance is a normal sale, negative balance is a refund, and zero balance is a normal-numbered exchange with no payment or cash movement. A pure return keeps the existing refund behaviour. Restore the return editor's type and receipt number if checkout is cancelled or fails so an operator can retry without losing refund-specific editing controls or retaining a number from the wrong sequence.

## Done when

- A return with a replacement product and positive balance receives a normal sale number, uses sale payment handling, and is included in sale totals at cash closing.
- A return with a negative balance keeps refund numbering, refund payment handling, and refund cash-closing totals.
- A return with zero balance completes without opening payment selection or recording a zero payment, while saving both stock movements.
- A pure return retains the existing refund behaviour.
- Automated coverage verifies classification, persistence, stock movements, and payment totals for all cases.

## Shipped

`TicketInfo.classifyExchangeByBalance()` classifies replacement returns at checkout. `JPanelTicket` uses the resulting sale/refund payment flow, skips the dialog for a zero balance, and restores the editor type and original receipt number when checkout fails. `ExchangePersistenceIT` covers all three exchange balances and a pure return, including numbering, stock and cash-closing totals. `./gradlew ciCheck` passed against Derby, MySQL and PostgreSQL; operator UI verification remains unperformed.
