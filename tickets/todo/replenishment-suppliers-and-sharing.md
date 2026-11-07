# Assign suppliers and share replenishment requests

Captured: 2026-11-01

Related: `tickets/done/sort-replenishment-export-by-brand-family.md`
Related: `tickets/todo/replenishment-next-step-status-action.md`
Related: `tickets/todo/replenishment-sales-and-admin-modes.md`
Related: `tickets/todo/replenishment-customer-phone.md`

## Goal

Assign shared supplier contacts to pending replenishments and customer orders, navigate their requests, and send or export an order without exposing customer information.

## Current decisions

- Suppliers have a business name, contact first/last names, WhatsApp number and optional email. Administration shows a 200–232px supplier rail at widths ≥980px and a combo below that width; sales mode hides supplier, assignment and export tools. The mode-specific list/detail layout is defined in the related sales/admin ticket. The application bar owns the screen title.
- The rail contains All suppliers, Unassigned, then named suppliers with active-status counts. Active/focused and hovered rows use shared rounded surfaces and tokens. Show a ghost + New supplier below the list. Each named row exposes a 36px ghost ellipsis when active/hovered; its Edit action opens the same packed single-form dialog used for creation, while Delete confirms that N requests for that supplier become Unassigned. Deletion unassigns before deleting the contact; when the active supplier is deleted, return to All suppliers. The supplier header's Edit action also opens the form. New supplier from a selected detail's Change menu or bulk Assign to action assigns the newly saved contact immediately.
- Only selected *pending* requests can be assigned/reassigned in bulk. Preserve their customer and status. The unassigned view stays newest-first; a named supplier view groups by catalogue brand/family and places entries with neither last. Show Supplier as a separate table column only in a non-compact All suppliers administration view; narrow administration lists include supplier/customer in the product caption.
- Status filters form a single horizontally scrollable row (Pending, Ordered, Received, Delivered, All); no labels wrap or clip. Details offer a branded next-step action and anchored keyboard-accessible status/supplier menus. Delivery requires a customer; status changes save unsaved detail edits together. Content derives widths from labels and uses scale-aware touch minimums (40px segments within a 48px shell, 44px search/new-request, 48px primary action). Preserve shared light/dark design tokens.
- Sharing uses an editable modal with WhatsApp/Text file/Print channels, All visible/Only selected scope, and brand/family-sorted reference/name text. Never include customer names, phones, notes or sellers. WhatsApp requires one supplier and a valid number; opening a link alone changes nothing. Only confirmation that it was sent, or successful UTF-8 file writing, marks *included pending* entries Ordered. Print/cancel/error leave statuses unchanged. Without a valid WhatsApp number default to Text file with a visible reason.
- The selected status filter remains active after advancing an entry, even when the moved request disappears from that filtered view. The list has a real select-all checkbox, mono references over product names (at most two ellipsized lines and full tooltip), compact dates and a footer showing selection/scope. A Received request without a customer offers Choose customer and explains why delivery is blocked. Use the customer finder for customer changes.
- Design reference: `https://claude.ai/artifact/EyrB94BHgeZZYzqjeweSdH`. The artifact content was unavailable through tooling; the decisions here and in the related mode/phone tickets are the implementable reference.

## Done when

- Supplier/contact records survive migration and restart on Derby, MySQL and PostgreSQL, including a second migration pass. Deleting a supplier leaves its requests readable and unassigned.
- Supplier and Unassigned filters, named-supplier grouping, pending-only assignment and new-supplier-from-Change/Assign flows work; the single supplier form and ellipsis menu are accessible by keyboard and pointer.
- Selected visible requests—or all visible requests if none selected—appear in the correctly sorted outgoing text, without any customer/phone/note/seller data. Text-file success or confirmed WhatsApp send updates only included pending statuses; cancellation, printing and failures do not.
- CA/ES/EN labels, supported widths/scales, both themes and the actual sales/admin screens are checked. Record MacBook Air/TPV tests of keyboard focus, supplier menus, WhatsApp handoff, file chooser and printing before closing.

## Verification and open checks

- `V58__replenishment_suppliers` and its checksum were added. Local `docker compose up -d --wait`, `./gradlew ciCheck` (Derby/MySQL/PostgreSQL and test-lane coverage), `docker compose down -v` passed; containers were removed. Focused supplier, status, sharing, order-text, layout, mode and theme-switch tests pass on the Mac. Derby integration coverage checks creation/editing, bulk assignment, preserved unsaved notes, deletion/unassignment, grouped rows and status transitions.
- Baseline FlatLaf Light 1× → Dark 2× reproduced a clipped New request action (165px available vs 215px needed); the repaired action passes an EN/ES/CA × 980/1200/1440 × 1/1.25/1.5/2 offscreen matrix. Offscreen screenshots under `build/reports/replenishment-screenshots/` cover sales/admin views, details and the new/edit/send dialogs in both themes. Inspected representative CA/ES/EN captures at narrow and wide widths. These do not prove a running cashier workstation scale or a real WhatsApp/print handoff.
- PR CI initially found `HeadlessException` in Swing-dialog tests. The Derby build lane now runs under Xvfb; the release `ciCheck` also uses Xvfb. A second Linux run exposed a confirmation-button minimum-height difference; both confirmation actions now derive a scale-aware 48px minimum without fixed widths. CodeRabbit also identified a persistent supplier left by a migration test; its cleanup now deletes the entries and then the supplier. **Re-run and confirm all CI checks and CodeRabbit review before merging.**
- Still open: manual pointer/keyboard/focus, live customer-finder selection, supplier deletion confirmation, WhatsApp send confirmation, file chooser, printing, and display scale on the actual MacBook Air and POS. Keep this ticket in `todo/` until these checks are recorded.
