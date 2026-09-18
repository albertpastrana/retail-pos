# Agent instructions

## Database migrations

- Never edit a migration that has been committed or released. Add the next
  versioned migration under `src-pos/db/migration/` instead.
- A migration must be safe for Derby, MySQL, and PostgreSQL. Keep
  database-specific SQL behind the existing Flyway placeholders or explicit
  database branches in the migrator.
- Keep schema changes and data changes in the migration. Do not depend on
  local files, TSV imports, or a manually prepared database.
- Update `migration-checksums.sha256` whenever a new migration is added.
- Add or update integration coverage for an empty database, an existing
  database when the migration path requires it, and running the migration a
  second time.
- Do not assert the total number of Flyway history rows. Assert that the
  migrated schema and representative data can be queried instead.

Before finishing migration work, run:

```sh
docker compose up -d --wait
./gradlew ciCheck
docker compose down -v
```

Leave the database containers running only when explicitly requested.
