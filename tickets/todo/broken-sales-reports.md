# Fix productsaletotals and soldproducts reports

Captured: 2026-09-14

Related: `tickets/todo/operator-docs.md`

## Goal

`productsaletotals.bs` and `soldproducts.bs` run on Derby and PostgreSQL, not only MySQL.

## Context

They select columns that are not in the `GROUP BY` (`TAXES.RATE`, `CUSTOMERS.NAME`). They fail on Derby and PostgreSQL. They are excluded from `ReportStatementsIT` until someone fixes them.

Operator docs should only mention reports the shop actually uses; these two are broken.

## Done when

Both reports execute on Derby and PostgreSQL, and `ReportStatementsIT` includes them.
