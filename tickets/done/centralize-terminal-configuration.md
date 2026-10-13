# Centralize terminal configuration

Captured: 2026-10-13

## Goal

Give each POS terminal a stable generated identity and store its operational
configuration in the shared database instead of using hostname-keyed XML
resources or a large local properties file.

## Context

On first launch, generate a random UUID when `terminal.id` is absent and save
it in the local properties file. The UUID, database driver, database URL,
database user, database password, and demo bootstrap values remain local so
the application can connect to the database.

After connecting, load the terminal record and its typed settings from the
database. The database is required for normal startup: if it is unavailable,
the application must not continue and must show a clear actionable error that
identifies the database connection failure.

Replace hostname-based terminal resource keys with the generated terminal ID.
Migrate existing `<hostname>/properties` data, including the active cash and
location values, to the new terminal record where possible. Keep the previous
hostname as diagnostic metadata only, not as an identifier.

Reduce local terminal settings to bootstrap concerns. Move stable terminal
settings such as display, printer, scanner, screen mode, menu state, locale,
and number/date formats to the database. Filesystem paths remain local when
they refer to a terminal-specific filesystem; backup policy and ownership are
shared configuration.

Use typed database columns for stable terminal settings rather than storing a
serialized XML `Properties` document. Keep terminal identity separate from
terminal settings, and enforce uniqueness of the terminal ID.

Backups must have one shared policy and one designated central terminal. The
shared database stores whether backups are enabled, scheduling, retention, and
the responsible terminal. The central terminal stores its local backup path.
Prevent duplicate execution with a database lock or lease.

## Done when

- A new installation generates and persists a stable `terminal.id` UUID.
- Existing hostname-keyed terminal state is migrated without losing active
  cash or location when it can be unambiguously matched.
- Database connection properties are the only required operational settings
  kept locally, apart from demo/bootstrap values and genuinely local paths.
- Terminal settings are stored in typed database columns associated with the
  terminal UUID.
- No runtime terminal state depends on hostname-based resource names.
- The application refuses to start without a database connection and shows a
  clear reason to the operator.
- Locale and formatting settings are loaded before the UI is built and can be
  configured consistently from the database.
- Backup policy is shared, only the designated central terminal runs the
  scheduled backup, and duplicate execution is prevented.
- Derby, MySQL, and PostgreSQL migration paths are covered for empty and
  existing databases, including a second migration run.
- English, Spanish, and Catalan startup and configuration errors remain
  understandable.

## Shipped

- `terminal.id` is generated and saved locally before connection; bootstrap
  preferences remain intact until they have been imported. The legacy hostname
  is recorded for diagnosis, while a single claimant imports the old resource,
  active cash, and location. Subsequent till state is keyed by UUID.
- `V52__terminal_configuration.sql` adds separate terminal identity, typed
  settings, and shared backup policy tables. The migration leaves historic
  resource rows untouched and validates legacy location and open cash before
  adopting them; partial pre-existing schemas also remain migratable. Legacy
  adoption happens at the first connection because only that terminal has its
  newly generated local UUID.
- Startup loads typed locale, formats, hardware and screen preferences before
  constructing Swing screens; unavailable databases stop startup with a
  translated, actionable error. Configuration can still edit connection
  settings while the database is unavailable.
- Scheduled backups run only on the central terminal after the configured hour,
  using an atomic 24-hour lease and shared last-run date. Retention is shared;
  the backup directory and executable path stay on the terminal.
- Verification: `./gradlew spotlessApply`, `./gradlew ciCheck` with Derby,
  MySQL and PostgreSQL (including repeat migrations), and automated
  configuration-label visibility and error-text checks in English, Spanish
  and Catalan. Database containers were removed with `docker compose down -v`.
