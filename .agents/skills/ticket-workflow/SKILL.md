---
name: ticket-workflow
description: Plan and execute work from this repository's Markdown tickets, including status, scope, verification, and handoff.
---

# Ticket workflow

Use this skill when the user asks to implement, review, refine, move, or close a
ticket, or when a task should become a tracked work item.

## Procedure

1. Read `tickets/README.md`, the target ticket, related tickets, and the current
   `git status`.
2. Summarize the goal, constraints, decisions, out-of-scope work, and
   observable `Done when` checks. Resolve contradictions before editing.
3. Move a `todo/` ticket to `in-progress/` before implementation. Do not leave
   a ticket in `in-progress/` when stopping.
4. Make the smallest change that satisfies the ticket. Keep follow-ups in a
   separate `tickets/todo/` file rather than expanding scope silently.
5. Update the ticket when a decision changes. Add a concise `Shipped` section
   only when the ticket is genuinely complete.
6. Work on a task branch, commit only the task files, and open a pull request.
   Never push task changes directly to `main`.
7. Run the relevant verification and report failures, skipped checks, and
   unrelated worktree changes explicitly in the pull request.
8. Wait for all required pull-request checks to be green before considering the
   change eligible for `main`. Do not merge without the user's review and
   approval.

## Failure modes to avoid

- Treating a written design note as shipped behaviour.
- Marking a ticket done because code exists without checking its acceptance
  criteria.
- Combining unrelated fixes or user changes into one commit.
- Treating a local build as a substitute for green pull-request checks.
- Guessing a ticket ID or using an external issue tracker.
