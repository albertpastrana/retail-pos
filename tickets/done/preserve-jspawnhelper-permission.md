# Preserve jspawnhelper permission

Captured: 2026-10-24

## Goal

Ensure portable Java runtime packages preserve the executable permission required
by `jspawnhelper` to start child processes on Linux and macOS.

## Context

The packaged runtime on the Casa Victoria terminal contains
`runtime/lib/jspawnhelper` with mode `664`. Java `ProcessBuilder` therefore
fails to launch `pg_dump` with `posix_spawn failed, error: 13`.

The portable package script creates the runtime with `jlink` and currently only
repairs launcher permissions before archiving.

## Done when

- Packaging fails clearly if `runtime/lib/jspawnhelper` is missing.
- The packaged helper is executable before archive creation.
- A packaging check verifies the helper permission.
- Existing application and release behaviour is unchanged.
