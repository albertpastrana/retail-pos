# Audit insecure code patterns

Captured: 2026-09-25

Related: `tickets/in-progress/audit-unused-code-and-dependencies.md`

## Goal

Identify and remediate insecure source-code patterns that can enable SQL
injection, code execution, unsafe deserialization, XML entity expansion, weak
authentication, or unsafe dynamic class loading.

## Context

The initial source audit found the following candidates:

- SQL is concatenated with barcode input in `src-pos/com/openbravo/pos/inventory/BFrame.java:193-198` and executed by `SQLQueryer` with a `Statement`.
- `ImageUtils.readSerializable` uses `ObjectInputStream.readObject()` without an object filter in `src-data/com/openbravo/data/loader/ImageUtils.java:132-148`. It is used by `DatasSERIALIZABLE` for database values.
- `ScriptEngineVelocity.eval` evaluates templates loaded from database resources in `src-pos/com/openbravo/pos/scripting/ScriptEngineVelocity.java:80-98`.
- `TicketParser` and `AppUser` create SAX parsers without disabling external entities in `src-pos/com/openbravo/pos/printer/TicketParser.java:79-83` and `src-pos/com/openbravo/pos/forms/AppUser.java:163-167`.
- `AppViewConnection` loads a driver class and optional driver library from configuration in `src-pos/com/openbravo/pos/forms/AppViewConnection.java:50-60`.
- `ProductsEditor` accesses a private method through `setAccessible(true)` in `src-pos/com/openbravo/pos/inventory/ProductsEditor.java:104-107`.
- `JRootApp` and `BeanFactoryData` instantiate configured or database-specific classes through reflection in `src-pos/com/openbravo/pos/forms/JRootApp.java:312-320` and `src-pos/com/openbravo/pos/forms/BeanFactoryData.java:38-43`.
- `Hashcypher` permits `plain:` passwords, unsalted SHA-1, and empty passwords in `src-pos/com/openbravo/pos/util/Hashcypher.java:37-63`.
- `AltEncrypter` uses deterministic `DESEDE/ECB/PKCS5Padding` with a key derived from a fixed phrase and database username in `src-pos/com/openbravo/pos/util/AltEncrypter.java:32-45`.

Reflection used for JDBC drivers, database-specific factories, and packaged
application restart or backup flows is not automatically a vulnerability. Each
such use must be classified as intentional, restricted, or replaceable before
being changed. Preserve compatibility with Derby, MySQL, PostgreSQL, installed
databases, ticket resources, and configured integrations.

## Related workstreams

The independent workstreams are tracked separately:

- `tickets/todo/security-sql-injection.md`
- `tickets/todo/security-deserialization.md`
- `tickets/todo/security-template-execution.md`
- `tickets/todo/security-xml-parsers.md`
- `tickets/todo/security-dynamic-loading.md`
- `tickets/todo/security-password-hashing.md`
- `tickets/todo/security-database-credentials.md`

## Done when

- Each related security workstream has an owner, a decision, and observable verification.
- The related tickets are completed without regressions to supported databases, printer resources, authentication, or configured integrations.
