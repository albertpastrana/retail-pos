# Redesign taxes screen

Captured: 2026-09-26

Design reference (wireframe, not binding on implementation details):
https://claude.ai/artifact/VVcAqeg9NENypz3eDmgZND

Mockups: `tickets/todo/redesign-taxes-screen/Main.html` (list and detail, one
tax selected), `tickets/todo/redesign-taxes-screen/Edicio.html` (new tax, with
cascade on and the ticket preview).

Related: `tickets/in-progress/redesign-categories-screen.md`,
`tickets/in-progress/remove-swing-layout-antipatterns.md`,
`tickets/todo/review-table-search-actions.md`

## Goal

`Menu.Taxes` shows what each tax actually does before the shopkeeper edits it:
rate, category, validity and cascade on the list itself, a readable detail
panel for the selected tax, and a form that states in money what "cascade",
"parent tax" and "order" change on a ticket. The screen follows the same chrome
as the products and categories screens — no legacy toolbar, one primary action,
one Save — and drops the absolute positioning in the editor.

## Context

Current screen structure:

- `src-pos/com/openbravo/pos/inventory/TaxPanel.java` — screen controller, a
  plain `JPanelTable` subclass with no filter, no custom list component and the
  default toolbar. `getListCellRenderer()` L81 renders column 1 (`NAME`) only;
  `getVectorer()` L71 already carries `NAME`, `RATE` and `RATEORDER` for search
  and sort, but none of it reaches the screen.
- `src-pos/com/openbravo/pos/inventory/TaxEditor.java` — right-hand record
  editor. `initComponents()` L230 uses `setLayout(null)` (L249) with manual
  `setBounds` for all sixteen children; the text fields are 19px high and the
  date button sits at a hard-coded `x=450`.
- `src-pos/com/openbravo/pos/forms/DataLogicSales.java` — `getTableTaxes()`
  L1542 (`ID`, `NAME`, `CATEGORY`, `VALIDFROM`, `CUSTCATEGORY`, `PARENTID`,
  `RATE`, `RATECASCADE`, `RATEORDER`), `getTaxList()`, `getTaxCategoriesList()`,
  `getTaxCustCategoriesList()`.
- `src-pos/com/openbravo/pos/sales/TaxesLogic.java` L111-L130 — cascade is what
  the rate applies to: `isCascade()` passes the accumulated amount (base plus
  the parent tax) instead of the base.
- `src-pos/db/migration/V1__baseline.sql` L117-131 — `TAXES`, with a
  self-referencing `PARENTID` FK and FKs to both tax-category tables.
- Precedent to follow: `ProductsPanel` (`getFilter()` L94, `showToolbar()` L117,
  `getListComponent()` L132, `confirmDelete()` L174) and the categories screen
  redesign in progress.

### Problems observed

1. **The list does not identify a tax.** A name on its own does not say what the
   tax charges. Worse, the same name legitimately repeats across validity
   periods — `VALIDFROM` exists so a rate change can be scheduled — so two rows
   reading "IVA General" are indistinguishable until you click each one.
2. **Nothing shows which tax is in force.** A tax valid from a future date looks
   exactly like the one being charged today.
3. **Cascade, parent tax and order are unexplained.** Three controls that only
   make sense together, with no indication of what they do to a ticket total.
   Cascade on a 5,20% surcharge over a 21% tax is +6,29 € on a 100,00 € base,
   not +5,20 € — the screen never says so.
4. **The rate field silently accepts the wrong magnitude.** `Formats.PERCENT`
   parses `21` and `21%` as 21%, but `0,21` as 0,21% — the multiplier a person
   might reasonably type is accepted as a valid, almost-zero rate.
5. **Absolute positioning.** `setLayout(null)` plus `setBounds` gives 19px
   fields, ignores window size, and leaves the longer Catalan and Spanish labels
   ("Categoria fiscal de la clienta") against a fixed 220px label column.
