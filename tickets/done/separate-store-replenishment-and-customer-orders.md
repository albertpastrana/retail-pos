# Separate store replenishment and customer orders

Captured: 2026-10-28

## Goal

The same product can have one open store replenishment and any number of open
customer orders at the same time.

## Context

Open entries are currently unique by product only. Adding a customer order for a
product that already has a store replenishment updates the existing entry,
which loses one of the two requests. Customer orders must never be deduplicated
by product. Store replenishment remains unique by product and must show a clear
warning when it is requested again.

Existing open entries must be migrated without changing their status, product,
customer, notes, or history.

## Done when

- A product can have one open store replenishment and multiple open customer
  orders.
- Adding the same product as a store replenishment again shows a warning and
  does not change the existing entry.
- Adding the same product as a customer order creates another entry.
- Existing open and closed entries remain readable after migration.
- Derby, MySQL, and PostgreSQL migration checks pass.
- A regression test covers the two request types and same-type deduplication.
