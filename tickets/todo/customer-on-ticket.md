# Customer lives on the ticket

Captured: 2026-09-14

Related: `tickets/todo/loyalty-customer-balance.md`

## Goal

The customer is part of the sale, not a mini-CRM. One search field at the till, a chip on the ticket, and the same customer sheet in sales and backoffice.

## Context

Today: ERP-style `CustomersPanel` (tax id, search key, fax, two addresses, tax category), a three-field finder with no create, debt in a separate **Customer payment** menu. At the till: button or scanner `c…` (`CARD`).

Square / Shopify / Toast: one field, minimal create, profile on the ticket (history, points). Identity is phone or card. Shop debt is rare there; this shop still needs it.

**Decision:** not a CRM. Option B — customer lives on the ticket. Search + chip + the same sheet at till and backoffice.

Keep: `CUSTOMERS`, `CARD`, `VISIBLE`, debt / `MAXDEBT`, tax id only when a invoice needs it. `TicketInfo.customer` as now.

Create at the till: **name + card**. System sets `SEARCHKEY` (card or phone). Visible by default.

Cut from create: manual search key, fax, address 2, tax category (hide under “Invoice”; `TaxesLogic` already reads it; almost unused in fashion). Customer payment menu moves onto the sheet.

Chip: name; debt if any; later loyalty `X/12` (see related ticket). Tap → sheet: notes, debt and pay, last purchases, usual buys.

Backoffice: list (name, card, debt, loyalty) + the same sheet. Debtors still make sense. Master list and diary are history of that sheet.

Do not build: segments, email, customer app, promo engine (`tickets/todo/brand-offers.md`).

## Slices

1. Search + minimal create + chip
2. Sheet with debt / pay
3. Loyalty on the chip + usual purchases

## Done when

Cashiers find or create a customer in one field, see them on the ticket, and open the same sheet from till and backoffice. Debt is paid from the sheet, not a separate menu.
