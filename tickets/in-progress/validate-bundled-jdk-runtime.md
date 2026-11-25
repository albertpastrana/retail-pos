# Validate bundled JDK runtime

Captured: 2026-11-23

## Goal

Ensure portable packages and manually extracted installations always have an
executable `jspawnhelper`, and never silently fall back to a system Java
runtime when the bundled runtime is broken.

## Context

`jspawnhelper` must be executable for Java `ProcessBuilder` to launch commands
such as `pg_dump`. Packaging already repairs and checks the helper before
creating the archive, but the updater does not validate the extracted runtime.
The launcher also fell back to `java` from `PATH`, which could hide a broken
bundled runtime and select a different JDK after an update or restart. Some
manual extraction tools or filesystems can also lose executable permission
bits even when the archive contains them correctly.

## Done when

- The updater validates and restores the executable permission on the bundled
  `runtime/lib/jspawnhelper` before replacing the installed application.
- The launcher fails clearly when a bundled runtime exists but its Java binary
  is not executable, instead of using system Java.
- The launcher restores and validates `jspawnhelper` before starting a bundled
  runtime, covering manual package extraction.
- Existing package and update flows remain unchanged when the runtime is valid.
