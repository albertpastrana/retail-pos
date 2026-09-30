# Restrict dynamic class loading

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Restrict configured class loading and remove unnecessary private reflection
without breaking supported database, hardware, payment, or restart flows.

## Context

`AppViewConnection`, `JRootApp`, `BeanFactoryData`, and `ProductsEditor` use
dynamic loading or private-method reflection.

## Done when

- Supported dynamic classes and locations are explicitly restricted.
- `ProductsEditor` no longer uses private reflection.
- Supported configuration-driven integrations remain covered by tests.
