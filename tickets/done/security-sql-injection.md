# Prevent SQL injection in product lookup

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Make barcode and lookup input unable to alter SQL statements while preserving
normal product search behaviour.

## Context

The originally reported `BFrame` product-label lookup was removed with the
product-label feature before this ticket started. The remaining product
barcode lookup is `DataLogicSales.getProductInfoByCode`, which already uses
parameterized exact-match SQL. The unused `SQLQueryer` legacy helper still
accepted and executed arbitrary SQL text, so it is removed rather than kept as
a future injection sink.

## Decision

Keep barcode lookup exact-match semantics, including the stored, zero-padded
barcode forms. Do not introduce regex matching or SQL construction from user
input.

## Done when

- The lookup uses parameterized SQL and handles malformed or regex-special input safely.
- Regression tests cover normal, malformed, and injection-shaped input.

## Shipped

- Removed the unused SQL-executing `SQLQueryer` helper.
- Added Derby integration coverage for normal, alternate, malformed, regex-special, and injection-shaped barcode input.
