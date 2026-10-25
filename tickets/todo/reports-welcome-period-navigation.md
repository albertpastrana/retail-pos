# Reports welcome: make the period being viewed explicit

Captured: 2026-10-25

Related: `tickets/in-progress/reports-welcome-screen.md` (the screen this
changes; its "Things to look at" block is removed by this ticket),
`tickets/todo/reports-welcome-custom-range.md` (follow-up: adds a fifth
"Custom" segment and retires the Sales summary screen)

Design canvas: https://claude.ai/artifact/JphTjeLkNixCZGJqYyJbpp (private to the
owner; artboards "En entrar · mes en curs", "Mes · després de ‹ Setembre",
"Setmana · després de ‹ Setmana anterior", "Any · en tocar Aquest any").
Figures in the canvas are partly illustrative; layout, labels and behaviour are
the spec.

## Goal

On "How the business is doing" (`JPanelReportsWelcome`), the owner can always
tell which period the detail figures and the chart belong to, and which parts of
the screen follow the previous/next controls and which do not.

## Problem today

Observed on 24 Oct 2026, month card selected, after pressing `<` once:

- The `<` `>` controls sit inside the selected card ("Aquest mes"), but that
  card does not change. It still reads "1/10/26 – 24/10/26 · 17.863,86 €". This
  is correct, because the four cards always end at the last closed day, but the
  controls placed on it suggest otherwise.
- The receipts / average receipt / loyalty / refunds strip and the chart switch
  to September. Nothing on screen names September: the chart is titled
  "Vendes per dia", and the header still says "Dissabte 24 d'octubre" and
  "Dades fins al 24 d'oct. 2026".
- The selected card is shown with a `brandSubtle` fill and a `brand` outline.
  In the design system, `brand` is for the single primary action, and the
  selected state is `border-strong`.
- "Coses a mirar" almost always says "Avui no hi ha res pendent" and takes a
  third of the row next to the chart.

## Decisions

1. **Two zones, named on screen.**
   - **"Fins avui" (up to today)**: the four running-total cards (week, month,
     year, last 12 months). They always end at the last closed day, as now. They
     have **no** previous/next controls. A caption under the section heading
     says they always end today and that tapping one shows its detail below.
   - **Detail**: one rounded `surface-100` section holding everything that
     follows navigation: the period title, the total, the four indicators and
     the chart.
2. **Selecting a period** works two ways, and both set the same state:
   - Tapping a card, as today. The selected card gets a 2px `border-strong`
     outline and a small "AL DETALL ↓" / "IN DETAIL ↓" / "EN DETALLE ↓" tag.
     Drop the `brandSubtle` fill.
   - A segmented control at the top of the detail section: Week · Month · Year ·
     12 months. It sits on a `surface-200` track; the selected segment is
     `surface-100` with a 2px `border-strong` outline and bold text. Use
     `aria`-equivalent Swing state: the segments are toggle buttons in one
     `ButtonGroup`, each at least 44px tall.
   - Build the control from a list of periods, not four hard-coded buttons:
     `reports-welcome-custom-range.md` adds a fifth segment, and five labels
     must still fit at the minimum width.
   - Selecting a period, whether from a card or a segment, always resets the
     detail to the current instance of that period (offset 0). This matches
     `selectPeriod` today, which resets `focusAsOf = asOf`.
3. **The detail header names the exact period** in the `display-sm` style:
   - Week: "Setmana del 12 al 18 d'oct." (current week: same pattern, ending on
     Sunday)
   - Month: "Setembre 2026"
   - Year: "Any 2026"
   - Last 12 months: "Darrers 12 mesos"

   Next to the title goes a neutral status chip (`surface-200`, `ink`, `label`
   style):
   - current instance: "EN CURS · FINS AL 24", "EN CURS · FINS DISSABTE", or
     "EN CURS · FINS AL 24 D'OCT." for the year
   - past instance: "MES TANCAT", "SETMANA TANCADA" or "ANY TANCAT"
   - rolling: "SEMPRE FINS AVUI"

   Below the title, one left-aligned `caption` line names the baseline's
   actual dates and year, for example "Comparat amb la setmana del 20 al 26
   d’oct. de 2025, de dilluns a dissabte." or "Comparat amb octubre 2025,
   de l’1 al 24 d’oct. de 2025." Base the cutoff on the last closed day used
   by the data, not on the calendar day; do not use an ambiguous "fins al N"
   referring to last year's date.
