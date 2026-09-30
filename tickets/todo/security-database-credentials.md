# Protect database credentials

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Replace deterministic database-password obfuscation with authenticated
encryption or an explicitly protected external secret mechanism.

## Context

`AltEncrypter` uses deterministic `DESEDE/ECB/PKCS5Padding` with a key derived
from a fixed phrase and database username.

## Done when

- Database credentials use authenticated protection with a documented key or secret-store path.
- Existing installations have a safe migration and failure-handling path.
- Configuration and startup tests cover supported database engines.
