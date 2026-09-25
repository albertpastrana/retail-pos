# Redesign categories screen

Captured: 2026-09-25

Design reference (wireframe, not binding on implementation details):
https://claude.ai/artifact/TzPRG67okhCeXAiFBBcEkM

Related: `tickets/done/redesign-products-screen.md`,
`tickets/todo/remove-swing-layout-antipatterns.md`,
`tickets/todo/improve-stock-diary-editor-layout.md`

## Goal

Make the categories management screen usable with the 138 categories the shop
actually has: show the hierarchy instead of a flat list of bare names, bring the
screen in line with the products screen's chrome (no legacy toolbar, one primary
action, one Save), replace the two truncated catalog buttons with an explained
block, and stop `Delete` from failing with a raw SQL constraint error.

## Context

Current screen structure:

- `src-pos/com/openbravo/pos/inventory/CategoriesPanel.java` — screen
  controller, a plain `JPanelTable` subclass with no filter, no custom list
  component and the default toolbar.
- `src-pos/com/openbravo/pos/inventory/CategoriesEditor.java` — right-hand
  record editor. `initComponents()` L182 uses `setLayout(null)` (L193) with
  manual `setBounds` for every child.
- `src-pos/com/openbravo/pos/ticket/CategoryPathList.java` — already renames a
  flat category list to full paths ("Batins Dona / Clàssics"); used today only
  by the parent combo (`DataLogicSales.getCategoriesList()` L846).
- `src-pos/com/openbravo/pos/forms/DataLogicSales.java` — `getRootCategories()`
  L754, `getSubcategories()` L759, `getCategorySiblings()` L856,
  `getCatalogCategoryAdd()` L1508, `getCatalogCategoryDel()` L1513,
  `getTableCategories()` L1519.
- `src-pos/db/migration/V1__baseline.sql` L96-103 — `CATEGORIES.PARENTID` has a
  self-referencing FK; `PRODUCTS.CATEGORY` references it too.
- Precedent to follow: `ProductsPanel` (`getFilter()` L94 for the header,
  `showToolbar()` L117, `getListComponent()` L132, `confirmDelete()` L174) and
  `ProductTableNavigator` as an example of a custom list component.

### Problems observed

1. **The list does not identify a category.** `CategoriesPanel.getListCellRenderer()`
   L70 renders column 1 (`NAME`) only. Since `V12__category_name_unique_per_parent`,
   a name is unique only among siblings — `CategoriesEditor.checkNameFreeAmongSiblings()`
   L162 exists precisely so "Clàssics" can live under both "Batins Dona" and
   "Batins Home". With 138 flat rows the operator cannot tell which one they are
   editing. The parent combo already shows full paths; the list does not.
2. **Legacy toolbar chrome.** First/prev/next/last, reload, magnifier, sort, and
   the new/delete/save icon buttons are still there. The products screen dropped
   all of this.
3. **Absolute positioning.** `setLayout(null)` plus `setBounds` clips the
   "Imatge" and "Categoria" labels, escapes the record counter, and truncates
   both catalog buttons to "Afegeix al cat…" / "Suprimeix del …".
4. **The catalog buttons are unexplained and unguarded.** They run against the
   database immediately, with no confirmation, no count of what they affect, and
   no feedback afterwards; "add" is a `DELETE` followed by an `INSERT`. Nothing
   on screen says what "the catalog" is.
5. **Delete fails on the FK.** Deleting a category that has subcategories or
   products raises a raw constraint error instead of explaining what blocks it.
6. **No search** over 138 categories, and the parent combo offers the category's
   own descendants, which allows a cycle (`CategoryPathList.pathOf()` carries a
   `visited` guard for exactly that).

## Proposed screen changes

1. **Tree instead of flat list.** Replace the default `JListNavigator` with a
   custom list component (`getListComponent()`, as products did) showing a
   collapsible tree: roots, then children, built from `getRootCategories()` /
   `getSubcategories()`. Each row shows the name plus a `subcategories · products`
   count.
2. **Search box above the tree**, filtering as you type on the full path
   (debounced, ~300ms, matching the products screen); matches keep their
   ancestors expanded, and a "Replega-ho tot" control collapses the tree.
3. **Remove the toolbar** (`showToolbar() → false`) and add a header with a
   single primary `+ Nova categoria` button, styled with
   `RetailPOSColors.primaryButton()` like the products screen.
