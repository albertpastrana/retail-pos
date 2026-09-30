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
- `Base64Encoder.encode(byte[])` (the class remains used by `ResourcesView`)

An additional class-level candidate was found during the follow-up graph and
repository-wide search and removed on 2026-09-25:

- `src-pos/com/openbravo/pos/ticket/Signumprovider.java`

The class and its `addPositive`/`addNegative` methods had no in-repository
callers. Its removal still requires checking external plugins or installed
extensions because it was a public class.

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

On 2026-09-30, removed twelve unreferenced PNG assets from
`src-beans/com/openbravo/images`: the eight unused alternate keypad states,
`color_line16.png`, `gohome.png`, and the superseded `menu-left.png` /
`menu-right.png` sidebar icons. The earlier review identified 69 PNGs without
textual repository references; these twelve are now removed, while the
remaining candidates stay pending targeted packaging, database-resource, and
external-extension checks. Do not classify all remaining PNGs as referenced.

On 2026-09-25, removed the seven high-confidence unused Gradle dependencies from
`build.gradle.kts`. SwingX was already removed by the main-menu change. The
remaining legacy support dependencies are retained because they may be required
by configuration-driven or indirect runtime integrations. Compilation and
`check` pass; database integration is pending because the Docker daemon is not
running in the current environment.

On 2026-09-25, removed the confirmed unused classes, methods, and
`DataLogicSales.FallbackPrice.priceSell`. `spotlessApply` and the focused Java
compilation tasks pass. External extension compatibility and database/runtime
integration checks remain pending.

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

## src-beans audit (2026-09-26)

The first repository-wide reference pass classified the contents of
`src-beans` as follows:

- `com.openbravo.basic.BasicException` is active throughout `src-data` and
  `src-pos`.
- `com.openbravo.format.Formats` and `DoubleUtils` are active. The legacy
  `FormatsConstrain`, `FormatsException`, `FormatsRESOURCE`, and
  `FormatsValidate` classes have no callers outside their own definitions and
  are removal candidates, pending public API and external extension checks.
- `com.openbravo.beans` is active. Calendar and time panels are reached from
  `JCalendarDialog`; `JFlowPanel`, `JNumberKeys`, `JPasswordDialog`,
  `LocaleResources`, `RoundedBorder`, and the related event classes have
  current callers. `DateUtils` needs a method-level audit rather than a
  package-level removal.
- `com.openbravo.editor.JEditorCurrencyPositive` is active in
  `JPaymentDebt`, with `JEditorNumber`, `JEditorAbstract`, `EditorComponent`,
  and `com.openbravo.format.Formats` as its implementation dependencies. The
  other editor widgets (`JEditorString`, `JEditorPassword`, `JEditorDouble`,
  `JEditorDoublePositive`, `JEditorIntegerPositive`, `JEditorCurrency`, and
  `JEditorStringNumber`) have no repository callers. `JEditorKeys` and the
  `EditorKeys` interface also have no active construction or registration path;
  they should be removed or explicitly retained only after checking external
  consumers.
- Of the 178 PNG files under `com/openbravo/images`, 109 have a textual
  repository reference and 69 have none. The 69 no-reference candidates are
  mostly old menu, KDE/desktop, calculator, report, and branding assets. This
  is only a static result: database-seeded resources are loaded from
  `RESOURCES`, while migrations seed templates under `pos/templates`, not
  these image files. The no-reference list must still be checked against
  installed database rows and external plugins before deletion.

No `src-beans` files were removed by this pass. The next slice should delete
only confirmed orphan editor/format classes and image candidates in a separate
reviewable change, then run compilation, packaging, and the database/resource
integration checks.

## Necessity review (2026-09-26)

`BasicException` is not merely an unused-looking wrapper. It is the checked
exception in the public contracts of the data loader (`SentenceList`,
`DataResultSet`, `DataWrite`, `DataRead`, `BaseSentence`, and related APIs),
and it is caught or asserted by current POS code and integration tests. It has
no special runtime behaviour beyond `Exception`, so it could be replaced by a
different exception type, but doing so would require a broad API and catch-site
migration. Decision: retain it as necessary current architecture.

The PostgreSQL database configured by `dev-pg.properties` was inspected on
2026-09-26. Its `RESOURCES` image rows are `Button.OpenDrawer`, `Button.Print`,
`Printer.Ticket.Logo`, and `Window.Logo`; none refers to a file under
`src-beans/com/openbravo/images`. The current database therefore adds no
retention requirement for the 69 image files with no repository reference.
The rows remain database-backed application resources and must not be removed
as part of this `src-beans` cleanup.

Necessity decisions from the current repository and database are:

- Retain `BasicException`, active `Formats`, `DoubleUtils`, and the active
  `beans` components.
- Retain the `JEditorCurrencyPositive` dependency chain used by
  `JPaymentDebt`.
- Mark the other editor widgets, `JEditorKeys`, `EditorKeys`, and the four
  unused format helper classes as removable candidates, subject to the
  external extension/API check.
- Mark the 69 unreferenced `src-beans` PNG files as removable candidates.
  No current code or current database row requires them, but removal should
  still be made as a separate change with packaging verification.
