# Catalogue variants: grouping, stock and sales

Project setup, build, and import tools: [README.md](README.md).

How to treat Ysabel Mora items that share a model (e.g. a t-shirt) but differ by colour, size and EAN — so you can search by name, see stock per variation, and get totals like “100 of this t-shirt, 10 of this size, 50 of that size”.

## Requirements

1. **Till / barcodes** — each physical variant keeps its own EAN. Scanning must hit the right size and colour.
2. **Stock** — search by product name (e.g. “t-shirt”) and see how many units exist of each variation.
3. **Sales** — the same grouping: units sold of the model, then a breakdown by size/colour.

## What this POS actually stores

| Piece | Table / field | Meaning |
|---|---|---|
| One sellable SKU | `PRODUCTS` | One row. `CODE` (EAN) and `NAME` are **unique**. |
| Barcode | `PRODUCTS.CODE` | Scanner lookup. Extra codes can sit in `BARCODE_TABLE`, but they still point at the **product**, not a size. |
| Stock | `STOCKCURRENT` | Units **per product id** (optionally per attribute instance). |
| Ticket line | `TICKETLINES.PRODUCT` | Sales are counted per product id. |
| Model in our import | `REFERENCE` prefix | e.g. `12733` in `12733UNGR` / `12745T37/41BLN`. |

Built-in reports (`soldproducts`, `productsales`, inventory) `GROUP BY PRODUCTS.NAME` (and often `REFERENCE`). Two sizes never add up unless they are the **same product row**.

The product filter in **Stock → Products** is a flat search on name. There is no parent/child screen.

## Option A — one product row, attributes for size/colour (do not use)

Openbravo has **Attribute sets** (Stock → Attributes). You attach Colour and Size to one product named “T-shirt”. Stock and ticket lines can store the combination (`ATTRIBUTESETINSTANCE_ID`). Inventory detail reports even have unused fields for the attribute description.

This looks like the requirement on paper. It is a poor fit for this shop:

- Scanning an EAN only finds the product. Extra barcodes do not select a size.
- Adding a line does not ask for size; you would open **Edit attributes** afterwards.
- There is **one sell price** per product. Several orders already use different prices by size (e.g. kids pyjamas 8,00 vs 9,20).

Use this only if you stop scanning per-size EANs at the till.

## Option B — one SKU per EAN, group by model (recommended)

Keep the current grain: one `PRODUCTS` row per barcode. Treat the Ysabel Mora **model number** as the parent identity. Colour and size are part of the SKU, not a separate product.

### Catalogue layout

| Field | Role | Example |
|---|---|---|
| Model | Totals and search key | `12733` |
| `REFERENCE` | Model + variant, unique | `12733UNGR` or `12733-NEGRO-36-41` |
| `CODE` | EAN-13 of that variant | `8434506…` |
| `NAME` | Searchable title + unique suffix | `CALCETÍN MUJER TRANSPIRABLE — NEGRO / 36-41` |
| Category | Till browsing only | Calcetines, Bragas… **not** one category per model |

`NAME` must stay unique, so two variants cannot share an identical string. Put colour and size after an em dash (or similar) so a search for the title still matches every variant.

Do **not** create a category per model. Categories are for the sales-screen catalog, not for variants.

### Stock today (no code change)

Type part of the model title in **Stock → Products**. Every variant whose `NAME` contains that text appears, each with its own stock — if stock has been received into `STOCKCURRENT` (importing the catalog does not create stock by itself).

That is a flat list, not “header + 100 total + breakdown”.

### What to add for the screens you described

The data is already in `REFERENCE` / `NAME`. The missing piece is **grouped reports** (and optionally a small stock screen):

1. **Stock by model** — filter `NAME` or model code; show total units; then one line per colour/size.
2. **Sales by model** — same grouping on `TICKETLINES` joined to `PRODUCTS`: total units of the model, then units per variant.

Grouping key: leading model code of `REFERENCE` (digits, or `PU` + digits for posters). Variant key: the remainder of `REFERENCE`, or the suffix after ` — ` in `NAME`.

No schema change is required for that. A new column for “model id” would only help if you do not want to parse `REFERENCE`.

## Decision

| Need | Approach |
|---|---|
| Scan the right EAN | One product per EAN (option B) |
| Search “t-shirt” and see sizes | Shared name prefix + grouped stock report |
| “100 sold, 10 of this size” | Grouped sales report on model code |
| One button on the till for all sizes | Attribute sets (option A) — conflicts with barcodes and prices |

**Organise the catalogue as option B.** Implement the two grouped reports when you want the charts; do not collapse SKUs.

## Current data (after the pedido prune)

- 203 SKUs, 22 models that existed in the Excel catalog.
- 132 models from the PDFs were never in that catalog, so they are not in the POS.
- References already encode the model (`12733UNGR`). Names currently look like `… [12733UNGR]`; renaming to `Title — colour / size` would make the product filter more usable without waiting for a new report.
