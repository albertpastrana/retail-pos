# Improve testing foundation

Captured: 2026-09-20

Related:

- `tickets/todo/critical-sale-and-stock-coverage.md`
- `tickets/todo/database-compatibility-coverage.md`
- `tickets/todo/ui-and-hardware-test-boundaries.md`
- `tickets/todo/continuous-test-quality-measurement.md`

## Goal

Build a reliable testing foundation with separate, diagnosable lanes that keep local and pull-request feedback fast.

## Context

The repository currently has approximately 71 test classes and 245 JUnit Jupiter tests. They are all compiled from `src/integrationTest/java`, although many are fast unit or Derby-only tests. Existing coverage includes Flyway migrations, shared tickets, loyalty, discounts, catalogue variants, price rules, backups, permissions, reports, taxes, payments, cash closing, and stock workflows.

There is no separate fast unit-test lane, Derby integration lane, or external-database lane. `ciCheck` runs the complete `integrationTest` task after starting MySQL and PostgreSQL. The default Gradle test source set is empty. CI does not upload JUnit or Gradle test reports.

Several tests use caller-relative files or write generated screenshots under `build/`. Multiple UI and formatting tests change locale or other global state. These behaviours need explicit isolation before parallel execution or reliable diagnosis can be introduced.

The release workflow already runs `ciCheck` with clean MySQL and PostgreSQL containers before packaging. That release gate is retained and is not part of this foundation slice.

Relevant current configuration:

- `build.gradle.kts:32-49` defines only the main, data-helper, and `integrationTest` source sets.
- `build.gradle.kts:181-199` defines the external database integration task and `ciCheck`.
- `.github/workflows/build.yml` starts both external databases for the check job.
- `.github/workflows/release.yml` runs `ciCheck` in a required integration job before the platform package jobs, which still run `check` for their platform-specific build.

Do not attempt to automate every Swing interaction or raise global coverage targets before the critical business paths have representative tests. Prefer tests at service/repository boundaries, with a small number of stable end-to-end tests for the most important till workflows.

## Slices

1. **Inventory and classify the current suite.** Record the real test counts and classify tests as fast, Derby, or external database without weakening assertions or duplicating expensive setup.
2. **Separate the test lanes.** Introduce independently runnable Gradle tasks for fast tests, local Derby integration, and MySQL/PostgreSQL integration. Prefer tags or source sets based on the smallest change that gives reliable selection.
3. **Stabilise test infrastructure.** Resolve fixtures from `projectDir`, classpath, or `@TempDir`; isolate tests that mutate locale or global formatting state; and avoid caller-relative generated output.
4. **Improve failure diagnosis.** Add explicit external-database connection and test timeouts, include the database engine and test task in diagnostics, and retain useful Gradle/JUnit reports on CI failures.
5. **Publish the lanes in CI.** Run the fast lane before Docker-dependent checks, run the slower lanes explicitly, upload JUnit and Gradle reports with `if: always()`, and keep database logs on failure.

The first release-gate slice is implemented: the tag-triggered release workflow
runs `ciCheck` with clean MySQL and PostgreSQL containers before any platform
package job can start. Releases use GitHub-generated notes from the tag and do
not require a versioned release-notes file.

## Decisions

- Keep the existing `integrationTest` source set so existing test fixtures and
  targeted commands remain compatible; define the three lanes as filtered,
  independently runnable Gradle tasks.
- Keep lane membership explicit in `build.gradle.kts`, including method-level
  selection for classes that contain both local and external database tests.
- Use Gradle-provided project and output directories for fixtures and generated
  screenshots rather than changing the tests' working directory.
- Run fast tests first in CI, then Derby and external database jobs, and upload
  each job's JUnit XML and Gradle HTML reports with `if: always()`.
- Keep the normal `check` job in pull-request CI and guard the explicit lane
  inventory with `verifyTestLaneCoverage` before each lane runs.

## Verification

Run the fast lane independently from Docker and confirm it provides useful feedback without external services. Run each Derby and external-database lane independently and verify that selecting one lane does not execute another.

Run the complete database verification with:

```sh
docker compose up -d --wait
./gradlew ciCheck
docker compose down -v
```

Also verify:

- JUnit and Gradle test reports are generated and uploaded by CI.
- A failed test leaves enough logs to identify the database, test class, and failure cause.
- Fixtures work regardless of the caller's working directory.
- Tests that alter global locale or formatting state are isolated and safe from parallel execution.
- The release workflow cannot publish a package without the agreed integration gate.

## Done when

- The current suite is inventoried accurately and every test belongs to a documented lane.
- Fast tests, Derby integration tests, and external database tests are independently runnable and do not require unnecessary services.
- The pull-request pipeline runs the fast lane before Docker-dependent lanes and uploads JUnit/Gradle reports for every test job.
- Fixtures and generated output are deterministic and independent of the caller's working directory.
- Tests that alter locale or global formatting state cannot interfere with one another when the selected lane runs in parallel.
- External database failures expose the engine, connection context, test class, and database logs.
- The complete verification commands pass, including `./gradlew ciCheck` with clean database containers.

Critical business coverage, database compatibility, UI/hardware boundaries, and coverage metrics are tracked in the related follow-up tickets rather than being required to close this foundation slice.
