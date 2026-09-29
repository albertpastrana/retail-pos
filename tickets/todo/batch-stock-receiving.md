# Batch stock receiving

Captured: 2026-09-24, revised 2026-09-28

Related: `tickets/done/stock-orders.md`, `tickets/todo/stock-count.md`,
`tickets/todo/improve-stock-diary-editor-layout.md`,
`tickets/todo/pending-barcodes.md`

Editable canvas: https://claude.ai/artifact/Xwva89iR8TbfQGKo3U8XQV (second row)

Mockups in `tickets/todo/batch-stock-receiving/`, 1280×800 unless noted; the
shared entry point is `tickets/todo/stock-count/Inici.html` / `.png`:

| File | Shows |
| --- | --- |
| `Obrir.html` / `.png` | Starting a receipt: supplier and delivery-note reference (720×540 dialog) |
| `Main.html` / `.png` | Scanning the delivery: sort control, per-line tick, footer totals |
| `Revisar.html` / `.png` | Review before posting: what enters stock, unknown code, unticked lines |

## Goal

When a supplier order arrives, staff can add all received products and quantities
to stock quickly in one receiving session instead of creating individual stock
movements one at a time.

## Context

The current stock diary is useful for individual movements, but receiving a
delivery is repetitive when an order contains many products. Add a dedicated
batch mode to the stock area that works well with a barcode scanner and a
keyboard:

- Scan or search a product and enter the received quantity.
- Scanning the same product again increases its quantity in the current batch.
- Show the pending lines with product, separate barcode and reference, quantity, and any stock
  notes needed to resolve a mismatch.
- Allow a line to be edited or removed before posting the batch.
- Confirm once to create the corresponding stock-in movements and update
  current stock.
- Keep the batch reviewable until confirmation, and do not change stock for an
  unconfirmed batch.

The flow should optionally link the receipt to a replenishment entry or a free
text supplier/order reference, but it is not a supplier purchase-order module.
Received quantities may differ from what was expected; the user must be able
to adjust them before posting. Unknown barcodes should be clearly reported and
must not create an ambiguous stock movement.

### Where it lives, and what it shares with counting

The stock welcome screen already has the entry point: its "Donar entrada a
mercaderia rebuda" job card currently opens `StockDiaryPanel` as a placeholder
(`src-pos/com/openbravo/pos/inventory/JPanelStockWelcome.java:154`). It should
open this instead.

Receiving gets **no landing screen of its own**. There is no standing question
for one to answer: a delivery arrives, you scan it, you post it, it is over.
Counting has one (the Inventari screen) because "what is left to count" lives
for weeks. The only shared surface is the welcome screen, which gains a
**"ho tens a mig fer"** band listing unfinished sessions of both kinds — that
band is how yesterday's half-scanned delivery is found again, and it is built by
whichever of these two tickets lands first.

Receiving and counting (`tickets/todo/stock-count.md`) share the scanning
rhythm — accumulate lines from a scanner, edit quantities, review, post once —
so build **one** session mechanism with a type rather than two screens
that drift apart. Generalise `STOCKCOUNT` to `STOCKSESSION` with
`TYPE = COUNT | RECEIPT`, and keep the differences where they belong:

| | Count | Receipt |
| --- | --- | --- |
| A scan means | there are this many (absolute) | one more arrived (adds up) |
| Posting | difference against the figure snapshotted when the line was created | the received quantity |
| Reason | `IN_INVENTORY` / `OUT_INVENTORY` | `IN_PURCHASE` |
| Compared against | the system's stock | the paper delivery note |
| Stamps "counted" | yes | no |

A receipt does not need `SNAPSHOTUNITS`; its line is what arrived. A receipt
carries a supplier name and a delivery-note reference, both free text, stored on
the session and copied onto the movements it posts.

### Ticking off against the delivery note

Decided against typing the delivery note in, and against importing supplier
files: **the person scans, and then compares the screen with the paper by eye.**
The screen's job is to make that comparison possible, not to model the note:

- A **sort control**: while scanning, last-scanned first (you see what you just
  did); for checking, sorted by name, the order a delivery note usually lists.
