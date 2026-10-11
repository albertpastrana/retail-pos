# Make Windows application updates restart reliably

Captured: 2026-10-11

Related: `tickets/done/application-updates.md`

## Goal

A verified local Windows package replaces the portable application and the POS starts again with the new version, or the operator gets a diagnosable failure without losing the current installation.

## Context

The current flow launches `update.bat` through `cmd /c start`, waits indefinitely for the JVM PID, replaces the application directory, and launches `start.bat` through another detached `start` command. It does not verify that the replacement launcher or new application process starts successfully.

The fix must preserve external configuration and databases, keep rollback behaviour, and remain compatible with portable Windows packages. Linux and macOS updater behaviour are out of scope.

## Done when

- A Windows update cannot wait forever for the old process; the updater records a clear failure and leaves the existing installation recoverable.
- A successful replacement launches the new `start.bat` from a stable working directory and records whether the launcher started successfully.
- Replacement or restart failures are visible in the update log and restore the previous installation when appropriate.
- The updater arguments and paths remain correct for normal Windows paths containing spaces.
- Automated tests cover the Java-side updater command construction and relevant update flow behaviour.