4. **Navigation moves into the detail header, at the right**, and only for
   week, month and year:
   - Two buttons, at least 48px tall, labelled with where they go:
     - month: "‹ Setembre" / "Novembre ›"
     - week: "‹ Setmana anterior" / "Setmana següent ›"
     - year: "‹ 2025" / "2027 ›"

     Replace the bare `<` `>` controls.
   - Next is disabled on the current instance, as `updatePeriodNavigation` does
     today. Keep the existing rule that navigation never goes past the current
     instance.
   - When not on the current instance, a text button "Tornar a octubre" /
     "Tornar a aquesta setmana" / "Tornar a 2026" appears left of the pair and
     resets the offset to 0.
   - The last 12 months has no navigation. In its place goes one `caption` line:
     "Aquest període sempre acaba avui; no té anterior ni següent."
5. **The detail figures row** gains a first cell, on a `surface-200` fill:
   - "TOTAL" label
   - the focus range in `price-sm`
   - the focus total in `price-lg`
   - the change badge
   - the baseline label and amount

   The four existing cells follow: receipts, average receipt, loyalty, refunds.
   The focus total is not on screen today when you navigate away from the
   current instance. That is the main reason the screen looks inconsistent.
6. **The chart title includes the period**: "Vendes per dia · setembre 2026",
   "Vendes per mes · 2026", "Vendes per mes · de nov. 2025 a oct. 2026".
   - Replace the single legend line with one entry per series that names the
     period: "Setembre 2026" filled, "Setembre 2025" outlined.
    - Every bar through the last closed day is complete and fully opaque. Days
      after it show only last year's dashed bar. Only the current instance
      shows the "future days (last year only)" note; there is no partial-today
      legend entry.
   - Keep the existing elapsed-comparison note only for the current instance.
7. **Remove "Coses a mirar"** (the notice card and its loading/empty states).
   The chart then takes the full width.
   - Remove the now-unused code: `ReportAttentionRepository`, `ReportAttention`,
     `showNotices`, and the `notices` field.
   - Remove the `reports.welcome.attention`, `noAttention`, `cashOpen`,
     `lowStock` and `debt` labels in all three locale files, after checking with
     `grep` that nothing else uses them.
   - Remove the `attentions.load` call from `loadFocus` and the `FocusData`
     field.
   - Record this decision in `tickets/in-progress/reports-welcome-screen.md`.
8. **Header**: keep the eyebrow, the date title and "Informes detallats".
   - Remove "Cada període es compara amb el mateix tram de l'any passat." from
     the header, because the comparison now lives in each card and in the
     detail header.
   - Keep "Dades fins al …".

### Not in scope

- No change to how periods, baselines or the last closed day are computed
  (`ReportsWelcomePeriod`, `SalesSummaryRepository`, `SalesTrendRepository`).
- The four cards' content and queries stay as they are.
- No new figures for week/year beyond what `showDetail` already loads for any
  period.
- No date-range picker (see `tickets/todo/date-range-picker.md`).

## Instructions for the implementing agent

Follow `AGENTS.md` and `tickets/README.md`. Before starting:

1. Fetch and branch from `origin/main`.
2. Move this file to `tickets/in-progress/`.
3. Read `tickets/in-progress/reports-welcome-screen.md`, because the period and
   baseline rules there still hold.

Code map (all in `src-pos/com/openbravo/pos/reports/`):