- A **tick per line** the person sets themselves as they find it on the paper,
  with a `tickat X of Y` counter. The tick is a reading aid, not a validation:
  an unticked line warns at review but never blocks posting.
- **Totals in the footer, lines and units**, in the same place on both screens,
  because that is what the foot of a delivery note states and it is the
  cheapest whole-delivery check there is.
- Items on the paper that never arrived are **not** stock lines: offer to write
  them to the replenishment list (`REPLENISHMENT_ENTRIES`, already shipped) as
  pending with the supplier, which is exactly what that list is for.

Cost prices are out of scope: receiving moves units only. A delivery whose cost
changed is a catalogue edit, kept separate so a busy goods-in moment cannot
silently change margins.

### What this is not

- **Not a purchase-order module.** No expected lines, no ordering, no invoices.
- **Not the replenishment list.** That list captures what is missing and tracks
  `PENDING → ORDERED → RECEIVED`; it holds no quantities and must keep not
  touching stock. Posting a receipt may mark matching entries `RECEIVED`, but
  the two remain separate records.

## Slices

Worth doing before `tickets/todo/stock-count.md`: it is the thinner half of the
shared mechanism (no snapshot, no counted stamp, no landing screen) and it is a
weekly job rather than a quarterly one, so it proves the scanning session on
real use before counting adds the harder parts. Either order works; they are
independently shippable.

1. The session mechanism shared with `tickets/todo/stock-count.md`, plus
   receiving: open with supplier and note reference, scan, review, post.
2. The paper-comparison affordances: sort control, per-line tick, footer totals.
3. Missing items to the replenishment list, and marking matching `ORDERED`
   entries as `RECEIVED` when a receipt is posted.

## Parked

Photographing the delivery note and reading its lines with OCR, to pre-fill the
expected quantities. Worth revisiting once the manual flow is in use and we know
which suppliers' notes are legible enough to be worth it; the screen above works
unchanged if expected quantities later appear beside the received ones.

## Done when

- A stock user can open a receiving session from the stock area with a supplier
  and a delivery-note reference, and the "goods received" job card on the stock
  welcome screen opens it.
- An unconfirmed receipt is listed on the stock welcome screen and can be picked
  up from there, without hunting through a menu.
- A scanner can add multiple products without leaving the receiving flow.
- Repeated scans of one product accumulate in its batch line, and a typed
  quantity can be set on its row after scanning.
- The user can change quantities, remove lines, and see the final review before
  confirmation.
- The list can be sorted last-scanned first or by name, each line can be ticked
  off by hand, and both screens show total lines and total units.
- Confirming a batch creates stock diary entries and updates current stock for
  every accepted line, carrying the supplier and delivery-note reference.
- Cancelling or closing an unconfirmed batch leaves stock unchanged, and a
  session survives restarting the application.
- Unknown products and invalid quantities are actionable errors and cannot be
  posted accidentally; unticked lines warn but do not block.
- Items on the note that did not arrive can be written to the replenishment list
  without creating a stock movement.
- Automated coverage verifies an empty batch, repeated scans, edits/removals,
  unknown products, cancellation, and successful posting of multiple lines.

## Implementation status (2026-09-28)

2026-09-30 PR verification: `./gradlew spotlessApply` and Docker-backed
`./gradlew ciCheck` pass (Derby, MySQL and PostgreSQL); disposable database
containers have been removed. EN/ES/CA offscreen previews cover receiving,
review and the pending-receipt card at counter and compact widths. Real-app
scanner, fallback-import dialog, keyboard/focus and physical display-scale
verification remain open, so this ticket stays in `todo/`.

2026-09-29 operator correction: Discard still clips in the upper actions even
with its short label. The button had a frozen preferred width, and the row
breakpoint was cached before translated/font-scaled sizes were laid out. Size
it from live content, retain at least a 48px touch height, and recompute the
row width when laying out at the active locale and display scale. The operator
already updated the Catalogue and prices pending-band heading; preserve it.

Implemented locally: removed Discard's frozen preferred width. Its touch height
comes from content and margins, and the upper action row now measures the
visible buttons at layout time. EN/ES/CA previews at 320, 850 and 980 logical
pixels and a larger-font regression check confirm the label fits. Focused
receiving, persistence and welcome tests pass. Live display-scale verification
remains pending before closing the overall receipt ticket.

