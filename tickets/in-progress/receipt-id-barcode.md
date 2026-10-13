# Receipt ID barcode

Captured: 2026-10-12

## Goal

Print a scannable receipt identifier on the default receipt and gift receipt, and let a cashier scan it from the sales screen to open the existing receipt options.

## Context

Use the existing numeric receipt ID as the visible value. Encode sales as `T<id>` and refunds as `R<id>` so receipt scans cannot be confused with product scans and the receipt type remains available. Remove the receipt number from the receipt header and show it as the human-readable barcode value. Keep the barcode short vertically.

The XML printer parser already supports barcode tags. Existing installations must be upgradeable without a schema migration and without overwriting customized receipt templates.

## Done when

- Default sale, preview, and gift receipt templates print a short barcode and no duplicate receipt number in the header.
- Existing printers supported by the application print and scan the encoded receipt identifier.
- A scanned sale or refund barcode from the sales screen opens the existing receipt options flow.
- Product and weighted-product scanning remains unchanged, including when a product code is numeric.
- Existing installations can apply the default template change without a database schema migration or unconditional replacement of customized templates.
- Focused automated checks cover barcode template output and receipt lookup routing.
