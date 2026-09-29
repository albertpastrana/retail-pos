# Improve application logging

Captured: 2026-09-24

## Goal

Make application logs useful for startup diagnostics, user-flow troubleshooting, and operational support without exposing secrets or sensitive personal data.

## Context

The application already uses `java.util.logging`, rotating log files, a structured single-line formatter, and operation correlation IDs. Logging is currently incomplete across startup, authentication, navigation, sales, cash management, administration, and device flows.

Add safe startup logging for Java and application versions, effective non-sensitive configuration, database type and endpoint metadata, locale, printer, display mode, backups, updates, and initialization results.

Add structured events for important user actions and business milestones, including login attempts, mode changes, navigation, permissions, sales, payments, refunds, ticket persistence, printing, cash closing, inventory operations, backups, updates, and failures.

Never log passwords, payment credentials, card data, full scanned codes, or unnecessary customer data.

## Slices

1. Add safe runtime and effective-configuration logging at startup, including secret-redaction tests.
2. Instrument authentication, mode changes, navigation, sales, printing, and cash closing.
3. Instrument inventory, customers, reports, backups, updates, and remaining device flows.
4. Add operation durations, asynchronous context propagation, operational documentation, and broader flow coverage.

The first two slices are implemented in the current branch. The ticket remains open until the remaining flows and operational documentation are complete.

When an error is presented through `MessageInf`, the dialog layer must also log
the user-visible failure and its throwable cause when available. Successful
notifications remain unlogged.

## Done when

- Startup logs include application version, Git revision, Java/runtime information, operating system, run ID, configuration source, and an allowlisted effective configuration.
- Database configuration is logged without passwords or credential-bearing URLs.
- Important user flows can be reconstructed using event names, operation IDs, user/role context, results, and durations.
- Sales, refunds, payments, printing, cash closing, inventory, backups, and update flows log their successful, cancelled, and failed outcomes.
- Sensitive values are redacted or excluded by tests.
- Structured log values are safely escaped and remain parseable as single-line records.
- Asynchronous operations preserve the relevant correlation context or explicitly create their own operation context.
- Default logging remains useful without logging every keystroke or SQL statement.
- Tests cover formatter output, configuration redaction, correlation IDs, and representative flow events.
- Logging behaviour is documented, including log locations, levels, event conventions, and temporary diagnostic logging.
