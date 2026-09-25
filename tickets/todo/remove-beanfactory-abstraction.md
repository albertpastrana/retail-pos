# Remove BeanFactory abstraction

Captured: 2026-09-25

Related: `tickets/todo/audit-unused-code-and-dependencies.md`

## Goal

Remove the legacy `BeanFactory` and `BeanFactoryApp` abstraction from bean
creation and replace it with explicit, type-safe construction and lifecycle
handling.

## Context

`JRootApp` currently supports two bean creation contracts: classes implementing
`BeanFactory` and ordinary classes constructed with an `AppView` constructor.
`BeanFactoryApp` also provides application-aware initialization, while the
registry provides caching through `m_aBeanFactories`.

The replacement must preserve lazy creation, caching, initialization failures,
database-specific factories such as `BeanFactoryData`, and beans that require
`AppView`. Review `BeanFactory`, `BeanFactoryApp`, `BeanFactoryObj`,
`BeanFactoryCache`, `BeanFactoryData`, and `BeanFactoryDataSingle` before
removing the abstraction.

Do not remove database-specific bean selection or change bean lifecycle as part
of a mechanical interface deletion.

## Done when

- Application code no longer depends on `BeanFactory` or `BeanFactoryApp`.
- Database-specific bean implementations still resolve correctly for Derby,
  MySQL, and PostgreSQL.
- Beans are created lazily and cached with the same observable lifecycle.
- Application-aware initialization and initialization failures remain covered.
- Beans requiring `AppView` continue to be constructed correctly.
- Obsolete adapters and factory classes are removed only after all callers and
  runtime configuration paths are migrated.
- Relevant unit and integration tests pass.
