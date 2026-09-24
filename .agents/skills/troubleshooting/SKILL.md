---
name: troubleshooting
description: Investigate POS failures using logs, reproduction steps, database state, and targeted instrumentation without guessing or over-scoping.
---

# Troubleshooting

Use this skill for user-reported errors, failed manual flows, remote logs,
performance complaints, deployment failures, or requests to add logging.

## Procedure

1. Capture the exact flow, environment, version, database target, timestamp,
   input, and expected result. Ask before acting when the target or scope is
   ambiguous.
2. Inspect the newest relevant log entries and correlate them with the source
   path. Distinguish the observed error from possible causes.
3. Reproduce against a safe local or isolated database where possible. For a
   remote installation, use read-only inspection first and protect credentials.
4. Trace the complete flow across UI, service, persistence, resources, and
   external hardware/configuration. Check recent commits and unrelated local
   changes before attributing a regression.
5. Add generic, structured logging at the flow boundary when evidence is
   insufficient. Never hard-code a product, customer, barcode, or one-off
   installation as the logging condition.
6. Fix the smallest confirmed cause, then rerun the original reproduction and
   the nearest regression tests.

## Reporting

Report facts, rejected hypotheses, root cause, changed files, reproduction
result, and remaining uncertainty separately. Do not keep pursuing a hypothesis
the user has explicitly ruled out.
