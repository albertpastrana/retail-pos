# Constrain database template execution

Captured: 2026-09-30

Related: `tickets/todo/security-antipatterns-audit.md`

## Goal

Make the trust boundary for database-backed Velocity templates explicit and
prevent templates from invoking arbitrary application methods.

## Context

`ScriptEngineVelocity.eval` evaluates templates loaded from database resources.

## Done when

- Template execution is constrained or the trusted-configuration boundary is explicitly enforced.
- Printer and ticket rendering flows remain covered by tests.
