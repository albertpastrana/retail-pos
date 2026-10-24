# Align the replenishment screen with the design system

Captured: 2026-10-24

Related: `tickets/done/customer-orders-delivered.md`
Related: `tickets/todo/replenishment-next-step-status-action.md`
Related: `tickets/todo/replenishment-bulk-status-actions.md`

## Goal

The replenishment and customer orders screen (`ReplenishmentPanel`) follows the
Retail POS design system (`design-system/README.md`,
`design-system/swing-development.md`): one clear primary action, semantic
colour only with its meaning, tabular figures for codes, consistent inputs and
touch-sized rows. Behaviour does not change; this ticket is visual and copy only.

## Context

A screen review on 2026-10-24 (Catalan UI, light theme) found:

1. **Selected row reads as an error.** The renderer uses
   `table.getSelectionBackground()`, which renders as a salmon/red tint next to
   the warning/info/success row tints. Selection must not look like `danger` or
   `warning`. Use a neutral selected state built from the surface scale plus
   `borderStrong()` (outline or left bar), keeping text `ink()`. Selection must
   stay distinguishable from every status tint in both themes.
2. **No primary action.** `+ New request` and `Save` are only bold. Use
   `RetailPOSColors.primaryButton(...)` for exactly one primary action per
   state: `Save` while the form is dirty or creating; otherwise the next-step
   action from `tickets/todo/replenishment-next-step-status-action.md` once that
   lands. `+ New request` becomes a normal secondary button. The active filter
   toggle may keep the brand (navigation), nothing else.
3. **Status is plain text.** Show status in the table as a badge and once in the
   form as the selected neutral status toggle
   (`StatusBadge` family: rounded-full, semantic tint plus the word). Mapping:
   pending → `warning`, ordered → `info`, received → `success`, delivered →
   neutral (surface-200 with `inkMuted()` text). Never colour without the word.
   Decide whether the row-wide tints from
   `tickets/done/replenishment-row-status-colors.md` stay once badges exist;
   if they stay, they must remain subtler than the selected state.
4. **Singular vs plural labels.** Table cells and the form show the filter
   labels (`Replenishment.Pending` = "Pendents", `Replenishment.Received` =
   "Rebuts"). A row's status is singular: use the `Replenishment.Form*` keys
   (or new singular keys) in `statusLabel(...)`. Filters keep the plural.
5. **Codes are not in mono.** EAN/reference in the form and the barcode input
   use the `price-sm` style (IBM Plex Mono, tabular). Product names stay in
   Manrope.
6. **Inconsistent inputs.** The barcode field has an outline; the search field
   and note area are borderless beige blocks. All text inputs use the same
   treatment: `surface200()` fill, `border()` outline, `borderStrong()` focus,
   `radius-sm`. The search field shows the `search` icon and its placeholder
   (`Replenishment.SearchPlaceholder`) visibly.
7. **Hard-coded colours.** Replace `Color.GRAY`, `Color.BLACK` and
   `new Color(210, 210, 210)` in `ReplenishmentPanel` with
   `RetailPOSColors.inkMuted()`, `ink()` and `border()`. Use FlatLaf's
   placeholder support instead of the manual grey/black placeholder if it fits.
8. **Rows too short for touch.** Table rows are about 30px. Set a row height
   of at least `space-12` (48px) or document why the screen is a seated
   back-office screen with a smaller target (the design system allows `body`
   density only where the operator is sitting).
9. **Column widths.** The product column truncates while the empty note column
    is wide, and the added date truncates (`23/10/26 - 23:16 / ...`). Give the
    product column the remaining width, omit note from the table, and make the added
   column fit date and time without an ellipsis (show the creator in the form
   only). Show an empty customer as empty or `Replenishment.NoCustomer`, not `--`.
10. **Filter counts.** Status filters show their count, e.g. "Pendents (3)".
11. **Right form hierarchy and alignment.** Section labels (product request,
    customer, note, status) use the `label` style so they read as headings;
    all form content shares one left edge. `O cerca el producte al catàleg`
    must look like an enabled secondary button, not disabled text.
