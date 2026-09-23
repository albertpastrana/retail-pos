# Replenishment list

Captured: 2026-09-10, 2026-09-11, 2026-09-17

Related: `tickets/todo/pending-barcodes.md`, `tickets/done/postgres-lan.md`, `tickets/done/customer-on-ticket.md`

## Goal

Staff can quickly record a product or product variant that needs replenishing. The
entry can optionally be linked to a customer when the missing item is a customer
request or reservation. The list is shared by all tills and the stock workstation.

This is a capture and follow-up list, not a supplier purchase-order module and not
the stock diary.

## User flow

### Add a missing product

The user starts **Add product that is missing** from the till or the stock area.
The action must be usable with a barcode scanner, keyboard, or product finder.

1. Scan or enter an EAN.
2. If it identifies a product, show the product and offer **Add**.
3. Also offer **Choose another variant** when the product belongs to a family.
   Family variants are shown with their reference, size and colour, so the user
   can choose the missing variant rather than the scanned one.
4. If the code is not found, search products as in the existing product finder,
   including reference/model search.
5. If the product still cannot be found, allow a manual entry with the typed EAN
   and/or a free-text description.
6. Optionally add a note.
7. Optionally assign a customer for an order or reservation.
8. Confirm. The entry is saved immediately to the shared database with status
   `PENDING`.

The user does not enter a quantity. Each open entry means that the selected
product or variant needs replenishing. Adding the same product again should reuse
the existing open entry rather than silently create duplicates; the user may add
or replace the note and customer during confirmation.

### Customer assignment

Customer assignment is optional. The user can search existing customers using the
same name/phone behaviour as the customer sheet and select one. If the search
returns no customer to select, the user can create one in this flow using the existing
find-or-create customer behaviour. Name and phone are both required, and the
phone is normalized to digits without spaces before searching and storing.

Before creating the customer, the application checks the normalized phone again.
If one or more customers already have that phone, it warns the user and shows
those customers so the user can select one. The user must explicitly choose
**Create anyway** to create a new customer with the same phone.

The list clearly distinguishes:

- No customer: normal replenishment.
- Customer assigned: customer order/reservation that must not be forgotten.

The customer link is informational and does not create a sale, reserve stock, or
create a ticket.

### Stock follow-up

The left-hand application menu contains a **Replenishment list** option. The
stock user opens it from there and sees all open entries from every till. Each
entry can move through:

`PENDING` -> `ORDERED` -> `RECEIVED`

- `PENDING`: needs review or has not yet been ordered.
- `ORDERED`: included in a supplier order.
- `RECEIVED`: the goods are known to have arrived.

The stock user can return an entry to an earlier state if the supplier order is
cancelled or the item was not received. A received entry is kept as history and
can be filtered out of the default view.

Changing status does not update `STOCKCURRENT` or insert into `STOCKDIARY`.
Goods-in remains a separate stock movement operation.

## Data captured

Each replenishment entry contains:

- Stable ID.
- Product ID when matched to `PRODUCTS`.
- Product reference/model, name, and EAN shown as a snapshot for readable history.
- Selected variant, including the product ID, size and colour where the catalogue
  represents them as separate products.
- Manual description and/or EAN when no catalogue product can be matched.
- Optional note.
- Optional customer ID, with customer name snapshot for history. An existing
  customer can be selected, or a new one can be created with the required name
  and normalized phone.
- Status: `PENDING`, `ORDERED`, or `RECEIVED`.
- Created-at and updated-at timestamps.
- User who created the entry and user who last changed its status.

The list displays the creation date/time and creator for every entry. The detail
view also displays the last update date/time and the user who made that update.

Only one open entry (`PENDING` or `ORDERED`) is kept for the same matched product.
Manual entries cannot be deduplicated reliably and may be added separately.

## UI wireframes

The wireframes describe the behaviour, not final visual design.

### Till or stock: add product

