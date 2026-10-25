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

- **Filter.** One text field searches every shown field: reference, product name, any level of the category path, and brand. There are no separate category or brand dropdowns (changed 2026-10-25 after reviewing the second build; this replaces the earlier category-with-descendants and brand filters). Typing a parent category name such as "Pijames" therefore matches its subcategories through the path. The filter combines with the selected period and applies to the loaded rows at once, without reloading.
- **Layout.** The text filter sits in the top card with the period presets (and the custom dates when shown). "Group by" (None / Category / Brand) and "Expand all" / "Collapse all" sit together in one toolbar directly above the table, below the summary; the expand and collapse actions are shown only while grouped.
- **Period.** Show period presets as buttons: Today, Yesterday, This week, This month, Previous month, Custom. A preset loads immediately. Custom shows the from/to dates; the report loads when the range is complete and valid (picker closed, field left, or Enter). There is no `Load` button on this screen. Keep the week navigation the existing ticket asked for if it fits. `ReportPeriodSelector` is shared by five reports (product, sales, payment, tax, cash closing): add this as an option so the other reports keep their current behaviour until they adopt it.
- **Summary.** Above the table, show total amount, units and number of products. When there are manual lines, say how much of the total they are ("Includes 240,21 € in 30 manual lines").
- **Columns.** Product, Reference, Category (only when not grouped by category), `#`, Amount. Amount and `#` are narrow, right-aligned, in IBM Plex Mono with tabular figures; text columns use Manrope. The product column takes the remaining width and shows the name without repeating the reference. The category shows its full path from the top-level parent ("Roba de casa › Pijames Dona › Micro"), with the ancestors muted and the leaf emphasised.
- **Manual lines.** Shown as their own row or group at the end, labelled "Manual lines" with a "No product" status badge (`warning` colour plus the word) and a short explanation. They are included in the totals and never shown as a blank row.
- **Total.** A labelled total row at the bottom of the table, including manual lines.
- **Grouping and order.** A "Group by" switch with None, Category and Brand; Category is the default (changed 2026-10-25 after reviewing the first build: the ungrouped list was harder to read than the grouped design). In Category mode, show a hierarchy of category → subcategory (and deeper descendants), with each product directly under its own category. Since indentation conveys the hierarchy, each 56px, single-line header shows only its own category name (changed after the fourth review); its tooltip retains the full path. Beside the bold name, show the product count and a locale-formatted share with one decimal in smaller muted text. The subtotal includes the category **and all its descendants**; ancestors and children fold independently. All groups, including Brand and manual lines, start **collapsed** for a compact overview; opening a parent does not automatically open its children. Sort top-level categories and each level's subcategory headers and direct products by sales amount (€), highest first (restored after the category-then-product alphabetical experiment); break ties by name. Unassigned products are a separate group before manual lines. Do not add parent and child header totals together: only top-level groups plus the manual group must sum to the overall total. This replaces the earlier flat grouping by the product's own category and the still-earlier immediate-parent grouping. The ungrouped default order is also by sales amount descending; Brand retains its amount-based group ordering. Each group header shows units, amount and share of the total and can be collapsed; "Expand all" and "Collapse all" are available. Products without a brand go into a clearly named group.
- **Not in this ticket:** CSV export and printing, which appear in the design.

## Done when

- One text filter matches reference, product name, any category path level and brand, combines with the period, and shows a clear empty result. No category or brand dropdowns remain.
- The filter is in the top card with the period presets; grouping and expand/collapse share one toolbar above the table.
- A preset loads the report with one tap; a custom range loads once both dates are valid, with no `Load` button. Other reports using `ReportPeriodSelector` behave as before.
- Manual lines appear as a labelled row or group, never as a blank row, and are included in the summary and total.
- The report shows a full category breadcrumb, narrow right-aligned amount and `#` columns in the mono font, and a product-name column that expands with the table without hiding important values.
- The report opens with a collapsed category/subcategory hierarchy ordered by sales amount. Parent and child fold independently; parent subtotals include descendants without double-counting in the grand total. Expand all opens every level. Top-level group subtotals and the manual group add up to the same total as the ungrouped view. Switching to None and Brand works.
- The filters, dates, grouping, and column layout are checked with representative category/brand data and manually on the report screen in English, Spanish, and Catalan, including keyboard use and resizing.

## Implementation status

The third review removed category and brand dropdowns and their catalogue-brand query. The one local text filter now matches reference, stored product name, full category path and brand; its magnifier and translated placeholder sit inside the period card, alongside presets when space permits and below them when narrow. The summary figures sit together at the left, and the grouping/expand/collapse toolbar is directly above the table. Catalan actions read "Desplega-ho tot" and "Plega-ho tot".

Automated coverage checks the search terms, empty result, group controls, keyboard bindings, custom period and layouts in English, Spanish and Catalan at 800, 1000 and 1280 px. Off-screen FlatLaf previews of that report were inspected at those widths; the live POS window still needs a manual keyboard/focus and translated-flow walkthrough before marking the ticket done.

The fourth review tightened the group header to one 56px line with an indented local category name, product count and a locale-formatted one-decimal share. Expand/collapse button widths now follow their text and current font rather than storing an initial width; the layout check measures actual label fit, including after a font-size change, not just the toolbar bounds. The manual group remains last and the labelled total follows it. `./gradlew spotlessApply integrationTest --tests com.openbravo.pos.reports.ProductSalesTest` passes. FlatLaf off-screen previews at 800, 1000 and 1280px were reviewed in all three languages. A running POS using `dev-pg.properties` was opened to Product sales in Catalan at a wide window: month selection loaded shop rows, translated actions were fully visible, group percentages had one decimal, and the manual group immediately preceded the total when scrolled to the foot; pressing Tab once moved focus to a table group with a visible outline. Accessibility access to the app window was denied and the POS process closed before mouse and full focus-order checks. **Still open:** actual 1280px/minimum-width checks and full mouse/keyboard/category-depth flows in Catalan, Spanish and English. Do not mark the ticket done on the strength of screenshots alone.
