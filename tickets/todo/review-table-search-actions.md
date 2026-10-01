# Review table search actions

Captured: 2026-09-21

Related: `tickets/in-progress/remove-swing-layout-antipatterns.md`
Related: `tickets/done/simplify-attribute-management-views.md`

## Goal

Review whether the toolbar search action backed by `JFind` is still useful on each management table, and remove or retain it based on the current user workflows.

## Context

`JFind` is opened by the search button in `JNavigator`. `JPanelTable` only adds
that button when the panel supplies a `Vectorer` and keeps its toolbar visible.

The original screen inventory is stale:

- People (`PeoplePanel`), roles (`RolesPanel`), resources (`ResourcesPanel`), products (`ProductsPanel`), categories (`CategoriesPanel`), and stock diary (`StockDiaryPanel`) explicitly hide the shared toolbar, so they do not currently expose `JFind`.
- Attributes (`AttributesPanel`), attribute values (`AttributeValuesPanel`), attribute sets (`AttributeSetsPanel`), and attribute usage (`AttributeUsePanel`) were removed by `tickets/done/simplify-attribute-management-views.md`.

The remaining screens that currently expose the action are:

- Taxes (`TaxPanel`)
- Tax categories (`TaxCategoriesPanel`)
- Customer tax categories (`TaxCustCategoriesPanel`)
- Auxiliaries (`AuxiliarPanel`)

`ProductsPanel` has a dedicated product filter, and `AuxiliarPanel` has a
dedicated product filter. They must remain unchanged while this ticket is
reviewed. `JNavigator` currently makes the search button non-focusable and does
not provide a documented keyboard shortcut, so keyboard and accessibility
behaviour must be checked before retaining or removing the action.

## Remaining work

- Classify `TaxPanel`, `TaxCategoriesPanel`, `TaxCustCategoriesPanel`, and `AuxiliarPanel` as retain or remove, based on the cashier or administrator workflow, expected dataset size, and the fields searched by `JFind`.
- Confirm the relevant permissions and whether users can reach each table without relying on the toolbar action.
- Check keyboard access, focus order, button tooltip/icon meaning, and the no-match message on a retained screen.
- Check English, Spanish, and Catalan labels in `JFind` and any replacement or retained toolbar controls.
- If removing the action, change the smallest shared or screen-specific surface and verify that no empty toolbar gap or broken separator remains.
- If retaining it, document one concrete search workflow and add the narrowest automated regression coverage available.
- Run the relevant Java formatting and checks after the decision is implemented.

## Done when

- Every applicable screen is classified as retaining or removing the search action, with a short user-facing reason; obsolete and already-hidden screens are recorded as such.
- Removing the action does not leave an empty toolbar gap or break toolbar layout, keyboard navigation, or accessibility.
- Product search and any other dedicated filtering flows remain unchanged unless explicitly included in the decision.
- The retained search action is covered by a concrete manual workflow on at least one representative screen.
- Relevant automated checks pass.
