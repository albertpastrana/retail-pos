# Edit customer allowed debt

Captured: 2026-10-08

Related: `tickets/done/customer-on-ticket.md`

## Goal

Allow the customer sheet to edit the allowed debt limit, and make new customers start with an allowed debt of 100 EUR.

## Context

The customer record already stores `MAXDEBT` and the existing debt behaviour must remain unchanged. This change is limited to the customer screen and new-customer default; it does not change debt accounting or payment rules.

Screenshot feedback (2026-10-08): the backoffice customer editor stretches the allowed-debt input, notes show a white scroller instead of a themed field, and the screen repeats the navigation title. Keep the notes field comfortably tall without an always-visible scrollbar; retain keyboard access to longer notes. Review the screenshot for other immediate layout problems.

First-selection feedback (2026-10-08): on entering the screen, clicking the first customer row does not open its details until another row has been selected. The initial list selection must agree with the customer shown on the right, including after refreshing/filtering the list.

Implementation in the working tree: the editor uses a content-sized six-line notes field with theme-backed backgrounds, normal-height debt input, and one navigation title. Zero debt is neutral rather than danger-coloured. Layout checks cover light/dark at 1024×768 and 1600×900; customer debt and stock receiving checks pass. Awaiting a fresh screenshot or operator confirmation of the running customer screen before calling the visual work shipped.

The initial-selection fix now opens the first customer's details as soon as the list loads, preserves the current row on refresh where possible, and clears selection rather than replacing unsaved edits. An automated test covers initial selection, switching rows, filtering to one row, empty results and returning to a customer. Awaiting operator verification in the running application.

Pull-request review found three more cases: empty search results now preserve unsaved edits, declining a row change clears a misleading highlight if the edited customer is filtered out, and grouped Catalan currency (for example `1.234,00`) can be read back when editing a customer. Automated regression checks cover the empty search and grouped amount.

## Done when

A cashier can change and save the allowed debt from the customer screen. Creating a new customer starts the allowed debt at 100 EUR. The value remains correct after reopening the customer. The initial first row opens the matching customer details, and filtering does not leave a highlighted row with unrelated details. The editor shows one screen title, normal-height inputs and a larger notes area styled by the active FlatLaf theme in light and dark modes. The user confirms the screenshot-based visual result.
