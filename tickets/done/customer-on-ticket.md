# Simple customer on the ticket

Captured: 2026-09-14

Related: `tickets/todo/manual-loyalty.md`

## Goal

The customer is part of the sale, not a mini-CRM. A cashier can find or create a customer with one simple flow using only name and phone, see a chip on the ticket, and open the same customer sheet from sales and backoffice.

## Context

Today: ERP-style `CustomersPanel` (tax id, search key, fax, two addresses, tax category), a three-field finder with no create, debt in a separate **Customer payment** menu. At the till: button or scanner `c…` (`CARD`).

Square / Shopify / Toast: one field and minimal create. Identity is the phone. Shop debt is rare there; this shop still needs it.

**Decision:** not a CRM. The customer lives on the ticket. Search + chip + the same sheet at till and backoffice.

Keep only the customer data needed by the shop: name, phone, visible flag, debt / `MAXDEBT`, and the database identifier. `TicketInfo.customer` remains the ticket reference. Loyalty, notes, purchase summaries and usual purchases are outside this ticket.

Create at the till: **name + phone**, with both fields required. Visible by default. Phone is normalized to digits without spaces for lookup and storage. It is not unique. Do not ask the cashier for a manual search key or international prefix.

Remove from the customer model and UI: `CARD`, manual `SEARCHKEY`, fax, both addresses, tax category, and other ERP-only fields. There are no legacy customer records to migrate. Customer payment moves onto the sheet. Invoice-specific tax data is not part of this customer record.

Chip: name and debt if any. Tap → sheet: debt and pay. Purchase history and purchase summaries are not part of this ticket.

Backoffice: list (name, phone, debt) + the same sheet. Debtors still make sense. Master list and diary are history of that sheet.

Do not build: segments, email, customer app, promo engine (`tickets/todo/brand-offers.md`).

## Wireframes

Keep these deliberately small and optimise for a cashier using a scanner or keyboard.

### Till: find or create

```text
Customer                         [x]

Find name or phone  [________________________] [Search]

Results
  Maria Garcia
  Marta Garcia

                         [New customer]
```

```text
New customer                     [x]

Name             [________________________]
Phone            [________________________]

                         [Cancel] [Create]
```

### Till: customer on ticket

```text
Ticket #1042
  1 x Product A                         20.00
  1 x Product B                         12.00

Customer: Maria Garcia                  [Change] [Remove]

                              [Pay]
```

### Customer sheet

```text
Maria Garcia                            [x]

Debt                                     18.00

[Pay debt]
```

### Backoffice

```text
Customers                 [Search name or phone __________]

Name                  Phone             Debt
Maria Garcia          600 123 456       18.00
Marta Garcia          600 987 654        0.00

Select a row to open the same customer sheet.
```

## Slices

1. Search + minimal create + chip
2. Sheet with debt / pay, using the existing debt behaviour

## Ticket flow

1. A cashier opens the customer action on the current ticket.
2. The cashier searches by name or normalized phone, or creates a customer with the required name and phone.
3. Selecting or creating a customer sets `TicketInfo.customer` on the current ticket and shows the customer chip.
4. `Change` repeats the find-or-create flow and replaces the ticket reference.
5. `Remove` clears the ticket reference; it does not delete the customer.
6. Saving, parking, resuming, paying or refunding the ticket keeps the customer reference according to the existing ticket behaviour. This ticket does not change debt accounting rules.
7. The customer sheet can be opened from the chip or from the backoffice list. Both entry points show the same customer data and debt actions.

## Done when

Cashiers find or create a customer using only name and phone, see them on the ticket, and open the same sheet from till and backoffice. Debt is paid from the sheet, not a separate menu, using the existing debt behaviour. The database stores no unused ERP customer fields, loyalty data, notes or purchase summaries.

## Shipped

Integrated customer management with name-and-phone lookup/create, customer assignment on tickets, shared customer sheets, and debt payment from the sheet. Main paths: `src-pos/com/openbravo/pos/customers/` and `src-pos/com/openbravo/pos/sales/JPanelTicket.java`.
