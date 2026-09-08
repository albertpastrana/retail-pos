# Retail POS

A point-of-sale application for retail shops: touch-screen sales, ESC/POS receipt printers, customer display, and barcode scanners. Built around a product catalogue with one SKU per barcode, stock per product, and sales reporting — not around tables, courses, or kitchen orders, so it is a poor fit for restaurants.

Forked from [Openbravo POS](https://sourceforge.net/projects/openbravopos/) 2.30.3.

This file is the project entry point. Catalogue variant rules live in [catalog-variants.md](catalog-variants.md).

## Requirements

- JDK 17 or newer to run Gradle (the build still emits Java 8 bytecode)
- The [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) (`./gradlew`); no local Gradle install needed
- Dependencies are already in `lib/` (not resolved from Maven Central)

Ant (`build.xml`) still works if you already use it.

## Build

```sh
./gradlew jar
```

Useful tasks:

| Task              | Result                                             |
| ----------------- | -------------------------------------------------- |
| `./gradlew jar`   | `build/jar/openbravopos.jar`                       |
| `./gradlew check` | Compile, jar, plus `locales.jar` and `reports.jar` |
| `ant jar`         | Same jar layout via the old Ant build              |
| `ant aio-jar`     | Fat jar (`lib.jar` bundled)                        |
| `ant cbits`       | Fat jar + Windows `.exe` via Launch4j              |
| `ant dist.bin`    | Binary zip under `build/dist/`                     |

CI runs `./gradlew check` on every push and pull request.

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

| Key                                     | Purpose                                              |
| --------------------------------------- | ---------------------------------------------------- |
| `db.driver` / `db.URL` / `db.driverlib` | Database (Derby, HSQLDB, MySQL, PostgreSQL, Oracle)  |
| `user.language` / `user.country`        | UI locale; translations live in `locales/`           |
| `machine.printer`                       | `screen` for on-screen tickets, or an ESC/POS device |
| `catalog.import.products`               | Path to the products TSV (scan-to-import)            |
| `catalog.import.categories`             | Path to the categories TSV                           |

First launch against an empty database URL creates the schema. Default users (empty password): Administrator, Manager, Employee, Guest.

**Quit the POS before running any database helper.** With embedded Derby the database directory is locked (`db.lck`); two processes at once fail with a lock error.

## Database

Schema creation scripts for each supported engine are in `src-pos/com/openbravo/pos/scripts/`. Empty sample databases live under `sampledb/`.

Resources the till actually uses (ticket layout, logo, buttons, role XML) live in the `RESOURCES` table, not only in `src-pos/com/openbravo/pos/templates/`. Edit templates in git, then push them into the database (Maintenance → Resources, or the helpers below).

## Catalogue

One sellable SKU per barcode. Colour and size are part of the product row, not attribute sets. Why, and how to group stock and sales by model: [catalog-variants.md](catalog-variants.md).

TSV files (UTF-8, tab-separated, no header):

- `data/import-categories.tsv` — `id`, `name`, optional `parentid`
- `data/import-products.tsv` — `id`, `reference`, `barcode`, `name`, `category_id`, `price_buy`, `price_sell`, optional `brand`

Prices in the file are **before tax**; imported products are assigned tax category `001`, whose rate you set in Stock → Taxes. The sales-screen buttons use `taxesincluded=true`, so the till displays prices with tax.

### Scan-to-import (preferred at the till)

If a scanned barcode is missing from `PRODUCTS` but present in the TSV, the sales screen offers to import that one product (and its category if needed). Set `catalog.import.products` and `catalog.import.categories` in the properties file.

Importing the catalogue **does not** create stock. Receive goods via Stock diary so `STOCKCURRENT` fills in.

### Bulk helpers in `data/`

One-off Java mains. Compile against Derby, **with the POS closed**:

```sh
cd data
javac -encoding UTF-8 -cp ../lib/derby.jar ImportCatalog.java KeepCatalog.java ApplyStoreResources.java
```

| Class                                                | What it does                                                                                                                                    |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `ImportCatalog`                                      | **Wipes** products, stock, and extra categories, then loads both TSVs, sets tax category `001`, and updates `Ticket.Buttons`. Destructive.      |
| `KeepCatalog`                                        | Drops products whose `REFERENCE` does not match codes in a CSV (first column after a header). Unlinks ticket lines instead of deleting history. |
| `ApplyStoreResources`                                | Upserts ticket, logo, and button templates from `src-pos/com/openbravo/pos/templates/` and adds discount permissions to roles                   |
| `UpdateResource`                                     | `db name file` — replace one `RESOURCES` row                                                                                                    |
| `DumpResource` / `DumpAllResources` / `ShowResource` | Inspect `RESOURCES`                                                                                                                             |

Example (paths relative to `data/`):

```sh
java -cp .:../lib/derby.jar ImportCatalog \
  openbravopos-database \
  import-categories.tsv \
  import-products.tsv \
  ../src-pos/com/openbravo/pos/templates/Ticket.Buttons.xml
```

On Windows, use `;` instead of `:` in `-cp`.

## Differences from upstream

- Scan-to-import: unknown barcodes can be pulled from the catalogue TSV at the till
- Sales keypad: line discount and total discount (Administrator and Manager need `button.discount` and `button.discount.total`; run `ApplyStoreResources` to patch an existing database)
- Extra reports: **Stock by model**, **Sales by model**, **Dead / slow stock**, **Sales by category** (Administrator and Manager roles)
- FlatLaf Light look and feel in Configuration → General

Receipt content, shop name, logo, and on-screen buttons are resources, not code. Edit `src-pos/com/openbravo/pos/templates/` (`Printer.Ticket.xml`, `Printer.TicketPreview.xml`, `Ticket.Buttons.xml`, `Window.Title.txt`) and apply them to the database.

## Source layout

| Path         | Contents                                           |
| ------------ | -------------------------------------------------- |
| `src-pos/`   | Till UI, sales, inventory, config                  |
| `src-data/`  | Persistence / session layer                        |
| `src-beans/` | Shared beans                                       |
| `reports/`   | Jasper reports (`.jrxml` + `.bs` menu scripts)     |
| `locales/`   | UI translations                                    |
| `lib/`       | Third-party jars + native libs                     |
| `data/`      | Local database, TSV catalogue, resource dump tools |

## Upstream

Original project: [SourceForge Openbravo POS](https://sourceforge.net/projects/openbravopos/). The historical wiki and forums linked from older READMEs are largely stale; treat this repo as the source of truth.

License: GNU GPL v3 (see `COPYING` / `licensing/`).
