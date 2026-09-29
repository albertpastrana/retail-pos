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
- Show the pending lines with product, reference/EAN, quantity, and any stock
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

Receiving and counting (`tickets/todo/stock-count.md`) are the same scanning
rhythm — accumulate lines from a scanner, keypad for quantities, review, post
once — so build **one** session mechanism with a type rather than two screens
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