4. **Editor shows the path.** The full path ("Roba de casa / Batins Dona /")
   above the name, so the record on screen is identifiable.
5. **Replace `setLayout(null)`** with an adaptive layout (`GridBagLayout`, as
   `StockDiaryEditor` did) and delete the generated `setBounds` code.
6. **Parent combo excludes the category itself and its descendants**, so a cycle
   cannot be created from the UI.
7. **Catalog block instead of two buttons**: a short explanation of what the
   sale-screen catalog is, the current state ("18 de 24 productes són al
   catàleg"), and the two actions with full labels. Removing products from the
   catalog asks for confirmation; both actions report the result.
8. **Editor footer**: `Elimina la categoria` on the left in `danger`, and a
   single `Desa els canvis` primary button on the right.
9. **Delete checks its consequences first.** If the category has subcategories or
   products, explain what blocks it and offer to move the content; if it is
   empty, ask for a plain confirmation before deleting. Reuse the
   `ProductsPanel.confirmDelete()` pattern.

## Already covered

The unsaved-changes guard is no longer screen-specific: `BrowsableEditableData`
now calls `confirmPendingChanges()` (L278) before every navigation, so this
screen inherits it. It only needs verifying against the new tree component, not
reimplementing.

## Scope decisions

- The tree is a display concern: `CATEGORIES` already stores `PARENTID`, so no
  schema change is needed. Reparenting stays where it is today — the parent combo
  in the editor — rather than drag-and-drop in the tree.
- "Move the content to…" in the blocked-delete dialog is desirable but is the
  largest piece here; if it does not fit, ship the blocked-delete explanation
  without the move action and capture the move as its own ticket.
- Decide whether the tree component belongs next to `ProductTableNavigator` in
  `inventory` or in `src-data` for reuse; nothing else needs it today, so
  `inventory` is the default.

## Done when

- The categories screen shows a searchable tree with counts, no
  first/prev/next/last/reload/search/sort toolbar, one `+ Nova categoria`
  primary button, and one Save at the bottom of the editor.
- The editor shows the selected category's full path and contains no `null`
  layout or manual child `setBounds`.
- The parent combo never offers the category itself or any of its descendants.
- The catalog actions state what they affect and how many products, and removing
  products from the catalog is confirmed first.
- Deleting a category with subcategories or products explains what blocks it
  instead of surfacing a database error; deleting an empty one is confirmed
  first.
- Labels and buttons are not clipped in English, Catalan and Spanish, and the
  screen stays usable below `1024x768`.
- No other `JPanelTable` screen regresses.

## Out of scope (unless separately approved)

- Drag-and-drop reparenting in the tree.
- Bulk editing of categories, or editing products from this screen.
- Changing the `CATEGORIES` schema or the catalog (`PRODUCTS_CAT`) data model.

## Implementation Notes

- The category list is a self-contained Swing `JTree` in
  `src-pos/com/openbravo/pos/inventory/CategoryTreeNavigator.java`. It is a
  view over the existing `BrowsableEditableData` list, so other `JPanelTable`
  screens keep their existing navigation contract.
- Category deletion is blocked with a consequence message when child categories
  or products exist; moving content remains a separate follow-up as allowed by
  the scope decision.
- Automated verification passed with `./gradlew spotlessApply`,
  `./gradlew compileJava`, `./gradlew test`, and `git diff --check`.
- Fixed the tree-table startup crash caused by treating its `CategoryNode` model
  root as a `DefaultMutableTreeNode`.
- Fixed the follow-up startup crash when the selection-path lookup visited the
  tree-table's technical root, which has no category ID.
- Fixed tree-table rendering so category rows use the category name and the
  expandable handles/indentation are retained in the first column.
- Fixed the tree-table renderer row offset so each table row paints its own
  category instead of repeating the first row and its selection background.
- Replaced the embedded tree paint renderer with an explicit first-column
  renderer that draws the category name, indentation, and expand/collapse
  indicator while the backing tree continues to manage visible rows.
- The category header is left-aligned; compact `Subcat.` and `Prod.` headers are
  right-aligned with full explanatory tooltips, and their count columns use
  narrow fixed bounds.
- The category navigator uses a plain `JTree` with name-only rows, a uniform
  background, no category icons, and search controls scoped to the tree panel.
- The search shows filtered/total results and a link-style collapse action;
  the split gives the tree 32% of the available width.
- Manual verification of the Swing screen at `1024x768` in English, Spanish,
  and Catalan is still required before moving this ticket to `done/`.
