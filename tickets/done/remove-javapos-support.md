# Remove JavaPOS support

Captured: 2026-09-25

Related: `tickets/todo/audit-unused-code-and-dependencies.md`

## Goal

Remove the JavaPOS dependency and JavaPOS-selected hardware paths while keeping
the supported ESC/POS, screen, window, and system-printer paths working.

## Context

JavaPOS was only used by the receipt printer, customer display, and fiscal
printer adapters. The configuration screen exposed `javapos` for up to three
receipt printers and for the display. Existing JavaPOS properties are out of
scope for compatibility; they must be migrated to a supported device type
before upgrading.

The JavaPOS fiscal adapter was removed with the rest of the integration. Fiscal
operations now use the existing null implementation unless another supported
fiscal integration is added later.

## Done when

- The JavaPOS dependency and JavaPOS adapter sources are absent from the build.
- `javapos` is no longer offered or selected by the configuration screen.
- No source, license notice, or supported documentation references JavaPOS.
- The project compiles and existing printer paths remain available.

## Shipped

Removed the JavaPOS dependency, printer/display/fiscal adapters, configuration
options, locale labels, and license notice. Updated the related hardware-support
documentation and audit notes.

Verification: `./gradlew spotlessApply`,
`./gradlew compileJava compileDataHelpersJava compileIntegrationTestJava`, and
`./gradlew check` passed. No manual Swing screen verification was performed.
