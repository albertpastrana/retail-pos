# Review table search actions

Captured: 2026-09-21

Related: `tickets/todo/remove-swing-layout-antipatterns.md`

## Goal

Review whether the toolbar search action backed by `JFind` is still useful on each management table, and remove or retain it based on the current user workflows.

## Context

`JFind` is opened by the search button in `JNavigator`, which is created by `JPanelTable`. The action is currently available on these screens:

- People (`PeoplePanel`)
- Roles (`RolesPanel`)
- Resources (`ResourcesPanel`)
- Products (`ProductsPanel`)
- Categories (`CategoriesPanel`)
- Taxes (`TaxPanel`)
- Tax categories (`TaxCategoriesPanel`)
- Customer tax categories (`TaxCustCategoriesPanel`)
- Stock diary (`StockDiaryPanel`)
- Attributes (`AttributesPanel`)
- Attribute values (`AttributeValuesPanel`)
- Attribute sets (`AttributeSetsPanel`)
- Attribute usage (`AttributeUsePanel`)
- Auxiliaries (`AuxiliarPanel`)

The search action may be redundant on some screens, particularly where the table is small or another product/filter search already exists. Do not change the action until each screen's workflow, dataset size, permissions, keyboard access, and localization have been checked.

## Done when

- Every listed screen is classified as retaining or removing the search action, with a short user-facing reason.
- Removing the action does not leave an empty toolbar gap or break toolbar layout, keyboard navigation, or accessibility.
- Product search and any other dedicated filtering flows remain unchanged unless explicitly included in the decision.
- The retained search action is covered by a concrete manual workflow on at least one representative screen.
- Relevant automated checks pass.
