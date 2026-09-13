# No products at €0

Captured: 2026-09-10

Related: `tickets/todo/operator-docs.md`

## Goal

There are no sellable products priced at 0 € (or staff have a health check that lists them so they can be fixed).

## Context

Import can still create a row with sell price 0 while waiting for a real price. This is a shop-data check, not a tutorial in the operator docs.

## Done when

Either the catalogue is clean, or the till/backoffice can list products at 0 € so staff can correct them.
