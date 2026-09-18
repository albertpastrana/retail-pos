# Live shared tickets between tills

Captured: 2026-09-11

Related: `tickets/todo/postgres-lan.md`, `tickets/todo/operator-docs.md`

## Goal

Till-2 (scanner) fills a ticket that the central till can see while it is still open. Only central takes payment. One till edits a ticket at a time.

## Context

Today the active ticket lives only in memory on the till that edits it. The list reads `SHAREDTICKETS`; that table only has parked tickets. If the other till starts a ticket, central cannot see it.

Current cycle (`JTicketsBagShared`): create → not saved; park (new ticket, change seller, leave sales) → `INSERT`; open from the list → delete the row and return to memory. `updateSharedTicket` exists and is unused. On opening sales, `selectValidTicket` takes the first parked ticket: with two tills on the same Postgres, one can steal the other’s.

Workaround with no code: on till-2, New ticket (or leave sales) parks; central’s list shows it and can charge. Needs discipline; not visible while they fill it.

**Decision:** persist live + list all open tickets, satellite without payment. The till with a configured receipt printer is central and can take payment; tills without a receipt printer are scanning tills and cannot take payment. A ticket is edited by one till at a time; opening it takes the lock. No warning is required when another till takes the ticket: the original till drops the ticket when its ownership check notices the change.

The implementation keeps the live row until payment completes and uses the till receipt-printer configuration to distinguish the central till from scanning tills.

## Current implementation

The first slice is implemented:

- The first line creates a shared row and every ticket change updates it.
- The list shows the till that currently owns each live ticket.
- Opening a ticket transfers ownership; stale writes and payment attempts are rejected.
- Starting Sales creates an empty local ticket instead of taking the first shared ticket.
- Payment locks the row, and saving the receipt removes the shared row in the same database transaction.

The shop validation is complete. The shop does not need an alert when ownership changes; the ticket disappearing from the original till is acceptable.

### Intended flow

- Start: nobody grabs another till’s parked ticket; each till starts empty (or its own).
- Fill on till-2: first line saves to Postgres (locked to `till-2`). Central list: seller, `#…`, till-2. Each line updates the same ticket.
- Pay: customer goes to cash. Central opens the list and takes the ticket (its lock). The scanning till loses the ticket and returns to an empty screen. Payment only on the till with a receipt printer.
- After payment: gone from `SHAREDTICKETS` and from lists.
- If central does not take it: till-2 can park (new ticket / change seller); it stays on the list. Till-2 still cannot pay.
- Wrong till: till-2 cannot pay. If central opens a ticket till-2 is still filling, till-2 loses it.
- Delete: the till that holds the lock removes it from the list. Central should not delete one till-2 still owns without taking it first.
- Two tickets at once: each till its own; central’s list shows both (and parked ones). Opening one parks the one central had; it is not lost.

## Done when

A line scanned on a till without a receipt printer appears on the central till’s list without parking. Only the till with a receipt printer can take payment. Opening a ticket on one till stops the other from editing it; the other till may simply lose the ticket without an alert.

## Shipped

Live shared tickets persist from the first line, transfer ownership between tills, reject stale writes, and remove the shared row transactionally after payment. Payment is available only on tills whose `machine.printer` is configured as a real receipt printer. Main paths: `src-pos/com/openbravo/pos/sales/shared/JTicketsBagShared.java` and `src-pos/com/openbravo/pos/sales/DataLogicReceipts.java`.
