# Operator documentation

Captured: 2026-09-14

Related: `tickets/done/customer-on-ticket.md`, `tickets/backlog/loyalty-customer-balance.md`, `tickets/todo/pending-barcodes.md`, `tickets/done/live-shared-tickets.md`, `tickets/done/postgres-lan.md`, `tickets/done/login-vs-seller.md`, `tickets/backlog/catalog-variants.md`, `tickets/backlog/broken-sales-reports.md`

## Goal

A shop-floor guide with screenshots: how this shop works. Not the whole Openbravo menu.

## Context

Floors, tables, ERP/JMS: one line “do not use”. Not size/colour as attribute sets (`tickets/backlog/catalog-variants.md` already says no). Reports: only ones you actually look at; `productsaletotals` / `soldproducts` are broken (`tickets/backlog/broken-sales-reports.md`). The PostgreSQL LAN has now been validated and can be documented as the shop's current operating model.

First cut: **till + loyalty + unknown EAN**. The two-till flow is now validated and should be documented as the current operating model, not as a future target.

Possible split of the doc itself: (1) till, (2) two tills, (3) stock, (4) close cash, (5) shop on the LAN.

### Outline

**1. Day-to-day till**

- Login and open sales
- Sale with scanner (known EAN): scan → lines → pay → receipt
- Find a product without EAN (name / model, size, colour)
- Change quantity, delete line, line / total discount
- Customer on the ticket (name / phone search); no customer, no loyalty. Target: one search field + name-and-phone create + chip (`tickets/done/customer-on-ticket.md`)
- Pay: cash, card, mixed, gift voucher
- Open drawer without a sale
- Refund / edited ticket (`TicketEdit`)
- New ticket, change seller, park (how you “save” today; ties to two tills)

**2. What is yours**

- Unknown EAN: notice under the keypad (no create dialog) → `PENDING_BARCODES` → Stock create or discard. “Not in TSV” vs “not in `PRODUCTS`”.
- Orders / out of stock: product or EAN + qty + note → Stock (review / ordered / arrived). Not the stock diary.
- Sales (`SALE_PERCENT`): % on the product, how it looks on the ticket, does not earn stamps.
- Gift voucher (`ISVOUCHER`): sell vs redeem. Not the −5 € loyalty line.
- Customers: one field (name / phone); create name+phone; chip (debt); same sheet at till and backoffice. Debt stays; search key/fax/tax category not on create. Pay from the sheet.
- Loyalty: customer with card; 10 € eligible base (remainder stored); sale / discount / voucher / redemption line do not count; ticket text “this ticket: +N · total: X/12”; 12 → −5 € (one per ticket); refund undoes the movement; at close cash it is a discount, not a payment.
- Two tills: PostgreSQL is shared today. A satellite till can send a live ticket to the central till without parking; the ticket lock transfers when it is opened, and only the central till with the receipt printer takes payment.

**3. Stock and catalogue**

- Create from the pending-EAN queue
- Manual create / TSV import (Linux only; the app does not generate the TSV)
- Variants: one EAN = one SKU, group by model (`REFERENCE`); screenshots of search and stock by size
- Sales / 20% labels
- Price rules / sale mark: only if you use them
- Inventory / diary / movement (goods-in ≠ order)
- Product at 0 €: health check, not a tutorial

**4. Close cash**

- Close cash (count, what prints)
- Closed cash (lookup)
- 2–3 reports you actually use. The current reports are sales summary, sales by product, sales by payment method, and low stock; sales by model and CSV export are not available yet.

**5. Start of day, LAN, if it breaks**

For whoever starts Linux and the other PCs, not the build README.

- Postgres on Linux before Windows/Mac; if Linux is off, nobody sells
- Each PC: same `db.URL`; printer / display / locale local
- Backup: Maintenance / on close; copying the file to USB is separate
- Do not: Derby+Syncthing, git of data, ERP menu
- Default users and who may close cash / edit tickets

## Done when

Staff can follow the first cut (till + loyalty + unknown EAN) from the doc without a developer. Two-till workaround and target flow are in different sections.

## Shipped

Added the bilingual operator guide pages for everyday till work, two tills and PostgreSQL LAN, stock and catalogue, cash closing, and current management reports. The validated shop flows are documented in `docs/user/ca/` and `docs/user/en/`.
