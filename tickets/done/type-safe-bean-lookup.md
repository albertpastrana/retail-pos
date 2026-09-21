# Introduce type-safe bean lookup

Captured: 2026-09-18

## Goal

Allow application code to retrieve beans without fully qualified class-name strings or manual casts.

## Context

Current call sites use patterns such as `(DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales")`. `JRootApp` resolves these names with reflection and caches the resulting bean factories.

Add a generic `getBean(Class<T>)` API to `AppView` and implement it in `JRootApp`. Migrate existing call sites to usage such as `app.getBean(DataLogicSales.class)`, while preserving the current cache and bean lifecycle.

Keep the string-based API temporarily for scripts, plugins, and legacy integrations. Removing reflection from the core bean registry and introducing explicit providers or constructor injection are separate follow-up work.

## Done when

- Migrated call sites no longer use fully qualified bean names as strings.
- Migrated call sites no longer require manual casts.
- Bean instances continue to be cached and initialized as they are today.
- Script-based and legacy string-based bean loading remains functional.
- Invalid typed lookups fail with a clear exception.
- Tests cover typed lookup, caching, initialization failures, and invalid types.
- The project builds successfully and the relevant test suite passes.

## Shipped

- Added generic `AppView.getBean(Class<T>)` lookup while retaining the string-based API for legacy and script callers.
- Migrated application call sites away from fully qualified bean-name strings and manual casts.
- Preserved bean caching and lifecycle behavior, with typed lookup tests covering caching and invalid lookups.
- Shipped in commit `3dc14d6`.
