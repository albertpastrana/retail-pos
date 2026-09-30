# Harden XML parsers

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Reject external entities and unsafe DTD expansion in application XML parsing.

## Context

`TicketParser` and `AppUser` create SAX parsers without the required XXE
protections.

## Done when

- Both parser paths reject external entities and unsafe DTDs.
- Malicious XML tests pass while printer templates and role parsing continue to work.