2026-09-29 operator request: discard a half-finished test receipt so it
disappears from the pending sessions without posting any stock. Offer a
confirmed destructive action on the open receipt and directly on its pending
card. Only open RECEIPT sessions may be discarded; committed/posted receipts
must remain untouched. The existing schema allows only OPEN/POSTED, so remove
the draft and its lines together in one transaction rather than altering a
released migration or introducing a new status for test data.

2026-09-29 correction: do not show Discard in the Catalogue and prices pending
band. The pending item is one full hit-area button with the supplier on the
first line and the delivery-note reference plus line count on the second; it
resumes that receipt. Keep the confirmed Discard action inside the receipt
screen only. Discarding there removes the receipt from the pending band.

Place Discard with the receiving screen's upper action buttons, not underneath
the scanning/details card. It is unavailable on the review step, whose header
remains free of action buttons.

Use the short label "Discard" on that button (CA/ES: "Descartar") to prevent
clipping; the confirmation prompt carries the supplier, delivery-note reference
and explicit stock impact.

Implemented locally: the open receipt and all of its draft lines are removed
atomically after confirmation, never changing stock or deleting posted receipts.
The short Discard button sits among the upper receiving actions. Catalogue and
prices shows one full hit-area pending card with supplier and delivery note;
clicking either line resumes it, with no discard action there. Focused Derby
tests cover a populated and empty draft, unchanged stock/diary, and refusal to
discard posted or already removed receipts. EN/ES/CA layout previews at narrow
and counter widths and receiving/welcome integration tests pass; real operator
interaction still needs validation before closing the overall ticket.

Pending wording proposal only: rename the Catalogue and prices band from the
generic "In progress" to "Pending goods receipts" (CA: "Recepcions pendents",
ES: "Recepciones pendientes"). Await operator approval before changing it.

2026-09-29 counter feedback: receiving rows are too tall. Use a single-line
unknown-product warning in the row, keep its full corrective instruction in
the tooltip and review panel, and reduce row height in both side-by-side and
stacked modes while keeping the quantity cell touch/keyboard accessible.

Implemented locally: row height is derived from the monospaced row font with a
52px touch minimum in both layouts. Unknown products use a one-line red title;
the existing full error stays in the cell tooltip and review warning. Focused
EN/ES/CA receiving, persistence and welcome tests pass, including row-height
and unknown-tooltip checks; scan and review previews were inspected at 600 and
980 logical px. Live display-scale and scanner verification remain pending.

2026-09-29 copy follow-up: refer to the open session consistently as a
"receiving session" across list headings, hints, catalogue-import prompts,
confirmation/removal messages and empty states in EN/ES/CA. Reserve "delivery
note" / "albarà" for the supplier's paper document, and keep references to
the physical delivery on the stock welcome screen where appropriate.

2026-09-29 visual feedback: the receiving content should line up with the host
view title instead of adding a second horizontal inset. The in-session heading
should distinguish the supplier, the labelled delivery-note reference and the
unconfirmed status as in the supplied wireframe, with a concise started-at and
stock-unchanged hint. Adapt the arrangement at narrow widths without changing
the shared shell's padding for other screens.

2026-09-29 further operator request: display the received, current-stock and
resulting-stock columns in both scanning and review; remove the New receipt and
Continue adding products buttons from the review header (the review pane's back
button remains). Keep the JScrollPane for vertical scrolling and header/selection
behaviour. Fit columns at counter width where readable; retain horizontal access
at narrow sizes rather than hiding data or forcing illegibly narrow columns.

Implemented locally: session copy consistently says receiving/receipt (except
paper delivery-note references), and the in-session header separates supplier,
labelled delivery note, unconfirmed badge, start time and stock notice. Its
content is inset to align with the host title; the shared navigation gutter was
not changed. All seven columns and their values appear in both phases. The
 review header has no action buttons; review actions remain in the side pane,
and entering review moves keyboard focus to the table. At 600–1200 logical px
 the table fits without horizontal scrolling in both phases; at 320 px it retains horizontal
