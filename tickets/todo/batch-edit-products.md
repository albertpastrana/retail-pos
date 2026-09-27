# Batch edit products in place

Captured: 2026-09-27

Earlier design reference (superseded by the decisions below):
https://claude.ai/artifact/9z34kzuS8r7NNMF5G6q2is

Mockups (HTML and rendered PNG; `1-seleccio.png` is the visual reference for
the batch-selection state, while behaviour decisions below remain authoritative):
`tickets/todo/batch-edit-products/1-seleccio.html` (multi-selection plus the
batch strip), `2-edicio-cella.html` (editing one cell, fill-down offer),
`3-canvis-pendents.html` (pending changes in the grid, one row locked),
`4-desat.html` (saved, with one row that failed).

Related: `tickets/todo/redesign-admin-screens.md`,
`tickets/todo/review-table-search-actions.md`,
`tickets/todo/simplify-product-search.md`

## Goal

A shopkeeper who has just filtered the products screen by family — the same
garment in four sizes — can change the **sale price** and the **category** of
all of them without opening each product in turn. The table shows the selected
rows and previews price/category changes while the right-hand pane edits the
batch. One Apply writes the selected changes to the database, with per-product
failures available to retry and successful edits available to undo.

## Context

Current screen structure:

- `src-pos/com/openbravo/pos/inventory/ProductsPanel.java` — screen controller,
  a `JPanelTable2`. `getFilter()` L94 builds the header with the filter and the
  `+ Nou producte` button, `showToolbar()` L117 is already false,
  `getListComponent()` L132 returns the custom list.
- `src-pos/com/openbravo/pos/inventory/ProductTableNavigator.java` — the list.
  `JTable` in `ListSelectionModel.SINGLE_SELECTION` (L32) over a read-only
  `AbstractTableModel` (three columns: Referència, Nom, PVP; no `setValueAt`,
  no `isCellEditable`). PVP is computed for display only, net price times the
  tax rate from `TaxesLogic` (L105-L108).
- `src-data/com/openbravo/data/user/BrowsableEditableData.java` — the editing
  model behind every maintenance screen: one current record, one editor,
  `saveData()` L259. The whole screen is built around editing a single row at
  a time.
- `src-pos/com/openbravo/pos/forms/DataLogicSales.java` — `getProductCatQBF()`
  L1267 (the row shape: `[1]` reference, `[3]` name, `[6]` PRICESELL net,
  `[7]` category, `[8]` tax category), `getProductCatUpdate()` L1306, which
  updates the whole product row plus `PRODUCTS_CAT` inside a transaction.
- `src-pos/com/openbravo/pos/inventory/ProductPriceMath.java` —
  `grossFromNet()` L97 and `netFromGross()` L104, already used by the product
  editor to move between the tax-inclusive price a shopkeeper types and the net
  `PRICESELL` stored in the database.
- `src-pos/com/openbravo/pos/inventory/PriceRuleService.java` — per-brand price
  rules; `findForBrand()` L40 and `repriceProductsUsingOldRule()` L147 derive a
  product's price from its brand rule. Products priced this way are the "locked"
  case in the mockups.

### Problems observed

1. **Editing the same field on four sizes takes four full round trips.** Select
   a row, edit the form on the right, save, select the next. For a family of
   sizes or colours — the normal shape of this shop's catalogue — every price
   change is repeated work.
2. **The list is single-selection**, so there is no way to even express "these
   four".
3. **A percentage increase has no path at all.** "Put everything in this family
   up 5%" means opening each product and doing the arithmetic by hand.
4. **Nothing shows what a set of products has in common** before you change it,
   so a shopkeeper cannot tell whether four rows already share a category.

## Proposed screen changes

1. **Multi-selection with a checkbox column.** `ProductTableNavigator` uses
   `MULTIPLE_INTERVAL_SELECTION` and a leading `Boolean` column plus a header
   select-all. The list occupies half the screen beside either editor. Only
   checkbox, reference, name, category and price are visible; tax category
   stays in the row for tax-inclusive price calculations. Selection is local
   to the table rather than `BrowsableEditableData`.
2. **The grid is a selection and preview surface.** Sale price and category
   are both edited only in the right-hand batch pane. At half width, editing
   cells in the grid obscures the before/after values; reference and name
   identify the row.
4. **The right-hand batch editor replaces the single-product editor** while
   2+ rows are selected. It shows the selected references, category, price,
   fixed / `+ %` / `+ €` price modes, shared or mixed current values, an
   `Apply to N` action and clear selection. Editing the value previews the
   proposed old/new values in selected rows without writing them; Apply writes
   them immediately. Apply and Discard live only in the right-hand pane.
5. **One confirmation.** Before Apply, a changed cell previews the new value
   with the previous one struck through; nothing is written during preview.
   Apply saves per product; only failed rows remain pending for retry. Discard
   clears a proposal or failed changes without reversing successful saves.
