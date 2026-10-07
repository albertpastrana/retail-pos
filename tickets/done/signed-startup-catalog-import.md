# Signed startup catalog import

Captured: 2026-10-05

## Goal

When the POS starts, it automatically imports signed local catalog files into
the shared database without exposing arbitrary SQL or destructive operations.

## Context

Each till has its own local import directory. CSV and TSV files are accepted,
and every production file must have a detached Ed25519 signature. Other tills
do not see the file, so every till may process its own files.

The importer supports `products` and `fallback-products` files. Product
upserts use barcode (`PRODUCTS.CODE`) as the identity. Existing products may
receive reference, name, category, purchase price, sale price, and brand
updates; stock, tax, history, and internal IDs are not changed. Missing
categories reject the whole file. Fallback rows remain non-sellable.

The import is startup-only, transactional, non-destructive, and reports only
through application logs. Invalid or failed files are rejected rather than
partially applied.

## Done when

- Startup scans the configured local directory and processes complete signed
  CSV/TSV files.
- Invalid, unsigned, duplicate-key, malformed, or database-failing files are
  rejected without partial writes.
- Product upserts are keyed by barcode and never modify stock or product
  history.
- Fallback imports update the shared fallback catalog without deleting rows.
- A valid file is recorded as processed and is not imported again on the next
  startup.
- Ed25519 verification uses Java 21 standard-library APIs with no new crypto
  dependency.
- Documentation explains directory configuration, file schemas, key setup,
  signing, rotation, and rejection logs.

## Shipped

`CatalogFileImporter` scans the configured local directory after database
startup, verifies detached Ed25519 signatures with Java 21 APIs, and applies
transactional product or fallback upserts. Processed and rejected files are
archived and all outcomes are logged. Integration coverage verifies inserts,
updates, fallback price overlays, invalid input handling, and idempotence.
