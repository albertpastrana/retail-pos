# Application updates

Captured: 2026-09-17

Related: `tickets/in-progress/portable-packaging-and-releases.md`

## Goal

Check for a newer published application release without delaying startup, notify the operator, and let them open the release download page.

## Context

The first implementation uses the GitHub Releases API for `latest`. The endpoint is configurable with `update.url`, and checks can be disabled with `update.check=false`. Network failures are silent and must never prevent the POS from starting. A local update directory configured with `update.dir` contains portable archives and their `.sha256` files.

Local packages are installed by an external updater after the POS exits. The updater replaces only the application directory, keeps operator configuration and databases outside it, verifies the package checksum, and rolls back if replacement fails. GitHub updates still open the release page because private-release authentication and asset download credentials are not bundled into the application.

## Done when

- Startup remains usable when the update server is slow, unavailable, or malformed.
- A newer stable release produces a clear notification with an action to open its download page.
- The current version is not reported as an update, including versions with a leading `v`.
- Update checks can be disabled or pointed at another compatible endpoint through the POS configuration.
- A local package can be selected and installed without manually extracting it.