12. **Destructive and disabled actions.** `Delete` stays beside the selected-row
     status actions with a full-height, low-emphasis danger outline; it keeps its
     confirmation. For a saved request, a disabled `Delivered` action shows
     why once by the form controls (reuse `Replenishment.DeliveryHint`); the
     list action retains its tooltip without repeating the visible explanation.

Do not add new colours: everything must come from `RetailPOSColors` /
design-system tokens, kept in sync for light and dark themes.

## Implementation notes (2026-10-24)

- Footer follow-up: Pending, Ordered, Received, Delivered, Delete and Export
  share a single GridBagLayout row. Vertical fill takes the height from the
  tallest button's natural preferred size; no button height or width is forced.
  Widths follow translated labels, while Delete retains its low-emphasis
  danger outline and separation. The layout test checks their rendered heights,
  top edges, non-overlap, and complete labels in en/es/ca, light/dark, at 980px
  and 1200px. The targeted tests pass and offscreen captures were regenerated;
  keyboard and scanner checks in the running app are still outstanding.
- Final screen review: the table's mono line contains only the reference; strip
  a trailing `[reference]` from the displayed product name and, when the same
  variant is already appended to the name, omit that suffix from the mono line.
  Keep stored product data unchanged. Add a subtle `border` rule between rows
  and align the status pill text to the row's top-aligned text baseline.
- Keep the delivery reason by the form controls for a saved request only; the
  list button retains its tooltip but does not repeat the helper. Put Export
  on the same footer row as the status actions. On a new request, hide the
  Delivered toggle and helper. Give the barcode input a 48px touch height.
  Replace the manual-entry dash copy with a short "Or" divider and shorten
  the manual-description label in all three languages.
- Screenshot follow-up: in the product table cell the reference and name must
  start at exactly the same left edge. The cell container owns the padding;
  neither its mono reference label nor its Manrope text area adds its own inset.
  Removed the text area's FlatLaf border/margin inset; the Swing layout test
  compares both text origins on product-linked rows.
- Follow-up screen review: row height follows wrapped content (48px for one
  line; add line height only when product/customer text wraps). Cells and
  headings share a left edge and vertical alignment; type and customer must
  not steal width from product. Give customer roughly 200px and product the
  remainder.
- The list previews at most two lines per product/customer. Exceptionally long
  names show an ellipsis; the full name remains in the selected detail form
  and on hover. This keeps nine ordinary rows visible at 980px rather than
  letting one long name consume six row lines.
- Delete stays with the selected-row actions as a full-height, low-emphasis
  danger outline/text control with confirmation. The active filter is clearly
  selected (brand is permitted for navigation); filter text uses normal button
  size. Badges themselves carry semantic warning/info/success tint; delivered
  is neutral. Show `DeliveryHint` once by the saved form's disabled delivery
  control. Keep Save in place, disabled and secondary when clean, primary when
  dirty. Dates use Manrope with tabular figures and no spaced dash; references
  and EAN remain mono. An assigned customer offers a translated Change customer
  action.
- Row-wide status tints were removed in favour of semantic word badges; the
  selected row uses `surface200()` and a `borderStrong()` outline. Delivered is
  neutral. Status badges must be compact radius-full pills rather than tinted
  cells. The 48px minimum row is a standing cashier touch target; height grows
  only for wrapped content, not a fixed 120px.
- Only the dirty form's `Save` is primary. Selected status toggles use
  `borderStrong()` rather than a brand fill; a clean saved form has a disabled,
  secondary Save until the related next-step status-action ticket lands. The
  status appears once in the form, as the selected toggle (no second badge).
- Delete is a full-height, outlined danger button beside the status actions.
  Saved
  requests display product and code as read-only text, without creation fields
  or scanner guidance. The list omits note to make room for customer names;
  its Type value is singular.
- The date column contains only the date/time; the creator is in the form.
  Product and customer previews use Manrope; dates use Manrope with digits
  painted at tabular advances, while references use IBM Plex Mono; the note
  column is omitted from the table.