6. **Locked rows are excluded and say so.** A product whose price comes from a
   brand price rule (`PriceRuleService.findForBrand`) is not repriced by a
   batch edit: mark it in the grid and leave it out of the count, rather than
   silently overriding the rule.
7. **Saving is per product, not one big transaction.** Each row goes through
   the conditional, field-limited `ProductBatchUpdate`; a row that fails keeps its pending
   change, is marked in the grid with the reason, and does not stop the others.
    Apply then offers `Reintenta N canvis`, rebasing conflicts before retrying.
8. **Undo the whole edit.** After a successful save, the banner offers
   `Desfés l'edició` for the operation as a unit (restore the previous values
   of every row saved in that batch) for as long as the banner is visible.
9. **One primary action per state.** `+ Nou producte` becomes a secondary
    button on this screen so that `Aplica als N` / `Reintenta N canvis` is the
    only `brand`-coloured control while a batch edit is in progress.

## Scope decisions

- Two fields only in this ticket: sale price and category. The same mechanism
  should extend to tax category, brand and supplier later; do not build those
  now, but do not hard-code the two either.
- With 0–1 checked rows and no pending batch edits, the right-hand product
  editor stays as it is. With 2+ checked rows the right pane swaps to the batch
  editor; both panes remain 50/50. While edits are pending the batch pane stays
  available even if the selection is cleared, so Discard is always accessible.
- No database or schema change: batch persistence uses the conditional,
  field-limited `ProductBatchUpdate` described below.
- Prices are typed tax-inclusive, as the rest of the screen already shows them,
  and converted with `ProductPriceMath` before storing.
- Light theme only; the mockups are in Catalan because that is the shop's
  locale, but labels in code stay `AppLocal` keys.

## Implementation decisions

- A rule locks a price only when the product's current price actually matches
  the rule's calculated price (the service returns a fallback rule for every
  brand). Categories remain editable on those products.
- Do not use `getProductCatUpdate()` for batch saves: the QBF list projects
  `IMAGE` as NULL and a whole-row update would erase product images and
  overwrite unrelated concurrent edits. Use a conditional, field-limited
  update for price/category with per-product conflict detection instead.
- Undo keeps the previous values in memory while the saved banner is visible;
  each reversal checks the saved values before writing back. Reload-and-retry
  explicitly rebases conflicted fields without overwriting unrelated edits.
- On entering batch mode with a dirty single-product editor: carry a price
  and/or category draft into pending and offer its value explicitly for the
  selection. If another field is dirty, ask to save or discard those other
  fields first. When both kinds are dirty, ask only about the other fields and
  retain the price/category draft whichever option is chosen. Leaving batch
  mode never prompts: carried drafts and failed changes remain pending until
  Apply/Retry or Discard.
- Both columns are read-only at 50% table width. Category and price remain
  visible in the grid; both are editable exclusively in the right-hand pane.
- Category and price have independent controls in the batch pane: changing
  either or both previews those fields together, without a field selector.
  Percentage changes round the resulting tax-inclusive price using each
  product's brand price rule; fixed prices and absolute increments retain
  cent rounding. After Apply the proposal controls reset.
- In the selection state, follow `1-seleccio.png`: a compact batch heading,
  selected-reference chips, an explicit "No change" category choice, separate
  category/price current-value hints, adjacent fixed/percent/amount price
  modes, and clear/apply actions at opposite ends of the bottom bar. Preserve
  the existing editable filter and pending-change workflow.
- Review feedback: show entire references on content-sized rounded chips
  (scroll horizontally when needed); put Clear selection in the list footer by
  the selection instead of the batch editor; show a short, declarative preview
  only when a proposed change exists, without a question mark or explanatory
  instruction appended to it. Keep pending/save feedback available.
- Clear selection stays in the list footer; Apply and Discard occupy the right
  pane and status feedback stays above the footer.
- The list-footer status is only for actionable feedback (pending changes,
  errors, saved/failed counts); hide the status row when there is nothing to
  report, regardless of whether products are selected.
- The price column heading in the products grid is "PVP" in every locale and
  its width is slightly reduced, leaving more room for names/categories.
- Apply and Discard are located only in the right-hand pane; the grid's PVP
  and category cells are never editable. A price is struck through only if
  its displayed tax-inclusive value changes; a changed category shows both
  names with the previous one struck through. Applying the original value
  removes that field's pending change.
- Apply is the only save action for the batch: it writes directly to the
  database after the live preview. Remove the separate Save and reload/retry
  buttons. If individual rows fail, keep only those rows pending; Apply changes
  to Retry and rebases conflicts before saving them. Undo saved changes remains
  a distinct reversal action. Discard clears unconfirmed input and failed
  changes, never already-saved rows. A carried dirty-editor price/category
  draft remains pending until Apply is pressed.

## Verification still required

