# Simplify product search

Captured: 2026-09-27

## Goal

Make product search in sales and stock screens a simple live “contains” search while the operator types.

## Context

Use the same predictable search behaviour in both product-search flows. Remove the adjacent search-mode selector, including its regular-expression option; operators should not need to choose a matching mode. Keep the search responsive as text is entered.

Show a localized placeholder in every text field in the sales and stock product filters so each searchable field is clear. Remove obsolete code left behind by deleting the search-mode selectors.

In the sales product finder, category is a selection rather than free text: provide a dropdown backed by the existing product categories, including an all-categories option.

When the sales product finder displays tax-inclusive sale prices, label that column “PVP” instead of “Price + taxes”.

## Done when

- Product search in sales and stock screens filters as the operator types, matching text contained in the product fields currently searched.
- Neither screen shows a selector for search modes or regular expressions.
- Every text field in both filters has a useful localized placeholder.
- The sales category filter is a localized dropdown populated from product categories and can be cleared to show all categories.
- The tax-inclusive price column in the sales product finder is labeled “PVP”.
- Clearing the query restores the unfiltered results, and existing product selection behaviour still works.
- Search labels and feedback remain usable in English, Spanish, and Catalan at supported window sizes.

## Verification pending

Manual validation is still needed in the sales product finder and stock product list: confirm live filtering, clearing, selection, and translated labels at supported window sizes in English, Spanish, and Catalan.
