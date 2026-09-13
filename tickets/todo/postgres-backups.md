# Database backups from Maintenance

Captured: 2026-09-11

Related: `tickets/todo/postgres-lan.md`

## Goal

The app (especially on Linux) can `pg_dump` into `backup.dir`. Staff copy that file to USB or another PC themselves.

## Context

Not inside Postgres. Button in Maintenance; optional on close cash or on a schedule.

The TSV catalogue is not an app-generated backup.

## Done when

A user on the Linux till can produce a dump file without running `pg_dump` by hand, and the path is configured.
