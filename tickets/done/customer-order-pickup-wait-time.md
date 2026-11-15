# Show customer order pickup wait time

Captured: 2026-11-15

## Goal

Staff can see how long a customer order has been waiting for pickup after it
becomes received.

## Context

Customer orders use `RECEIVED` while ready for pickup and `DELIVERED` after the
customer takes them. `CREATED_AT` is not suitable because an order may spend
time waiting for stock, and `UPDATED_AT` also changes when notes or customer
details are edited. Persist a dedicated `RECEIVED_AT` timestamp. Set it when an
order enters `RECEIVED`, preserve it while the order remains received, and clear
it when the order is reopened. Show the elapsed time in the replenishment list
and in the customer's order cards. Add English, Spanish, and Catalan labels.

## Done when

- A received customer order shows elapsed time since it became received.
- The elapsed time remains stable when notes or customer details are edited.
- Reopening an order clears the pickup-wait timer; delivering it removes it from
  pending orders.
- Existing databases migrate safely on Derby, MySQL, and PostgreSQL.
- Automated coverage verifies migration, state transitions, persistence, and
  translated UI labels.
- The relevant Swing screens remain usable with keyboard, resizing, light and
  dark themes, and English, Spanish, and Catalan labels.

## Verification

`./gradlew spotlessApply` passed. Targeted status and replenishment layout tests
passed. `docker compose up -d --wait`, `./gradlew ciCheck`, and
`docker compose down -v` passed with Derby, MySQL, and PostgreSQL. Migration
checksums passed and the containers are stopped.

## Shipped

Added `RECEIVED_AT` in V59, persisted it across individual, bulk, and form
status changes, and cleared it when orders are reopened. The replenishment list,
order detail, and customer order cards now show the localized pickup wait time.
Key paths include `DataLogicReplenishment`, `ReplenishmentPanel`,
`CustomersPanel`, and `V59__replenishment_received_at.java`.
