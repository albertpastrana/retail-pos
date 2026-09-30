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

## Slices

1. Reproduce and test the barcode SQL injection path, then replace string concatenation with parameterized SQL and safe regex handling.
2. Restrict or replace Java deserialization and add regression coverage for valid serialized values and rejected classes.
3. Decide whether database-backed Velocity templates are trusted configuration. If they are not strictly trusted, remove method access or replace the scripting mechanism with a constrained renderer.
4. Harden every XML parser against XXE and entity expansion while preserving printer templates and role-permission parsing.
5. Replace private-method reflection in `ProductsEditor` with an explicit typed API and document/whitelist remaining dynamic class loading.
6. Design a password-hash migration from plain/SHA-1 formats to a modern salted password KDF without locking out existing installations.
7. Replace database-password obfuscation with authenticated modern encryption or a protected secret-store/configuration approach, including migration and failure handling.

## Done when

- Barcode input cannot alter the SQL statement and malformed or regex-special input is handled safely.
- Untrusted serialized data cannot instantiate arbitrary classes or cause uncontrolled resource consumption.
- Database-backed templates cannot invoke arbitrary application methods, or the trust boundary is explicit, enforced, and tested.
- Both SAX parser paths reject external entities and unsafe DTDs, with tests covering malicious XML.
- Dynamic class loading is restricted to supported classes and locations, and private reflection has been removed from application code.
- Existing password formats are migrated or safely verified using a modern password KDF; empty and plaintext credentials are no longer accepted for new or changed passwords.
- Database credentials are protected with authenticated encryption or an explicitly protected external secret mechanism.
- Relevant unit, integration, database, printer, login, configuration, and packaging checks pass for supported environments.
