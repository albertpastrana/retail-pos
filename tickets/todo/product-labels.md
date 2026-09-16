# Product labels / tags

Captured: 2026-09-10

Related: `tickets/todo/catalog-variants.md`, `tickets/todo/brand-offers.md`

## Goal

Staff can attach labels or tags to products so the catalogue can be classified beyond category, brand, and sale percent (filters, grouping, maybe printing).

## Context

Shop **families** (Sostenidors, Calces, Samarretes Home…) are already categories. Mapping rules: `external-data/categories-botiga.md`. Do not use tags to rebuild that tree.

**Model family** (same t-shirt, other size/colour, scan a barcode to see siblings) is `tickets/todo/catalog-variants.md`. Colour and size stay on the SKU name / reference, not as tags.

Tags are the extra layer: e.g. tèrmic, novetat, rebaixes, encàrrec — things that cut across categories and are not a sellable variant.

Not specified yet: free text vs a closed list, and whether printed shelf labels are in scope. Do not invent a promo engine here (`tickets/todo/brand-offers.md`).

## Done when

A product can carry labels, they are visible where staff would use them, and they survive across tills on the shared database.