- `JPanelReportsWelcome.createCards()`: builds the four card buttons and,
  today, the per-card `<` `>` (`periodPrevious` / `periodNext`). Remove the
  per-card navigation and its arrays. The card click stays `selectPeriod`.
- `selectPeriod(...)`: replace the `brandSubtle` / `brand` styling with a
  `border-strong` 2px outline, using `RetailPOSColors.borderStrong()`, which
  already exists. Also show the selected tag on the card, and sync the
  segmented control.
- `changePeriod(...)` / `updatePeriodNavigation()`: keep the date arithmetic and
  call them from the new header buttons. Add a "back to current" action that
  sets `focusAsOf = asOf` and reloads, as `selectPeriod` already does.
- `createDetail()`: wrap the detail in one section that contains:
  - the segmented control
  - the title row (title, status chip, comparison caption, navigation)
  - the figures row, with the new TOTAL cell first
  - the chart, moved here from `createOverview()`
- `loadFocus()` / `showDetail(...)`: fill the TOTAL cell from
  `focus.comparison` (net sales, delta, baseline), and set the title, chip,
  caption and button labels from `selected` + `focusAsOf`. Add the label
  formatting as small package-private static helpers so they can be
  unit-tested. They must not be screen-local strings.
- `createOverview()`: becomes the chart only, full width, inside the detail
  section. The notice card is deleted.
- `SalesTrendChart`: its legend and notes move to the per-series labels
  described in Decision 6. If the chart paints the legend itself, pass the two
  series names in. Otherwise keep the legend as labels under the chart.

Locales (`locales/pos_messages.properties`, `_es`, `_ca`): add keys under
`reports.welcome.*` for:

- the section heading "Fins avui" and its caption
- the selected tag
- the four segment labels
- the status chips (current/closed × week/month/year, plus rolling)
- the comparison captions, with placeholders
- the navigation labels (previous/next/back × week/month/year)
- the rolling "no navigation" caption
- "TOTAL"
- the chart title pattern

Month names come from `SimpleDateFormat` in the user's locale, never from a
hardcoded list. Keep English, Spanish and Catalan wording parallel.

Layout: standard layout managers and preferred sizes, as in the current file.
The detail header uses a wrapping or `GridBagLayout` row, so the navigation
drops below the title at the smallest supported width instead of clipping.
Touch targets are at least 48px (segments at least 44px).

Tests (`src/integrationTest/java/com/openbravo/pos/reports/`):

- Extend `ReportsWelcomeLayoutTest`:
  - in all three languages at the minimum viewport width, the segmented
    control, the detail title, the status chip and both navigation buttons are
    visible and not clipped
  - no "Coses a mirar" component is present
  - the cards contain no previous/next buttons
- Add unit coverage for the title / chip / caption / button-label helpers:
  - current week, month and year
  - a closed week, month and year
  - the rolling 12 months
  - a month that crosses a year boundary (January, then "‹ Desembre" →
    "Desembre 2025")
- Keep `ReportsWelcomePeriodTest`, `ReportsWelcomeElapsedComparisonTest` and
  `SalesTrendRepositoryTest` green, unchanged.

Verification, in order:

1. `./gradlew spotlessApply`
2. the four tests above
3. `./gradlew check`
4. Manual check in the running POS, in light and dark theme, in Catalan, Spanish
   and English, at the smallest supported window. Walk through these flows:
   - Open the screen; month is selected and the title reads the current month
     with "EN CURS".
   - Press "‹ <previous month>": the title, chip, TOTAL, indicators and chart
     title all name that month; the four cards are unchanged; "Tornar a …"
     appears; next is enabled.
   - Press "Tornar a …": you are back on the current month and next is disabled.
   - Tap "Aquesta setmana", then "‹ Setmana anterior": the title reads the
     previous week's Monday–Sunday range.
   - Use the segmented control to pick "Any", then "12 mesos": the selected
     card follows the segment, and "12 mesos" shows the no-navigation caption.
   - Keyboard: Tab reaches the cards, the segments and the navigation buttons,
     in order, with a visible focus ring.

   Name these screens and flows in the PR.

