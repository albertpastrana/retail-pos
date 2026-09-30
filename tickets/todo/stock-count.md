# Stock counts (inventory by scanning)

Captured: 2026-09-28

Related: `tickets/todo/stock-welcome-screen.md`,
`tickets/todo/batch-stock-receiving.md`, `tickets/todo/pending-barcodes.md`,
`tickets/in-progress/improve-stock-diary-editor-layout.md`,
`tickets/done/unknown-barcode-notice.md`

Editable canvas: https://claude.ai/artifact/Xwva89iR8TbfQGKo3U8XQV

Mockups in `tickets/todo/stock-count/`, 1280×800 unless noted:

| File | Shows |
| --- | --- |
| `Inici.html` / `.png` | The stock welcome screen, the entry point both flows share, with the "ho tens a mig fer" band |
| `Main.html` / `.png` | Inventari: open counts, share of the catalogue counted, per-category status |
| `Obrir.html` / `.png` | Starting a count (720×540 dialog) |
| `Comptant.html` / `.png` | Scanning: the accumulating list, the system figure beside each counted one, keypad |
| `Revisar.html` / `.png` | Review and apply: only the products that differ, with the cost effect |

## Goal

A shopkeeper can walk the shop with a scanner, pass every item on a shelf or of
a brand, and end with the stock of those products corrected — without closing
the shop, without doing the whole catalogue at once, and knowing afterwards
which products carry a verified figure and which still carry a number nobody
has checked.

## Context

Today the only way to correct stock is `StockDiaryPanel` /
`StockDiaryEditor`: one movement, one product, one form at a time. That is a
correction tool, not a counting tool. Counting is a scanning rhythm — the same
rhythm as the sales screen, which accumulates lines as codes come in.

### The model: a count is a session

A **count** (`Recompte`) is an open session with a free-text name the person
chooses — `Prestatge 3 — Vins`, `Marca — Estrella Damm`, `Nevera`. It holds
one line per product. It stays open across days and restarts until it is
applied. Several counts may be open at once; a product may only be in one open
count at a time (scanning it into a second one is an error that names the other
count).

Scanning is the only required interaction:

- Each scan of a code adds `+1` to that product's line, creating it on the
  first scan. The line moves to the top of the list so the person sees what
  they just did.
- Typing a number on the keypad before a scan sets that quantity instead of
  adding one (eight identical bottles: `8`, then scan one).
- A line can be selected and its counted quantity overwritten, or removed.
- A code that belongs to no product becomes an unresolved line, not a silent
  drop — same treatment as `tickets/done/unknown-barcode-notice.md` and feeding
  `tickets/todo/pending-barcodes.md`. A count with unresolved codes cannot be
  applied.

Nothing about stock changes while a count is open. The count is a statement of
what was seen, not a movement.

### The decision that makes "shop stays open" work

When a line is first created, store the system's stock for that product **at
that moment** (`SNAPSHOTUNITS`) alongside the counted quantity. Applying the
count does not set stock to the counted number: for each line it posts a stock
movement of `COUNTED − SNAPSHOTUNITS`, the discrepancy the person actually
found. Sales, receipts and breakages recorded between counting and applying
therefore survive — they are separate movements on top of the correction, and
the till never has to be locked.

Lines whose difference is zero produce no movement at all.

### Knowing what is verified and what is not

Applying a count stamps each of its products as counted: last counted date, the
count it came from, and who applied it. The **Inventari** screen reads that
stamp to answer the question the shopkeeper actually has — *what still needs
doing?* — as a share of the sellable catalogue, a per-category breakdown, and a
status per category: counted recently, partially counted, never counted, or
stale (older than a configurable window, three months by default). The same
stamp is worth surfacing on the product screen and in the stock welcome scan
result, so a product found by scan says whether its figure has ever been
verified.

### Where it lives

