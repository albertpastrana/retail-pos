# Replace legacy attributes with product characteristics

Captured: 2026-09-22

## Goal

Replace the unused legacy attribute and variant maintenance model with optional
product-level characteristics edited directly from the product screen.

Each sellable size or colour combination remains a normal product with its own
barcode. Characteristics are optional `key = value` metadata, or a key-only
label.

## Context

No supported installation currently uses the legacy attribute/variant model.
The migration is intentionally destructive: it removes the legacy attribute,
attribute-value, attribute-set, attribute-use, and attribute-instance tables and
runtime code without attempting to infer old variants or barcodes.

Characteristics are stored in `PRODUCT_ATTRIBUTES`. Keys and values are
normalized case-insensitively for comparison while preserving display text. A
product may contain each key only once. Existing keys and values are suggested
while entering a characteristic, but arbitrary new values remain valid.

## Done when

- Product editing allows adding, editing, removing, and saving characteristics.
- Key-only characteristics and key/value characteristics are supported.
- Duplicate keys are rejected case-insensitively.
- Existing keys and values are suggested from the shared database.
- Product characteristics load and save transactionally.
- The legacy attribute and variant runtime model, menu entries, permissions,
  and schema are removed by migration.
- Migration and persistence coverage verifies case-insensitive keys and the
  destructive schema change on Derby.
- English, Spanish, and Catalan labels are provided for the new workflow.

## Shipped

- Added `ProductAttributesPanel` to the product editor.
- Added `V43__product_attributes` and updated `migration-checksums.sha256`.
- Removed the legacy attribute, attribute-set, usage, and variant UI/runtime
  paths and their menu and role entries.
- Added localized characteristic controls and persistence coverage.
- Fixed the unshipped Derby migration by removing a duplicate index drop.

Key paths: `src-pos/com/openbravo/pos/inventory/ProductAttributesPanel.java`,
`src-pos/db/migration/V43__product_attributes.java`, and
`src/integrationTest/java/com/openbravo/pos/forms/AttributeManagementPersistenceTest.java`.
