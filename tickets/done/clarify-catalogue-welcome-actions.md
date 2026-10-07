# Clarify catalogue welcome actions

Captured: 2026-10-07

Related: `tickets/done/share-welcome-workflow-cards.md`

## Goal

Make the catalogue and prices welcome cards clearly distinguish editing
products and prices from changing a brand's pricing rules.

## Context

Replace the markdown shortcut with an action that opens the product screen for
editing product details and prices. Keep the new-product card as the create
flow. Make the price-rules card explicitly describe pricing rules in English,
Spanish, and Catalan. Keep the same behaviour in search-result shortcuts.

## Done when

- The product-editing card opens `ProductsPanel` without starting product creation.
- The new-product card still starts product creation.
- The price-rules card names pricing rules rather than prices in all three locales.
- No welcome or search shortcut still presents the old markdown action.
- Focused welcome tests pass.

## Shipped

Replaced the markdown welcome action and search shortcut with product editing,
clarified the price-rules wording, and added English, Spanish, and Catalan
coverage in `JPanelStockWelcome` tests.
