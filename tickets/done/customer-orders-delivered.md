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

## Verification

An isolated task branch based on the latest `origin/main` contains only the
delivered-status migration, persistence/form changes, and safe failure logging.
It preserves the already-merged export-order/search behaviour. Its targeted
status/logging tests and `docker compose up -d --wait` / `./gradlew ciCheck` /
`docker compose down -v` pass; containers are stopped. The user confirmed
manual focus, keyboard, scanner and supported screen size/scale and theme
checks on 2026-10-25.

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
No live/remote database was changed. The independent delivered-status PR is
`https://github.com/albertpastrana/retail-pos/pull/130`.

`./gradlew ciCheck` passed with Derby, MySQL and PostgreSQL after the edit-form
changes; Compose containers were stopped. An automated Swing test checks
selection, wrapping, edit-mode product controls, status selection and export
button margins in English, Spanish and Catalan, in both light and dark themes,
at 980px and 1200px. A persistence test verifies that note, customer and
status are saved atomically and the product remains unchanged.
The separate design-system change follows this PR; see
`tickets/todo/replenishment-screen-design-system-alignment.md`.

## Shipped

Customer orders can be delivered after receipt; V52/V53 upgrade existing
databases, the list and edit form preserve selection and product identity,
and failed status updates log safe SQL diagnostics. Key paths:
`src-pos/com/openbravo/pos/inventory/{DataLogicReplenishment,ReplenishmentPanel}.java`,
`src-pos/db/migration/V52__customer_orders_delivered.java`, and
`src-pos/db/migration/V53__repair_postgres_replenishment_status.java`.
