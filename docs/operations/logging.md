# Application Logging

Retail POS writes rotating application logs under `<install>/logs/`. Update
diagnostics are written to `<install>.update.log`. The normal application log
uses `java.util.logging` and one record per line, so records can be collected
by Grafana Alloy or inspected with standard text tools.

## Levels

- `INFO`: startup milestones, user-flow milestones, successful operations, and
  expected cancellations.
- `WARNING`: a user-visible failure, a denied action, or a recoverable device,
  database, report, backup, or update failure.
- `SEVERE`: initialization or business-operation failures that prevent the
  requested operation from completing.
- `FINE`: diagnostic details useful during temporary troubleshooting only.

## Event Conventions

Messages begin with `event=<snake_case_name>` and use `key=value` fields. The
formatter adds the UTC timestamp, level, logger, run ID, and operation ID when
one is active. Long-running flows include `duration_ms`; asynchronous report,
backup, and update flows emit start and terminal events.

Do not add logging for every keystroke, barcode value, SQL statement, or UI
paint. Use stable identifiers, counts, result states, and safe metadata at the
flow boundary instead.

## Sensitive Data

Never log passwords, payment credentials, card data, full scanned codes, or
unnecessary customer data. Database startup diagnostics use type, host, port,
database name, and whether credentials are configured. Configuration values
are allowlisted and sanitized before logging. Exception messages and stack
traces must also be reviewed when adding diagnostics because database drivers
and remote services may include connection details.

## Temporary Diagnostics

Use `FINE` for short-lived diagnostics and remove the change after the issue is
understood. If temporary production diagnostics are required, document the
event name, expected duration, affected installations, and the redaction
review before raising the configured log level. Never enable SQL or credential
logging as a workaround.
