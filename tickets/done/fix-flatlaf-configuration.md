# Fix FlatLaf configuration

Captured: 2026-09-25

## Goal

The application always uses FlatLaf Light and the configuration screen no longer exposes a look-and-feel setting.

## Context

Look-and-feel selection currently reads and writes `swing.defaultlaf`, offers the installed Swing look-and-feels, and the launch scripts can force Metal. FlatLaf is the supported application theme, so the setting and obsolete Substance licensing entries are no longer needed.

## Done when

- The configuration screen has no look-and-feel selector.
- Startup and configuration mode always install FlatLaf Light, regardless of system properties or saved configuration files.
- Launch configuration files and scripts do not set `swing.defaultlaf`.
- No active build or licensing reference remains for the obsolete Substance look-and-feel libraries.
- The relevant Java compilation and repository checks pass.

## Shipped

- Fixed FlatLaf Light installation in application and configuration startup.
- Removed the look-and-feel selector, persisted property, launch overrides, and obsolete Substance licensing entries.
- Verification: `./gradlew spotlessApply`, `./gradlew compileJava`, `./gradlew compileDataHelpersJava compileIntegrationTestJava`, and `./gradlew check`.
