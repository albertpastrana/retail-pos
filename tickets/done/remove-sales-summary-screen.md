# Retire the separate sales summary screen

Captured: 2026-10-26

Related: `tickets/todo/reports-welcome-period-navigation.md`, `tickets/in-progress/reports-welcome-screen.md`

## Goal

Remove the separate Sales summary report/screen from the POS, as requested.

## Context

The reports welcome screen already displays sales totals, but currently ends at the last closed day and does not offer an arbitrary date range. Removing the separate report now temporarily removes the ability to see today's sales or choose an arbitrary date range from a sales-total screen. A future custom-range follow-up can restore this; this removal happens ahead of that work. Keep the shared sales summary data model and repository used by the welcome screen and other reports.

Existing database roles can retain the now-obsolete permission without affecting the remaining reports. Fresh installs use the bundled role templates; do not change stored roles or historical migrations just to remove that permission.

## Done when

- The separate Sales summary screen is absent from the reports menu and administration catalogue, including the welcome screen's report links.
- New role templates do not grant the retired screen, while all remaining reports still work for administrator and manager roles.
- Its unused screen class, labels and user documentation are removed; shared sales summary calculations remain available.
- Relevant automated checks pass; any unavailable manual POS checks are reported explicitly.

## Shipped

Removed the separate report from the sales submenu, administration catalogue, bundled role/menu templates, translations, and user docs. The reports welcome screen now lists the six remaining report actions; shared sales-summary calculations remain in use.

Verified with `./gradlew spotlessApply`, targeted workflow/role/layout/Derby migration tests, `./gradlew check`, and `./gradlew ciCheck` with MySQL and PostgreSQL containers (removed afterwards). A hands-on POS check of the reports menu and administration catalogue in English, Spanish, and Catalan at compact size, including keyboard/focus and both themes, was not performed.