and vertical scrolling so barcodes and counts remain readable. EN/ES/CA
offscreen layout previews and focused receiving/persistence/welcome tests pass.
The physical scanner, real window scaling and full post/import flows still
need manual verification before closing this ticket.

2026-09-29 approved operator copy: use "Goods receiving" for the screen,
"New goods receipt", "Exit (saved)", "Review goods receipt" for the non-posting review
action, a separate "Goods receipt review" heading, "Continue adding products" to
go back, and "Confirm stock entry" for the posting action. Translate the same
distinction between a draft receiving session and the final stock movement in
Spanish and Catalan. Keep review and post labels distinct.

Implemented locally in the EN/ES/CA messages and a separate review-title key.
Offscreen scanning and review previews at 320, 600, 850 and 980 logical pixels
show the new buttons without truncated labels. Focused receiving, persistence
and welcome layout tests pass. Actual app, scanner, keyboard/focus and display
scaling still need manual validation before closing the receipt ticket.

2026-09-29 final short-header wording: CA Entren / Actual / Final (ES Entran /
Actual / Final; EN In / Current / Final). Keep the barcode centred and the
complete quantity tooltips and accessible column names.

Implemented locally in the EN/ES/CA resources. The receiving layout test passes
and review previews confirm that the short labels fit. Full-app and display-scale
validation are still pending before the receipt ticket can be closed.

2026-09-29 operator decision: keep compact text headers, now CA Ara / Actual /
Final (ES Ahora / Actual / Final; EN Now / Current / Final); remove the icon
option. Center the barcode column's header and values. Review the screen title
and button wording as proposals before changing that copy.

Implemented locally: the three translated short headers remain text-only with
their full tooltips, and the barcode column is centred in both views. EN/ES/CA
offscreen previews and focused receiving/persistence/welcome tests pass. Wording
changes to the screen title and actions are still awaiting operator feedback;
live scanner and display-scale checks remain pending.

2026-09-29 operator comparison: temporarily replace the three icon headers with
compact text (CA: Entren / Ara / Final; ES: Entran / Ahora / Final; EN: In /
Now / Final). Preserve the full localized tooltips and accessible column names
while the operator compares the text and icon options.

Implemented locally as right-aligned short labels in both scanning and review.
The minimum widths are derived from the translated labels so auto-resize does
not clip them; full tooltips and accessible names remain. Focused EN/ES/CA
receiving, persistence and welcome layout tests pass; review screenshots at the
stock columns were checked. The operator's preferred header style is still to
be decided, and live display-scale checks remain pending.

2026-09-29 approved compact headers: use the Lucide package-plus, warehouse,
and equal icons on the received-units, current-stock, and resulting-stock
columns. Show the full meaning in localized header tooltips and accessible
names. Keep the underlying quantity column names for accessibility.

Implemented locally with theme-aware Java2D renderings of the Lucide shapes.
The JTable header supplies localized mouse tooltips, and the icon renderers
retain full accessible column names in EN/ES/CA. Review screenshots scrolled
to the stock columns and focused receiving/persistence/welcome tests pass.
Actual application and display-scale checks are still pending before closing
the ticket.

2026-09-29 review table follow-up: show the current and resulting stock columns
as whole numbers, without changing the stored quantities; right-align the PVP
column header to match its prices. Short quantity headers are to be proposed
for operator feedback before changing them.

Implemented locally: current and resulting stock are rounded to whole units
for display only, and PVP's header aligns with the right-aligned price cells.
EN/ES/CA layout tests cover fractional stored stock, the resulting displayed
stock, and header alignment; focused receiving/persistence/welcome tests pass.
The three quantity headers were awaiting the operator's choice; the Lucide
icons above are now approved.

2026-09-29 counter feedback: keep the right-hand tools pane the same width in
scanning and review at the same window size. Wrap review notes inside the shared
width; additional review columns remain horizontally scrollable in the table.

Implemented locally: review now uses the scanning pane's width instead of
growing to fit its warnings. Warnings wrap within that width and review buttons
remain fully visible. EN/ES/CA offscreen previews and receiving/persistence/
welcome tests pass at 320, 600, 850, 980 and 1200 logical pixels; live resizing
and actual display-scale checks still remain before closing the ticket.

