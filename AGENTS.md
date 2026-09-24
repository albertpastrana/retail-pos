# Agent instructions

## Project workflow

- Read `tickets/README.md` and the relevant ticket before changing code.
- Treat the ticket as the source of truth. Record changed decisions in the
  ticket instead of leaving them only in chat.
- Keep one concern per ticket and one ticket in progress unless the user says
  otherwise. Move the ticket to `in-progress/` before implementation and to
  `done/` only when its observable checks are true.
- Start each ticket from the latest `origin/main`: fetch the remote refs before
  creating the task branch, and branch from `origin/main` rather than a stale
  local `main`.
- Do not create, search, update, or mention Linear issues. Work items live
  under `tickets/`.
- Inspect `git status` before editing. Do not revert, include, or reformat
  changes that are not part of the current task.
- Keep changes minimal and preserve existing behaviour unless the ticket
  explicitly changes it. Do not perform opportunistic cleanup.
- Never push task changes directly to `main`. Work on a task branch and open a
  pull request for every change, including documentation and configuration.
- A change reaches `main` only through its pull request after all required
  checks are green. Use GitHub's rebase merge for the approved pull request;
  do not use a merge commit or squash merge unless the user explicitly asks.
- Before opening or updating a pull request, report the changed files,
  verification state, and any unrelated worktree changes. Never include those
  unrelated changes in the task branch.
- Never claim a visual or operational change is complete without the relevant
  manual or automated verification.

## Safety and diagnosis

- Separate observed facts, hypotheses, and conclusions when diagnosing a bug.
  Do not keep arguing for a hypothesis after the user has rejected it.
- Before changing a real or remote database, identify the target, scope, and
  rollback or backup path. Never expose or request passwords in chat.
- Treat database-backed resources, reflection, scripts, XML, optional hardware,
  and packaging files as runtime dependencies even when static references are
  absent.
- For manual reproductions, inspect the newest relevant log entries first and
  add generic flow logging rather than product- or customer-specific logging.

## Retail POS behaviour

- Design for a cashier at a counter: clear action labels, touch-sized controls,
  keyboard and scanner input, safe defaults, and no unnecessary steps.
- Validate user-visible changes in English, Spanish, and Catalan. Check that
  labels remain visible at supported window sizes and display scales.
- Prefer standard Swing layout managers, `pack()`, and component preferred
  sizes derived from content. Document any intentional fixed geometry.
- Keep the shared design-system tokens as the source of truth. Keep light and
  dark themes in sync and do not introduce screen-local colours casually.

## Verification

- After Java changes, run `./gradlew spotlessApply` and then the narrowest
  relevant checks before broader verification.
- For UI changes, name the concrete screens and flows manually verified,
  including keyboard, focus, resize, and translated-label checks where relevant.
- For changes involving shared components, inspect and verify all important
  call sites before declaring the change complete.

## Database migrations

- Never edit a migration that has been committed or released. Add the next
  versioned migration under `src-pos/db/migration/` instead.
- A migration must be safe for Derby, MySQL, and PostgreSQL. Keep
  database-specific SQL behind the existing Flyway placeholders or explicit
  database branches in the migrator.
- Keep schema changes and data changes in the migration. Do not depend on
  local files, TSV imports, or a manually prepared database.
- Update `migration-checksums.sha256` whenever a new migration is added.
- Add or update integration coverage for an empty database, an existing
  database when the migration path requires it, and running the migration a
  second time.
- Do not assert the total number of Flyway history rows. Assert that the
  migrated schema and representative data can be queried instead.

Before finishing migration work, run:

```sh
docker compose up -d --wait
./gradlew ciCheck
docker compose down -v
```

Leave the database containers running only when explicitly requested.
