---
name: migration-review
description: Design and verify safe Flyway migrations for Derby, MySQL, and PostgreSQL in this project.
---

# Migration review

Use this skill for schema changes, persisted data changes, migration failures,
checksums, or database compatibility questions.

## Procedure

1. Read `AGENTS.md`, the migration directory, the migrator, checksum manifest,
   and existing integration tests before writing SQL or Java.
2. Never edit a committed or released migration. Add the next version under
   `src-pos/db/migration/`.
3. Keep schema and required data changes in the migration. Do not load local
   TSV files, backups, or manually prepared databases from migration code.
4. Use existing Flyway placeholders or explicit migrator branches for database-
   specific syntax. Check nullability, indexes, constraints, quoting, boolean
   values, generated IDs, and parameter types on all three engines.
5. Update `migration-checksums.sha256` and add representative coverage for a
   fresh database, an existing database when relevant, and a second run.
6. Assert the resulting schema and representative data, not the total number of
   Flyway history rows.

## Required verification

```sh
docker compose up -d --wait
./gradlew ciCheck
docker compose down -v
```

Leave containers stopped unless the user explicitly asks otherwise. Never use a
real shop database as a disposable test database.
