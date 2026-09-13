# Catalogue sale percent

Captured: 2026-09-10

Related: `tickets/todo/product-labels.md`, `tickets/done/loyalty-stamps-on-ticket.md`

## Goal

Mark products on sale (e.g. 20%) so the till applies that percent on the line. Sale lines do not earn loyalty stamps.

## Context

This is a percent on the product (`SALE_PERCENT`), not brand “2 for X € off” (`tickets/todo/brand-offers.md`) and not a printed tag system (`tickets/todo/product-labels.md`).

## Done when

Stock can set a sale percent; scanning the product puts a discounted line on the ticket.

## Shipped

`PRODUCTS.SALE_PERCENT` (migration `V9`), `SaleMarkPanel` / `SaleService`, `LineDiscount.shouldApplyCatalogSale` in `JPanelTicket`. Loyalty skips sale/discounted lines (`LoyaltyStamps.countsForStamp`).
