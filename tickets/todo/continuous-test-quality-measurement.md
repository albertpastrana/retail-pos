# Measure test quality continuously

Captured: 2026-10-30

Related: `tickets/in-progress/improve-testing-foundation.md`

## Goal

Measure production coverage and test health continuously without imposing arbitrary global targets before critical paths are covered.

## Context

The project currently has no JaCoCo report, package-specific threshold, mutation testing, suite-duration trend, or flaky-test signal.

## Done when

- JaCoCo publishes coverage reports for the relevant test lanes.
- Initial thresholds are package-specific, documented, and based on measured coverage.
- Threshold increases are tied to added critical-path coverage rather than a global arbitrary target.
- Suite duration and flaky tests are visible in CI.
- Mutation testing is evaluated for monetary calculations, tax rules, discounts, and inventory changes before being enabled broadly.
