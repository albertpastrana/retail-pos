# Expand database compatibility coverage

Captured: 2026-10-30

Related: `tickets/in-progress/improve-testing-foundation.md`

## Goal

Verify critical persistence and migration behaviour consistently on Derby, MySQL, and PostgreSQL.

## Context

The project already exercises migrations and selected persistence paths, but coverage is uneven across engines and does not consistently verify representative existing data, constraints, indexes, and repeat runs.

## Done when

- Critical queries and persistence paths pass on Derby, MySQL, and PostgreSQL.
- Migration tests cover empty databases, representative existing databases, and a second migration run.
- Malformed or duplicate data, constraints, indexes, and rollback paths have explicit assertions.
- Database failures identify the engine, schema/setup stage, and representative failing operation.
- Backup tests prove that representative data can be restored, not only that an archive exists.
