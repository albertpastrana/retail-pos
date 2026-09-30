# Remove ProductsEditor private reflection

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Replace the private-method reflection in `ProductsEditor` with an explicit,
typed API.

## Context

`ProductsEditor` currently accesses a private method through
`setAccessible(true)`. This work is separate from restrictions on configured
class loading.

## Done when

- `ProductsEditor` no longer uses private reflection.
- Product editing behaviour and its relevant tests remain unchanged.
