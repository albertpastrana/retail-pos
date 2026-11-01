# Live customer finder search

Captured: 2026-11-01

## Goal

The customer finder searches while the cashier types, without requiring the
Enter key or a separate search-button click.

## Context

Use the existing Swing debounced-search pattern so typing does not issue a
database query for every keystroke. Remove the redundant search button and
keep Enter as an immediate search action for keyboard and scanner workflows.

## Done when

- Typing in the customer finder refreshes results after a short pause.
- Clearing or changing the query also refreshes results after the same debounce.
- Enter still searches immediately.
- Existing customer selection and create/remove flows are unchanged.

## Shipped

Added a 300 ms non-repeating Swing timer and document listener to
`JCustomerFinder`, removed its redundant search button, and added a localized
placeholder to the search field. Manual visual verification of the customer
finder was not performed in this session.
