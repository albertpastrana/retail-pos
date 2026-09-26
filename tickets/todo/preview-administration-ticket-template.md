# Preview and test-print ticket resources in administration

Captured: 2026-09-26

Related: `tickets/todo/redesign-admin-screens.md`

## Goal

Show what a ticket template renders beside the Resources editor, and provide a
test-print action without issuing a real sale.

## Context

The administration screen redesign adds search, type filtering, shipped-template
comparison and restore, and a line-numbered text editor. A faithful preview
needs a realistic ticket model and the existing printer template engine; printing
must go through the selected device without producing a sale or cash movement.

## Done when

- Editing a ticket template updates a safe, representative preview without a
  printer or database write.
- Test print names the target printer, asks for confirmation, and prints the
  same representative ticket without affecting sales or cash data.
- Missing device/template errors are explained in English, Spanish and Catalan.
