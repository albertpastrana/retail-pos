# Improve stock diary editor layout

Captured: 2026-09-23

Related: `tickets/todo/remove-swing-layout-antipatterns.md`

## Goal

Make the stock movement editor comfortable and visually consistent across supported window sizes, display scales, fonts, and translations.

## Context

`StockDiaryEditor` now uses an adaptive `GridBagLayout` for its main fields, but the generated layout code with manual `setBounds` calls remains in the class and should be removed. The right-hand editor and the catalog area also need a visual pass for spacing, proportions, and small-window behaviour.

## Done when

- The editor contains no regular `null` layout or manual child `setBounds` positioning.
- The form, action buttons, and labels remain aligned without clipping in English, Catalan, and Spanish.
- The editor and catalog use space proportionally at supported window sizes and display scales.
- The stock movement screen remains usable below `1024x768`.
- Relevant automated checks and a manual visual check pass.
