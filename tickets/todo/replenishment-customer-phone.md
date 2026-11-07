# Show customer phone in replenishment

Captured: 2026-11-07

Related: `tickets/todo/replenishment-suppliers-and-sharing.md`

## Goal

Let counter and administration staff recognize a customer's replenishment by name and phone without opening the customer finder.

## Context

- Join the existing customer table for `CUSTOMERS.PHONE` (or the phone field the customer finder uses) in `DataLogicReplenishment.list`, keep the saved customer-name snapshot, expose the phone on `ReplenishmentEntry`, and allow list search by phone. Do not change the supplier-facing exported order text.
- In the Customer table column, show the name on top and the phone below as a 13px ink-muted caption; omit the caption when no phone exists. The narrow admin product caption continues to show customer and supplier information.
- In the new-request and detail customer rows, use vertically centered name and phone labels; the phone is a muted 13px caption under the name beside the Change/Choose button. No customer shows the localized No customer selected message; a customer without a phone shows only the name. Preserve focus, selection and theme behavior in sales and administration.

## Done when

- Searching by a customer's phone finds that customer's replenishment request in both modes.
- The table and both right-panel customer fields show name and phone (when available), with no empty second line and no truncation at supported widths in CA/ES/EN.
- Focused query/layout tests, `./gradlew spotlessApply`, and offscreen light/dark captures pass. Record any remaining workstation checks before closing.

## Implementation and verification (2026-11-08)

- `DataLogicReplenishment.list` and its single-entry read join `CUSTOMERS.PHONE` by customer ID while retaining the saved customer name. The list search accepts both literal phone text and normalized digit searches (including spaces or a country prefix). `ReplenishmentEntry.customerPhone` is read from the current customer record; no schema change is needed. `ReplenishmentOrderText` continues to exclude customer phones from outgoing supplier text.
- The Customer table cell now uses a name and optional 13px muted-mono phone caption. Its row height and column width accommodate both; absent phones have no second line. The compact administration product caption includes the phone. Both the new-request and detail customer controls show the name and optional caption as one vertically centered value; the no-customer state retains the localized message, and a long translated name can move the Choose action below it rather than truncate it.
- Focused Derby tests cover a saved customer with a phone, a saved customer without one, searching a phone with spaces, the table and right-hand labels in CA/ES/EN and light/dark, and exclusion of the phone from supplier-facing text. Offscreen captures `build/reports/replenishment-screenshots/sales-customer-phone-{light,dark}-{en,es,ca}-1000.png`, `sales-flow-find-light-ca-1470.png`, and `light-es-detail-customer-980.png` were visually reviewed; each shows the name and phone when present. `./gradlew spotlessApply integrationTest --tests '*ReplenishmentPanelLayoutTest' --tests '*ReplenishmentModesLayoutTest' --tests '*ReplenishmentSupplierFlowTest' --tests '*ReplenishmentExportOrderTest' --tests '*ReplenishmentStatusTest' --tests '*ReplenishmentSupplierTest' --tests '*ReplenishmentPanelScaleTest'` passes. `./gradlew ciCheck` passes with 253 assigned tests across fast/Derby/external-database lanes; disposable MySQL/PostgreSQL containers were removed with `docker compose down -v` afterward. The external-database tests cover migrations; the phone-query assertion itself runs on Derby.
- **Pending before done:** verify live customer-finder selection, name/phone visibility, keyboard focus and real display scale on the sales terminal and MacBook Air; offscreen screenshots do not prove those workstation flows.
