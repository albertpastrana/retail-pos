# Restrict Java deserialization

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Prevent untrusted serialized database values from instantiating arbitrary
classes or consuming uncontrolled resources.

## Context

`ImageUtils.readSerializable` calls `ObjectInputStream.readObject()` for
database-backed serializable values.

## Done when

- Deserialization uses an allowlist or is replaced with a safer representation.
- Input size and object-graph limits cover depth, references, or an equivalent
  bounded resource budget; rejected oversized and deeply nested values cannot
  consume uncontrolled memory or CPU.
- Valid existing values, rejected classes, oversized input, and deeply nested
  values are covered by regression tests.
