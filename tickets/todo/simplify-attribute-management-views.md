# Simplify attribute management views

Captured: 2026-09-22

Related: `tickets/todo/remove-swing-layout-antipatterns.md`

## Goal

Simplify the attribute-management workflow by integrating attributes, attribute
values, attribute sets, and attribute usage into a coherent user-facing area
instead of exposing four separate maintenance views.

The existing functionality must remain available for installations that use
product attributes and variants.

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

- Decide whether the integrated area is one screen with nested panels, a
  master-detail workflow, or a primary screen with dedicated dialogs.
- Review whether `AttributeUsePanel` should become an embedded set-members
  editor rather than a standalone view.
- Update the menu and role permissions so users are not presented with
  duplicate or orphaned entries.
- Preserve localization, keyboard navigation, dirty-state handling, and
  save/cancel behaviour.
- Keep product editing and product filtering unchanged unless integration
  requires a concrete compatibility change.

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
