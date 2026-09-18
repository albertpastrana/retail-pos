# Retail POS

A point-of-sale application for retail shops: touch-screen sales, ESC/POS receipt printers, customer display, and barcode scanners. Built around a product catalogue with one SKU per barcode, stock per product, and sales reporting — not around tables, courses, or kitchen orders, so it is a poor fit for restaurants.

Forked from [Openbravo POS](https://sourceforge.net/projects/openbravopos/) 2.30.3.

This file is the project entry point. Catalogue variant rules live in [catalog-variants.md](catalog-variants.md). ESC/POS printers (Windows, Linux, macOS USB bridge): [escpos-printer.md](escpos-printer.md). Planned work: [tickets/](tickets/README.md).

## Requirements

- Any JDK to launch the wrapper; Gradle then runs on JDK 21, downloading it if the machine has none (`gradle/gradle-daemon-jvm.properties`). The build still emits Java 8 bytecode. SDKMAN users: `.sdkmanrc` pins Temurin 21 (`sdk env`, or `sdkman_auto_env=true` so `cd` switches it).
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
| `./gradlew check`           | That, plus data helpers, plus `spotlessCheck`               |
| `./gradlew spotlessApply`   | Format Java in the source sets (do not format Java by hand) |
| `./gradlew spotlessCheck`   | Fail if Java is not formatted                               |
| `./gradlew integrationTest` | Flyway against Derby, Compose MySQL, and Compose PostgreSQL |

CI runs `./gradlew spotlessCheck` on its own job, and `./gradlew check integrationTest` on every push and pull request.

`./gradlew jar` copies `retail-pos.jar`, `locales.jar`, and `reports.jar` next to `start.sh`. The `locales/` and `reports/` directories are also on the classpath, so a source checkout still runs after only the app jar is present:

```sh
./start.sh path/to/config.properties
```

On Windows: `start.bat`. Configuration UI: `./configure.sh` or `configure.bat`.

## Releases

Pushing a tag such as `v3.0.0-RC1` starts the GitHub Actions release workflow. It builds portable x86_64 packages for Linux, Windows, and macOS and publishes them with SHA256 checksums to the GitHub Release. Windows and macOS packages include a bundled JRE; the Linux package uses the system Java installation to avoid cross-platform JRE mismatches. The workflow uses `macos-13` for the current Intel macOS package; Apple Silicon and native installers are follow-up work.

## Configuration

Everything below is per-installation: locale, tax rates, receipt layout, shop details. Nothing is baked into the build.

If you pass no argument, the app reads `~/retail-pos.properties`.

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
| `update.check`                          | Set to `false` to disable the startup update check                                                   |
| `update.dir`                            | Directory containing platform packages and matching `.sha256` files for local updates              |
| `update.url`                            | Compatible releases API endpoint; defaults to the project GitHub `latest` release                  |

The update check runs in the background and never prevents startup. When a newer stable release is found, the POS offers to install a verified local package and restart, or to open the remote release page in the system browser. Local updates replace only the application directory; configuration and databases remain external. Update diagnostics are written to `<install>.update.log` next to the application directory, while normal application logs are in `<install>/logs/`.

First launch against an empty database URL runs the Flyway migrations and creates the schema. Default users (empty password): Administrator, Manager, Employee, Guest.

A database created by the old per-engine scripts is adopted on first launch: it is baselined at version 2, so the schema and seed data it already holds are left alone and only later migrations apply. Derby databases still in the pre-10.7 file format are hard-upgraded at the same time, which older Derby versions cannot undo — take a copy of the database directory first.

**Quit the POS before running any database helper.** With embedded Derby the database directory is locked (`db.lck`); two processes at once fail with a lock error.

## Database

Versioned migrations are in `src-pos/db/migration/`. `V1__baseline.sql` is shared by Derby, MySQL, and PostgreSQL; the startup code supplies the few database-specific data types and ticket-number definitions as Flyway placeholders. Binary templates and role permissions are loaded by the versioned Java migration.

Do not edit a migration after it has shipped. Add the next `V<n>__description.sql` or Java migration instead.

To run the same checks as CI, including a new install on MySQL and PostgreSQL
as well as Derby:

```sh
docker compose up -d --wait
./gradlew ciCheck
docker compose down -v
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

`import-products.tsv` is the complete scan lookup and is loaded into the shared
`CATALOG_FALLBACK_PRODUCTS` table for scan-to-import. `import-prices.tsv`
overlays costs in `CATALOG_FALLBACK_PRICES`, indexed by barcode and reference.
Invoice lines that cannot yet be joined remain in
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

If a scanned barcode is missing from `PRODUCTS`, the sales screen looks it up in the products TSV and overlays a matching row from `import-prices.tsv`. When found, it opens an editable create-product dialog prefilled with the catalogue data. Saving creates the database product (and its catalog category if needed); cancelling leaves the database and ticket unchanged. If the barcode is also absent from the TSV, the sales screen does not open a dialog: it shows a notice below the keypad and logs `event=unknown_barcode code="<barcode>"`. Stock → Products still opens the blank create-product dialog when you search for an unknown barcode.

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
| `./gradlew applyStoreResources`                                                         | Upserts ticket, logo, and button templates and adds discount permissions to Administrator and Manager. Default DB: `data/retail-pos-database`. |
| `./gradlew importCatalog`                                                               | **Wipes** products, stock, and extra categories, then loads both TSVs, sets tax category `001`, and updates `Ticket.Buttons`. Destructive.       |
| `./gradlew loadFallbackCatalog --args='JDBC_URL USER PASSWORD categories.tsv products.tsv prices.tsv'` | Replaces the shared fallback tables from TSVs. Does not modify products or stock. |
| `./gradlew keepCatalog --args='data/retail-pos-database path/to/keep.csv'`            | Drops products whose `REFERENCE` does not match codes in a CSV (first column after a header). Unlinks ticket lines instead of deleting history.  |
| `python3 data/apply-model-price.py Avet 3267 --cost 3.66 --price 5.95 --apply --insert` | Sets cost and ticket price on every variant of a model, then inserts them.                                                                       |
| `updateResource` / `dumpResource` / `dumpAllResources` / `showResource`                 | Inspect or replace `RESOURCES` rows. See `./gradlew tasks --group pos`.                                                                          |

`--args` replaces the whole argument list, including the database path. Paths are relative to the repo root.

Example with a different Derby directory:

```sh
./gradlew applyStoreResources --args='data/retail-pos-database'
```

## Differences from upstream

- Scan-to-import: unknown barcodes can be pulled from the catalogue TSV at the till
- Sales keypad: line discount and total discount (Administrator and Manager need `button.discount` and `button.discount.total`; run `./gradlew applyStoreResources` to patch an existing database)
- Management reports are being rebuilt; the former BeanShell/Jasper report menu is no longer installed by the default roles.
- FlatLaf Light look and feel in Configuration → General

Receipt content, shop name, logo, and on-screen buttons are resources, not code. Edit `src-pos/com/openbravo/pos/templates/` (`Printer.Ticket.xml`, `Printer.TicketPreview.xml`, `Ticket.Buttons.xml`, `Window.Title.txt`) and apply them to the database.

## Source layout

| Path         | Contents                                             |
| ------------ | ---------------------------------------------------- |
| `src-pos/`   | Till UI, sales, inventory, config                    |
| `src-data/`  | Persistence / session layer                          |
| `src-beans/` | Shared beans                                         |
| `reports/`   | Retained invoice JRXML templates pending installation audit |
| `locales/`   | UI translations                                      |
| `lib/`       | Native serial-port libraries for supported platforms |
| `data/`      | Local database, TSV catalogue, resource dump tools   |

## Observability

Grafana Alloy configuration for the Linux POS host, PostgreSQL, host metrics,
and rotating POS logs lives in [observability/alloy/](observability/alloy/).
The configuration reads Grafana Cloud and database credentials from
`/etc/alloy/env`; credentials are intentionally not stored in this repository.

## Upstream

Original project: [SourceForge Openbravo POS](https://sourceforge.net/projects/openbravopos/). The historical wiki and forums linked from older READMEs are largely stale; treat this repo as the source of truth.

License: GNU GPL v3 (see `licensing/`).