2026-09-29 latest counter feedback: place Remove from list directly below the
selected-line paper-check button in the details card. The selection itself
identifies the line, so remove the reference from the button label. Give both
card actions the same touch height and tighten the spacing between barcode,
reference, brand, and retail price.

Implemented locally: both card buttons share a full-width, equal-height layout;
the remove action keeps its confirmation and appears only with a selected line.
The detail rows now use their content heights instead of four equally tall
slots. Focused EN/ES/CA offscreen layout previews at 980px and the receiving,
persistence, and stock welcome tests pass. Live scanner, focus, and display-scale
validation is still pending before the receipt ticket can be closed.

2026-09-29 counter decisions: match the barcode input height to its search button;
add a touch-sized selected-line tick toggle synchronised with the table checkbox.
Use a descriptive remove label with the selected reference (barcode fallback).
Replace the table reference column with a narrower tax-inclusive PVP column,
widen the table barcode column, and allow data columns to be resized. When a
scanned barcode is absent from PRODUCTS but present in the fallback catalogue,
offer the existing product import dialog; importing for a receipt must never
change stock until that receipt is posted. Cancelling the dialog leaves the
scan pending; codes absent from both catalogues remain unresolved lines.

Implemented locally: the selected-line toggle changes the persisted paper-check
state and follows the table checkbox; removal identifies the selected reference
(truncated on the button, full in its tooltip), and the table uses a wider
barcode column, a narrower tax-inclusive PVP column, and resizable data columns.
Fallback barcode matches open a receipt-specific import dialog that has no
initial-stock field and does not add stock; cancellation preserves the scan.
EN/ES/CA offscreen layout checks, receiving persistence and welcome layout tests
pass. Actual import-dialog interaction, scanner, screen scaling and full-app
receipt posting remain to be checked before this ticket can be closed.

2026-09-29 further counter feedback: immediately select the line produced by a
barcode scan or product-finder choice. Shorten the scan label to "Barcode",
align the right-hand controls with the table (rather than its toolbar), add a
remove icon, and make the barcode and received-count columns narrower in favour
of product names. Round the details card corners, align its heading, and let
long names/references wrap rather than clipping at supported scales.

Implemented locally: matching barcode and finder product IDs select and reveal
their row after the repository refresh, while scan focus returns to the barcode
field. The table and right-hand scan label align at counter width. The remove
button has a themed trash icon; columns allocate more width to product, with
the full received-count header preserved in EN/ES/CA (the Spanish table barcode
header uses a shorter label, while the field/card retain the full label).
The details card uses the shared radius-lg shape and wraps long references.
Focused receiving/persistence/welcome tests and offscreen EN/ES/CA previews pass;
the full app and actual scanner/display-scale flow still need manual validation.

2026-09-29 operator review: scanner input matches barcodes (`PRODUCTS.CODE`)
only; staff use the existing product finder for name/reference lookup. Separate
barcode and product reference in the table rather than concatenating them.
Replace the selected-line number pad with a product-details card: full name,
barcode and reference, plus brand and tax-inclusive retail price when available.
Quantity remains editable directly in the table; no redundant selected-line
quantity control is needed.

Implemented locally: barcode scans ignore references even if another product
uses that reference; finder selection still uses product ID. Table columns show
barcode and reference separately, and the details card replaces the keypad.
Brand and PVP are shown only when catalogue and tax data are available; older
minimal schemas still show name, barcode and reference. Focused Derby tests
cover the reference collision, finder selection, tax-inclusive price, empty
selection and unknown barcode. EN/ES/CA offscreen previews pass at 320–1200
logical px; manual application/scanner and actual display-scale checks remain
pending before closing this ticket.

2026-09-29 counter feedback: the table/tools proportions are automatic, so do
not present a draggable `JSplitPane` handle. Use a non-draggable layout in both
the side-by-side and compact stacked modes. Rename the received-quantity column
header to "# received" (ES: "# recibidos", CA: "# rebuts").

