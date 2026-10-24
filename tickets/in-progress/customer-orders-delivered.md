# Mark customer orders as delivered

Captured: 2026-10-23

Related: `tickets/done/stock-orders.md`

## Goal

Staff can mark a customer replenishment order as delivered after the requested
product has been received, so the list distinguishes stock that arrived from an
order that has already been handed to the customer.
The replenishment screen should keep that workflow clear and usable when
reviewing and updating existing requests.

## Context

Customer orders are stored in the shared replenishment list and currently use
the same `PENDING` -> `ORDERED` -> `RECEIVED` statuses as ordinary replenishment.
Ordinary replenishment must keep `RECEIVED` as its terminal state. Only entries
with a customer may move from `RECEIVED` to `DELIVERED`.

The new status must be persisted by a new migration; existing migrations must
not be edited. Reopening a delivered order returns it to `PENDING`, matching the
existing reopen action. The list must be able to filter delivered entries and
the edit form must not allow `DELIVERED` on entries without a customer.

Screen review (2026-10-23): use a single "Mark as:" row with four short state
actions. A later screen review restores the status controls in the right form
for saved entries. The saved product remains read-only, with no scan instruction
or catalogue-search button. Notes, customer and status can be saved together.
Delivery is only available for a received customer order: the list action
requires a saved customer, while the form can assign one in the same save. New requests
remain fully editable; saved requests may only change note and customer in the
form, except for status as clarified above. Keep the selected request and its
form synchronized after status changes.
Show the creator's current name from PEOPLE, rather than the stored identifier.
New requests can start as pending, ordered or received; delivery requires first
saving a received request and then using a status action on its selected row or form.
The detail form displays the actual saved status as a selected button. Delivery
can be selected on a received order when a customer is selected in the form;
assigning the customer and delivering in the same save is allowed. Changing
notes, customer and status must be atomic.

## Done when

- A customer order in `RECEIVED` can be marked `DELIVERED` from the list.
- A normal replenishment entry cannot be marked `DELIVERED`.
- `DELIVERED` entries are shown with a translated label and can be filtered.
- Existing entries and the existing `PENDING` -> `ORDERED` -> `RECEIVED` flow
  continue to work on Derby, MySQL, and PostgreSQL.
- Automated tests cover the new status and the customer-only restriction.
- PostgreSQL databases that ran V52 still accept `DELIVERED` after the repair
  migration, including after a restart; failures are logged with SQL state and
  constraint without exposing the failing row to the cashier or the log.
- Selected and unselected table rows have consistent contrast in both themes;
  export and status actions fit their labels in English, Spanish and Catalan.
- The right form fits without a horizontal scrollbar and a saved request's
  product cannot be changed through it; note, customer and status remain editable.
- The catalogue-search button and scan instruction are hidden while editing;
  the EAN/reference is shown as read-only product information.
- Changing a status refreshes the selected request and its form immediately.

## Verification remaining

An isolated task branch based on the latest `origin/main` contains only the
delivered-status migration, persistence/form changes, and safe failure logging.
It preserves the already-merged export-order/search behaviour. Its targeted
status/logging tests and `docker compose up -d --wait` / `./gradlew ciCheck` /
`docker compose down -v` pass; containers are stopped. Manual review of the
live screen at supported sizes/scales and keyboard/scanner checks remain.

Failure investigation (2026-10-25): marking a received customer order delivered
on PostgreSQL hit the old `replenishment_status` check even though Flyway
reported V52 applied. V52 looks up the unquoted table by uppercase name in
JDBC metadata; PostgreSQL stores that name in lowercase, so the migration
returns without changing the check. Add a new migration to repair already
upgraded PostgreSQL databases (without editing V52), and verify a real
`RECEIVED` → `DELIVERED` transition in all three engines, including reruns.
When a status update fails, log the action and safe database diagnostics at
the UI boundary; do not expose PostgreSQL's failing-row detail (which contains
customer and product data) in a cashier dialog or application log.

Repair verification (2026-10-25): `V53__repair_postgres_replenishment_status`
corrects the check without changing V52. An isolated PostgreSQL schema test
reinstates the V52-era check, applies the repair twice, reruns Flyway, and
first verifies that delivery is rejected by the old check, then persists a
`RECEIVED` → `DELIVERED` customer order. The targeted status and
safe-logging tests pass. `./gradlew spotlessApply`, the targeted integration
tests, and `docker compose up -d --wait` / `./gradlew ciCheck` /
`docker compose down -v` passed (Derby, MySQL, PostgreSQL; containers removed).
No live/remote database was changed. Manual verification of the running screen
and the actual installation where the screenshot was taken is still pending;
the delivered-status and design-system changes remain interleaved in the
pre-existing worktree, so no independent PR has been opened.

`./gradlew ciCheck` passed with Derby, MySQL and PostgreSQL after the edit-form
changes; Compose containers were stopped. An automated Swing test checks
selection, wrapping, edit-mode product controls, status selection and export
button margins in English, Spanish and Catalan, in both light and dark themes,
at 980px and 1200px. A persistence test verifies that note, customer and
status are saved atomically and the product remains unchanged.
Manually inspect this screen at supported display scales and sizes, including
keyboard focus, scanner entry, row selection and resizing, before shipping.
Repeated `docker compose up -d --wait`, `./gradlew ciCheck`, and
`docker compose down -v` on 2026-10-24; all passed and containers were removed.
The migration and its worktree changes have not yet been committed or reviewed
on a pull request. They must be integrated before the separate design-system
commit (`tickets/todo/replenishment-screen-design-system-alignment.md`).
