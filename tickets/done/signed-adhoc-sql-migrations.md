# Signed ad-hoc SQL migrations

Captured: 2026-10-28

## Goal

Allow a trusted operator to deliver a signed SQL migration to the existing
local startup import directory and have the POS apply it safely to that till's
database.

## Context

The existing `catalog.import.directory` and `catalog.import.publicKey`
configuration is shared with signed product and fallback catalog imports. SQL
files use the `migration-*.sql` prefix and the same detached Ed25519 signature.
The SQL importer must remain separate from the catalog importer while sharing
the directory, signature verification, and processed/rejected archive flow.

SQL migrations run after Flyway migrations, one file per transaction. A
successful migration is recorded in a dedicated database table using its
identifier and checksum so it cannot be applied twice. A failed migration is
rolled back as a whole and rejected. The implementation must account for
Derby, MySQL, and PostgreSQL compatibility and must not expose unsigned or
arbitrary interactive SQL execution.

The README documents the file format, signing, processing behaviour,
database compatibility, backup guidance, and operational restrictions.

## Done when

- The existing local import directory accepts signed `migration-*.sql` files.
- Unsigned, invalidly signed, malformed, and unsupported files are rejected.
- Each SQL file runs only after regular Flyway migrations and in one transaction.
- A failed file leaves no partial database changes and is archived in `rejected/`.
- A successful file is recorded with an identifier and checksum and archived in `processed/`.
- Re-presenting an applied file does not execute it again.
- Integration coverage verifies fresh, existing, and repeated database scenarios for Derby, MySQL, and PostgreSQL.
- The README explains configuration, signing, execution order, rollback, archives, compatibility, backup guidance, and restrictions.

## Shipped

Added `AdHocSqlMigrationImporter`, the `AD_HOC_MIGRATIONS` history table, startup integration, integration coverage, and README operating instructions. Signed `migration-*.sql` files now run transactionally after Flyway and are archived after success or failure.
