# Remove space-to-payment shortcut from till

Captured: 2026-11-23

Related: `tickets/done/unknown-barcode-notice.md`

## Goal

Space never opens payment from the till, including when a scanner inserts it in a barcode.

## Context

The physical scanner was confirmed to send `843322110201 7` followed by Enter. The till currently treats a space as the Total action even while the code field contains the barcode prefix; Enter can then confirm the default payment button. The scanner cannot be reconfigured. The cashier confirmed that the space-to-total shortcut has never been used and requested its complete removal. Ignore spaces typed into the code field so the remaining digits and Enter still look up the intended product. Preserve equals, empty-field Enter and explicit payment actions.

The submitted video shows two ticket lines, but the available logs do not identify its exact sale; scanning this string need not have produced a product-lookup log because the space took the payment path first.

## Done when

- Scanning `843322110201 7` followed by Enter with two lines on a ticket does not open payment on the space, and attempts to look up `8433221102017` on Enter.
- Space cannot open payment with either an empty or a nonempty code field; normal barcode and keyboard entry, empty-field Enter, equals, and explicit Total still work.
- Focused checks cover scanner input and the removed space shortcut in both field states.

## Verification pending

The software change and focused Swing tests on `fix/scanner-space-payment` pass `./gradlew spotlessApply`, `./gradlew integrationTest --tests com.openbravo.pos.sales.ScannerSpacePaymentTest`, and `./gradlew check verifyTestLaneCoverage fastTest`. The tests cover spaces in both empty and populated code fields and preserve explicit equals payment. A physical till scan still needs verification: a two-line ticket remains open after scanning the affected barcode, the unknown-code notice appears when lookup fails, and space cannot open payment even with an empty code field. No cashier-facing label or layout changes were made.
