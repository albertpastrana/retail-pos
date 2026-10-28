# Ignore empty fallback families

Captured: 2026-10-28

## Goal

A fallback product whose `FAMILY` value is empty is imported as a single
product, not as a family containing every fallback row with an empty family.

## Context

The fallback family lookup currently rejects `NULL` but accepts blank family
values. The development database has 37 rows with an empty family, so scanning
one of them can load unrelated products and fail on duplicate references.

This ticket changes the lookup guard only. Existing fallback reference data is
not changed automatically; a separate SQL correction will be provided for
review before it is run against a database.

## Done when

- `NULL` and blank `FAMILY` values return no family variants.
- A non-blank family still returns all of its variants.
- Automated coverage verifies both behaviours.
- No database is modified by the change.

## Shipped

`DataLogicSales.getCatalogProductFamily` now ignores null and blank family
keys. `DatabaseMigratorIT` covers the blank-family case. No database changes
were executed.