```text
+----------------------------------------------------------+
| Add product that is missing                         [X] |
|                                                          |
| Scan EAN / reference                                     |
| [ 8412345678901________________________ ] [Scan/Search] |
|                                                          |
| Scanned product                                          |
| [ Blue T-shirt                          ]                |
| Ref: TS-BLUE-M       EAN: 8412345678901                 |
|                                                          |
| [Choose another variant]                                 |
|                                                          |
| Note (optional)                                          |
| [ Customer asked for it_______________________________ ] |
|                                                          |
| Customer (optional)                                      |
| [ Search name or phone________________ ] [Find]          |
| [ New customer ]                                         |
| [ No customer selected                                  ] |
|                                                          |
|                                      [Cancel] [Add]       |
+----------------------------------------------------------+
```

### Family variant selection

```text
+----------------------------------------------------------+
| Choose the missing variant                           [X] |
| Family: T-shirt / model TS                               |
|                                                          |
| Size       Colour       Reference          Stock          |
| [S]        Blue         TS-BLUE-S          0              |
| [M]        Blue         TS-BLUE-M          2              |
| [L]        Blue         TS-BLUE-L          0              |
| [M]        Red          TS-RED-M           1              |
|                                                          |
| Select the product that is missing: [TS-BLUE-S]          |
|                                      [Cancel] [Select]   |
+----------------------------------------------------------+
```

The family view is only shown when there are related catalogue products. It must
not automatically choose a size or colour based on the scanned product.

### Stock: replenishment list

```text
+--------------------------------------------------------------------------------+
| Replenishment list                                      [Add missing product]   |
| [Pending 12] [Ordered 5] [Received]   Search: [___________________________]   |
|                                                                                |
| Product / variant       Customer             Note                 Status       |
| T-shirt TS-BLUE-S       Maria Garcia         Customer order       PENDING      |
| EAN 8412345678901       --                  Display stock        ORDERED      |
| Jeans JN-BLACK-42       Joan Puig            --                   PENDING      |
| Manual: black hooks     --                  EAN 123...           PENDING      |
|                                                                                |
| Selected: T-shirt TS-BLUE-S                                                   |
| Customer: Maria Garcia   Added by: Seller    Added: 17/09/2026 10:42          |
| Note: Customer order       Updated by: Stock  Updated: 17/09/2026 11:15        |
|                                                                                |
| [Edit] [Mark ordered] [Mark received] [Reopen]                                |
+--------------------------------------------------------------------------------+
```

## Scope and non-goals

- Use the existing product finder and customer search patterns where possible.
- Add a shared app-owned table for replenishment entries through a Flyway
  migration; do not use side files or local Derby copies as a synchronisation
  mechanism.
- Add a stock-area panel and the till action, subject to the existing role/menu
  permission model. The panel must be reachable from a named item in the
  left-hand application menu.
- Refresh the list when the panel is opened or explicitly refreshed; push
  notifications are not required.
- Do not create supplier documents, supplier-specific quantities, prices, costs,
  reservations, sales tickets, or automatic inventory movements in this ticket.
- An unknown scanned EAN may still be recorded in `PENDING_BARCODES` according to
  its own flow. Adding a manual replenishment entry is a separate user action.

## Acceptance criteria

- A user can add an exact matched product by scanning its EAN without entering a
  quantity.
- A user can choose a different size/colour from the scanned product's family.
- A user can find a product by reference/model using the product finder.
- A user can save a manual EAN or description when no product matches.
- A note and customer assignment are optional and visible in the stock list.
- A seller can create and assign a customer with a name and phone when the
  search returns no customer to select.
- Creating a customer re-checks the normalized phone and warns about existing
  customers with that phone; a duplicate requires explicit **Create anyway**
  confirmation.
- The left-hand menu contains a **Replenishment list** item that opens the panel.
- Every list entry shows when it was added and which user added it.
- The detail view shows when it was last updated and which user made the update.
- The same matched product cannot produce duplicate open entries.
- A second till connected to the same PostgreSQL database sees a newly added entry.
- A stock user can move entries through `PENDING` -> `ORDERED` -> `RECEIVED` and
  reopen them.
- Status changes never modify `STOCKCURRENT` or `STOCKDIARY`.
- Received entries remain available through history/search.

## Shipped

Shared replenishment entries with product and variant selection, optional customer assignment, status workflow, stock menu access, permissions, and database migrations. Main paths: `src-pos/com/openbravo/pos/inventory/` and `src-pos/db/migration/V22__replenishment_entries.java`.
