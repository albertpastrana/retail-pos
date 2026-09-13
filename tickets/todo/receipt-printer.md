# Receipt printer in the shop

Captured: 2026-09-10

## Goal

Each till prints tickets (and opens the drawer) on its own ESC/POS printer.

## Context

How to configure printers is documented in `escpos-printer.md`. The POS writes raw ESC/POS; do not use the OS spooler path (`printer`) for cutter and drawer.

This ticket is wiring the shop machines, not rewriting that guide.

## Done when

The tills that must print, print. Drawer kick works where there is a drawer.
