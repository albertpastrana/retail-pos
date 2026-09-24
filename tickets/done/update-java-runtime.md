# Update Java runtime and bytecode

Captured: 2026-09-24

## Goal

Make Java 21 the required build, bytecode, and packaged runtime version for every supported portable package.

## Context

The build and CI already use JDK 21, but the application is still compiled as Java 8 bytecode and Linux portable packages use the system Java installation. RXTX native support has been removed, so the application no longer needs the old runtime compatibility target.

## Done when

- Java compilation targets and documents Java 21.
- Linux, Windows, and macOS portable packages include and launch with a Java 21 runtime.
- CI verifies the Java 21 build and packaged launchers.
- Documentation no longer describes Java 8 bytecode or a Linux system-Java requirement.

## Shipped

Java 21 is now the Gradle toolchain, compilation target, and runtime for all portable packages. Packaging rejects non-Java-21 `jlink` binaries to prevent producing an incompatible runtime.
