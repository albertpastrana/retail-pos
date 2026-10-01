# Add a date-range picker to management reports

Captured: 2026-10-01

Related: `tickets/in-progress/rebuild-management-reports.md`

## Goal

Allow a cashier or manager to select a report's start and end dates from one
calendar dialog, with the complete selected interval visibly highlighted.

## Context

The product sales report currently uses two independent date controls backed by
`JCalendarDialog` and `JCalendarPanel`. Swing has no built-in date-range picker,
but the existing calendar components can be extended without adding a third-
party dependency.

The new interaction should be:

- The first day click sets the start date.
- The second day click sets the end date.
- All days from start through end are highlighted, including both endpoints.
- The dialog exposes the selected start and end dates and confirms the range
  with an explicit action.
- Cancelling leaves the previously applied range unchanged.
- Selecting the dates in reverse order produces a normalized ascending range.

Keep the existing date-only semantics, localization, touch-sized controls, and
light/dark theme behaviour. The report query must continue treating the end
date as inclusive for the operator and as the next day boundary internally.

The first consumer is the product sales report. The component should be
reusable by the other management reports that need a date interval.

Do not add a date/time range picker, replace the project's calendar with an
external library, or change report query semantics as part of this ticket.

## Done when

- Product sales report presents one date-range control instead of separate
  start and end calendar controls.
- A user can select an interval in one dialog and sees every selected day
  highlighted across the visible calendar month.
- Start and end dates are clearly identifiable, including for a one-day range.
- Cancel, window close, and incomplete selection do not apply a partial range.
- Reverse-order selection results in the earlier date as start and the later
  date as end.
- The selected interval is preserved when the report is reset according to its
  existing default behaviour.
- The control is usable with mouse, keyboard focus, and touch-sized controls.
- English, Spanish, and Catalan labels remain visible at supported window sizes.
- Light and dark themes use the shared design-system tokens for the range
  highlight and selected-day states.
- Unit or component tests cover one-day, multi-day, reverse-order, cancel, and
  incomplete selections.
- The report's inclusive end-date filtering remains covered by the relevant
  report tests.
