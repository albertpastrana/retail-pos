# Restrict Java deserialization

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Prevent untrusted serialized database values from instantiating arbitrary
classes or consuming uncontrolled resources.

## Context

`ImageUtils.readSerializable` calls `ObjectInputStream.readObject()` for
database-backed serializable values.

The supported serialized value is the shared-ticket object graph. The filter
therefore allows the ticket and customer domains plus the JDK value and
collection classes used by that graph. Other classes are rejected.

## Done when

- Deserialization uses an allowlist or is replaced with a safer representation.
- Input size and object-graph limits cover depth, references, or an equivalent
  bounded resource budget; rejected oversized and deeply nested values cannot
  consume uncontrolled memory or CPU.
- Valid existing values, rejected classes, oversized input, and deeply nested
  values are covered by regression tests.

## Shipped

- Added an allowlist and bounded `ObjectInputFilter` to `ImageUtils`.
- Added regression coverage for valid tickets, rejected classes, oversized
  input, and deeply nested graphs in `ImageUtilsTest`.