## Done when

- After moving to any previous week, month or year, the detail title, status
  chip, TOTAL cell, indicators and chart title all name that same period, and
  the four "Fins avui" cards are visibly unchanged and carry no navigation
  controls.
- The period can be chosen both from the cards and from the segmented control,
  and both always show the same selection.
- Navigation buttons say where they go, next is disabled on the current
  instance, and "Tornar a …" returns to it. The last 12 months shows no
  navigation and says why.
- "Coses a mirar" is gone, and no unused code or locale keys from it remain.
- The selected card uses `border-strong`, not a `brand` fill or outline.
- Layout tests pass in English, Spanish and Catalan at the minimum width, and
  the manual flows above are verified in light and dark theme.

## Implementation status

### Feedback after the first visual review (2026-10-25)

- Put comparison directly below the left-aligned title and the navigation on
  the same row at the right, wrapping only if there is not enough width; remove
  extra space above the figures.
- Make the four segments one `surface-200` track: transparent unselected
  segments without borders, and a `surface-100`, `border-strong` selected
  segment. Do not use a default Swing selection fill.
- Since all periods end at the last closed day, remove translucent bars. Show
  future days as dashed last-year bars only. Give week chart ticks a weekday
  **and** day number, and ensure both named series are visible.
- Comparison captions name the baseline's actual dates unambiguously: for
  example, Catalan week "Comparat amb la setmana del 20 al 26 d’oct. de 2025,
  de dilluns a dissabte." Month and year captions likewise name the dates and
  year of the baseline in English, Spanish and Catalan.
- Add 16px padding, a card-style delta badge and mono price-sm baseline amount
  to the TOTAL cell. Remove the nested chart card. The selected-card tag is an
  ink/surface-100 radius-full chip; its heading uses the same inset as the
  amounts below.
- Recheck minimum-width layout in three languages, then the previous-week and
  previous-month flows in both themes; title, status, TOTAL and chart title
  must change together and the return button must appear.

The follow-up implementation now places the comparison directly under the
title, lets the navigation and chart legend wrap only when they do not fit,
uses one painted segmented track, removes the nested chart card and partial
bar styling, and makes the last-closed-day and future bars explicit. Week
ticks include day numbers; month/year captions name the prior year's cutoff.
The TOTAL and card-tag styles now match their intended tokens, and the compact
TOTAL range keeps its complete dates in a tooltip. Automated Swing layout,
paint, period-label and synthetic previous-month/week checks cover English,
Spanish and Catalan at 1000px, 1600px and resizing back. `SalesTrendChartTest`
checks that the last closed day is fully opaque and future days have no
current-period bar. A real POS walk-through in both themes is still required
before this ticket can be marked done.

The two-zone layout, synchronized card/segment selection, named focus periods,
TOTAL cell, detail navigation, period-specific chart labels, and removal of the
attention card/repository/model/labels are implemented on
`task/reports-welcome-period-navigation`. The related welcome-screen ticket
records the decision to remove the attention block.

Verified: `./gradlew spotlessApply`; `ReportsWelcomeLayoutTest`,
`ReportsWelcomeLabelsTest`, `ReportsWelcomePeriodTest`,
`ReportsWelcomeElapsedComparisonTest`, `SalesTrendRepositoryTest`; `./gradlew
check`; `git diff --check`. The layout and date tests cover the minimum 1000px
viewport and English, Spanish and Catalan, including a January/December year
boundary. **Still open:** a real POS walkthrough with shop data in both themes
and all three languages, at minimum width, including keyboard focus, resize,
the previous/back/next paths, selected-card sync, and visual legibility of the
five-cell detail row. Do not move this ticket to `done/` until these checks pass.
