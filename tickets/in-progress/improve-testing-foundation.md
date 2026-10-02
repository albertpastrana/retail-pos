# Improve testing foundation and critical-path coverage

Captured: 2026-09-20

## Goal

Build a reliable, measurable testing strategy that protects the POS's money, stock, database, and hardware-facing behaviour while keeping local and pull-request feedback fast.

## Context

The repository currently has 19 test classes and approximately 73 JUnit 4 tests, all under `src/integrationTest/java`. The suite gives useful regression coverage to recent work such as Flyway migrations, shared tickets, loyalty, discounts, catalogue variants, price rules, backups, permissions, and selected reports.

There is no separate unit-test source set, code-coverage report, mutation testing, or coverage threshold. As a result, fast calculations and database integrations share the same test lane, and the actual production coverage is not measured.

The highest-risk gaps are the core shop flows:

- Complete sales from ticket creation through payment, change, persistence, and stock updates.
- Cash, card, mixed payments, refunds, voids, and cash closing.
- Tax calculations and persistence across Derby, MySQL, and PostgreSQL.
- Product, stock, customer, and sales persistence beyond migration-only checks.
- Receipt generation, preview, reprint, drawer, printer, barcode scanner, scale, and other configured peripherals.
- Critical Swing workflows, permissions as observed in the UI, and EDT/threading behaviour.
- Invalid input, malformed TSV files, duplicate data, missing resources, connection failures, and rollback paths.

Current CI runs `./gradlew ciCheck` after starting MySQL and PostgreSQL, so every pull request depends on Docker, fixed local ports, and external database startup. It does not publish Gradle test reports. The release workflow now runs the same integration gate before packaging, but still needs the broader test-lane and reporting work described below.

Relevant current configuration:

- `build.gradle.kts:32-49` defines only the main, data-helper, and `integrationTest` source sets.
- `build.gradle.kts:181-199` defines the external database integration task and `ciCheck`.
- `.github/workflows/build.yml` starts both external databases for the check job.
- `.github/workflows/release.yml` packages releases using `check` without `ciCheck`.

Do not attempt to automate every Swing interaction or raise global coverage targets before the critical business paths have representative tests. Prefer tests at service/repository boundaries, with a small number of stable end-to-end tests for the most important till workflows.

## Slices

1. **Separate the test lanes.** Introduce a fast unit-test lane, a local Derby integration lane, and an external-database lane for MySQL/PostgreSQL. Classify the existing tests without weakening their assertions or duplicating expensive setup unnecessarily.
2. **Stabilise the test infrastructure.** Resolve fixtures from `projectDir` or the classpath, isolate tests that mutate `Locale` or global formatting state, add explicit database timeouts and diagnostics, and publish JUnit/Gradle reports from CI.
3. **Protect the money and stock paths.** Add representative tests for ticket creation, taxes, line and total discounts, cash/card/mixed payments, change, refunds, voids, cash closing, sale persistence, stock mutation, and transaction rollback.
4. **Expand database compatibility coverage.** Test critical queries and representative existing data on Derby, MySQL, and PostgreSQL. Cover empty databases, existing databases, malformed or duplicate data, constraints, indexes, and running migrations twice.
5. **Strengthen assertions and fixtures.** Replace fragile `contains` checks with parsed XML or structured assertions where practical. Verify backup contents can be restored. Add builders or factories for recurring tickets, products, users, payments, and database fixtures.
6. **Cover critical UI and hardware boundaries.** Add focused tests for permission-driven UI behaviour, receipt rendering, preview/reprint, drawer commands, printer/scanner/scale adapters, and error handling using fakes rather than physical devices.
7. **Measure quality continuously.** Add JaCoCo with initial package-specific thresholds, then increase them as critical paths are covered. Consider mutation testing for monetary calculations, tax rules, discounts, and inventory changes. Track suite duration and flaky tests.
8. **Align release guarantees.** Make the release workflow run the same relevant integration guarantee as pull requests, or document and enforce an equivalent release gate before publishing packages.

The first release-gate slice is implemented: the tag-triggered release workflow
runs `ciCheck` with clean MySQL and PostgreSQL containers before any platform
package job can start. Releases use GitHub-generated notes from the tag and do
not require a versioned release-notes file.

## Verification

Run the fast lane independently from Docker and confirm it provides useful feedback without external services.

Run the complete database verification with:

```sh
docker compose up -d --wait
./gradlew ciCheck
docker compose down -v
```

Also verify:

- JUnit and coverage reports are generated and uploaded by CI.
- A failed test leaves enough logs to identify the database, test class, and failure cause.
- Critical money and stock scenarios pass on all supported database engines.
- Migration tests cover empty, existing, and repeat-run databases.
- Backup tests create and restore representative data.
- Fixtures work regardless of the caller's working directory.
- Tests that alter global locale or formatting state are isolated and safe from parallel execution.
- The release workflow cannot publish a package without the agreed integration gate.

## Done when

- Fast unit tests, Derby integration tests, and external database tests are separate and documented.
- The pull-request pipeline runs a fast lane before the slower external-database lane and publishes test reports.
- Critical sale, payment, tax, stock, rollback, and cash-closing behaviours have automated regression coverage.
- Critical persistence and migration paths are verified on Derby, MySQL, and PostgreSQL, including existing databases and repeat migrations.
- Fixtures and assertions are deterministic, structurally meaningful, and independent of the caller's working directory.
- Receipt, preview/reprint, permissions, and configured hardware boundaries have focused automated coverage using test doubles where physical devices are unnecessary.
- JaCoCo reports coverage and enforces agreed initial thresholds for critical packages; any threshold increase is based on measured progress.
- CI and release checks expose failures with actionable reports and do not rely on undocumented local state.
- The complete verification commands pass, including `./gradlew ciCheck` with clean database containers.