Implemented locally with a `BorderLayout` workspace: the table takes remaining
width beside the tools, and switches above the scrollable tools at compact
widths, without an interactive divider. Updated the header in EN/ES/CA. Focused
receiving, persistence, and welcome layout tests pass at 320, 600, 850, 980 and
1200 logical pixels. The full app and high-DPI scanner flow still needs manual
validation before closing the ticket.

2026-09-29 further counter review: at high display scale the two-column layout
can clip the keypad. Reflow the table and tools vertically before either is
cut off; align the table toolbar controls; use a clickable select-all checkbox
in the tick column header, right-align received units, allocate more width to
product names, and use the POS table's IBM Plex Mono for row text. Make Find
product a full-width button with a themed magnifier icon. Verify narrow/large
window layouts in EN/ES/CA.

Implemented locally: compact widths stack the list over the scrollable tools,
with table-only horizontal scrolling when its columns cannot fit, and a
two-row list toolbar. The header renders a real checkbox (disabled in review);
clicking it updates all ticks in one repository statement. Row text uses
`RetailPOSTheme.PLEX_MONO_REGULAR`; product gets more width and received units
and their header align right. The product search button fills the tool column
with a token-coloured magnifier. EN/ES/CA offscreen previews and focused tests
pass at 320, 600, 850, 980 and 1200 logical pixels. Real high-DPI and scanner
operation still require manual validation before closing this ticket.

2026-09-29 follow-up from the counter review: remove the pre-scan quantity
field; each scan adds one unit and corrections happen on the selected row or
inline in the table. Put Remove line next to the table. Give the table more
width and size the shared keypad column from its content instead of a fixed
width. C clears the whole selected quantity, as on the other numeric pads.
Use a descriptive heading for the received-lines list.

Implemented these follow-ups locally. The list owns its Remove line action;
its heading is "Goods unpacked" (translated in ES/CA). The keypad is the shared
touch bean, with the tool-column minimum derived from its key size and the
scrollbar; the list takes the rest of the width. At 850 px the receipt heading
wraps so the delivery reference stays visible. The review table scrolls
horizontally rather than truncating its extra stock columns. Focused EN/ES/CA
layout, selection/Enter/C, persistence, and welcome tests pass, including 850,
980 and 1200 px layout checks. Actual scanner, keyboard/focus, scaling, and the
full app flow remain to be manually checked before marking the ticket done.

2026-09-29 UI decisions from `Main.png`: use the same product finder as sales
(`JProductFinder`), scanning/Enter adds directly with no redundant add button,
and the sort choice belongs above the table. Received quantities are whole,
positive units; edit them in the table (and with the selected-line keypad).
Selecting a row highlights it like the products table; ticking against the
paper remains a separate checkbox action.

Implemented these UI decisions on the local task branch. Finder selection uses
the chosen product ID even if its code is ambiguous; receipt writes reject
fractional quantities. The EN/ES/CA layout previews (including selected rows)
and focused receiving, persistence, and welcome integration tests pass. Manual
scanner, keyboard/focus, resizing, scaling, and the full application flow are
still pending, so this ticket remains open.

The task branch implements receipt sessions, persistent lines, an atomic posting
operation, replenishment follow-up, the welcome-screen resume cards, and a
scanner/review panel. Automated Derby tests cover the migration (including a
second run), scanning, editing, unresolved codes, restart, and multi-line
posting. EN/ES/CA layout previews are generated under `build/receiving-*.png`.

Before closing this ticket, verify the actual application flow with a scanner,
keyboard and focus changes, resizing and display scaling in EN/ES/CA. The
required Docker-backed `ciCheck` passed with MySQL and PostgreSQL on 2026-09-28;
the disposable containers were removed afterwards. The shared welcome band
displays receipt sessions; count-session cards depend on the separate count
flow being shipped.

The first manual attempt opened the receipt screen successfully according to
`logs/pos-0.log` but failed at an unrecorded later step. Generic flow events for
opening, resuming, scanning and posting now record the flow without supplier or
barcode values. The receiving layout was also corrected to reuse the shared
numeric keypad and avoid right-side clipping; the EN/ES/CA layout test and the
repository integration tests pass. The actual scanner, keyboard, focus,
resizing, and display-scaling flow still needs manual verification before
closing this ticket.
