# Rebuild management reports from scratch

Captured: 2026-09-18

Related: `tickets/backlog/broken-sales-reports.md`

## Goal

Remove the unused legacy management-report subsystem and replace it with a small, typed, tested reporting module aimed at the needs of a small shop.

The new module should initially provide:

- Daily and period sales summaries.
- Sales by product and category.
- Sales by payment method.
- Tax summaries.
- Cash closing summaries.
- Low-stock and current-inventory views.
- Customer debt.
- CSV export, with PDF export only if there is a concrete shop need.

## Context

The current management reports were spread across BeanShell scripts, embedded SQL, JRXML templates, JasperReports 3.7.6, Swing panels, positional field arrays, and the custom `JRViewer300` fork. Those management resources and their SQL-extraction test have now been removed, including the remaining invoice JRXML templates and the `reports.jar` packaging path.

- `src/integrationTest/java/com/openbravo/pos/forms/ReportStatementsIT.java` (removed with the legacy scripts)
- `reports/com/openbravo/reports/soldproducts.bs` (removed)
- `reports/com/openbravo/reports/productsaletotals.bs` (removed)

This code is not used by the normal daily receipt flow. Closing a sale uses the Velocity/XML printer resources through:

- `src-pos/com/openbravo/pos/sales/JPanelTicket.java:1214`
- `src-pos/com/openbravo/pos/sales/JPanelTicket.java:1257-1259`
- `src-pos/com/openbravo/pos/templates/Printer.Ticket.xml`
- `src-pos/com/openbravo/pos/templates/Printer.Ticket2.xml`
- `src-pos/com/openbravo/pos/templates/Printer.TicketTotal.xml`

The old Jasper path is no longer present in the application source, and no invocation exists in the versioned scripts, templates, SQL, or TSV data. Deployed databases should still be audited independently for stale custom event scripts.

Do not remove the operational receipt-printer path, `Printer.*.xml` resources, `DeviceTicket`, or drawer/printer behaviour as part of this ticket.

The replacement should not reproduce the old generic BeanShell/JRXML architecture. Prefer typed parameters, explicit query/repository code, named result fields or DTOs, a simple table view, and asynchronous execution so a long report does not block Swing.

## Slices

1. Confirm that no installed database event script calls the removed Jasper/report bridge and record any required exception.
2. Remove the unused management report resources and legacy report UI/runtime code, while keeping receipt and invoice behaviour that is confirmed operational.
3. Add the new report model, query boundary, typed parameters, and table presentation.
4. Implement the initial small-shop report set, starting with sales summary, cash closing, product sales, taxes, and low stock.
5. Add CSV export and tests against representative Derby, MySQL, and PostgreSQL data.
6. Add a documented path for adding a report without editing SQL strings inside a script or JRXML expression.

## Done when

- The normal sale, receipt preview, receipt reprint, cash closing, and drawer/printer flows still work.
- No active installation path depends on the removed Jasper/BeanShell management-report subsystem.
- The old `.bs` management reports, `JRViewer300`, unused report-only dependencies, invoice JRXML templates, and `reports.jar` packaging path are removed or explicitly retained with a documented reason.
- The initial reports answer the agreed small-shop questions and do not block the Swing event thread.
- Report queries and representative results are covered on Derby, MySQL, and PostgreSQL.
- Empty results, null customer data, refunds, tax differences, and date filters are tested.
- CSV output is usable by a shop operator.
- The distinction between management reports and operational receipt templates is documented.
