# Edit completed sales

Captured: 2026-09-19

## Goal

Provide an obvious way to edit a completed sale from the till when the operator needs to correct it.

## Context

The edit-sales option was previously available but is no longer visible. Confirm whether the feature is hidden, unavailable in the current flow, or has regressed, and restore the operator-facing action without changing unrelated sale flows.

## Done when

- An operator can find the edit-sales action from the expected sales screen.
- A completed sale can be opened, corrected, and saved through the normal till flow.
- Existing permissions and sale/payment safeguards remain enforced.

## Performance investigation

Captured: 2026-09-19

The edit action in `JTicketsBagTicket.m_jEditActionPerformed` switches to the edit view, reloads the catalog, and assigns a copy of the selected ticket. Entering the sales screen also loads the 10 most recent tickets.

The main database query is defined in `DataLogicSales.getTicketsList()` and is executed by `getRecentTickets()`:

```sql
SELECT T.TICKETID, T.TICKETTYPE, R.DATENEW, P.NAME, C.NAME, SUM(PM.TOTAL)
FROM RECEIPTS R
JOIN TICKETS T ON R.ID = T.ID
LEFT OUTER JOIN PAYMENTS PM ON R.ID = PM.RECEIPT
LEFT OUTER JOIN CUSTOMERS C ON C.ID = T.CUSTOMER
LEFT OUTER JOIN PEOPLE P ON T.PERSON = P.ID
WHERE T.TICKETTYPE = 0
GROUP BY T.ID, T.TICKETID, T.TICKETTYPE, R.DATENEW, P.NAME, C.NAME
ORDER BY R.DATENEW DESC, T.TICKETID
LIMIT 10
```

`EXPLAIN ANALYZE` was run against the configured PostgreSQL database over SSH from the till host. Results:

- Execution time: `117.657 ms`.
- PostgreSQL scans approximately 33,000 receipts and 66,000 payments, then performs a hash join and grouping before applying the limit.
- The existing `PAYMENTS(PAYMENT)` index does not support the join on `PAYMENTS.RECEIPT`; PostgreSQL correctly chooses a sequential scan for the current data volume.
- Catalog queries are not significant: the `TAXES` query took `0.257 ms` and the root `CATEGORIES` query took `0.265 ms`.

Conclusion: the recent-sales query does work over the complete result set, but it is currently below 120 ms. A multi-second delay is therefore unlikely to be caused by these database queries alone. If the delay is still reproducible, instrument the edit flow and Swing/UI initialization to identify the operation after the database calls.
