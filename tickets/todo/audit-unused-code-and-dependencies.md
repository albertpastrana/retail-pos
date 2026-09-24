# Audit unused code and dependencies

Captured: 2026-09-20

## Goal

Identify and safely remove code, resources, and declared dependencies that are no longer used, without breaking configuration-driven features, database migrations, optional hardware integrations, installed databases, or external integrations.

## Context

The repository contains legacy Swing utilities, remnants of the old management-reporting stack, optional hardware/payment integrations, database-seeded resources, and a manually curated dependency list with transitive resolution disabled. A static call-graph audit therefore produces both real orphan candidates and false positives.

The audit found the following high-confidence code candidates with no normal callers or usages:

- `src-pos/com/openbravo/pos/forms/BeanFactoryCache.java`
- `src-data/com/openbravo/data/gui/CompoundIcon.java`
- `src-data/com/openbravo/data/gui/JListData.java`
- `src-pos/com/openbravo/pos/util/LabelIcon.java`
- `src-pos/com/openbravo/pos/util/CurrencyChange.java`
- `src-pos/com/openbravo/pos/util/BarcodeImage.java`
- `DataLogicSales.FallbackPrice.priceSell`

The following methods also appear unused:

- `ListQBFModelNumber.getNonMandatoryString()`
- `ListQBFModelNumber.getNonMandatoryNumber()`

`BarcodeImage` is separate from the active Barcode4J usage in `PrintItemBarcode`; its removal must not remove Barcode4J itself.

The following declared dependencies in `build.gradle.kts` have no application imports or other repository references and are high-confidence removal candidates:

- `commons-beanutils:commons-beanutils:1.9.4`
- `commons-digester:commons-digester:1.8.1`
- `com.lowagie:itext:2.1.0`
- `jfree:jcommon:1.0.16`
- `jfree:jfreechart:1.0.13`
- `org.apache.poi:poi:3.2-FINAL`
- `org.eclipse.jdt:core:3.1.1`
- `org.swinglabs:swingx:0.9.5`

The following dependencies are used directly or may be required at runtime and must not be removed based only on import searches:

- Derby, Flyway, Flyway MySQL, MySQL, and PostgreSQL drivers.
- Barbecue, Barcode4J, Velocity, FlatLaf, and JavaPOS.
- Axis, JAX-RPC, SAAJ, WSDL4J, Commons Logging, Commons Discovery, Commons Collections, Commons Lang, and ORO.
- Commons Codec.

The project disables transitive dependency resolution for most implementation dependencies, so apparently indirect libraries may be required by Velocity or Axis. `ImportLegacyTransactions.java` also references HSQLDB without a declared HSQLDB dependency; this is a possible missing feature-specific runtime dependency, not an unused dependency.

## Code and resource candidates

Review these items after confirming all dynamic and external uses:

- `reports/com/openbravo/reports/*.jrxml`, especially the invoice and custom invoice templates.
- `src-pos/com/openbravo/pos/templates/printerfiscalticket.xml`.
- `src-pos/com/openbravo/pos/templates/ticketline_taxesincluded.xml`.
- `src-pos/com/openbravo/pos/templates/printerproduct.xml`.
- `data/ImportLegacyTransactions.java`.

The JRXML files are still packaged in `reports.jar`. `JPanelTicket.printReport()` can be exposed through an optional script bridge, and deployed databases may contain custom event scripts that reference it. The invoice templates must therefore remain until installed database resources have been audited.

## False-positive risks

Do not classify these as unused without targeted verification:

- Flyway migrations under `src-pos/db/migration/`, which are discovered by naming convention.
- Classes loaded with `Class.forName`, including database-specific bean factories and look-and-feel classes.
- Payment gateway implementations selected through `PaymentGatewayFac` and configuration.
- JavaPOS, ESC/POS, scale, drawer, and display implementations selected by configuration.
- The Velocity engine selected by `ScriptFactory`.
- `Printer.*`, `Role.*`, `Menu.Root`, and other resources loaded from or inserted into the `RESOURCES` database table.
- Native libraries under `lib/`, launch scripts, `locales.jar`, and `reports.jar`.
- Public or extension-facing methods with no in-repository callers.

## Investigation slices

1. Confirm every code candidate with graph references, repository-wide searches, reflection/configuration review, and a check for external/public API expectations.
2. For each dependency candidate, remove it temporarily in an isolated change and run compilation plus the relevant runtime tests.
3. Inspect the resolved runtime classpath and use `dependencyInsight` to distinguish direct requirements from manually declared support jars.
4. Exercise or test Velocity ticket rendering, barcode generation, FlatLaf, JavaPOS, Axis/PayPoint, and all supported database drivers.
5. Audit deployed database `RESOURCES` rows and event scripts before deleting JRXML or database-configured templates.
6. Remove confirmed dead classes, methods, resources, and dependencies in small reviewable commits.
7. Document retained legacy or optional dependencies with the runtime feature that requires them.

## Verification

For dependency and code removals, run at minimum:

```sh
./gradlew compileJava compileDataHelpersJava compileIntegrationTestJava
./gradlew check
docker compose up -d --wait
./gradlew integrationTest
docker compose down -v
```

Also verify:

- `./gradlew dependencies --configuration runtimeClasspath`
- `./gradlew dependencyInsight --dependency velocity --configuration runtimeClasspath`
- `./gradlew dependencyInsight --dependency axis --configuration runtimeClasspath`
- `./gradlew dependencyInsight --dependency commons-collections --configuration runtimeClasspath`
- application packaging and all launchers
- normal sale, receipt preview, reprint, cash closing, drawer, and printer flows
- Derby, MySQL, and PostgreSQL migration paths
- the legacy import helper, if it remains supported

## Done when

- Every candidate is classified as removed, retained with a documented reason, or requiring an external installation check.
- The eight high-confidence dependency candidates are removed or have explicit evidence explaining why they remain.
- Confirmed orphan classes and methods are removed without deleting active package functionality.
- No active configuration, reflection, migration, script, database resource, hardware, payment, or packaging path regresses.
- The runtime classpath contains only required dependencies for supported features.
- The HSQLDB requirement for `ImportLegacyTransactions.java` is either declared and tested or the helper is explicitly retired.
- Database and application integration checks pass on Derby, MySQL, and PostgreSQL.
