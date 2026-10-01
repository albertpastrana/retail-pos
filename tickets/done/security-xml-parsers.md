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

## Shipped

- Added a shared secure SAX parser factory that disables DTDs, external entities, external DTD loading, and XInclude.
- Updated printer template and role-permission parsing to use the secure factory.
- Added regression coverage for rejecting external entities and parsing normal XML.
