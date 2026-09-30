# Improve the product sales report

Captured: 2026-09-28

Related: `tickets/in-progress/rebuild-management-reports.md`

## Goal

Make the product sales report easier to narrow down by product and period, easier to read, and optionally view by immediate parent category.

## Context

The current product sales screen (`JPanelProductSales`) offers a period selector and a table with reference, product, category, units, and amount. This ticket concerns that report specifically; coordinate with the ongoing reporting-module work.

- Add a category filter that includes descendant categories: choosing a parent such as Pyjamas includes all its subcategories, while choosing Cotton pyjamas includes only that category and its descendants.
- Add free-text filtering over product references (and product names if practical), plus a brand filter for sales of products belonging to the selected brand. Filters should combine with the selected period.
- Prefer a single date-range picker in which one interaction chooses both start and end. If that is impractical, offer simple period presets such as last 20 days, previous week, and last month, with controls to move backward and forward by week. Keep custom date selection available.
- Make amount and units substantially narrower; use `#` as the units header. Show the full category path from top-level parent to the product's category. Give the product-name column the remaining horizontal space.
- As a secondary enhancement, allow the table to be grouped by the product category's **immediate parent**, not by its highest ancestor (e.g. keep the pyjama subcategories together under Pyjamas). Define a sensible group for products without a parent category and keep the ungrouped view available.

## Done when

- Selecting a parent category includes products in its descendant categories; selecting a child restricts results to that subtree. Brand and reference/name text filters work alone and in combination with category and period, including an empty-result case.
- A cashier can choose a useful date interval without manually picking both endpoints every time; the implemented picker or presets support navigating successive weeks and still allow a custom interval.
- The report shows a full category breadcrumb, narrow amount and `#` columns, and a product-name column that expands with the table without hiding important values.
- The optional grouped view groups by immediate parent category, handles root/unassigned categories, and can be switched off; sales totals and units remain consistent between views.
- The filters, dates, grouping, and column layout are checked with representative category/brand data and manually on the report screen in English, Spanish, and Catalan, including keyboard use and resizing.
