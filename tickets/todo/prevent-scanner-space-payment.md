# Verify scanner space handling at the till

Captured: 2026-11-23

Related: `tickets/done/unknown-barcode-notice.md`

## Goal

Verify with the physical scanner that the software change prevents space from opening payment and still looks up the complete barcode.

## Context

The physical scanner was confirmed to send `843322110201 7` followed by Enter. Before the software change, the till treated a space as the Total action even while the code field contained a barcode prefix; Enter could then confirm the default payment button. The scanner cannot be reconfigured. The cashier confirmed that the space-to-total shortcut was never used and requested its complete removal. The fix in PR #170 ignores spaces typed into the code field so the remaining digits and Enter look up the intended product; equals, empty-field Enter and explicit payment actions remain available.

The submitted video shows two ticket lines, but the available logs do not identify its exact sale; scanning this string need not have produced a product-lookup log because the space took the payment path first.

## Done when

- On an updated till, scanning `843322110201 7` followed by Enter with two lines on a ticket does not open payment on the space, and attempts to look up `8433221102017` on Enter.
- On the updated till, space cannot open payment with either an empty or a nonempty code field; normal barcode and keyboard entry, empty-field Enter, equals, and explicit Total still work.

## Software checks completed

The software change and focused Swing tests on `fix/scanner-space-payment` pass `./gradlew spotlessApply`, `./gradlew integrationTest --tests com.openbravo.pos.sales.ScannerSpacePaymentTest`, and `./gradlew check verifyTestLaneCoverage fastTest`. The tests cover spaces in both empty and populated code fields and preserve explicit equals payment. No cashier-facing label or layout changes were made. Field verification has not started.
