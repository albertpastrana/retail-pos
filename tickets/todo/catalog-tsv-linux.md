# Catalogue TSV only on Linux

Captured: 2026-09-11

Related: `tickets/todo/postgres-lan.md`, `tickets/todo/pending-barcodes.md`

## Goal

The sell catalogue is `PRODUCTS`. The TSV only pre-fills creates. Only the Linux till needs the file (network path), or it is loaded into a table later.

## Context

The app does not generate the TSV. Without the TSV, other PCs can still sell and record pending EANs; they lose import name/price for unknown codes.

## Done when

Satellite tills do not depend on a local TSV to sell. Import/pre-fill works from the Linux path (or a table) when a product is created from the file.
