# Bulk status actions on the replenishment list

Captured: 2026-10-24

Related: `tickets/todo/replenishment-next-step-status-action.md`
Related: `tickets/done/customer-orders-delivered.md`

## Goal

Staff can select several replenishment requests or customer orders and apply the existing status and delete buttons beneath the table to them together.

## Context

Decision (2026-10-26): all five existing footer actions (Pending, Ordered, Received, Delivered, Delete) operate on selected rows. A mixed-status selection can be marked Received together, including entries already Received or Delivered. Keep the existing single-row editing form and buttons. Multiple selections show a summary instead of an arbitrary row's edit form. Selection is by touch-friendly checkbox, with keyboard support; changing search or status filter clears multiple selection unless the form has unsaved edits, in which case its request remains selected. A single edited request stays selected through search to preserve unsaved form edits. Deletion always requires confirmation.

Delivery applies only to received requests with a customer; ineligible requests are explicitly reported before applying to eligible requests. Bulk actions ask staff to save or cancel unsaved form edits first. Status changes do not post stock movements or send customer notifications. A bulk change is one transaction, and an error rolls back the entire action.

User visual review (2026-10-26): the checkbox column shows alternating beige bands while the adjacent cells use the shared row colours. Match the checkbox column background and selection border to the rest of the row in both themes.

## Done when

- Several rows can be selected with touch and keyboard; search and filter changes clear multiple selection.
- Existing footer status buttons update selected requests in one transaction and show a clear result; delivery explains skipped rows and never delivers an unreceived request or one without a customer.
- Delete confirms the selected count and deletes atomically.
- Table, badges and counts refresh after an action; single-request editing still works.
- Integration coverage checks bulk transition, mixed eligibility and rollback on Derby, MySQL and PostgreSQL.
- Manual EN/ES/CA check at 980px and 1200px, including keyboard, focus and resize, is recorded here.

## Implementation and verification (2026-10-26)

Implemented locally on `task/replenishment-bulk-status-actions`: checkbox selection, all footer actions, atomic batch persistence, and V55 to preserve unique open products while allowing multiple received entries on Derby. `./gradlew spotlessApply` and `./gradlew ciCheck` pass with disposable Derby, MySQL and PostgreSQL databases; the Docker containers were removed. Automated UI layout checks cover English, Spanish and Catalan at 980px and 1200px. Live-app touch, keyboard/focus, resizing and physical display-scale checks remain to be recorded before marking done.

Visual correction after the user's screenshot: render checkbox cells with the same shared selected/unselected row surfaces and selected border as the Type column. `./gradlew spotlessApply integrationTest --tests '*ReplenishmentPanelLayoutTest'` passes; checked generated Catalan 980px screenshots for selected and unselected rows. User confirmation on the live screen remains pending.
