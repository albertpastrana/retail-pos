# Migrate password hashing

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Move new and changed passwords to a modern salted password KDF without locking
out users with existing supported password formats.

## Context

`Hashcypher` accepts plaintext, unsalted SHA-1, and empty passwords.

## Done when

- Existing credentials migrate or verify safely.
- Empty and plaintext credentials are rejected for new or changed passwords.
- Login and password-change regression coverage passes.
