# Improve the product sales report

Captured: 2026-09-28
Updated: 2026-10-25 (design review)

Related: `tickets/in-progress/rebuild-management-reports.md`

Design: https://claude.ai/artifact/PPkC8qFpY2TwywNR7UJ9Xc, artboard "A + agrupació
(proposta)". Earlier artboards (A, B, C) are the options it was chosen from.

## Goal

Make the product sales report easier to narrow down by product and period, easier to read, and optionally view grouped by category or brand.

## Context

The current product sales screen (`JPanelProductSales`) offers a period selector and a table with reference, product, category, units, and amount. This ticket concerns that report specifically; coordinate with the ongoing reporting-module work.

Problems seen on 2026-10-25 (Catalan, 24–25 Oct 2026):

- The first table row has an empty reference and product and carries only units and amount (30 / 240,21 €). It reads like a total, but it is the ticket lines sold without a catalogue product (manual lines).
- Every cell uses the mono font, units and amount are wide and left-aligned, and the product name repeats the reference in brackets, so names are cut off.
- The leaf category alone ("Micro", "Roba") does not say what kind of product it is.
- Every period change needs an extra tap on `Load`.

Decisions:

- **Filters.** Add a category filter that includes descendant categories: choosing a parent such as Pyjamas includes all its subcategories, while choosing Cotton pyjamas includes only that category and its descendants. Add free-text filtering over product references and names (also category path and brand if practical), plus a brand filter. Filters combine with the selected period. Text filtering and grouping apply to the loaded rows at once, without reloading.
- **Period.** Show period presets as buttons: Today, Yesterday, This week, This month, Previous month, Custom. A preset loads immediately. Custom shows the from/to dates; the report loads when the range is complete and valid (picker closed, field left, or Enter). There is no `Load` button on this screen. Keep the week navigation the existing ticket asked for if it fits. `ReportPeriodSelector` is shared by five reports (product, sales, payment, tax, cash closing): add this as an option so the other reports keep their current behaviour until they adopt it.
- **Summary.** Above the table, show total amount, units and number of products. When there are manual lines, say how much of the total they are ("Includes 240,21 € in 30 manual lines").
- **Columns.** Reference, Product, Category (only when not grouped by category), `#`, Amount. Amount and `#` are narrow, right-aligned, in IBM Plex Mono with tabular figures; text columns use Manrope. The product column takes the remaining width and shows the name without repeating the reference. The category shows its full path from the top-level parent ("Roba de casa › Pijames Dona › Micro"), with the ancestors muted and the leaf emphasised.
- **Manual lines.** Shown as their own row or group at the end, labelled "Manual lines" with a "No product" status badge (`warning` colour plus the word) and a short explanation. They are included in the totals and never shown as a blank row.
- **Total.** A labelled total row at the bottom of the table, including manual lines.
- **Grouping.** A "Group by" switch with None, Category and Brand; None is the default. Grouping by category groups by the product's own category, with the full path in the group header. This replaces the earlier decision to group by the immediate parent. Each group header shows units, amount and share of the total, sorted by amount, and can be collapsed; "Expand all" and "Collapse all" are available. Products without a category or brand go into a clearly named group.
- **Not in this ticket:** CSV export and printing, which appear in the design.

## Done when

- Selecting a parent category includes products in its descendant categories; selecting a child restricts results to that subtree. Brand and reference/name text filters work alone and in combination with category and period, including an empty-result case.
- A preset loads the report with one tap; a custom range loads once both dates are valid, with no `Load` button. Other reports using `ReportPeriodSelector` behave as before.
- Manual lines appear as a labelled row or group, never as a blank row, and are included in the summary and total.
- The report shows a full category breadcrumb, narrow right-aligned amount and `#` columns in the mono font, and a product-name column that expands with the table without hiding important values.
- Grouping by none, category and brand works; group subtotals add up to the same total as the ungrouped view.
- The filters, dates, grouping, and column layout are checked with representative category/brand data and manually on the report screen in English, Spanish, and Catalan, including keyboard use and resizing.

## Implementation status

Implemented on `task/improve-product-sales-report`: period presets with immediate loading, a custom calendar range, week navigation, category subtree and brand/text filtering, local category/brand grouping with collapsible subtotals, explicit manual-line counts and totals, and localized summaries in the three languages. `ReportPeriodSelector` keeps its existing default mode for the other four reports.

Verified: `./gradlew spotlessApply`; `ProductSalesTest` (Derby query, filtering/grouping and three-locale layout); `./gradlew check`. Still open: manually walk through the Product sales screen using representative shop data in English, Spanish and Catalan, including custom-calendar completion, keyboard focus and Enter on groups, grouping/empty results, light/dark theme, and resizing at the supported display scales. Do not move this ticket to `done/` until those checks are performed.
