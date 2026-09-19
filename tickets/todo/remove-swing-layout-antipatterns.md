# Remove Swing layout antipatterns

Captured: 2026-09-20

Related: `tickets/todo/edit-sale-button-label-layout.md`
Related: `tickets/todo/replace-legacy-numeric-keypads.md`

## Goal

Replace fragile Swing layouts and fixed component geometry with adaptive layouts that remain usable across supported window sizes, display scales, fonts, and translations.

## Context

The UI contains 14 regular `null` layout assignments with manual `setBounds` positioning, numerous fixed preferred/minimum/maximum dimensions, rigid dialog sizes, and generated layouts with large absolute gaps.

Prioritise user-facing forms and dialogs. Preserve intentional fixed sizing for touchscreen controls, kiosk mode, receipt/label printing, customer displays, and custom rendering/layout implementations unless testing shows a problem.

The first implementation slice should cover:

- `JFind`
- `JSort`
- `ProductFilterSales`
- `PaymentPanelMagCard`
- `JPaymentCashPos`
- `StockDiaryEditor`
- `TaxEditor`
- `TaxCustCategoriesEditor`
- `CategoriesEditor`
- `JProductLineEdit`
- `DeviceFiscalPrinterJavaPOS`
- `JDlgChangePassword`
- `JPanelNull`

Use standard layout managers and `pack()` where appropriate. Do not change business behaviour.

## Done when

- No regular user-facing form uses `setLayout(null)` or manual child `setBounds` positioning.
- Intentional exceptions are documented in code or in this ticket.
- Fixed component dimensions are removed where they prevent resizing, localization, font scaling, or HiDPI support.
- Dialogs calculate their size from their contents and remain usable at supported resolutions.
- Touch-friendly controls retain their required minimum interaction size.
- Catalan, Spanish, and English labels remain visible without clipping.
- The affected screens work at resolutions below `1024x768` and with increased display scaling.
- Relevant automated checks pass.
