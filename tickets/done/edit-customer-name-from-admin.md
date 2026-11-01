# Edit customer name from administration

Captured: 2026-11-01

Related: `tickets/done/customer-on-ticket.md`

## Goal

Keep customer name editing in the administration customer editor, not in the
customer assignment sheet on the sales ticket.

## Context

The administration screen already presents the selected customer's name as an
editable field. The ticket customer sheet is for viewing debt and changing or
removing the customer assigned to the ticket; it must not provide customer
record editing actions.

## Done when

In **Administration → Customers**, selecting a customer enables the **Name**
field and **Save** persists a changed non-empty name. The ticket customer sheet
does not show a name-edit action, and its existing change/remove actions keep
their meanings.

## Shipped

Removed the misplaced ticket-sheet name editor and documented the
administration editor as the supported place for changing customer names.
