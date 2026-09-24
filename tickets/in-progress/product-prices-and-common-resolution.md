# Product prices and common resolution layout

Captured: 2026-09-24

## Goal

Make product prices visible while editing products and keep the till's loyalty
notice readable at the common 1440x900 resolution.

## Context

The product navigator currently shows only reference and name even though the
editor already exposes the product price fields. The list must show the PVP,
meaning the sell price including the product tax, rather than the stored net
sell price. The product editor also needs a compact general tab: reference and
family share a row, the barcode sits below the name, factory price and its
tax-inclusive cost sit together, the image moves to its own tab, and unsaved
changes can be discarded explicitly. The loyalty notice is placed
in a narrow fixed-width panel and its HTML does not declare a text width.

The first slice makes prices visible in the product list. Editing multiple
prices at once remains a follow-up because the current editor and save provider
operate on one product at a time.

The sales ticket column definitions now live in Java instead of the database
resource. The item column is 470 px, units is 10 px, tax is 10 px, and both
price and total are 45 px. Ticket headers follow the same left/right alignment
as their cells. JTable automatically resizes the columns with the table, and
the fixed row height is 35 px.
Long item names are truncated with `...`; the full name is available in the
cell tooltip. Row height remains fixed.

The sales table now delegates selection colours and numeric cell rendering to
the standard Swing/FlatLaf renderers. Only the item cell keeps a custom renderer
for width-aware ellipsis and the full-name tooltip. The table uses 16 pt Plex
Mono and 13 pt Manrope headers.

## Done when

- The product management list shows the PVP, including the product tax, next to
  its reference and name.
- The product editor's general tab is smaller and places reference/family,
  name/barcode, and factory price/tax-inclusive cost as requested.
- The product image is available in its own tab without changing image save
  behaviour.
- The editor has a discard button that restores the current saved record and
  leaves it clean.
- The loyalty notice reserves enough width for its wrapped text and remains
  readable at 1440x900.
- Existing product editing and till behaviour remain unchanged.

## Verification

`./gradlew spotlessApply compileJava` and `./gradlew test` pass. A manual
1440x900 Swing check is still required before moving this ticket to `done/`.
