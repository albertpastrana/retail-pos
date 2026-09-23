# Add desktop UI end-to-end tests

Captured: 2026-09-23

## Goal

Add a repeatable end-to-end test suite that launches the Retail POS desktop application, interacts with its Swing UI, and verifies complete till workflows against an isolated test database.

## Context

The application is a Java Swing desktop application, so browser tools such as Playwright and Cypress are not a direct fit. Evaluate AssertJ Swing or Jemmy first; use `java.awt.Robot` only where component-level interaction is not sufficient.

The existing `src/integrationTest/` suite covers database and service behaviour but does not launch the application or click through the UI. Tests should use an isolated Derby database and the existing configuration mechanism (`RETAIL_POS_CONFIG`). Avoid production databases, real printers, and real payment gateways.

The first slice should cover starting the application, signing in, selecting or scanning a product, adding it to a ticket, completing a cash payment, and verifying the saved sale. Add a stable way to locate important controls without relying on screen coordinates. CI will need a virtual display such as Xvfb on Linux; macOS runs may require Accessibility permissions.

## Done when

- A documented Gradle task runs the desktop E2E suite separately from the existing integration tests.
- The suite starts and stops the real POS application using an isolated, disposable database and test configuration.
- A test completes a representative sale through the UI and verifies the resulting ticket and payment data.
- The test can run reliably without a physical printer, scanner, display, or payment terminal.
- Failures capture enough diagnostics to investigate them, including application logs and a screenshot when available.
- The suite runs in CI with a virtual display and does not access production configuration or data.

## Parked

Intentionally deferred until there is a need for regression coverage of complete operator workflows. Unit and database integration tests remain the current testing strategy.
