# Product fallback bulk updates

Captured: 2026-09-19

## Goal

Apply product price and category changes consistently to every product in the fallback flow opened from a sales line.

## Context

When a product does not exist and the operator opens the fallback from the sales line, changing the price sometimes does not update all products, especially when many products are involved. Changing the category also does not assign it to every product, although this was expected to work previously.

## Done when

- A price change in the fallback is applied to every selected product, including when the selection is large.
- A category change is assigned to every selected product.
- The updates are persisted and are visible when the products are opened again.
- Partial failures are reported clearly instead of silently leaving products unchanged.