- Implementation is on branch `batch-edit-products`; Derby integration tests
  cover conditional saves, unrelated columns, rule-priced locks, the four
  dirty-editor handoff cases and a Swing component flow for preview, applying
  price and category, a concurrent failure, retry and undo.
- Manually check Products at and below 1024x768 in English, Catalan and Spanish:
  select-all, category and price editing in the right pane, the three price
  modes, unsaved-editor handoff, live preview,
  pending and failed rows, retry and undo, resizing/focus, and the unchanged
  single-product editor. The ticket stays open until those observable checks
  pass. The Swing integration harness tests the components offscreen, but has
  not launched the full application. Docker is unavailable locally, so the
   complete external-database `ciCheck` could not run; `check` and the inventory
   integration tests passed.
- The batch editor now offers independent category and price controls, with
  compact references, clearer localized labels, and brand-rule rounding for
  percentage changes. The offscreen Swing test covers simultaneous price and
  category proposals and different brand-rounding settings. `spotlessApply`,
  `check`, and the inventory integration tests passed locally. Full-screen
  manual review (including focus, resizing and translated labels) remains
  required before this ticket can be closed.
- The selection-state wireframe has been reflected in the batch pane: compact
  heading, horizontally scrollable reference chips, "No change" category
  default, separate category/price hints, touch-sized price-mode buttons, and
  clear/apply actions at opposite ends of the footer. The filter remains
  editable above the split rather than becoming a static active-filter chip.
  `spotlessApply`, `check`, and inventory integration tests pass in EN/CA/ES
  offscreen layout checks. Full application visual comparison against
  `1-seleccio.png` at supported sizes/scales is still required.
- Refined the selection-state feedback: reference chips display full labels
  and use rounded, token-coloured outlines with horizontal scrolling; Clear
  selection now sits in the list footer where the empty pending message was.
  The preview is blank without a proposal and otherwise names only the
  proposed category/price, without a question mark or instructional suffix.
  `spotlessApply`, `check`, inventory integration tests and `git diff --check`
  pass; visual confirmation in the running Products screen remains pending.
- Earlier offscreen layout checks covered the former list footer at a 900px
  split width in English, Catalan and Spanish; the new one-action layout needs
  the same checks. Live-screen visual verification is still pending.
- Removed the redundant empty "No pending changes" message. The footer status
  row is hidden without pending changes or feedback and appears for pending
  counts, errors and save results. Offscreen Swing coverage checks empty and
  selected states as well as the pending state; `spotlessApply`, `check` and
  inventory integration tests pass.
- The grid now labels its price column "PVP" in all three locales and uses an
  80px preferred / 90px maximum column width; the offscreen Swing test passes.
  Visual confirmation in the running Products screen is still pending.
- Removed inline cell editing and fill-down. Apply and Discard are both in the
  right-hand batch pane. With pending edits,
  the batch pane stays available even after clearing the selection so Discard
  is reachable. Price previews equal to the displayed original no longer
  stage or strike through a price; category previews show the new and old
  category, and choosing the original category removes its pending change.
  `spotlessApply`, `check`, and inventory integration tests pass, including
  preview/apply/discard/retry/undo coverage. The full Products screen still
  needs manual visual and keyboard verification.
- Apply now immediately writes the previewed category/price changes through
  `ProductBatchUpdate`; there is no separate Save or reload/retry action. Only
  failed products remain pending, and the same primary button becomes Retry
  (rebasing conflicts). The button also saves a carried single-product draft.
  Discard clears a live proposal or failed changes without reversing earlier
  successes; Undo still reverses successful saves. Derby/Swing coverage checks
  the database before and after Apply, partial failure, Retry, Undo, the draft
  handoff, and discard of an unconfirmed preview. `spotlessApply`, `check`, and
  inventory integration tests pass. Running-screen verification is pending.

## Done when

- The products list supports multi-selection with checkboxes and a select-all
  in the header.
- PVP and category can only be edited in the right-hand batch panel; the grid
  shows the old value struck through only for fields whose displayed value
  changes.
- The right-hand editor applies a fixed price, a percentage or an absolute
  increment to selected rows, and previews what it will do before Apply.
- A proposed value previews old and new without writing. Apply saves directly,
  and only failed rows keep their before/after display until Retry or Discard.
- Saving reports how many products were updated, marks any row that failed with
  its reason, keeps that row's change pending, and offers a retry.
- A product priced by a brand rule is visibly excluded from a batch price
  change instead of being silently overwritten.
- The existing single-product editor still works unchanged, including on a row
  that was just batch-edited.
- No other `JPanelTable` screen regresses; labels are not clipped in English,
  Catalan and Spanish, and the screen stays usable below `1024x768`.

## Out of scope (unless separately approved)

- Editing reference, barcode or name in bulk.
- Importing or exporting products as a spreadsheet.
- Applying a batch edit to a filter result larger than the loaded page without
  an explicit "select all N results" affordance — this ticket is selection-based.
