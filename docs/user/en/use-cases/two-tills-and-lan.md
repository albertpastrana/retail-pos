# Two tills and the shop LAN

**Situation:** The shop has more than one till and all tills must use the same data.

## Before opening the tills

1. Start the Linux machine that hosts PostgreSQL.
2. Confirm that PostgreSQL is running before opening the other tills.
3. Configure every till with the same PostgreSQL `db.URL`.
4. Keep the printer, customer display, locale, and other machine settings local to each till.

Do not share Derby databases with Syncthing or git. Tickets, catalogue, stock, and cash data belong in the shared database.

## Working with two tills

- A line scanned on the satellite till appears on the central till without parking the ticket.
- The other till can claim an open ticket; the lock prevents stale edits.
- Only the till with the receipt printer can take payment and print the receipt.
- The central PostgreSQL database is the source of truth for products, stock, and tickets.

## If Linux is down

The other tills cannot sell. Do not switch to a Derby copy or try to recover the sale in a local database. Start PostgreSQL again and report the connection error if it does not recover.

## Backup

On the Linux till, use **Maintenance** to create a backup. Copy the resulting file to USB or another machine according to the shop procedure. A backup does not replace the shared database, and the catalogue TSV is not a backup.

Do not commit passwords to the repository or share a configuration file containing credentials.
