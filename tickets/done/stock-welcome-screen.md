# Stock submenu welcome screen ("Start from the product")

Captured: 2026-09-26

Related: `tickets/todo/administration-welcome-screen.md`,
`tickets/in-progress/reports-welcome-screen.md`,
`tickets/todo/batch-stock-receiving.md`,
`tickets/todo/pending-barcodes.md`,
`tickets/done/redesign-products-screen.md`,
`tickets/done/unknown-barcode-notice.md`

Mockups: `tickets/todo/stock-welcome-screen/Main.html` (the landing screen as it
opens), `tickets/todo/stock-welcome-screen/Resultat.html` (a product found by
scan), `tickets/todo/stock-welcome-screen/Desconegut.html` (a scanned code that
belongs to no product).

## Goal

Opening `Menu.StockManagement` lets someone start from the job they came to do,
instead of from the application's filing system. Today it opens a `JPanelMenu`
list of six entries — Products, Price rules, Markdowns, Categories, Taxes, Stock
movements (`src-pos/com/openbravo/pos/forms/JPrincipalApp.java:136-149`) — which
asks a question the person cannot answer: *which of these six screens holds the
field I want to change?*

The six existing entries stay reachable from the new panel, with their current
labels, without depending on scroll.

## The observation this is built on

The submenu is called "Estoc", but stock is one entry out of six. The other five
are the catalogue: what you sell, what it costs, what tax it carries, which
shelf it belongs to, and whether it is on offer. The name sends people looking
for stock and the contents are product maintenance.

More usefully: **the six entries are six facets of one object.** A product has a
price (Products), a pricing policy (Price rules), a markdown (Markdowns), a
category (Categories), a tax rate (Taxes) and a quantity (Stock movements). The
menu is a list of the *fields*, sorted by which screen happens to own them, and
it asks the shopkeeper to know that mapping before they can start.

Almost every real visit begins with a physical thing: a product in hand, a
barcode, a delivery on the counter, a supplier price list. So the screen begins
where the person begins — with the product — and the menu structure becomes an
implementation detail they never have to learn.

The house idiom for this already exists in the submenu: `SaleMarkPanel`
(Markdowns) is a scan field that focuses itself on `activate()`, a percentage,
and a batch to apply. This screen extends that idiom to the whole submenu rather
than inventing one.

### What this screen is not

- **Not a stock dashboard.** An earlier draft of this ticket proposed one — days
  of cover, dead stock, movement charts. It was dropped: the submenu is not about
  stock, and a screen that answers "how is the business doing" already exists
  (`MenuSalesManagement`). If stock health is worth a screen it is a separate
  ticket, and it would have to reckon with `STOCKLEVEL` being read by one query
  and written by nothing (see "What we found in the schema" below).
- **Not a second workflow search.** `JPanelWelcome` (administration) already
  searches *screens and workflows* across the whole application. This screen
  searches *products*. Two search fields that look alike and search different
  things would be worse than either alone, so this one is labelled for what it
  takes — a barcode or a product name — and never returns a screen as a result.
- **Not a place to edit a product.** Every action opens the screen that owns the
  field. Editing a price in two places is how a product ends up with two
  validation rules and one of them wrong.

## What goes on it, and why

### 1. Scan or search a product — the centrepiece, focused on open

One field, focused when the screen activates, that takes a barcode from the
scanner or a typed name / reference. This single control covers most visits and
is why the screen is worth building; the rest of the page is what you see while
you have not scanned anything yet.

A match shows **one card per product with the facts that today live in five
different screens**, each one a link to the screen that changes it:

| Fact on the card | Source | Opens |
|---|---|---|
| Name, reference, barcode | `PRODUCTS.NAME`, `REFERENCE`, `CODE` | Products |
| Selling price | `PRODUCTS.PRICESELL` | Products |
| Markdown in effect | `PRODUCTS.SALE_PERCENT` (`V9__product_sale_percent`) | Markdowns |
| Category | `CATEGORIES.NAME` via `PRODUCTS.CATEGORY` | Categories |
| Tax rate | `TAXCATEGORIES.NAME` / `TAXES.RATE` via `PRODUCTS.TAXCAT` | Taxes |
| Units on hand | `SUM(STOCKCURRENT.UNITS)` for the product | Stock movements |
| Last movement | Latest `STOCKDIARY.DATENEW` for the product | Stock movements |
| Brand and its price rule | `PRODUCTS.BRAND`, `PRICE_RULES` (`V6__price_rules`) | Price rules |
| Already on the replenishment list | `REPLENISHMENT_ENTRIES.OPEN_PRODUCT_ID` (uniquely indexed), its `STATUS` and `CUSTOMER_NAME` | Replenishment |

