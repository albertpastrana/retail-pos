# Product labels / tags

Captured: 2026-09-10

Related: `tickets/backlog/catalog-variants.md`, `tickets/todo/brand-offers.md`, `tickets/done/gift-vouchers.md`

## Goal

The application does not provide product-label or product-barcode printing.

## Context

Shop **families** (Sostenidors, Calces, Samarretes Home…) are already categories. Mapping rules: `external-data/categories-botiga.md`. Do not use tags to rebuild that tree.

**Model family** (same t-shirt, other size/colour, scan a barcode to see siblings) is `tickets/backlog/catalog-variants.md`. Colour and size stay on the SKU name / reference, not as tags.

Tags are a separate future catalogue concern: e.g. tèrmic, novetat, rebaixes, encàrrec — things that cut across categories and are not a sellable variant.

Not specified yet: free text vs a closed list. Printing product labels and barcodes is explicitly out of scope. Do not invent a promo engine here (`tickets/todo/brand-offers.md`).

## Special-product barcodes

The product editor should allow assigning or generating a unique barcode for products that do not arrive with a manufacturer code, such as bags, vouchers, or arrangements. The barcode must behave like the product's normal code at the till so it can be scanned instead of searched by name.

Assignment must prevent collisions with existing product and alternative barcodes, and voucher handling must retain the existing `ISVOUCHER` behaviour. Product barcodes remain usable for lookup and sales, but are not printable from the application.

## Done when

The product editor has no label-printing action, the stock menu has no product-label report, and the product-label printing implementation is removed. Product barcodes remain usable for lookup and sales.

## Shipped

Removed the product label button, stock-menu entry, label printer classes, product printer template, related icon, and translations. Receipt barcode printing remains available.
