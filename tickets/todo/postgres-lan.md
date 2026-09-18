# Validate the PostgreSQL shop LAN

Captured: 2026-09-11, 2026-09-17

Related: `tickets/todo/pending-barcodes.md`, `tickets/done/stock-orders.md`, `tickets/done/postgres-backups.md`, `tickets/done/catalog-tsv-linux.md`, `tickets/done/live-shared-tickets.md`

## Goal

Deploy and validate the shop so every till sells against the same PostgreSQL server on the Linux machine. The application already supports PostgreSQL; this ticket covers the real shop installation and its operational checks.

## Context

PostgreSQL is installed and the application has PostgreSQL migrations, integration tests, backup support, and a LAN configuration template in `till-lan.properties`.

Configure the Linux host and each till with the same `db.URL`. Printer, display, locale, and other machine-specific settings stay in each till’s `*.properties`.

Everything is LAN-only; PostgreSQL is not exposed to the internet. If Linux is off, the other tills cannot sell.

Do not sync Derby copies with Syncthing or git. Stock, tickets, pending EANs, orders, and backups belong in the shared database or its backup process. The ERP/JMS menu is leftover and should not be used.

## Checks

- PostgreSQL is running before any till tries to sell; the tills may start in any order.
- Each till connects using the same `db.URL` and can complete a sale.
- Two tills can read and write the same catalogue, stock, and tickets.
- A line scanned on the satellite till appears on the central till’s shared-ticket list without parking.
- Opening the live ticket transfers its lock, prevents stale edits, and only the till with a receipt printer can take payment.
- A backup can be produced from Maintenance on the Linux till.
- Stopping PostgreSQL causes the tills to fail with an error; no special recovery flow is required.
- The operator guide records the PostgreSQL availability requirement, configuration, and failure behaviour.

## Done when

The shop’s tills have been configured and tested against the shared PostgreSQL server, including the live shared-ticket flow. Two tills can read and write the same catalogue, stock, and tickets; the central till alone can take payment; and the documented failure behaviour is confirmed.
