# Support multi-term product search

Captured: 2026-10-04

## Goal

Make product name searches match every whitespace-separated term independently,
so a search such as `suje blan` finds names containing both `suje` and `blan`.

## Context

Sales and administration product filters currently use the shared QBF
`COMP_CONTAINS` comparator, but it treats the whole query as one substring.
The administration welcome product lookup uses a separate SQL query. Both
paths should use the same multi-term matching behaviour.

## Done when

- Sales product search matches every whitespace-separated name term.
- Administration product search matches every whitespace-separated name term.
- The administration welcome product lookup follows the same rule.
- Single-term searches, empty searches, barcode lookups, and existing selection
  behaviour remain unchanged.
- Automated tests cover the shared behaviour and the welcome lookup query.

## Shipped

Implemented shared whitespace-separated term matching in `SearchTerms` and the
QBF `COMP_CONTAINS` comparator. The stock welcome product lookup now uses the
same matching rule. Added integration coverage for the shared comparator and
the administration lookup.

Verification: `./gradlew spotlessApply`, focused integration tests, and
`spotlessCheck` pass. Full `ciCheck` was blocked locally because PostgreSQL was
not running for `DatabaseMigratorIT`.
