# Update Derby driver configuration

Captured: 2026-10-01

## Goal

Allow the Derby 10.17 dependency update to run database migrations successfully.

## Context

Derby 10.17 no longer ships `org.apache.derby.jdbc.EmbeddedDriver`. Flyway 9
still attempts that class when it auto-detects a Derby JDBC URL, so the
migrator must select Derby's available driver explicitly.

## Done when

- The Derby migration integration test passes with Derby 10.17.1.0.
- MySQL and PostgreSQL migration configuration is unchanged.

## Shipped

Configured Flyway to use Derby 10.17's `AutoloadedDriver` through its
`DriverDataSource` while preserving automatic driver selection for other
databases. Updated `DatabaseMigrator`.
