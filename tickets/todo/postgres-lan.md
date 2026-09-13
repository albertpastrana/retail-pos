# PostgreSQL on the LAN

Captured: 2026-09-11

Related: `tickets/todo/pending-barcodes.md`, `tickets/todo/stock-orders.md`, `tickets/todo/postgres-backups.md`, `tickets/todo/catalog-tsv-linux.md`, `tickets/todo/live-shared-tickets.md`

## Goal

All tills sell against one PostgreSQL on the Linux machine. Same `db.URL` on Windows and Mac. Printer, display, and locale stay in each till’s `*.properties`.

## Context

Everything is LAN-only. Postgres is not on the internet. If Linux is off, the other tills cannot sell.

Do not sync Derby copies: `db.lck` and diverging stock/tickets/creates. Not Syncthing or git for till data. The ERP/JMS menu is leftover; do not use it.

Pending EANs, orders, and backups are app tables/actions, not side files — but they need this shared database first.

Suggested order: (1) Postgres on Linux and each PC’s config, (2) pending EANs, (3) orders, (4) backup from Maintenance.

## Done when

Two tills on the LAN read and write the same catalogue, stock, and tickets. Killing the Linux host stops sales on the others.
