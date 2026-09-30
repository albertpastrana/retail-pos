# Reset database baseline

Captured: 2026-09-25

## Goal

Replace the historical Flyway migration chain with a single new `V1` baseline that represents the current application database state.

Fresh installations must build the current schema from `V1`. The one controlled production installation must be adopted at version `1` without losing any business data. The resulting application package must no longer need historical migration fixtures such as `Menu.Root.txt` or `Ticket.Buttons.xml`.

## Context

This project currently contains migrations `V1` through `V46`. Several old migrations load resources that are no longer used by the application:

- `Menu.Root.txt` is no longer the active menu definition. The menu is built in `JPrincipalApp` and `V42` removes `Menu.Root` from `RESOURCES`.
- `Ticket.Buttons.xml` is no longer used by the sales screen. `JPanelTicket` creates the application-owned controls directly and `V35` removes `Ticket.Buttons` from `RESOURCES`.
- `Role.*.xml` files contain permissions rather than menu definitions. Their effective permissions must still be represented in the new baseline.
- Receipt templates and other intentionally configurable resources must remain database-backed and must still be seeded where the current application expects them.

The only production installation is controlled by the project team, and its current schema version is known. This permits a deliberate one-time Flyway history transition instead of preserving upgrade support for every historical database version.

The new strategy is not a compatibility migration from every old version. It is a schema snapshot for new installations plus a controlled adoption procedure for the current production database. The migration version is intentionally reset to `V1` because the old migration history is being discarded for the only controlled installation.

## Design

### Migration layout

- Replace the historical migrations with `src-pos/db/migration/V1__current_baseline.sql` or an equivalent Java migration containing the complete current schema and required initial data.
- Remove the old `V1` through `V46` migration implementations from the source tree after the new baseline has been verified.
- Leave the new `V1` as the permanent migration for new installations; it must not be removed in a later cleanup.
- Update `migration-checksums.sha256` so it contains exactly the versioned migration files that remain.
- Keep the migration location as `classpath:db/migration` unless the implementation demonstrates a concrete need to change it.

The baseline must be produced from the current application schema and migration behaviour, not from a copy of production business data. It must include the final schema changes currently supplied by `V1` through `V46`, including tables, columns, constraints, indexes, seed rows, role permissions, and required configurable resources.

It must not seed obsolete application-owned resources:

- `Menu.Root`;
- `Ticket.Buttons`.

It must retain resources that are still runtime dependencies, including receipt layouts, logos, payment scripts, and other operator-customizable templates after each one has been confirmed against current code and configuration behaviour.

### Flyway startup configuration

Change `DatabaseMigrator` to use `baselineVersion("1")` with `baselineOnMigrate(true)`.

Expected behaviour:

- An empty database has no tables, so Flyway executes the new `V1` and creates the current schema.
- The controlled production database is non-empty and has its old Flyway history reset during the one-time transition, so Flyway records baseline version `1` without executing the new `V1` over live data.
- A second application startup is a no-op.

Do not rely on `CREATE IF NOT EXISTS` as a substitute for the production adoption procedure. The production database is already at the target schema and should be baselined, not rebuilt in place.

### Production adoption

The exact operational procedure must be written and tested before changing production:

1. Record the current production database engine, schema state, Flyway history, application version, and backup location.
2. Take and verify a restorable backup.
3. Restore that backup into an isolated test database.
4. Apply the proposed Flyway history transition to the restored copy while preserving all business tables and data.
5. Start the application against the restored copy and verify normal operation.
6. Repeat the same controlled history transition in production during an agreed maintenance window.
7. Start the new application and verify that Flyway records version `1` without running the baseline SQL against existing data.

The transition must not use `clean`, drop business tables, or recreate the production schema. The implementation must document how `flyway_schema_history` is reset or replaced and how the operation can be rolled back using the verified backup.

### Historical backups and unsupported states

After this change, databases whose history still contains the old `V1` through `V46` chain are not automatically supported by the new package. Document this explicitly.

Before opening an old backup with the new application, restore it to an isolated database and perform the same schema/history adoption only after confirming that its schema matches the current production state. Databases from an earlier schema state require a separate recovery plan and are outside this ticket unless explicitly added to the scope.

## Scope

Included:

- Consolidating the current schema and seed behaviour into the new `V1`.
- Removing the historical migration source files after verification.
- Removing migration-only menu resource fixtures that no remaining code packages or reads.
- Updating Flyway configuration and the checksum manifest.
- Updating migration integration tests and operational documentation.
- Verifying the controlled production adoption procedure on a restored production backup.

Excluded:

- Changing the current application menu or sales-screen button behaviour.
- Removing receipt templates, role permissions, or other resources still used at runtime.
- Supporting arbitrary databases from historical intermediate migration versions.
- Copying production customers, products, tickets, payments, or other business data into the baseline.

## Verification

Automated checks must cover:

- Fresh Derby installation executes only the new `V1` and reaches the current schema.
- Fresh MySQL installation executes only the new `V1` and reaches the current schema.
- Fresh PostgreSQL installation executes only the new `V1` and reaches the current schema.
- The restored current production database can be adopted at version `1` without loss of business data.
- A second migration run is idempotent.
- `Menu.Root` and `Ticket.Buttons` are absent from `RESOURCES` after a fresh installation and after production adoption.
- Required receipt resources and role permissions are present and representative content can be queried.
- Normal application startup, login, permissions, sales, receipt preview/reprint, cash closing, drawer, and resource editing flows still work.
- The migration checksum script passes and no deleted migration is unexpectedly required by the build.

Required commands before completion:

```sh
docker compose up -d --wait
./gradlew spotlessApply
./gradlew ciCheck
docker compose down -v
```

Also run the narrow migration integration tests and verify the production-backup rehearsal before changing the live installation.

## Done when

- The source tree contains only the new current-state `V1` migration for Flyway versioned migrations.
- A fresh supported database is fully usable after executing the new `V1`.
- The controlled production database is safely adopted at version `1` with all business data preserved.
- Flyway does not attempt to replay or validate the removed historical migration chain in the supported deployment.
- `Menu.Root.txt` and `Ticket.Buttons.xml` are no longer required by the packaged application or migration path.
- Runtime-customizable resources and role permissions continue to work.
- Derby, MySQL, and PostgreSQL checks pass.
- The backup, rehearsal, rollout, rollback, and historical-backup limitations are documented.
- No production change is made until the restored-backup rehearsal is successful.
