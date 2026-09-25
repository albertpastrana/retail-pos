# Audit unused code and dependencies

Captured: 2026-09-20

## Goal

Identify and safely remove code, resources, and declared dependencies that are no longer used, without breaking configuration-driven features, database migrations, optional hardware integrations, installed databases, or external integrations.

## Context

The repository contains legacy Swing utilities, remnants of the old management-reporting stack, optional hardware/payment integrations, database-seeded resources, and a manually curated dependency list with transitive resolution disabled. A static call-graph audit therefore produces both real orphan candidates and false positives.

The audit found the following high-confidence code candidates with no normal callers or usages. They were removed on 2026-09-25:

- `src-pos/com/openbravo/pos/forms/BeanFactoryCache.java`
- `src-data/com/openbravo/data/gui/CompoundIcon.java`
- `src-data/com/openbravo/data/gui/JListData.java`
- `src-pos/com/openbravo/pos/util/LabelIcon.java`
- `src-pos/com/openbravo/pos/util/CurrencyChange.java`
- `src-pos/com/openbravo/pos/util/BarcodeImage.java`
- `DataLogicSales.FallbackPrice.priceSell`

The following unused methods were also removed on 2026-09-25:

- `ListQBFModelNumber.getNonMandatoryString()`
- `ListQBFModelNumber.getNonMandatoryNumber()`

The public `AppView.getBean(String)` lookup is also being retired. There are no
in-repository callers that require the untyped API after the dynamic task
lookups are migrated to typed-by-name lookup. The internal class-name
resolution remains in `JRootApp` for menu tasks and bean factories.

`BarcodeImage` is separate from the active Barcode4J usage in `PrintItemBarcode`; its removal must not remove Barcode4J itself.

The following declared dependencies in `build.gradle.kts` have no application imports or other repository references and are high-confidence removal candidates:

- `commons-beanutils:commons-beanutils:1.9.4`
- `commons-digester:commons-digester:1.8.1`
- `com.lowagie:itext:2.1.0`
- `jfree:jcommon:1.0.16`
- `jfree:jfreechart:1.0.13`
- `org.apache.poi:poi:3.2-FINAL`
- `org.eclipse.jdt:core:3.1.1`
- `org.swinglabs:swingx:0.9.5` (removed; the main menu now uses plain Swing components)

The following dependencies are used directly or may be required at runtime and must not be removed based only on import searches:

- Derby, Flyway, Flyway MySQL, MySQL, and PostgreSQL drivers.
- Barbecue, Barcode4J, Velocity, and FlatLaf.
- Axis, JAX-RPC, SAAJ, WSDL4J, Commons Logging, Commons Discovery, Commons Collections, Commons Lang, and ORO.
- Commons Codec.

The project disables transitive dependency resolution for most implementation dependencies, so apparently indirect libraries may be required by Velocity or Axis. `ImportLegacyTransactions.java` also references HSQLDB without a declared HSQLDB dependency; this is a possible missing feature-specific runtime dependency, not an unused dependency.

## Code and resource removals

The following items were removed on 2026-09-25 after repository-wide reference
and packaging checks:

- `reports/com/openbravo/reports/*.jrxml`, including the invoice and custom invoice templates.
- `src-pos/com/openbravo/pos/templates/printerfiscalticket.xml`.
- `src-pos/com/openbravo/pos/templates/printerproduct.xml` (already absent from the tree).
- `data/ImportLegacyTransactions.java`.

The JRXML files were not referenced by current Java code or runtime
dependencies. The `reports.jar` packaging path was removed together with the
templates. Fiscal receipt support remains in `TicketParser`, but the unused
repository template was removed; existing database-backed resources must be
audited separately before changing installed databases.

## False-positive risks

Do not classify these as unused without targeted verification:

- Flyway migrations under `src-pos/db/migration/`, which are discovered by naming convention.
- Classes loaded with `Class.forName`, including database-specific bean factories and look-and-feel classes.
- Payment gateway implementations selected through `PaymentGatewayFac` and configuration.
- ESC/POS, scale, drawer, and display implementations selected by configuration.
- The Velocity engine selected by `ScriptFactory`.
- `Printer.*`, `Role.*`, `Menu.Root`, and other resources loaded from or inserted into the `RESOURCES` database table.
- Native libraries under `lib/`, launch scripts, and `locales.jar`.
- Public or extension-facing methods with no in-repository callers.

## Investigation slices

1. Confirm every code candidate with graph references, repository-wide searches, reflection/configuration review, and a check for external/public API expectations.
2. For each dependency candidate, remove it temporarily in an isolated change and run compilation plus the relevant runtime tests.
3. Inspect the resolved runtime classpath and use `dependencyInsight` to distinguish direct requirements from manually declared support jars.
4. Exercise or test Velocity ticket rendering, barcode generation, FlatLaf, Axis/PayPoint, and all supported database drivers.
5. Audit deployed database `RESOURCES` rows and event scripts before changing database-configured templates.
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

## Progress

On 2026-09-25, removed the seven high-confidence unused Gradle dependencies from
`build.gradle.kts`. SwingX was already removed by the main-menu change. The
remaining legacy support dependencies are retained because they may be required
by configuration-driven or indirect runtime integrations. Compilation and
`check` pass; database integration is pending because the Docker daemon is not
running in the current environment.

## Dependency inventory

The remaining declared dependencies have been classified as follows:

| Dependency group | Evidence | Decision |
| --- | --- | --- |
| Derby, Flyway Core, Flyway MySQL, MySQL JDBC, PostgreSQL JDBC | Database migration, embedded database, data helpers, and configured JDBC drivers | Retain |
| Barbecue and Barcode4J | Product and receipt barcode generation | Retain |
| Commons Codec | `Base64Encoder` uses the Base64 API | Retain; consider `java.util.Base64` replacement separately |
| FlatLaf | Look-and-feel configuration and theme code | Retain |
| Velocity | `ScriptEngineVelocity` renders configured ticket scripts | Retain |
| JUnit Jupiter, AssertJ, JUnit Platform Launcher | Integration test source and Gradle test execution | Retain |
| Spotless and Foojay toolchain resolver | Gradle formatting and JDK toolchain setup | Retain |
| Commons Collections, Commons Discovery, Commons Lang, Commons Logging, ORO | No direct application imports; possible legacy Axis/Velocity runtime support | Retain pending runtime validation |
| Axis, Axis JAX-RPC, Axis SAAJ, WSDL4J | No direct application imports; possible configured PayPoint or SOAP integration | Retain pending runtime validation |

The pending groups must be exercised through the supported legacy integrations
before removal. The next validation should cover Velocity ticket rendering,
Axis/PayPoint or SOAP flows, barcode generation, all configured database
drivers, application packaging, and the normal sale and receipt flows.
