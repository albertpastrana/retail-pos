# Portable packaging and releases

Captured: 2026-09-17

## Goal

Publish downloadable Openbravo POS releases automatically from version tags, with a portable package for Linux, Windows, and macOS.

## Context

The application is a Gradle-built Java Swing desktop application. The current build emits Java 8 bytecode but runs the build with JDK 21. The package must include a JRE so operators do not need to install Java separately.

The work is split into two phases:

1. **Portable packages:** produce x86_64 `.tar.gz` for Linux, `.zip` for Windows, and `.tar.gz` for macOS. Include the application jars, runtime dependencies, platform libraries, launchers, and a JRE. A `v*` tag creates a GitHub Release and uploads the packages and checksums.
2. **Native installers:** evaluate and add AppImage/deb/rpm for Linux, MSI or an equivalent installer for Windows, and DMG/PKG for macOS. Add platform signing and notarization where applicable, and validate native printer support on each target architecture.

The first phase targets x86_64. Apple Silicon and Linux ARM64 need separate native-library validation because the current RXTX library set does not clearly provide those architectures.

Phase 1 implementation uses `scripts/package-portable.sh`, a bundled JRE produced with `jlink`, and `.github/workflows/release.yml`. The workflow packages on Linux, Windows, and Intel macOS, then publishes the archives and checksums to GitHub Releases for `v*` tags.

## Done when

- Pushing a `v*` tag runs the existing verification and release packaging workflow.
- The GitHub Release contains one downloadable portable package for Linux, Windows, and macOS.
- Each package starts with its bundled JRE and does not require a system Java installation.
- Platform-specific RXTX libraries and the existing licensing files are present in the relevant packages.
- SHA256 checksums are attached to the release.
- The release and packaging steps are documented for maintainers.
