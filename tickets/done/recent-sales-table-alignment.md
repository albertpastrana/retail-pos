# Align recent sales columns

Captured: 2026-09-25

## Goal

Align the recent-sales table columns consistently in the completed-sale editor.

## Context

The ticket, date, employee, and customer columns should be centered. The total
column should be right-aligned. The selected alignment must apply to both table
values and column headers. Ticket numbers must be displayed without brackets.

## Done when

- Ticket, date, employee, and customer headers and values are centered.
- Total header and values are right-aligned.
- Recent-sale ticket numbers are shown without square brackets.

## Shipped

- Applied the requested value and header alignment in
  `src-pos/com/openbravo/pos/sales/JTicketsBagTicket.java`.
- Removed square brackets from displayed recent-sale ticket numbers.
- Verified with `./gradlew spotlessApply`, `./gradlew spotlessCheck`, and
  `./gradlew compileJava`.
