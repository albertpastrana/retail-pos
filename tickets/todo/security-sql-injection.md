# Prevent SQL injection in product lookup

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Make barcode and lookup input unable to alter SQL statements while preserving
normal product search behaviour.

## Context

`BFrame` concatenates barcode input into SQL executed by `SQLQueryer`.

## Done when

- The lookup uses parameterized SQL and handles malformed or regex-special input safely.
- Regression tests cover normal, malformed, and injection-shaped input.
