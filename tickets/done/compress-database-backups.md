# Compress database backups

Captured: 2026-10-01

## Goal

Database backups created by the POS should use gzip compression to reduce their
storage footprint.

## Context

PostgreSQL and MySQL currently create plain SQL files. Derby creates a backup
directory using Derby's physical backup procedure. Keep the existing database-
specific backup and restore semantics: SQL backups become `.sql.gz`, while the
Derby directory becomes a gzip-compressed tar archive.

Do not compress database traffic or add a new external backup service.

## Done when

- PostgreSQL backups are written as valid `.sql.gz` files.
- MySQL backups are written as valid `.sql.gz` files.
- Derby backups are written as valid `.tar.gz` archives.
- Daily backup detection recognises the compressed files.
- Integration coverage verifies the compressed output and existing backup flow.

## Shipped

- PostgreSQL and MySQL SQL dumps are compressed with gzip and Derby backups are
  archived as gzip-compressed tar files.
- Added compressed Derby backup integration coverage.