That table *is* the argument for the screen. Answering "what do I charge for this,
is it on offer, and how many are left?" costs three screens and three searches
today; here it costs one scan.

**A code that matches nothing** is the other half. Today the till already has a
notice for this (`tickets/done/unknown-barcode-notice.md`); here it becomes an
offer to create the product, which is what the person actually wants at that
moment. When `tickets/todo/pending-barcodes.md` lands, the same result shows how
many times that code has been scanned at the till and by whom — see
`Desconegut.html`.

### 2. The jobs, not the screens

These sit **directly under the search field**, because they are its alternative:
the divider reads "o bé, sense cap producte a la mà". Anything placed between
the two breaks that either/or and the page stops reading as a sentence. That is
why the replenishment figures come after them rather than before.

Six cards in task language, ordered by how often a small shop does them:

| Card | Opens | Why it is a job and not a menu entry |
|---|---|---|
| Donar d'alta un producte nou | `ProductsPanel`, new record | The single most common reason to come here |
| Posar o treure rebaixes | `SaleMarkPanel` | Nobody looking to discount things searches for the word "Markdowns" first |
| Donar entrada a mercaderia rebuda | `StockDiaryPanel`, goods-in | Becomes the batch flow when `tickets/todo/batch-stock-receiving.md` lands, and then this card is one line changed |
| Corregir un estoc que no quadra | `StockDiaryPanel`, adjustment | Same screen, different job, and today you must know that |
| Canviar com es calculen els preus d'una marca | `PriceRulesPanel` | "Price rules" does not say markup-percent-and-rounding-per-brand; the card does |
| Ordenar el catàleg | `CategoriesPanel` | — |

**Taxes deliberately has no card.** Changing a tax rate is rare, global, and
expensive to get wrong; it does not belong among six things presented as everyday
jobs. It keeps its entry in the grid at the foot, where someone who means it will
find it.

Cards resolve through the task-name registry the administration welcome screen
introduced (`app.getAppUserView().getTaskAction(taskName)`), so permissions and
behaviour are identical to the menu's, and a card whose destination the user
cannot open is not drawn.

### 3. Replenishment and customer orders — the work already waiting

`ReplenishmentPanel` lives in `Menu.Main`, not in this submenu, and it is the
only place in the product area that holds *work waiting to be done* rather than
records to maintain: a shared list of what to reorder and what a customer has
asked for, with Pendent → Demanat → Rebut states
(`REPLENISHMENT_ENTRIES`, `V22__replenishment_entries`).

Its own section below the jobs, under a "Reposició i encàrrecs" heading — one
compact row of three figures, each a link, and a button into the list:

| Figure | Query | Opens |
|---|---|---|
| Per demanar | `STATUS = 'PENDING'`, with `MIN(CREATED_AT)` for the oldest | Replenishment, filtered to Pendent |
| Demanats, esperant que arribin | `STATUS = 'ORDERED'` | Replenishment, filtered to Demanat |
| Encàrrecs de client oberts | `CUSTOMER_ID IS NOT NULL AND STATUS <> 'RECEIVED'` | Replenishment, filtered to Encàrrecs |

All three are counts over `REPLENISHMENT_STATUS_INX` (`STATUS, CREATED_AT`),
which V22 already creates, so this costs three cheap indexed aggregates.

**This is not the dashboard the earlier draft was.** These are not metrics about
the business; they are a queue of jobs someone has already written down and
nobody has finished. A count that represents a person waiting earns its place on
a landing screen in a way that "units sold per week" does not.

The section is always present for a user with permission for
`ReplenishmentPanel`, and absent entirely for one without it. When nothing is
pending it says so — "cap reposició pendent" — and keeps the button, which is
also where "apuntar què falta" lives. That is why there is no seventh job card
for it: the section is both the state and the way in, and a card would be a
second door to the same room.

