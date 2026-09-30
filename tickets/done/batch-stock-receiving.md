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
- Show pending lines with product, barcode, PVP and quantities; the selected
  product details include its reference and any stock notes needed for a mismatch.
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

## Implementation status (2026-09-30)

Implemented on PR #78: receipt sessions persist until confirmation or explicit
Discard. Scanning matches `PRODUCTS.CODE` only, repeated scans add whole units,
and choosing a product in the finder matches its ID even when codes collide.
Pending lines show barcode, tax-inclusive PVP, received/current/final units,
manual paper-note ticks, totals, and a product-details card. The stock welcome
screen shows each open receipt as a single resume card.

Unknown barcodes remain unresolved and block posting. Catalogue fallback import
requires ProductsPanel access, adds only the scanned variant and opens an editor
without initial stock changes or edits to existing family variants;
a scanned secondary alias never imports a duplicate. The receipt and its lines
are locked for scans, unknown resolution, editing, review, posting, and Discard.
`post()` and `discard()` leave caller-owned transactions under caller control;
Derby rollback tests cover both. Optional tax columns are discovered through
metadata so a missing tax table cannot abort a PostgreSQL transaction. If
another till closes the receipt during editing, the panel shows a localized
notice and clears the stale session.
Posting updates stock, diary and matching replenishment entries atomically;
Discard deletes only an open receipt and its draft lines, leaving stock intact.
V50/V51 add the schema and role grants, and the administrator permission
catalogue exposes the receiving action to role editors.

Automated Derby tests cover rescans, duplicate-barcode finder selection,
quantity and tick changes, restart, unknowns, discard (including stale/posted
receipts), concurrent edits while posting, and multi-line posting. Role editor
tests cover granting and revoking receiving. Docker-backed
`./gradlew ciCheck` passed with Derby, MySQL and PostgreSQL; disposable
containers were removed. EN/ES/CA light-theme offscreen previews cover scan,
review and the pending card from compact through counter widths.

The ticket remains open for real-app scanner and keyboard/focus flows,
fallback-import dialog behavior, posting, resizing and physical display-scale
checks in EN/ES/CA. Count-session cards depend on the separate count flow.

## Shipped

Implemented in commits `85cc0c0e`, `f721878e`, `5ef3438f`, `1fbfe6df`, and
`8ec7b188`. Receipt sessions, scanning, review, posting, discard, persistence,
permissions, localization previews, and Derby/MySQL/PostgreSQL CI checks are
implemented. The ticket is marked done for the shipped scope; live scanner,
keyboard/focus, resize, and physical display-scale checks remain follow-up
verification.
