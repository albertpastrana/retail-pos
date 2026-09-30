# Simplify product search

Captured: 2026-09-27

## Goal

Make product search in sales and stock screens a simple live “contains” search while the operator types.

## Context

Use the same predictable search behaviour in both product-search flows. Remove the adjacent search-mode selector, including its regular-expression option; operators should not need to choose a matching mode. Keep the search responsive as text is entered.

Show a localized placeholder in every text field in the sales and stock product filters so each searchable field is clear. Remove obsolete code left behind by deleting the search-mode selectors.

In the sales product finder, category is a selection rather than free text: provide a dropdown backed by the existing product categories, including an all-categories option.

When the sales product finder displays tax-inclusive sale prices, label that column “PVP” instead of “Price + taxes”.

The sales product finder also shows current stock as the sum of STOCKCURRENT
across locations. Its title is localized as product search, and the reference,
name, stock, and price columns are aligned left, centre, right, and right. The
stock column is kept narrow and the dialog viewport is 900 px wide.

## Done when

- Product search in sales and stock screens filters as the operator types, matching text contained in the product fields currently searched.
- Neither screen shows a selector for search modes or regular expressions.
- Every text field in both filters has a useful localized placeholder.
- The sales category filter is a localized dropdown populated from product categories and can be cleared to show all categories.
- The tax-inclusive price column in the sales product finder is labeled “PVP”.
- The sales product finder shows current stock and uses the requested column
  alignments and narrow stock column.
- The sales product finder title is localized as product search.
- Clearing the query restores the unfiltered results, and existing product selection behaviour still works.
- Search labels and feedback remain usable in English, Spanish, and Catalan at supported window sizes.

## Verification pending

Manual validation is still needed in the sales product finder and stock product list: confirm live filtering, clearing, selection, and translated labels at supported window sizes in English, Spanish, and Catalan.

## Shipped

Implemented in commits `2f5b1f85`, `9a0e8fb1`, `51dd8cd6`, and `e0aa66aa`.
Live contains-search filtering, localized placeholders, category selection,
tax-inclusive PVP labeling, stock display, and the requested table layout are
in the tree. The ticket is marked done for the shipped scope; the documented
manual sales and stock-flow checks remain follow-up verification.
