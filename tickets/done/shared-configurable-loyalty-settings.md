# Shared configurable loyalty settings

Captured: 2026-09-25

## Goal

Store loyalty configuration once per shared database and make the loyalty calculation values configurable for every till.

## Context

The current configuration is serialized in `RESOURCES` under a machine-hostname key. Loyalty is shared by all tills, so hostname-scoped resource properties are the wrong storage boundary.

Move the settings to a singleton `LOYALTY_SETTINGS` row. Keep the current defaults: loyalty disabled when no setting exists, an empty display name, 10.00 units of eligible spend per stamp, and 5.00 units for a redemption. Use currency-neutral names because amounts follow the store currency.

The customer balance remains manual and is not added by this change. Therefore, the number of stamps required for redemption is not a setting yet; the cashier still decides when to apply a redemption.

## Done when

- Fresh and upgraded databases contain one shared loyalty settings row with the current defaults.
- Existing hostname-keyed loyalty properties are migrated without losing enabled state or display name.
- All tills read and write the same loyalty settings.
- Configuration exposes enabled state, display name, eligible spend per stamp, and redemption value.
- Loyalty calculations use the configured monetary values rather than hardcoded euro values.
- Missing or invalid settings safely use the documented defaults.
- Focused migration and loyalty tests pass.

## Shipped

Added `V47__shared_loyalty_settings` and legacy-property migration, shared runtime persistence, configurable Loyalty settings fields in English, Spanish, and Catalan, ticket-level rule snapshots, and currency-aware loyalty receipt output. The test database's previous `V47` history row was removed before applying the new migration. The Loyalty settings layout was adjusted for translated labels and scaled text fields. Verified with the migration checksum check and focused Derby/loyalty tests.
