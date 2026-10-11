# Do not offer missing fallback categories

Captured: 2026-10-11

## Goal

When a fallback product refers to a category that does not exist locally, the
import dialog must leave the category unselected and require the operator to
choose an existing category before creating the product.

## Context

The import dialog currently adds a synthetic category from the fallback data
when the category ID is not present in the local category list. That value
cannot be persisted and causes product creation to fail. Do not add fallback
categories that are not available locally. Keep the existing required-field
validation and do not change fallback catalog data.

## Done when

- A missing fallback category does not appear in the import dialog.
- The category selection is empty for that product or variant.
- Confirming without choosing an existing category remains blocked.
- Existing fallback products with valid local categories retain their selected
  category.
- Relevant tests pass.

## Implementation and verification

The import dialog now only selects categories from the local category list;
missing fallback categories leave the selection empty. Added focused coverage
for missing and available category IDs. `./gradlew spotlessApply` and
`./gradlew integrationTest --tests com.openbravo.pos.inventory.CatalogVariantModelTest`
pass.

## Shipped

Updated `CatalogImportDialog` and its integration coverage so unavailable
fallback categories are not offered or persisted.
