# Tickets

Local work items for this repo. Do not use Linear, GitHub Issues, or any other tracker.

Status is the folder the file sits in. Moving the file is the status change.

| Folder         | Meaning                                                |
| -------------- | ------------------------------------------------------ |
| `todo/`        | Agreed work, not started                               |
| `in-progress/` | Someone is implementing it now                         |
| `done/`        | Behaviour is in the tree (or the ops step is finished) |
| `backlog/`     | Intentionally parked; not planned for now              |

Design notes and decisions live **inside** the ticket they belong to. A written decision is not done work.

## For agents

1. Read this file and the ticket you are about to touch before editing code.
2. Never create, search, update, or mention Linear issues. Never invent ticket IDs (`POS-123`, and so on).
3. One concern per file. Filename: lowercase kebab-case, short, stable (`live-shared-tickets.md`). Do not rename for wording nits; rename only if the work itself changed.
4. New work: add a markdown file under `todo/`. Do not append to a retired `NOTES.md`. Do not start a second notes dump.
5. When you start: move the file `todo/` → `in-progress/`. One ticket in progress unless the user said otherwise. Do not leave a file in `in-progress/` after you stop.
6. When the **Done when** section is true: move `in-progress/` → `done/`. Add a short **Shipped** section (what landed, key paths). Do not mark done for design-only, parked, or “first slice still open”.
7. Edit the ticket when the user changes a decision. Keep the file the source of truth; do not split context across chat.
8. English only: titles, body, and filenames.
9. Do not create tickets for drive-by cleanup. If the user captures a follow-up, write a `todo/` file.
10. Related work: link other ticket paths (`` `tickets/todo/foo.md` ``). Do not duplicate the same job in two files.

## Ticket shape

Keep it short. Use this skeleton:

```markdown
# Short title

Captured: YYYY-MM-DD

Related: `tickets/todo/other.md`

## Goal

What should be true when this is finished.

## Context

Decisions, current behaviour, tables, and what not to build.

## Done when

Observable checks. Prefer shop/till behaviour over file lists.
```

Optional: **Slices** (order of cuts), **Parked** (why it is waiting), **Shipped** (only in `done/`).

## Humans

Pick a `todo/` file, move it to `in-progress/`, do the work, move it to `done/`.
