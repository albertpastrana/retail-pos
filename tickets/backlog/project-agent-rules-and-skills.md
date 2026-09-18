# Project-wide agent rules and skills

Captured: 2026-09-18

## Goal

Define a maintainable set of project-level rules and reusable skills for
AI-assisted development, with one source of truth where practical and
consistent behaviour in Claude, Cursor, and OpenCode.

## Context

- The previous chat transcripts are not stored in the repository and are not
  available in this session. The evidence currently available is the recent
  ticket history, repository instructions, design-system documentation, and
  Git history; review any exported conversation notes if they become
  available before implementation.
- Recent project work consistently starts from a written ticket with explicit
  scope, decisions, constraints, and observable acceptance criteria. Tickets
  are local Markdown files and are the source of truth; status is represented
  by their folder, and parked work belongs in `tickets/backlog/`.
- The project is an operator-facing retail POS. Guidance should prioritise
  reliable shop-floor behaviour, clear cashier language, touch-sized controls,
  localisation, and preserving existing workflows over generic UI patterns.
- Recent UI work introduced a retail POS design system and a Swing migration
  path. Candidate guidance should cover the design tokens, content and CTA
  conventions, accessibility-by-glance, and the boundary between the design
  system and legacy Swing screens.
- Recent release work spans Java/Gradle, bundled and system runtimes, Linux,
  Windows, and Intel macOS. Agent guidance should include the supported build,
  packaging, platform, and verification expectations rather than assuming a
  single developer platform.
- Database changes have strict project constraints: migrations must remain
  safe for Derby, MySQL, and PostgreSQL, include schema and data changes,
  update checksums, cover fresh/upgraded/rerun paths, and run the documented
  Docker/Gradle verification sequence.
- The current repository instructions cover migrations only. The ticket
  workflow is documented in `tickets/README.md`, while no Claude, Cursor, or
  OpenCode configuration files are currently present in the working tree.
- A completed Spotless ticket records an always-on Cursor rule as a useful
  precedent for tool-specific guidance, but its current presence should be
  verified rather than assumed when this work starts.
- Candidate reusable skills include ticket-first planning, repository and
  impact exploration, database migration review, UI/design-system review,
  cross-platform release verification, and operator documentation review.
- Compare the supported instruction and skill formats for Claude, Cursor, and
  OpenCode, including what can be shared verbatim and what needs adapters or
  generated files.
- Decide where the canonical project guidance lives, how tool-specific files
  are synchronised, and how contributors should update it without drift.
- Keep this as a design and documentation task first. Do not change agent
  configuration or application behaviour as part of this ticket.

## Done when

- The relevant recent conversations and repository guidance have been
  reviewed, with recurring rules and candidate skills documented.
- A compatibility matrix covers Claude, Cursor, and OpenCode instruction and
  skill mechanisms, limitations, and required adapters.
- The proposed canonical layout, ownership, update workflow, and validation
  checks are documented.
- The plan identifies which rules/skills should be shared, which are
  tool-specific, and how to detect divergence between generated or mirrored
  files.
- The resulting proposal is specific enough to implement in a follow-up
  ticket without making unrecorded assumptions.

## Parked

This is intentionally deferred until the project has accumulated enough
conversation and agent-workflow history to make the rules representative.
