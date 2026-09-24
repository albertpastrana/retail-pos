# Project Agent Guidance

`AGENTS.md` is the canonical project-wide rule set. The skills under
`.agents/skills/` are reusable procedures that complement those rules; they do
not override a ticket or the repository instructions.

## Tool adapters

- OpenCode discovers project skills from `.agents/skills/`.
- Cursor keeps its tool-specific adapters under `.cursor/rules/`; adapters
  should point contributors back to `AGENTS.md` rather than copy the rules.
- Claude can use `AGENTS.md` as project context. If a future `CLAUDE.md` is
  added, it should reference this file and avoid a second canonical rule set.

## Ownership

Update `AGENTS.md` for shared rules and update a skill only when the procedure
is reusable across more than one task. Keep tool-specific syntax in the tool
adapter, not in the shared guidance.

## Review checklist

- Does the proposed rule describe a repeated project behaviour rather than a
  one-off preference?
- Does it conflict with `tickets/README.md`, the design system, or migration
  instructions?
- Can it be checked by a file inspection, command, test, or manual flow?
- Does the change need a Cursor/OpenCode/Claude adapter, or is shared Markdown
  sufficient?
