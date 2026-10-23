# Improve safe database operation logging

Captured: 2026-10-23

## Goal

Make database operation failures diagnosable without recording SQL text or
parameter values in application logs.

## Context

`StaticSentence` and `PreparedSentence` currently log complete SQL statements
at `INFO`. This conflicts with the application logging policy and can expose
business data or values embedded in dynamically built statements. The shared
database boundary is the right place to emit one safe event for failures,
without adding logs to individual screens.

Keep successful database operations quiet at the default level. Failure events
may include the sentence kind and SQL length, but must not include SQL text,
parameters, or driver-provided SQL details.

## Done when

- Complete static and prepared SQL is no longer logged at `INFO`.
- Database failures emit a structured warning with safe operation metadata and
  the exception type.
- Tests prove SQL text and parameters are absent from the emitted event.
- Existing database behaviour is unchanged.

## Shipped

- Replaced raw SQL logging in `StaticSentence` and `PreparedSentence` with
  safe failure events containing operation kind, SQL length, duration, and
  exception type only.
- Added `DatabaseLoggingTest` covering SQL and exception-message redaction.
- Verified `spotlessCheck`, `check`, and the focused integration test.