- The design-system commit is separate from the already-merged Delivered PR
  #130, based on the latest `origin/main`, and preserves the merged export
  search and order behaviour.
- An opening log on 2026-10-24 showed FlatLaf `unknown style 'arc'` and
  `unknown style 'borderColor'` errors on `JTextArea`. Apply FlatLaf's field
  styling only to single-line text fields; text areas use the shared fill and
  a scroll-pane outline that switches to `borderStrong` on focus. The targeted
  Swing test no longer logs these errors.

## Verification

`./gradlew spotlessApply integrationTest --tests '*ReplenishmentPanelLayoutTest'
--tests '*ReplenishmentStatusTest'` passed on the local worktree. The layout
test exercises light/dark, English/Spanish/Catalan, 980px/1200px, date width,
selection, dirty Save, mono barcode and status words. Twelve offscreen Swing
captures are generated at
`build/reports/replenishment-screenshots/{light,dark}-{en,es,ca}-{980,1200}.png`.
Extra `light-en-nine-980.png` and `light-ca-blocked-980.png` capture nine
ordinary rows (all visible at 750px high) and both disabled-delivery hints.
The images were inspected for wrapping, actions and filters; they are test
renderings, not screenshots of a running application. On 2026-10-25 the user
  confirmed manual checks for keyboard focus, scanner entry, screen sizes/scales,
  and both themes. The prerequisite Delivered PR #130 was rebase-merged.

The latest targeted tests and `git diff --check` pass; the layout test log has
no FlatLaf unknown-style errors. The design branch preserves export-order/search
work already on main.
`light-ca-nine-980.png` was also reviewed with the reference and product name
aligned on the same left edge; the light/dark, en/es/ca, 980px/1200px captures
were regenerated after the alignment fix.

Final pass: the layout test verifies that the visible product cell removes
duplicated `[reference]` and variant text without changing stored data. The
subtle row rules, aligned status pill, single visible delivery explanation,
and Export on the actions row were reviewed in both themes. New requests hide
Delivered and its hint, have a 48px barcode field, and use the shortened manual
entry labels. Both the saved-form and new-form offscreen captures were checked
in light/dark, en/es/ca, at 980px and 1200px; the new-form captures are named
`{light,dark}-{en,es,ca}-new-{980,1200}.png` in the same directory. The targeted
Swing and status tests pass without FlatLaf style errors. The isolated design
branch passes `docker compose up -d --wait`, `./gradlew ciCheck`, and
`docker compose down -v` on Derby, MySQL and PostgreSQL; containers were
removed. The refreshed branch from merged main also passes targeted layout,
status and export tests, with a regression check for unsaved note edits after
search refresh.

## Shipped

The replenishment screen uses neutral selection, semantic status pills,
readable codes and translated controls; footer buttons share natural height,
and the new/edit product form uses the shared inputs and safe actions. Key
paths: `src-pos/com/openbravo/pos/inventory/ReplenishmentPanel.java`,
`src/integrationTest/java/com/openbravo/pos/forms/ReplenishmentPanelLayoutTest.java`,
and `locales/pos_messages{,_ca,_es}.properties`.

## Done when

- Selecting a row never looks like an error or warning in light or dark theme,
  and the selected row is distinguishable from every status tint.
- Creating or editing has one brand-coloured `Save` when dirty; a clean saved
  form has no primary until the next-step action lands.
- Status is shown as a badge with its singular word in the table, and once as
  the selected neutral toggle in the form.
- EAN/reference values render in the mono face.
- Search, barcode and note inputs share one style; search shows icon and
  placeholder.
- `ReplenishmentPanel` has no hard-coded `java.awt.Color` values.
- Rows meet the touch target or the exception is written in this ticket.
- Product/customer previews fit in at most two lines with an ellipsis and
  access to the full value for long names; dates and filter labels with counts
  fit at 980px and 1200px in English, Spanish and Catalan.
- `ReplenishmentPanelLayoutTest` (or a new Swing test) covers selection colour,
  badge labels and column fit; manual check of keyboard focus and scanner
  entry is recorded here.
