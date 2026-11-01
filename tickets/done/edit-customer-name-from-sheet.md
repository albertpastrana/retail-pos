# Edit customer name from customer sheet

Captured: 2026-11-01

Related: `tickets/done/customer-on-ticket.md`

## Goal

Allow a cashier to edit and save a customer's name from the customer sheet
opened on the current ticket.

## Context

The customer sheet previously displayed the name as read-only. Its existing
`Change` action changes which customer is attached to the ticket; it keeps that
meaning. The backoffice customer editor already supports editing the name.
This change is limited to the name action in the customer sheet and does not
change customer assignment or debt behaviour.

## Done when

A cashier can open a customer's sheet, edit the name, save it, and see the new
name on the sheet and current ticket. Cancelling or entering an empty name
does not change the stored name. The existing `Change` and `Remove` actions
keep their current behaviour.

## Shipped

Added a localized `Edit name` action to `CustomerSheet`. It validates and
persists the trimmed name through `DataLogicCustomers`, updates the open sheet
and ticket customer object, and leaves assignment and removal actions intact.
