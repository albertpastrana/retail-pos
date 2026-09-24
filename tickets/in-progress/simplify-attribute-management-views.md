# Simplify attribute management views

Captured: 2026-09-22

Related: `tickets/todo/remove-swing-layout-antipatterns.md`

## Goal

Simplify the attribute-management workflow by integrating attributes, attribute
values, attribute sets, and attribute usage into a coherent user-facing area
instead of exposing four separate maintenance views.

The existing functionality must remain available for installations that use
product attributes and variants.

## Direction change

No supported installation currently uses the legacy attribute/variant model.
Replace it with product-level characteristics instead of integrating the old
maintenance screens. Each sellable size/colour combination is a normal
product with its own barcode. Characteristics are optional `key = value`
metadata, or a key-only label, edited directly from the product screen.

The migration is intentionally destructive: remove the legacy attribute,
attribute-value, attribute-set, attribute-use, and attribute-instance model
and its runtime code. Do not attempt to infer old variants or barcodes.

Characteristics are stored in `PRODUCT_ATTRIBUTES`. Keys and values are
normalized case-insensitively for comparison while preserving the original
display text. A product may contain a key only once. Existing keys and values
are suggested while entering a new characteristic, but arbitrary new ones
remain valid.

## Context

The current workflow is split across these views:

- `AttributesPanel`: defines product attributes such as size or colour.
- `AttributeValuesPanel`: defines the values belonging to an attribute.
- `AttributeSetsPanel`: defines reusable sets of attributes.
- `AttributeUsePanel`: assigns attributes to a set and controls their order.

The views are implemented as separate `JPanelTable`/`JPanelTable2` screens and
are exposed as separate permissions in `Role.Administrator.xml` and
`Role.Manager.xml`. This makes the relationship between the records difficult
to understand and requires users to navigate between screens to complete one
configuration task.

## Proposed workflow

- Provide one primary **Attributes** management area.
- Allow an attribute's values to be viewed and edited from the attribute
  context.
- Allow an attribute set's members and order to be viewed and edited from the
  attribute-set context.
- Make the relationship between attributes, values, sets, and usage explicit in
  the UI.
- Keep the existing persistence model and product-variant behaviour unless a
  separate migration is explicitly required.
- Preserve the ability to create, edit, delete, and reorder all currently
  supported records, subject to the existing validation and permissions.

## Scope decisions

### Screen proposal

Use one `AttributeManagementPanel` as a master-detail screen. Do not change
the persistence model or the product editor.

```text
+--------------------------------------------------------------------------+
| Product attributes                                                       |
+----------------------------+---------------------------------------------+
| Attributes                 | Selected attribute: Colour                 |
|                            |                                             |
| [New] [Edit] [Delete]      | Name: [ Colour                         ]    |
|                            |                                             |
| > Colour                   | Values                                      |
|   Size                     | [New] [Edit] [Delete]                       |
|                            |                                             |
|                            | Red                                         |
|                            | Blue                                        |
|                            | Green                                       |
+----------------------------+---------------------------------------------+
| Attribute sets                                                           |
| [New] [Edit] [Delete]      Set: [ T-shirt sizes                         ] |
|                            | Members                                     |
|                            | [Move up] [Move down] [Add] [Remove]       |
|                            | 1. Size                                    |
|                            | 2. Colour                                  |
+--------------------------------------------------------------------------+
| [Save] [Cancel]                                                          |
+--------------------------------------------------------------------------+
```

The attribute and attribute-set lists are the two master selectors. The
selected attribute shows and edits its values inline. The selected set shows
and edits its members inline. Creating or editing a record uses the existing
editor behaviour, preferably in a small modal dialog when a form is needed.
The screen must keep the existing keyboard navigation and dirty-state rules.

### Permissions

Add one canonical permission:

`com.openbravo.pos.inventory.AttributeManagementPanel`

Add only this permission to the Administrator and Manager role templates. The
four old permissions must no longer create menu entries. For existing
databases, treat any of the old four permissions as a compatibility grant for
the new panel until roles are saved again; this prevents an upgrade from
removing access. The old class names remain accepted only for this permission
compatibility and are not used as UI entry points.

### Menu

Expose one Stock Management entry using the existing `Menu.Attributes` key,
renamed in the three locale files to the equivalent of "Product attributes".
It opens `AttributeManagementPanel`. Remove the standalone entries and their
old titles from the menu. Keep old localization keys temporarily if they are
needed by stored resources, but do not reference them from the new UI.

### Deletion rules

Deletion is rejected when the record is in use; it is never silently cascaded
by the integrated UI.

- An attribute cannot be deleted when it is referenced by `ATTRIBUTEUSE` or
  `ATTRIBUTEINSTANCE`.
- An attribute value cannot be deleted when its attribute/value is referenced
  by `ATTRIBUTEINSTANCE`.
- An attribute set cannot be deleted when it is referenced by `PRODUCTS` or
  `ATTRIBUTESETINSTANCE`.
- A set's membership rows are owned by the set and may be removed when the
  set itself is deletable.

Perform the checks before saving/deleting and show a localized message that
identifies why deletion is blocked. Keep the database constraints as the
final safeguard. No migration is expected.

### Reordering

Replace direct editing of the order number with selecting a member and using
Move up/Move down buttons. Keyboard shortcuts should be available for these
actions. Save the complete set order in one transaction, renumbering members
to contiguous values starting at 1. This avoids transient collisions with the
unique `(ATTRIBUTESET_ID, LINENO)` index.

### Testing

Add integration coverage for the persistence service/operations using the
existing Derby, MySQL, and PostgreSQL test setup where applicable:

- create, edit, and delete attributes, values, sets, and memberships;
- reject deletion for every in-use case above;
- add, remove, and reorder set members and verify contiguous order;
- load and save a representative product with an attribute set and variant;
- verify the operations are safe to repeat and preserve existing records.

Add focused UI/component tests for selection changes, dirty-state handling,
delete blocking, and reordering. Finish with a manual Administrator workflow
through the integrated menu. Full locale smoke coverage must include English,
Spanish, and Catalan.

### Localization

All new titles, buttons, validation messages, and deletion errors must be
added to `pos_messages.properties`, `pos_messages_es.properties`, and
`pos_messages_ca.properties`. Do not rely on the old standalone menu labels.

Keep product editing and product filtering unchanged unless the integrated
workflow exposes a concrete compatibility defect.

## Done when

- A user can create an attribute and its values without leaving the primary
  attribute workflow.
- A user can create an attribute set, add attributes to it, and change their
  order without opening a separate usage-maintenance screen.
- Existing attribute, value, set, and usage records remain readable and
  editable after the change.
- Existing products using attributes or attribute sets continue to load, edit,
  and save correctly.
- Administrator and Manager permissions expose the integrated workflow without
  leaving obsolete standalone menu entries or permission-only dead ends.
- Validation prevents invalid references and handles deletion of attributes or
  values that are already used by products or sets.
- The workflow is localized in the supported application locales.
- Automated coverage verifies CRUD operations, set membership/order, and a
  representative product using attributes.
- A manual workflow confirms that an administrator can configure a complete
  attribute set from the integrated area.

## Out of scope

- Redesigning the product editor or product catalog search.
- Changing the database schema without a demonstrated need.
- Removing attribute and variant functionality from the application.
