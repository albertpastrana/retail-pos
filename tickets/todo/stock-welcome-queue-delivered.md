# Fix stock welcome replenishment counters

Captured: 2026-10-26

Related: `tickets/done/customer-orders-delivered.md`

## Goal

Make the replenishment counters on `JPanelStockWelcome` match the rows opened by
each counter in `ReplenishmentPanel`.

## Context

`StockWelcomeRepository.queue()` currently excludes only `RECEIVED`, which
includes delivered customer orders. Its grouped result also assigns every
non-pending group to `ordered`, making the result depend on database row order.
The query must count `PENDING`, `ORDERED`, and open customer orders explicitly;
open customer orders have a customer and a status that is neither `RECEIVED`
nor `DELIVERED`.

Extend the repository test with ordered, received, and delivered entries both
with and without customers, including the `REPLENISHMENT_ENTRIES` columns needed
by those fixtures.

## Done when

- Pending, ordered, and customer-order counters each match their list filter.
- Repository coverage asserts all three counters with the delivered and
  received combinations.
- `./gradlew spotlessApply`, the targeted test, and `./gradlew check` pass.
- The POS manual check confirms the customer-orders card count equals the rows
  shown when tapped.

## Verification

- `./gradlew spotlessApply` passed.
- `./gradlew integrationTest --tests 'com.openbravo.pos.inventory.StockWelcomeRepositoryTest'` passed.
- `./gradlew check` passed.
- The live POS customer-orders card flow was not checked in this session because
  no running POS session with prepared replenishment data was available.
