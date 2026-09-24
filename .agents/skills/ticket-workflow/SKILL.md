---
name: ticket-workflow
description: Plan and execute work from this repository's Markdown tickets, including status, scope, verification, and handoff.
---

# Ticket workflow

Use this skill when the user asks to implement, review, refine, move, or close a
ticket, or when a task should become a tracked work item.

## Procedure

1. Fetch the latest remote refs and start from `origin/main`; never branch a
   new ticket from a stale local `main`.
2. Read `tickets/README.md`, the target ticket, related tickets, and the current
   `git status`.
3. Summarize the goal, constraints, decisions, out-of-scope work, and
   observable `Done when` checks. Resolve contradictions before editing.
4. Move a `todo/` ticket to `in-progress/` before implementation. Do not leave
   a ticket in `in-progress/` when stopping.
5. Make the smallest change that satisfies the ticket. Keep follow-ups in a
   separate `tickets/todo/` file rather than expanding scope silently.
6. Update the ticket when a decision changes. Add a concise `Shipped` section
   only when the ticket is genuinely complete.
7. Work on a task branch, commit only the task files, and open a pull request.
   Never push task changes directly to `main`.
8. Run the relevant verification and report failures, skipped checks, and
   unrelated worktree changes explicitly in the pull request.
9. Wait for all required pull-request checks to be green and for the user's
   review approval. Then merge the pull request with GitHub's rebase method;
   never use a merge commit or squash merge unless explicitly requested.

## Failure modes to avoid

- Treating a written design note as shipped behaviour.
- Marking a ticket done because code exists without checking its acceptance
  criteria.
- Combining unrelated fixes or user changes into one commit.
- Treating a local build as a substitute for green pull-request checks.
- Guessing a ticket ID or using an external issue tracker.
