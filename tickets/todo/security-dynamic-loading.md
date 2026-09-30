# Restrict dynamic class loading

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Restrict configured class loading without breaking supported database, hardware,
payment, or restart flows.

## Context

`AppViewConnection`, `JRootApp`, and `BeanFactoryData` use dynamic loading.
`ProductsEditor` private-method reflection is tracked separately in
`tickets/todo/security-products-editor-reflection.md`.

## Done when

- Supported dynamic classes and locations are explicitly restricted.
- Supported configuration-driven integrations remain covered by tests.