6. **Legacy toolbar chrome**, as on the other maintenance screens: first / prev /
   next / last, reload, magnifier, sort, and new / delete / save icon buttons.
7. **Delete is unguarded.** A tax referenced as another tax's `PARENTID` raises a
   raw FK error instead of explaining what blocks it.

## Proposed screen changes

1. **Table instead of a name list.** Replace the default `JListNavigator` with a
   custom list component (`getListComponent()`, as products did): Name, Tax
   category, Rate, Valid from, Order. Rate and order in the monospaced numeric
   style, right-aligned. A cascaded tax shows "Cascaded on <parent>" under its
   name. Sorted by order of application.
2. **Validity is visible.** A tax whose `VALIDFROM` is in the future carries a
   `Scheduled` badge; the row in force carries none. Nothing else changes about
   how validity is stored.
3. **Search box above the table** filtering on name and tax category, matching
   the products and categories screens.
4. **Remove the toolbar** (`showToolbar() → false`) and add a header with a
   single primary `New tax` button, `RetailPOSColors.primaryButton()`.
5. **Detail panel for the selected tax**: name, rate as the headline figure,
   then category, customer category, parent tax, cascade, valid from and order,
   plus a worked example on a 100,00 € base showing base, tax and total.
6. **Edit form grouped into Identification / Validity / Calculation** instead of
   one flat column of eight rows, with a live ticket preview beside it that
   recomputes as the rate, parent and cascade change, and states the difference
   cascade makes.
7. **Replace `setLayout(null)`** with an adaptive layout (`GridBagLayout`, as
   `StockDiaryEditor` did) and delete the generated `setBounds` code. Fields at
   the touch sizes the other rebuilt screens use.
8. **Rate field states its unit**: a `%` suffix inside the field and helper text,
   so the multiplier ambiguity in problem 4 is visible rather than silent.
9. **Parent tax excludes the tax itself** so a cycle cannot be created from the
   UI, and is only enabled when cascade is on.
10. **Delete checks its consequences first.** If the tax is a parent of another,
    say so and name it; otherwise confirm before deleting. Reuse the
    `ProductsPanel.confirmDelete()` pattern.

## Scope decisions

- Display and layout only: no change to `TAXES`, to `TaxesLogic`, or to how tax
  is applied at the till. The worked example on screen calls the existing
  calculation, it does not restate it.
- Tax categories and customer tax categories keep their own screens
  (`TaxCategoriesPanel`, `TaxCustCategoriesPanel`); this ticket does not merge
  them in.
- Light theme only. The dark theme is out of scope here — it is a global
  `RetailPOSTheme` concern, not a per-screen one.
- The mockups are in Catalan because that is the shop's locale; the labels in
  the code stay the existing `AppLocal` keys.

## Done when

- The taxes screen lists name, category, rate, valid-from and order, sorted by
  order of application, with cascaded taxes naming their parent and future-dated
  taxes marked as scheduled.
- Selecting a tax shows a detail panel with a worked example of what it adds to
  a 100,00 € base.
- The edit form is grouped, contains no `null` layout or manual child
  `setBounds`, and shows the resulting ticket total change as the rate, parent
  or cascade change.
- The rate field shows its unit, and entering a multiplier is not silently
  accepted as a rate.
- The parent-tax selector never offers the tax being edited.
- Deleting a tax that is another tax's parent explains what blocks it instead of
  surfacing a database error.
- There is no first/prev/next/last/reload/search/sort toolbar and one primary
  `New tax` button.
- Labels and buttons are not clipped in English, Catalan and Spanish, and the
  screen stays usable below `1024x768`.
- No other `JPanelTable` screen regresses.

## Out of scope (unless separately approved)

- Any change to how taxes are calculated or stored.
- Bulk editing, importing or duplicating taxes.
- Merging the tax-category and customer-tax-category screens into this one.
