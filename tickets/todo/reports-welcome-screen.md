# Reports welcome screen ("How the business is doing")

Captured: 2026-09-25

Related: `tickets/todo/administration-welcome-screen/`, `tickets/done/redesign-main-menu-sidebar.md`

Mockups: `tickets/todo/reports-welcome-screen/Main.html` (top of the page, week
selected), `tickets/todo/reports-welcome-screen/Any.html` (same screen with the
year selected), `tickets/todo/reports-welcome-screen/Avall.html` (scrolled down).

## Goal

Opening `Menu.SalesManagement` answers "how is the shop doing?" before the owner
picks any report. Today it opens a `JPanelMenu` list of seven report entries,
which answers nothing on its own: every question costs a click, a date range and
a read of a table.

The new panel is the landing view for that submenu. The seven existing reports
stay reachable from it, at the bottom, unchanged.

## What goes on it, and why

Ordered by how often a small-shop owner acts on it. Everything on the screen is
a period-over-period comparison — an absolute figure alone ("2.928 €") tells the
owner nothing they don't already feel.

**1. Four running totals, always on screen: week, month, year, last 12 months.**
These are the figures *and* the period selector — one row of four cards, each
showing what has been taken so far and how that compares with the same stretch
of last year. The owner does not have to pick a period to learn that the month
is ahead and the year is behind; picking one only changes the detail below
(chart, receipts, products).

| Card | Range | Compared with |
|---|---|---|
| This week | Monday → today | Same weekdays of the same week last year |
| This month | 1st → today | 1st → same day-of-month last year |
| This year | 1 Jan → today | 1 Jan → same day last year |
| Last 12 months | Rolling, today − 12 months → today | The 12 months before that |

Two rules make these honest, and both matter more than the layout:

- **Always compare elapsed against elapsed.** Month-to-date against *all of*
  last September would show a shop that is 18 % up as 18 % down. The baseline is
  cut at the same elapsed point, and the card says which ("Any passat, mateixos
  dies"). Where the full previous period is worth knowing (the target to beat),
  it is shown as a separate, quieter line, never as the comparison.
- For week and month, if today is Tuesday or the 15th, only Monday–Tuesday or
  the 1st–15th of the matched period last year contributes to the percentage.
  Remaining days shown on the chart are a full-period target only; label them
  explicitly so they cannot be mistaken for the comparison baseline.
- **The partial day/month is drawn as partial** — half-opacity bar, and the copy
  says so.

The rolling 12 months is the answer to "and is the business actually growing?":
it is the only one of the four that no single season, holiday shift or cold
spring can distort, and it is the reason a shop can tell a bad month from a
decline. Year-to-date answers "how is the year going", which is a different
question and is also on the screen.

**2. Detail of the selected period: receipts, average receipt, stamps redeemed, refunds.**

| Figure | Why it earns the space | Source |
|---|---|---|
| Takings (net sales) | The headline question, on the four cards above | `PAYMENTS.TOTAL` over `RECEIPTS.DATENEW`, sales minus refunds, as `SalesSummaryRepository` already does |
| Receipts | Splits "more customers" from "bigger baskets" | `COUNT(DISTINCT TICKETS.ID)` |
| Average receipt | The lever a shop can actually pull | net / receipts |
| Stamps redeemed ("victorines") | How much the loyalty card is actually used, and what it costs | Redemption lines — see the caveat below |
| Refunds | A quiet number that only matters when it moves | `TICKETTYPE = 1` |

**Gross margin is deliberately not on this screen.** Product cost prices
(`PRODUCTS.PRICEBUY`) are not maintained reliably yet, and a margin figure
computed from wrong costs is worse than no margin figure: it is precise, it is
believed, and it is wrong. Add the card once cost prices are trustworthy — the
query is `SUM(TICKETLINES.UNITS * (PRICE − PRODUCTS.PRICEBUY))` and the layout
already has the slot.

**Stamps redeemed needs a schema change; nothing else on this screen does.**
A redemption is a `TICKETLINES` row with no product, a negative price, and
`loyalty.redemption=true` inside the serialised XML in `ATTRIBUTES`
(`LoyaltyStamps.markRedemption`). A blob is not queryable in a way that works on
Derby, MySQL and PostgreSQL alike, and deserialising a year of lines in Java to
count them is not acceptable on a landing screen. So:

- add a versioned migration with a queryable flag on `TICKETLINES` (for example
  `LOYALTYREDEMPTION`), written when the ticket is saved, indexed by nothing
  more than the existing receipt join;
- backfill existing rows in the same migration by parsing their `ATTRIBUTES`
  properties, the way `V13__victorines_on_ticket` already reads resources — the
  history is small on a shop database and the alternative is a figure that
  starts at zero on the day the feature ships;
- follow `AGENTS.md`: the migration is safe for all three databases, is never
  edited afterwards, updates `migration-checksums.sha256`, and is covered for an
  empty database, an existing one, and a second run.

The card shows the count, the euros handed back, and last year's count. Both
halves matter: a rising count with flat takings means the card is buying
discounts, not visits.

**3. One chart, matched to the selected period, always against last year.**
Paired bars: filled for now, outlined for last year.

| Selected | Bars |
|---|---|
| Week | 7 days |
| Month | Days of the month |
| Year | 12 months — months not yet reached show last year only, dashed, as the figure to beat (see `Any.html`) |
| Last 12 months | 12 months, each against the same month a year earlier |

A line chart of a 7- or 12-point series is not more informative and reads worse
at arm's length.

**4. "Things to look at" — the part that makes this more than a dashboard.**
Four at most, each a link into the screen that fixes it, each stated as a fact
with its number:

- Cash not closed for N days (`CLOSEDCASH.DATEEND IS NULL`) → Close cash
- Best sellers below their stock minimum (`STOCKCURRENT` vs `STOCKLEVEL.STOCKSECURITY`, as `LowStockRepository` already joins them, restricted to the period's top sellers) → Low stock report
- Customer debt outstanding and the oldest one (`CUSTOMERS.CURDEBT`) → Customer debt report
- A record worth knowing: best weekday of the year, first week over X, a category up N points

A shop owner does not open a POS to browse data; they open it to find out
whether anything needs them today. This block is the difference between a
screen they visit and one they check.

**5. Below the fold: products, categories, payment mix, hours.**

- Top 5 products by takings, each with its year-ago delta — the movers matter more than the ranking, which barely changes.
- Category share, with the points gained or lost against last year — where the mix is drifting.
- Payment mix (cash / card / voucher-debt) — drives how much change to keep and what the card fees really cost.
- Average sales per hour of day over the last 30 days — the only view that answers opening hours and staffing, and it is not derivable from any current report.

**6. Detailed reports, reachable two ways.**
Nothing that the current menu offers is taken away, and none of it depends on
scrolling:

- a secondary **Informes detallats** button in the header, always visible, which
  jumps to the list (in Swing: scrolls the list into view, or drops a menu — decide
  once the panel is built);
- the seven existing entries at the foot of the page as 48px targets in a grid,
  with their current labels and behaviour.

Each block also links into its own report ("Veure l'informe de vendes per
producte"), so the usual route into a report is one click from the figure that
raised the question.

### Deliberately not on it

- A "most loyal customers" ranking: the shop that would use it has the customer screen for it, and it invites a ranking the owner cannot act on weekly.
- Forecasts or targets: no data to set a target against, and a wrong forecast on the landing screen costs trust in every other figure.
- Per-user (seller) sales: this is a one-till shop product; it reads as surveillance more than insight. Add only if asked.

## Design

Follows `design-system/README.md`; every value is a token, nothing screen-local.

- `surface-0` page, `surface-100` cards, `radius-lg` (16px), one `shadow-raised` step, `border` hairline. No second shadow, no card-level accent.
- One accent: `brand` fills the selected period button and the current-period bars. Last year is drawn as an outlined bar on `surface-100` — the comparison must never compete with the present.
- Deltas carry a word or arrow plus colour, never colour alone: `success` up, `danger` down, `ink-muted` flat. Attention items use `warning` (cash, stock), `info` (debt), `success` (record), each with its own icon.
- Every figure is IBM Plex Mono (`price` / `price-lg`), labels and copy Manrope. Amounts with two decimals and the locale's currency placement.
- Period buttons are 48px tall (`space-12`); attention rows are full-width targets.
- Charts are painted with Java2D in a small `JComponent`, styled from `RetailPOSColors`. Do **not** reach for `jfree:jfreechart:1.0.13` — it is already listed for removal in `tickets/todo/audit-unused-code-and-dependencies.md`, and its default styling fights this system.
- Dark theme comes free if every colour goes through `RetailPOSColors`; the chart must be verified in it, since the outlined "last year" bar is the one element that can vanish.

### Empty and slow states

- A shop with less than a year of history: the comparison column reads "no data for last year" rather than "−100 %". This is the common case on a fresh install and must not look like a collapse in sales.
- Queries run off the EDT with the card showing its label and a muted placeholder; the period buttons stay live. Six aggregates over a year of receipts is small, but Derby on a slow disk is not instant.

## Slices

1. Panel replaces `JPanelMenu` for `Menu.SalesManagement`: header, the four running totals (week / month / year / last 12 months) each against its elapsed-matched baseline, the selected period's detail figures, detailed-report links. One repository, one aggregate query run per period and baseline.
2. The period chart with the year-ago series (day bars for week/month, month bars for year and rolling year).
2b. Migration adding the queryable redemption flag with backfill, then the stamps-redeemed figure.
3. Things to look at (cash, low stock, debt), each linking to its screen.
4. Products, categories, payment mix.
5. Sales per hour.

## Done when

- Opening "How the business is doing" shows, without any further click, what has been taken this week, this month, this year and over the last 12 months, each with its variation against the same elapsed stretch a year earlier.
- A month-to-date or year-to-date figure is never compared against a full previous period; the card names the baseline it used.
- Selecting a period updates the detail figures and the chart, and the screen states which period and which comparison it is showing.
- A till whose cash has not been closed, whose best sellers are below minimum, or which has customer debt outstanding says so on this screen and links to the screen that resolves it.
- All seven existing reports are still reachable from this screen, with their current labels and behaviour, both from the header button and from the list at the foot.
- The top products, categories and payment blocks are on the screen and each links into its detailed report.
- The number of stamps redeemed in the period, and what they cost, is shown against last year's count, and the figure is correct for receipts taken before the flag existed.
- No margin or cost-derived figure appears anywhere on the screen.
- A database with no sales last year shows the comparison as unavailable, not as a 100 % drop.
- Verified in English, Spanish and Catalan, in light and dark theme, at the smallest supported window size.

## Implementation status

### Follow-up UI decisions

- Show the loyalty discount alongside the redeemed-stamp count, with last year's
  count below; use concise wording that fits at the smallest supported width.
- The detailed-report buttons need real left padding inside their rounded border
  so icons and text do not touch the outline. Raise small supporting text only
  where labels still fit in English, Spanish, and Catalan.
- Administration's common reports shortcut opens the reports welcome screen;
  its existing sales-summary report remains available in the catalogue. Use a
  full-sized house icon on the administration welcome navigation item;
  `gohome.png` is a small legacy exit arrow, not a home icon.
- The inline loyalty count/discount and report-button inset pass the layout
  checks at 1000 px in English, Spanish, and Catalan. `WorkflowCatalogueTest`
  confirms that the reports landing view and detailed sales summary are both
  reachable. Manual visual verification in both themes remains open.

The landing view is being implemented on the task branch:

- `Menu.SalesManagement` opens a non-blocking welcome view with week, month,
  year and rolling-12-month totals, elapsed-matched previous periods, selected
  period detail, and all seven existing report actions at the bottom.
- The comparison is unavailable when the previous period has no sales. Monday
  is the week start, matching the mockups. The view uses the existing theme
  tokens and has English, Spanish and Catalan labels.
- The welcome view uses the design-system `radius-lg`/`radius-md` shapes and
  `space-4`/`space-6` internal gaps. The selected-period row has four cards:
  receipts, average receipt, refunds and loyalty; net sales remain on the
  period selector instead of being repeated. The scroll content tracks the
  viewport width and stays anchored to the top; the screen header supplies
  the title without repeating it in the application's title bar.
- The header now follows the mockup's eyebrow/date/context layout; each period
  card shows its date range, change with an arrow, and actual matched baseline.
  Card ranges use the locale's compact start date followed by "today"; the full
  date remains in the header so labels fit at the smallest tested width on CI.
  The indicators share one rounded strip. The paired-bar chart and cash, top-
  seller stock and customer-debt notices sit below that strip and before the
  seven report links, using real data rather than mockup values. The week
  baseline aligns weekdays (364 days back); the rolling comparison ends where
  the current rolling window begins.
- `V49__loyalty_redemption_flag` adds the queryable flag, writes it for newly
  saved redemption lines, and backfills historical XML attributes. The selected
  period detail now includes redeemed stamps, returned euros, and last year's
  redeemed count.

The ticket remains open. The products/categories/payment/hour blocks,
the optional record notice, and manual
visual checks in all three languages, both themes and the smallest supported
window are still outstanding. No `Shipped` section is added until all
`Done when` checks are observable.

Verification so far: `./gradlew spotlessApply`, targeted Swing layout/date
range/Derby trend tests, and a Derby regression with Tuesday/week-to-date and
15th/month-to-date sales plus large future-day amounts from last year. The
future amounts appear on the chart as targets but not in the card baseline.
The empty Derby migration rerun, a historical loyalty-redemption XML backfill
test, `./gradlew check`, and `git diff --check` pass. MySQL/PostgreSQL `ciCheck`
could not run locally because the Docker daemon is not running. A live POS
visual check against the mockup is still required; compilation/layout
assertions are not a screenshot review. This partial PR delivers the implemented
landing view and leaves the remaining slices tracked here. Move this ticket to
`in-progress/` when work on those slices resumes.

## Open questions

- Week start: Monday as in the mockup; the last-year baseline starts 364 days
  earlier so its seven bars use the same weekdays.
- 29 February and month lengths: a year-ago baseline is cut at the same day-of-year. Confirm that day-of-year (not same weekday) is the right alignment for month and year totals; the week card aligns by weekday.
- Does "takings" mean with or without tax on this screen? The mockup shows gross paid (what went into the till).
- Should stamps *earned* (issued) also be shown next to stamps redeemed? It needs the same per-line eligibility rules as `LoyaltyStamps.stampsEarned`, so it is a bigger job than counting redemptions; left out for now.
- Multi-till / multi-location shops: figures are aggregated across locations. Confirm that is wanted before adding a filter.