**It also closes the loop with the search.** Scanning a product tells you whether
it is already on the list — `REPLENISHMENT_ENTRIES.OPEN_PRODUCT_ID` is uniquely
indexed, so it is one lookup — and whether it is waiting to be ordered, already
ordered, or was asked for by a named customer. Finding out that the thing in your
hand was already requested three days ago is the single most useful thing this
screen can tell you about it.

And a code that matches no product can still be written down: the list accepts a
manual entry with `MANUAL_EAN` and `MANUAL_DESCRIPTION` (that is what
`Replenishment.WithoutCode` is for), so "no existeix" can offer "apunta'l per
demanar" as well as "crea'l" — see `Desconegut.html`.

**One honest limit.** `RECEIVED` is terminal: there is no state for "the customer
has collected it". So a customer order that has arrived leaves the open list the
moment it is marked received, and nothing afterwards says anyone still has to be
told. The screen does not pretend otherwise — it counts open orders only. Adding
a fourth state is the real fix and belongs to the replenishment ticket, not this
one; see the open questions.

### 4. The six existing entries

- a secondary **Totes les pantalles** button in the header, always visible;
- the six entries at the foot as 48px targets in a grid, with their current
  labels and unchanged behaviour: `Menu.Products` (Productes), `Menu.PriceRules`
  (Regles de preu), `Menu.SaleMark` (Rebaixes), `Menu.Categories` (Categories),
  `Menu.Taxes` (Impostos), `Menu.StockDiary` (Moviments d'estoc).

Nothing that the current menu offers is taken away, and none of it depends on
scrolling.

### Deliberately not on it

- **Inline editing of any product field.** Links open the owning screen.
- **A products table.** `ProductsPanel` already has a filter bar with barcode,
  reference, name, category and brand, filtering as you type
  (`tickets/done/redesign-products-screen.md`). Reproducing it here would be a
  second list with a second set of behaviours to keep in step. The search here
  returns a summary and hands over.
- **Anything cost-derived** — margin, stock value, what a markdown costs.
  `PRODUCTS.PRICEBUY` is not maintained reliably, and a precise figure from bad
  data is worse than none. Worth noting that `PriceRulesPanel` computes selling
  prices *from* that cost, which is a problem this screen surfaces but does not
  solve.
- **Counts and totals in general.** "296 products, 12 categories" is a fact about
  the database, not about anything the person came to do.

### What we found in the schema, and what it means for later tickets

Recorded here because it was established while designing this screen and will
otherwise be rediscovered:

- `STOCKLEVEL` (`STOCKSECURITY`, `STOCKMAXIMUM`) is read by exactly one query in
  the repository (`LowStockRepository`) and **written by nothing** — no screen,
  no editor, no migration. The two catalogue import tools only `DELETE` from it.
  So "products below minimum" is empty on every installation, and any future
  screen that counts breaches must distinguish "nothing is below its minimum"
  from "no minimums exist". This is also why the Low stock report looks broken.
- Every sold line with a product writes a `STOCKDIARY` row in the same
  transaction as the ticket (`DataLogicSales.saveTicket`: `REASON = -1` with
  negative units for a sale, `+2` for a refund), and `addProductStock` writes
  `+1` for goods-in. The diary is therefore a complete movement ledger, indexed
  by `DATENEW` only.
- `STOCKCURRENT` has no floor: selling a product that was never received leaves a
  negative row. That is worth a notice somewhere, but not on this screen.

## Design

Visual review (2026-09-26): the first Swing implementation was shown on a real
screen and did not match `Main.html`: rectangular controls, missing job/search
icons, and sections/grids horizontally misaligned. Rework the actual first
paint against the wireframe before asking for another review. A passing
headless layout test alone did not catch these visual failures. Keep the branch
available locally for user review; do not open a pull request until requested.

Second visual pass: page rows now share a GridBagLayout width and left edge;
the wide view uses three job columns and six footer targets, collapsing to two
columns at 1024px. Search, job and navigation controls paint their own rounded
surfaces using shared light/dark colour tokens; each job has a theme-aware
stroke icon and a short explanatory line. Replenishment counts and the list
action sit inside one rounded row. Check offscreen renders of the landing,
found-product and unknown-code states in `build/stock-welcome-*.png` before
asking for real-screen feedback. No PR is to be opened until the user asks.

Result-card review (2026-09-26): the real scan result still paints a tall list
of field labels and values instead of the hierarchy in `Resultat.html`. Build
one product header, five equally weighted fact tiles (price, markdown,
category, tax, stock), a replenishment notice when applicable, and a quiet
brand-rule footer. At narrow widths the tiles may reflow; facts, actions and
their labels must remain readable. Only show facts actually represented by the
schema: there is no timestamp for when a markdown began and no historical
original price separate from `PRICESELL`. Derive the discounted amount using
the same percentage calculation as `LineDiscount`; label the stored list price
as the before price. Do not fabricate a markdown age or category parent where
none exists. Keep result links permission-filtered and leave the PR closed
until the user reviews the actual screen.

Third visual pass: the result now presents one header, five equal-sized fact
tiles (price, markdown, category, VAT and stock), an existing-request strip
when relevant, and a quiet brand-rule footer. The original six screen links
remain at the foot; three compact job links follow a search result, as in
`Resultat.html`. An effective markdown price is calculated from `PRICESELL`
and `SALE_PERCENT` in the same way as `LineDiscount`, with the stored catalogue
price explicitly labelled "before". The category parent is only shown when
`CATEGORIES.PARENTID` exists; the effective tax rate is displayed separately
from the tax category name, so a name such as "IVA 21%" is not mistaken for a
second 21% charge. The request date is read from the open entry. No markdown
age, invented cost figure or absent brand rule is presented.

Fourth visual review (2026-09-27): the five fact tiles are still too tall in
the running application. Shorten the tiles and remove surplus vertical space
inside their values and hints; keep the price, category, VAT, negative-stock
notice and action links fully legible at narrow and wide widths. Make no other
visual changes in this pass. Let the user review again before any PR.

This pass reduces fact tiles from 160px to 124px and gives short values and
hints only one line of preferred height; longer text retains two. Offscreen
renders of both the marked-down coffee and the -2-unit bag confirm that the
captions, values and bottom actions still fit at wide and narrow widths.

Verification: `./gradlew spotlessApply integrationTest --tests
'com.openbravo.pos.inventory.StockWelcome*'` and `./gradlew check` pass;
the runnable jar was rebuilt. This remains in `todo/` for the user's visual
review in the running POS, with no PR reopened.

Follows `design-system/README.md`; every value is a token, nothing screen-local.

- `surface-0` page, `surface-100` cards, `surface-200` for the search field's own
  fill (it is a sunken, editable control, which is exactly what `surface-200` is
  for), `radius-lg` (16px) cards, `radius-md` (10px) buttons and entry targets,
  `radius-sm` (6px) the field, one `shadow-raised` step, `border` hairlines.
- **One accent, and it stays on the navigation.** This screen has no single
  primary action — a search field is not a button — so no card and no job tile
  carries `brand`, the same rule the administration welcome screen settled on.
  The search field's focus ring is `border-strong`, which is what that token
  exists for.
- Semantic colour always with a word or an icon: `warning` for a markdown in
  effect, `danger` for negative units on hand, `info` for a passive notice such
  as an unknown code. Never colour alone.
- Every figure — price, percentage, units, barcode — is IBM Plex Mono (`price`,
  `price-sm`); every label and sentence is Manrope. Prices carry two decimals and
  the locale's currency placement.
- 48px (`space-12`) minimum on the search field, every job card, every result
  action and every entry in the foot grid.
- Keyboard and scanner first, because that is how this submenu is used: focus
  starts in the search field on `activate()` as `SaleMarkPanel` already does,
  Enter opens the single match or the first result, Esc clears the field, Tab
  walks search → jobs → entries.
- Dark theme comes free if every colour goes through `RetailPOSColors`; the
  result card's markdown badge is the one element to check in it.

### Empty and slow states

| Situation | What the screen does |
|---|---|
| Nothing typed yet (the normal opening state) | The jobs and the entries; the field explains what it takes — a barcode or a name |
| No match | Names the code or text that failed and offers to create the product, rather than showing an empty list |
| Code known to the import TSV but not in `PRODUCTS` | A different message from "does not exist anywhere" — the till already draws this distinction |
| Several matches | All of them as summary cards, the field keeps focus |
| Empty catalogue (fresh install) | "Encara no hi ha cap producte" and the create card first; the entries stay live |
| Nothing pending in the replenishment list | The section stays, says "cap reposició pendent", and keeps the way in |
| A destination the user cannot open | Its card, its entry and its figures are not drawn at all |

Search runs off the EDT, debounced at ~300 ms as `ProductsPanel`'s filter already
is; a scan arrives as a whole line and skips the debounce. The jobs and the six
entries are live from the first paint and never wait on a query.

## Naming

Decision (2026-09-26): use the recommended "Catalogue and prices" / "Catálogo y precios" / "Catàleg i preus" for the submenu title; the six destination names remain unchanged.

The submenu label no longer describes its contents. Recommendation: change
`Menu.StockManagement` from "Estoc" to **"Catàleg i preus"** ("Catálogo y
precios", "Catalogue and prices") — a translation-key change in the three
`pos_messages*.properties` files, with the six entries and every task name
untouched. Stock movements sit there legitimately: the quantity is one of the
product's facets, like its price.

The alternative is "Productes", which is shorter but collides with the
`Menu.Products` entry inside it. Confirm the wording before slice 1 ships, since
it is the screen's own title too.

## Database

No schema change. Every column the screen reads exists in `V1__baseline.sql` or a
released migration: `PRODUCTS.SALE_PERCENT` (V9), `PRODUCTS.BRAND` and `FAMILY`
(V36), `PRICE_RULES` (V6),
`PRODUCTS.ISVOUCHER` (V11), `REPLENISHMENT_ENTRIES` with its `STATUS, CREATED_AT`
and `OPEN_PRODUCT_ID` indexes (V22). Note that `ATTRIBUTESETINSTANCE_ID` was dropped from
`STOCKCURRENT` and `STOCKDIARY` in `V43__product_attributes`, so the units-on-hand
and last-movement lookups must not reference it.

If the unknown-code result is to show how often a code has been scanned, that
needs the `PENDING_BARCODES` table from `tickets/todo/pending-barcodes.md` — it
belongs to that ticket, not this one, and this screen degrades to a plain "no
existeix" until it lands.

## Slices

Implementation note (2026-09-26): the scan field now reuses the existing product
barcode filter when opening Products; a new-product action uses the existing
editor's insert action. Replenishment links select Pendent, Demanat, or open
customer orders in the existing list, and an unknown code starts a manual
request with its barcode filled in. Other fact links still open their owning
screens cleanly; those screens do not expose a product-selection contract.

1. Panel replaces `JPanelMenu` for `Menu.StockManagement`: header, the six job
   cards, and the six existing entries. No search yet — already better than the
   list it replaces, and it ships without touching any query.
2. The scan/search field with the product summary card, its facts, and its links
   to the owning screens. Focus, Enter, Esc, and a scanner as input.
3. "No match" handling: the offer to create the product, the option to write it
   down with `MANUAL_EAN`, and the distinct message for a code the import TSV
   knows.
4. The replenishment row and the "already on the list" state on the product
   card, both read-only, linking into `ReplenishmentPanel`.
5. Pre-selecting the product in the destination screen. There is no contract for
   this today — the administration welcome ticket flagged the same gap for
   "Latest cash closings" — so until it exists every link opens its screen clean,
   and this slice is where that contract gets designed. It also covers opening
   Replenishment already filtered to Pendent, Demanat or Encàrrecs. Do not let
   slices 1–4 wait on it.
6. The label change, if confirmed: `Menu.StockManagement` in three locales and
   the regenerated locale jar.

## Done when

- Opening the stock submenu presents the jobs someone comes here to do, in task
  language, instead of a list of six screen names.
- Scanning a barcode or typing a product name on this screen shows that product's
  price, any markdown in effect, its category, its tax rate and its units on hand
  together, without opening any other screen first.
- Each of those facts opens the screen that changes it, with the same permission
  behaviour as the menu.
- Scanning a code that belongs to no product offers to create it, and says so in
  words rather than showing an empty result.
- The screen is usable end to end from a scanner and a keyboard: focus starts in
  the search field, Enter opens a single match, Esc clears.
- All six existing entries are reachable with their current labels and behaviour,
  both from the header button and from the grid at the foot, and reaching the
  header button never requires scrolling.
- A destination the user has no permission for is not offered as a job card or an
  entry.
- An empty catalogue and a no-match search each produce a stated result, not a
  blank area.
- How many products are waiting to be ordered, how many are ordered and not yet
  in, and how many customer orders are open is visible without opening any other
  screen, and each figure opens the replenishment list.
- Scanning a product that is already on the replenishment list says so, with its
  state and, for a customer order, whose it is — and the screen never offers to
  write it down twice.
- A shop with an empty replenishment list sees no row of zeroes.
- No cost-derived figure appears anywhere on the screen.
- Verified in English, Spanish and Catalan, in light and dark theme, at the
  smallest supported window size.

## Verification (2026-09-26)

- `./gradlew spotlessApply integrationTest --tests 'com.openbravo.pos.inventory.StockWelcome*'` passes: Derby lookup and queue fixtures, permission-filtered and six-entry layout at 1024px in all three languages, and offscreen painting under light and dark FlatLaf.
- `./gradlew check` passes; the locale jar was regenerated by the build.
- A live till/scanner walk-through (focus, navigation, new-product and manual-request flows, resizing and translated labels on screen) is still required before moving this ticket to `done/`.
- 2026-09-26 visual correction: the earlier headless test proved only the
  width of buttons, not their actual placement or painting. The revised tests
  assert shared section alignment, visible hints, responsive grid columns and
  rounded-card pixels in an offscreen rendered image. The screen still needs a
  fresh user review in the real application.
- 2026-09-26 follow-up: `./gradlew spotlessApply integrationTest --tests
  'com.openbravo.pos.inventory.StockWelcome*' check` passes. I inspected the
  generated landing, found-product and unknown-code renders at wide and
  1024px-equivalent widths, plus the light/dark previews. The runnable jar and
  locale jar were rebuilt. Await the user's review of the actual application
  before treating visual acceptance as complete; keep the pull request closed.
- 2026-09-26 result-card follow-up: the Derby test covers category-parent,
  current VAT rate, brand-rule and open-request date separately. Layout tests
  render the mockup-like marked-down coffee, the real-review example (a bag
  with no markdown and -2 units), and a negative-stock card in dark theme.
  Visual previews are `build/stock-welcome-product-wide.png`,
  `build/stock-welcome-product-negative.png` and
  `build/stock-welcome-product-dark.png`. Tests assert five tiles, locale price
  and VAT formatting, responsive columns and permission-filtered links. The
  user still needs to approve the screen in the running application.

Implementation is on the `stock-welcome-screen` branch; the ticket is back in
`todo/` until the live till walk-through and user visual review confirm the
observable UI checks.

## Shipped

The product-focused catalog welcome screen, product search summary, replenishment
links, permission filtering, responsive layout, and translated states are on
the updated `main` branch.

## Open questions

- **The submenu label (resolved 2026-09-26).** Use "Catàleg i preus" / "Catálogo y precios" / "Catalogue and prices" for the submenu and screen title.
- **Should Taxes get a job card after all?** The argument against is that a rare,
  global, expensive change should not sit among everyday jobs. If a shop changes
  VAT often enough to disagree, the card is one line.
- **Pre-selecting the product in the destination** is the difference between "one
  scan gets me to the price field" and "one scan tells me the price, then I search
  again". Worth deciding early even if it ships late, because it shapes slice 5.
- **There is no "collected" state for a customer order.** `RECEIVED` closes the
  entry, so once the goods arrive nothing tracks that the customer still has to
  be told and still has to come in. That is a real gap in
  `tickets/done/stock-orders.md`'s model, not something this screen can paper
  over; a fourth state would make "encàrrecs arribats, per avisar" the most
  valuable figure on this row. Decide there, not here.
- **Should scanning a product let you add it to the list from this screen?**
  `DataLogicReplenishment.add` already enforces one open entry per product, so it
  is safe; the argument against is that this screen is otherwise read-only and
  hands every write to the screen that owns it. Left out for now.
- **How many results before the field stops being a scanner box?** A scan returns
  exactly one product; typing "llet" may return thirty. The assumption taken here
  is: show the first few as cards and hand the rest to `ProductsPanel` with the
  text carried over — which is the same missing contract as above.
