# Retail POS

A point-of-sale application for retail shops: touch-screen sales, ESC/POS receipt printers, customer display, and barcode scanners. Built around a product catalogue with one SKU per barcode, stock per product, and sales reporting — not around tables, courses, or kitchen orders, so it is a poor fit for restaurants.

Forked from [Openbravo POS](https://sourceforge.net/projects/openbravopos/) 2.30.3.

This file is the project entry point. Catalogue variant rules live in [catalog-variants.md](catalog-variants.md). ESC/POS printers (Windows, Linux, macOS USB bridge): [escpos-printer.md](escpos-printer.md).

## Requirements

- JDK 17 or newer to run Gradle (the build still emits Java 8 bytecode)
- The [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) (`./gradlew`); no local Gradle install needed
- Access to Maven Central when dependencies are not already in the Gradle cache

## Build

```sh
./gradlew jar
```

Useful tasks:

| Task                        | Result                                                      |
| --------------------------- | ----------------------------------------------------------- |
| `./gradlew jar`             | App jar plus locales/reports jars next to `start.sh`        |
| `./gradlew check`           | That, plus compile the `data/` database helpers             |
| `./gradlew integrationTest` | Flyway against Derby, Compose MySQL, and Compose PostgreSQL |

CI runs `./gradlew check integrationTest` on every push and pull request.

`./gradlew jar` copies `openbravopos.jar`, `locales.jar`, and `reports.jar` next to `start.sh`. The `locales/` and `reports/` directories are also on the classpath, so a source checkout still runs after only the app jar is present:

```sh
./start.sh path/to/config.properties
```

On Windows: `start.bat`. Configuration UI: `./configure.sh` or `configure.bat`.

## Configuration

Everything below is per-installation: locale, tax rates, receipt layout, shop details. Nothing is baked into the build.

If you pass no argument, the app reads `~/openbravopos.properties`.

Pass a file to use a repo-local config (see `dev.properties` as a template — copy it; do not commit machine-specific paths):

```sh
./start.sh ./dev.properties
```

Important keys:

| Key                                     | Purpose                                                                                             |
| --------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `db.driver` / `db.URL` / `db.driverlib` | Database (Derby, MySQL, or PostgreSQL); `db.driverlib` is optional for the bundled drivers          |
| `user.language` / `user.country`        | UI locale; translations live in `locales/`                                                          |
| `machine.printer`                       | `screen`, or `epson:file,<path>` for a raw ESC/POS printer ([escpos-printer.md](escpos-printer.md)) |
| `catalog.import.products`               | Path to the products TSV (scan-to-import)                                                           |
| `catalog.import.categories`             | Path to the categories TSV                                                                          |

First launch against an empty database URL runs the Flyway migrations and creates the schema. Default users (empty password): Administrator, Manager, Employee, Guest.

A database created by the old per-engine scripts is adopted on first launch: it is baselined at version 2, so the schema and seed data it already holds are left alone and only later migrations apply. Derby databases still in the pre-10.7 file format are hard-upgraded at the same time, which older Derby versions cannot undo — take a copy of the database directory first.

**Quit the POS before running any database helper.** With embedded Derby the database directory is locked (`db.lck`); two processes at once fail with a lock error.

## Database

Versioned migrations are in `src-pos/db/migration/`. `V1__baseline.sql` is shared by Derby, MySQL, and PostgreSQL; the startup code supplies the few database-specific data types and ticket-number definitions as Flyway placeholders. Binary templates and role permissions are loaded by the versioned Java migration.

Do not edit a migration after it has shipped. Add the next `V<n>__description.sql` or Java migration instead.

To prove a new install on MySQL and PostgreSQL as well as Derby:

```sh
docker compose up -d --wait
./gradlew integrationTest
```

Compose publishes PostgreSQL on `127.0.0.1:15432` (user/password/database `pos`) and MySQL on `127.0.0.1:13306` (user `root`, password `pos`, database `pos`). `docker compose down -v` drops those containers so the next run starts from an empty schema.

Resources the till actually uses (ticket layout, logo, buttons, role XML) live in the `RESOURCES` table, not only in `src-pos/com/openbravo/pos/templates/`. Edit templates in git, then push them into the database (Maintenance → Resources, or the helpers below).

## Catalogue

One sellable SKU per barcode. Colour and size are part of the product row, not attribute sets. Why, and how to group stock and sales by model: [catalog-variants.md](catalog-variants.md).

TSV files (UTF-8, tab-separated, no header):

- `data/import-categories.tsv` — `id`, `name`, optional `parentid`
- `data/import-products.tsv` — `id`, `reference`, `barcode`, `name`, `category_id`, `price_buy`, `price_sell`, optional `brand`
- `data/import-prices.tsv` — header plus `barcode`, `reference`, `price_buy`, `price_sell`, `brand`, `source`
- `data/import-reference-prices.tsv` — invoice costs which only have a supplier reference, description, supplier, invoice, and date

Prices in the file are **before tax**; imported products are assigned tax category `001`, whose rate you set in Stock → Taxes. The sales-screen buttons use `taxesincluded=true`, so the till displays prices with tax.

`import-products.tsv` is the complete scan lookup; it is not bulk-loaded into a
new database. `import-prices.tsv` overlays costs which can already be joined to
an EAN. Invoice lines that cannot yet be joined remain in
`import-reference-prices.tsv` for later reference-based imports. Regenerate the
invoice joins after updating `external-data/preus-cost-factures.csv`:

```sh
python3 data/enrich-prices-from-invoices.py --write
```

To put one model on the till (every size and colour), give the cost before tax and
the ticket price with VAT:

```sh
python3 data/apply-model-price.py Avet 3267 --cost 3.66 --price 5.95
python3 data/apply-model-price.py Avet 3267 --cost 3.66 --price 5.95 --apply --insert
```

`--apply` writes `import-prices.tsv`. `--insert` creates or updates the matching
`PRODUCTS` rows (no stock). Uses `dev.properties` for the database URL.

Selling prices for scan-to-import are offered from the purchase cost rather
than copied from `import-prices.tsv`. Configure the default markup, optional
brand overrides, and rounding under **Stock → Price rules**. The dialog shows
both markup on cost and margin on the tax-included retail price; staff can
override the offered price. Changing a saved rule can optionally reprice only
existing products that still match the previous rule.

### Scan-to-import (preferred at the till)

If a scanned barcode is missing from `PRODUCTS`, the sales screen looks it up in the products TSV and overlays a matching row from `import-prices.tsv`. It then opens an editable create-product dialog prefilled with whatever it found. If neither file contains the barcode, the same dialog opens with only the scanned barcode. Saving creates the database product (and its catalog category if needed); cancelling leaves the database and ticket unchanged. Stock → Products does the same when you search a barcode that is not in the till.

When the catalogue contains several EANs for the same model, the dialog lists
the whole family with every variant selected. Clicking a row opens that
variant's fields, so exceptional costs or selling prices (such as 3XL) can be
edited independently. Saving creates or updates the selected variants. On the sales screen only the
scanned one goes on the receipt; on Stock → Products you stay on the scanned
one.

Importing the catalogue **does not** create stock. Receive goods via Stock diary so `STOCKCURRENT` fills in.

### Bulk helpers in `data/`

Gradle compiles them against Derby. **Quit the POS first** — with embedded Derby the database directory is locked (`db.lck`), and the tasks refuse to run if that file is present.

| Task                                                                                    | What it does                                                                                                                                     |
| --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `./gradlew applyStoreResources`                                                         | Upserts ticket, logo, and button templates and adds discount permissions to Administrator and Manager. Default DB: `data/openbravopos-database`. |
| `./gradlew importCatalog`                                                               | **Wipes** products, stock, and extra categories, then loads both TSVs, sets tax category `001`, and updates `Ticket.Buttons`. Destructive.       |
| `./gradlew keepCatalog --args='data/openbravopos-database path/to/keep.csv'`            | Drops products whose `REFERENCE` does not match codes in a CSV (first column after a header). Unlinks ticket lines instead of deleting history.  |
| `python3 data/apply-model-price.py Avet 3267 --cost 3.66 --price 5.95 --apply --insert` | Sets cost and ticket price on every variant of a model, then inserts them.                                                                       |
| `updateResource` / `dumpResource` / `dumpAllResources` / `showResource`                 | Inspect or replace `RESOURCES` rows. See `./gradlew tasks --group pos`.                                                                          |

`--args` replaces the whole argument list, including the database path. Paths are relative to the repo root.

Example with a different Derby directory:

```sh
./gradlew applyStoreResources --args='data/openbravopos-database'
```

## Differences from upstream

- Scan-to-import: unknown barcodes can be pulled from the catalogue TSV at the till
- Sales keypad: line discount and total discount (Administrator and Manager need `button.discount` and `button.discount.total`; run `./gradlew applyStoreResources` to patch an existing database)
- Extra reports: **Stock by model**, **Sales by model**, **Dead / slow stock**, **Sales by category** (Administrator and Manager roles)
- FlatLaf Light look and feel in Configuration → General

Receipt content, shop name, logo, and on-screen buttons are resources, not code. Edit `src-pos/com/openbravo/pos/templates/` (`Printer.Ticket.xml`, `Printer.TicketPreview.xml`, `Ticket.Buttons.xml`, `Window.Title.txt`) and apply them to the database.

## Source layout

| Path         | Contents                                               |
| ------------ | ------------------------------------------------------ |
| `src-pos/`   | Till UI, sales, inventory, config                      |
| `src-data/`  | Persistence / session layer                            |
| `src-beans/` | Shared beans                                           |
| `reports/`   | Jasper reports (`.jrxml` + `.bs` menu scripts)         |
| `locales/`   | UI translations                                        |
| `lib/`       | Third-party jars still vendored here, plus native libs |
| `data/`      | Local database, TSV catalogue, resource dump tools     |

## Upstream

Original project: [SourceForge Openbravo POS](https://sourceforge.net/projects/openbravopos/). The historical wiki and forums linked from older READMEs are largely stale; treat this repo as the source of truth.

License: GNU GPL v3 (see `COPYING` / `licensing/`).
