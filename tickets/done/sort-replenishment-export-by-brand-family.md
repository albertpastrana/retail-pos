# Sort replenishment export by brand and family

Captured: 2026-10-24

Related: `tickets/done/stock-orders.md`

## Goal

Export replenishment and customer orders grouped by catalogue brand and family.

## Context

The current TSV follows the list's newest-first order. Keep the screen's order and
the current search, type and status filters; only the exported rows change order.
Use the linked catalogue product's BRAND and FAMILY, rather than guessing them
from the request snapshot. Place entries with no brand or no catalogue product
last; within each brand, entries without a family follow named families. Break
ties by newest creation date and then entry ID.
Export using the search actually applied to the displayed list; finish any
pending search before exporting to keep the TSV and visible rows in sync.

## Done when

- Exported catalogue entries sort by brand then family, ascending, while the
  on-screen list remains newest first.
- Manual entries and missing-brand entries sort last and the current filters
  still select the exported rows.
- Automated coverage checks ordering, ties, and filtered customer orders.

## Shipped

The TSV export now uses catalogue BRAND and FAMILY for ordering, with missing
values last and newest entries first on ties. The screen retains its existing
order and filters. `DataLogicReplenishment.listForExport` and
`ReplenishmentPanel.exportVisibleEntries` implement the change; the Derby
integration test `ReplenishmentExportOrderTest` verifies sorting and filters.