Under `Menu.StockManagement` ("Catàleg i preus"), as a job on the stock welcome
screen (`tickets/todo/stock-welcome-screen.md`) — the "Corregir un estoc que no
quadra" card already drafted there is the single-product path; this is the
walk-the-shelf path and needs its own entry. The counting screen itself is
full-width and scanner-first, like the sales screen.

Counting keeps a landing screen of its own (**Inventari**) because "what is left
to count" is a question that lives for weeks. Receiving does not
(`tickets/todo/batch-stock-receiving.md`): it opens from its card and ends when
the delivery is posted. What they do share is the welcome screen, which gains a
**"ho tens a mig fer"** band listing every unfinished session of either kind —
that band is how an interrupted receipt is found again, and it should be built
by whichever of the two tickets lands first.

### Schema

Two new tables, plus the stamp. `STOCKDIARY.REASON` is an integer, so
inventory movements take new reason codes rather than reusing `OUT_BREAK`,
which would make shrinkage and counting indistinguishable in reports:
`IN_INVENTORY (+5, stock.in.inventory)` and
`OUT_INVENTORY (-5, stock.out.inventory)` in `MovementReason`.

```
STOCKCOUNT       ID, NAME, LOCATION, DATESTART, DATEEND, STATUS, APPUSER
STOCKCOUNTLINE   ID, STOCKCOUNT, PRODUCT, ATTRIBUTESETINSTANCE_ID,
                 UNITS, SNAPSHOTUNITS, DATEFIRST, DATELAST
STOCKCOUNTED     PRODUCT, LOCATION, DATECOUNTED, STOCKCOUNT
```

`STOCKCOUNTED` is the stamp, one row per product and location, replaced on
every apply — the "when was this last verified" question must not need a scan
of the whole diary. A unique index on `(STOCKCOUNT, PRODUCT,
ATTRIBUTESETINSTANCE_ID)` keeps repeated scans on one line. Use a Flyway
migration in `src-pos/db/migration`, same as `V44`–`V49`.

### What this is not

- **Not a blind count.** The counting screen shows the system figure next to
  the counted one. A blind mode (hide the expected number so the counter is not
  anchored) is a good later option, not the first cut.
- **Not stock valuation or shrinkage reporting.** The review screen shows the
  cost effect of the differences it is about to post, and stops there.
- **Not a replacement for receiving.** Goods arriving are
  `tickets/todo/batch-stock-receiving.md`; that posts purchases, this posts
  corrections, and mixing them would make both unreadable in the diary. The two
  do share their scanning mechanism: `STOCKCOUNT` is generalised there to
  `STOCKSESSION` with `TYPE = COUNT | RECEIPT`, so build whichever lands first
  with that split in mind.

## Slices

1. Schema, `MovementReason` codes, and the open/scan/apply cycle with one
   count at a time. Counting screen and review screen.
2. The Inventari landing screen: open counts, progress, per-category status.
3. The counted stamp surfaced on the product screen and the stock welcome scan
   result.

## Done when

- A stock user can open a named count, scan a series of barcodes, and see each
  scan accumulate on its product's line with the system figure beside it.
- Repeated scans of one product add up; a typed quantity before a scan sets it;
  a line can be corrected or removed.
- A count survives closing and reopening the application, and two counts can be
  open at once without sharing products.
- An unknown barcode lands as an unresolved line and blocks applying until it
  is turned into a product or removed.
- The review screen lists only the products whose counted figure differs from
  the system, with the difference and its cost effect, and states how many
  products matched.
- Applying posts one inventory movement per difference, equal to the difference
  found at counting time, so sales made between counting and applying are not
  overwritten; matching products produce no movement.
- Applying stamps every product in the count as counted today and closes the
  count; the movements are visible in the stock diary under an inventory
  reason.
- The Inventari screen shows open counts, the share of the sellable catalogue
  counted, and per-category status including never counted and stale.
- Automated coverage verifies: repeated scans, quantity override, unknown code
  blocking apply, a sale posted between counting and applying leaving the final
  stock right, zero-difference lines posting nothing, and a count staying
  intact across a restart.
