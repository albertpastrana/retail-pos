# Move core till buttons out of database resources

Captured: 2026-09-18

## Goal

Keep stable, built-in till actions such as print ticket and open drawer in the application code (or packaged resources), while retaining database-backed resources only for configuration that genuinely needs per-installation customization.

## Context

`Ticket.Buttons.xml` is versioned in the repository but copied into the `RESOURCES` table and parsed at runtime by `JPanelButtons`. The database therefore controls the presence and presentation of core actions, while Java still implements their behavior. This creates configuration drift and requires migrations such as `V28` to repair the stored resource.

Role permissions such as `button.print` are a separate concern and must continue to work. Receipt layouts and other operator-customizable templates may still belong in `RESOURCES`.

## Requirements

- Identify which sales-screen buttons are core UI and which are intentionally configurable.
- Move core button definitions out of `RESOURCES`, without moving their action or permission logic out of Java.
- Preserve `button.print` and related role permissions for existing users.
- Keep receipt layouts, logos, and other genuinely customizable resources database-backed.
- Define a safe path for existing databases and remove any migration that is only needed to repair the old button resource.
- Add tests covering a fresh database and an existing database upgraded through the migration path.

## Done when

- Core till buttons render without requiring `Ticket.Buttons` in `RESOURCES`.
- Existing role permissions still enable or disable the corresponding actions.
- Customizable resources remain editable through the existing resource mechanism.
- Fresh and upgraded Derby, MySQL, and PostgreSQL databases pass the relevant tests.
- Documentation explains the boundary between code-owned UI and database-owned configuration.

## Parked

This is backlog work because the current resource mechanism is functional and the migration boundary should be designed before changing persisted installations.
