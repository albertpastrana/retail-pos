# Build an administration welcome screen around workflows

Captured: 2026-09-24

Related: `tickets/in-progress/rebuild-management-reports.md`,
`tickets/todo/batch-stock-receiving.md`,
`tickets/todo/audit-unused-code-and-dependencies.md`,
`tickets/todo/reports-welcome-screen.md`

Design reference (wireframes, not binding on implementation details):
`tickets/todo/administration-welcome-screen/` — each wireframe is there both as
a standalone HTML file to open in a browser and as a PNG rendered at 2x from
it. Editable canvas: https://claude.ai/artifact/SEqtu8UuMe7AR221wqSp4M

| File | Shows |
| --- | --- |
| `Main.html` / `Main.png` | The landing screen at 1280×800: greeting, search, six common workflows, the catalogue starting below the fold |
| `Cataleg.html` / `Cataleg.png` | The same page scrolled: the full catalogue, seven categories |
| `Filtrat.html` / `Filtrat.png` | Search active (`caix`): results replace the common workflows, focus ring, keyboard legend |
| `Estret.html` / `Estret.png` | 1024×768: menu rail, two columns, horizontal cards |
| `Manager.html` / `Manager.png` | Manager role: the System category is absent rather than disabled |

## Goal

Give administrators a task-oriented starting screen where they can choose what
they want to do instead of having to know the application's menu structure.

The screen should make common jobs immediately visible, such as handling
replenishment and customer orders or viewing the latest cash closings, while
also providing an easy way to discover every available administration
workflow.

## Context

The current administration entry point exposes application areas and menus,
but it does not help an operator start from a concrete job to be done.

Use the existing navigation targets and permissions rather than creating a
second implementation of those screens. Do not turn this into a redesign of
every administration screen. The wording, ordering, and grouping of workflows
should be easy to adjust as real usage is learned.

### How navigation works today

- The active menu is built by `JPrincipalApp.buildMenu()` from Java code. It
  creates `MenuPanelAction` / `MenuExecAction` instances whose
  `ACTION_TASKNAME` is the destination class name. The side menu and the
  compact rail share those same `Action` objects.
- `ScriptGroup.addAction()` filters by `m_appuser.hasPermission(...)` —
  `JPrincipalApp.java:273`. `ScriptSubmenu.addPanel()` does **not**: submenu
  entries (`JPanelMenu`) are always drawn, and the permission check only
  happens in `showTask()`, which shows "no permissions" after the click.
- `activate()` opens the first permitted action of the script. Today that is
  Sales for every role.
- `isAvailableTask()` (`JPrincipalApp.java:168`) silently hides entries whose
  class is missing.

### Dead menu entries — do not put these in the catalogue

These are declared in `Menu.Root.txt` but the classes do not exist in the
repository, so `isAvailableTask()` has always hidden them:

| Entry | Class |
| --- | --- |
| `Menu.Floors` | `com.openbravo.pos.mant.JPanelFloors` |
| `Menu.Tables` | `com.openbravo.pos.mant.JPanelPlaces` |
| `Menu.ERPProducts` | `com.openbravo.possync.ProductsSyncCreate` |
| `Menu.ERPOrders` | `com.openbravo.possync.OrdersSyncCreate` |
| `Menu.StockMovement` | `com.openbravo.pos.inventory.StockManagement` |
| `Menu.ProductsWarehouse` | `com.openbravo.pos.inventory.ProductsWarehousePanel` |
| `Menu.Locations` | `com.openbravo.pos.inventory.LocationsPanel` |

`Menu.Floors` and `Menu.Tables` also have no translation in any
`pos_messages*.properties`.

Two further inconsistencies, both out of scope here:

- `Menu.DemoMode` (`com.openbravo.pos.admin.DemoModeAction`) is in the menu
  but no role grants it, so it is unreachable for everyone.
- `TaxCustCategoriesPanel` and `TaxCategoriesPanel` are granted by the
  Administrator and Manager roles but have no menu entry.

### Live destinations (Administrator: 28)

| Destination | Current label | Category |
| --- | --- | --- |
| `sales.JPanelTicketSales` | Sales | Selling and cash |
| `sales.JPanelTicketEdits` | Edit sales | Selling and cash |
| `panels.JPanelPayments` | Cash movements | Selling and cash |
| `panels.JPanelCloseMoney` | Close cash | Selling and cash |
| `panels.JPanelClosedCash` | Cash closed | Selling and cash |
| `inventory.ProductsPanel` | Products | Catalogue and prices |
| `inventory.CategoriesPanel` | Categories | Catalogue and prices |
| `inventory.PriceRulesPanel` | Price rules | Catalogue and prices |
| `inventory.SaleMarkPanel` | Markdowns | Catalogue and prices |
| `inventory.TaxPanel` | Taxes | Catalogue and prices |
| `reports.JPanelLowStock` | Low stock | Stock |
| `inventory.ReplenishmentPanel` | Replenishment and orders | Stock |
| `inventory.StockDiaryPanel` | Stock diary | Stock |
| `customers.CustomersPanel` | Customers | Customers |
| `reports.JPanelCustomerDebt` | Customer debt | Customers *and* Reports |
| `forms.MenuSalesManagement` | How the business is doing | Reports |
| `reports.JPanelSalesSummary` | Sales summary | Reports |
| `reports.JPanelProductSales` | Sales by product | Reports |
| `reports.JPanelPaymentSales` | Sales by payment method | Reports |
| `reports.JPanelTaxSummary` | Tax summary | Reports |
| `reports.JPanelCashClosing` | Cash closing report | Reports *and* Selling and cash |
| `admin.PeoplePanel` | Users | Shop and team |
| `admin.RolesPanel` | Roles | Shop and team |
| `Menu.ChangePassword` | Change password | Shop and team |
| `admin.ResourcesPanel` | Resources | System |
| `config.JPanelConfiguration` | Configuration | System |
| `panels.JPanelPrinter` | Printer | System |
| `admin.BackupDatabaseAction` | Database backup | System |

Manager has the same set minus the four System entries (26). A catalogue entry
may appear in more than one category; the catalogue is many-to-one onto
destinations. `Menu.Exit` stays out of the catalogue — a large "Exit" tile in a
grid is a misclick hazard. It remains in the side menu.

### Common workflows, in order

1. **Sell** → `JPanelTicketSales`. In a small shop the administrator also works
   the counter; this is their most frequent action and keeps the landing screen
   from costing them a click every day.
2. **Replenishment and customer orders** → `ReplenishmentPanel`. The most
   important administration job the product has today: the panel already holds
   both restock requests and customer orders, with Ordered → Received states.
3. **Add or change a product** → `ProductsPanel`.
4. **Latest cash closings** → `JPanelCashClosing`.
5. **Customers and debts** → `CustomersPanel`.
6. **How the business is doing** → `MenuSalesManagement`, the reports welcome
   view. The existing `JPanelSalesSummary` remains in the Reports catalogue for
   direct access to the detailed sales summary.
   The navigation item uses a theme-aware 24px house icon rather than the
   catalogue icon or the legacy `gohome.png` exit arrow.

Low stock is deliberately *not* prominent: proper stock handling does not exist
yet, and replenishment is what people actually use. It stays in the catalogue.

When `tickets/todo/batch-stock-receiving.md` lands, "I received a delivery"
becomes a new workflow entry and most likely takes slot 3, pushing products
down. Adding it must be one line in the catalogue plus its translation keys.

## Design

### Define the menu and reuse its actions

The active menu definition now lives in a Java menu builder. Define groups,
submenus, panels, executions, icons and translation keys in code, preserving
the current order and destinations.

The old `Menu.Root.txt` remains only as a legacy migration fixture because
released migrations replay it while building a fresh database. It is not read
by the application or stored in `RESOURCES` by the current menu code.

Change `JPrincipalApp`'s `List<Action> m_menuActions` to a `Map<String, Action>`
keyed by task name, populated from every level of the Java menu definition
(groups and submenus), not only the top level. The rail keeps consuming the
values in order. Reuse the same `Action` objects for the side menu, rail and
welcome screen so navigation, permissions and behaviour stay identical.

A workflow entry does not know how to open anything; it points at a task name
and the screen resolves it against that registry. That is what makes the
destination, the permissions, and the behaviour identical to the menu's.

### The catalogue is defined in code

Define the catalogue in Java code, next to the welcome-screen model or another
small workflow-catalogue class. Do not add a `Workflow.Root.txt` resource or
store catalogue definitions in the database `RESOURCES` table. The definition
must remain declarative and easy to reorder or reword without changing the
navigation implementation:

```
catalogue.addCategory("Workflow.Category.Stock")
    .addCommon("Workflow.Replenishment", "com.openbravo.pos.inventory.ReplenishmentPanel");
catalogue.addEntry("Workflow.LowStock", "com.openbravo.pos.reports.JPanelLowStock");
```

The catalogue is not stored in the database: reordering and rewording must not
require a migration. Each entry carries an id, a label key, a hint key, an
icon, a target task name, a category, and search synonyms. Synonyms are
required, not optional — without them searching "IVA" does not find "Taxes"
and vice versa.

### The screen

`JPanelWelcome implements JPanelView` resolves each entry against the registry
and drops the ones with no `Action` (missing class, or `hasPermission` false —
check it explicitly, do not rely on the submenu). An empty category disappears.

Layout, per the wireframes: greeting, search field, a "Common" grid of six
cards, then "Everything you can do" grouped by category on the same page — no
"see all" button, one scroll instead. `GridLayout(0, n)` inside a container
whose `ComponentListener` drops n from 3 to 2 to 1 by width; preferred sizes
derived from content, no fixed geometry.

Keyboard: buttons here are focusable, unlike `MenuItemDefinition` and the rail.
Initial focus on the search field so the screen can be driven by typing. Tab
order search → common → catalogue; Enter opens the focused card; Esc clears.

Tokens: cards on `surface-100` with `radius-lg`; **no brand accent on any
card** — this screen has no primary action, and the accent is reserved for the
active navigation item.

### Landing and returning

Add a group at the top of the Java menu definition:

```
menu.addGroup("Menu.Home")
    .addPanel("…/menu-home.png", "Menu.Home", "com.openbravo.pos.forms.JPanelWelcome");
```

Because `activate()` opens the first *permitted* action, the landing screen is
driven entirely by permissions with no role branching: grant `JPanelWelcome` to
Administrator and Manager and they land on it; Seller, Employee and Guest do
not have it and land on Sales exactly as today. Returning is the first item of
the side menu and of the rail, by construction.

### Permissions and migration

- Add `com.openbravo.pos.forms.JPanelWelcome` to `Role.Administrator.xml` and
  `Role.Manager.xml`.
- Existing installations keep their permissions in `ROLES.PERMISSIONS`, so add
  `V46__administration_welcome_screen.java` following the existing
  `V45__remove_legacy_script_resources.java`,
  update `migration-checksums.sha256`, and cover empty database, existing
  database, and a second run.
- Note that this migration rewrites the whole role XML, so a customised
  production role loses its customisation. V44 already assumes this; confirm it
  is acceptable before repeating it.

### Translations

New `Workflow.*` keys in all three `pos_messages*.properties`, kept separate
from `Menu.*` (which stay nouns for the side menu). Task phrasing is longer in
Catalan and Spanish: use a short bold label plus a smaller hint line, and check
at 1024×768 before closing the ticket.

## Open decisions

- **"Latest cash closings"**: open `JPanelCashClosing` clean, or pre-filtered to
  the last 7 days? The label promises "latest"; an empty date form does not keep
  that promise. Pre-filtering requires a new contract between the catalogue and
  the destination panel, which does not exist today. First version opens it
  clean unless decided otherwise.
- **Submenu permission bug**: `JPanelMenu` presents destinations the user cannot
  open and fails on click. It is the same rule as this ticket's "not presented
  as usable actions", but it is a separate concern; a separate ticket is
  preferred.

## Verification Notes

- 2026-09-24: The first administration run reached migration V46 and then
  failed while constructing the menu with `NullPointerException` from
  `ImageIcon` via `HiDpiIcon`, because a menu icon resource resolved to `null`.
  Menu panel and execution actions now treat a missing decorative icon as no icon
  so menu construction does not prevent opening the administration view.
- 2026-09-24: The Java menu adds `Menu.Home`; its English, Spanish, and Catalan
  translations are included, and the runnable locale jar was regenerated.
- 2026-09-24: `StartPOS` previously initialized `AppLocal` before applying the
  configured locale, permanently caching the default English bundle. `AppLocal`
  now reloads its bundles after applying `user.language` and `user.country`.
- 2026-09-24: The catalogue section headings use uppercase presentation styling
  and left alignment without changing their translation copy. Administration
  mode no longer hides principal sales and cash workflows, so `Sell` remains the
  first common and catalogue workflow.
- 2026-09-24: The welcome screen no longer exposes the redundant global
  administration title, its content starts directly beside the side menu, and
  the search field uses rounded borders.
- 2026-09-24: Verification passed with `./gradlew spotlessApply`,
  `./gradlew compileJava jar`, and `git diff --check`.
- 2026-09-24: The welcome screen scroll bar now belongs to the full page,
  including the header, and is positioned at the outer panel edge instead of
  beside the inner content area.
- The Home navigation item uses a 24px house icon. The sixth
  common workflow and its Reports catalogue link open the reports landing
  view; the detailed sales summary remains a separate catalogue destination.

## Slices

1. Write the in-code workflow catalogue and the `Workflow.*` keys in English,
   Spanish, and Catalan from the destination table above. No UI yet.
2. Turn the menu action list into a task-name-keyed registry covering submenu
   entries, add `JPanelWelcome` with the common workflows and the catalogue, and
   add the `Menu.Home` entry.
3. Permissions: role templates, the V46 migration, checksums, integration
   coverage. Filter by permission and by missing class.
4. Search with synonyms, focus and tab order, 1024×768 and full screen, and
   label validation in the three languages.

## Done when

- Entering administration opens the workflow-oriented welcome screen for
  Administrator and Manager, while the other roles keep landing on Sales.
- The screen presents the six agreed common workflows as clear, actionable
  choices using task language.
- An administrator can discover and open every permitted administration
  destination without needing to know the old menu structure.
- A workflow opens the existing destination with the expected role and
  permission checks; inaccessible destinations are not presented as usable
  actions, and an empty category is not rendered.
- Returning to the administration landing point is the first item of the menu
  and the rail.
- The screen remains usable with keyboard focus and at supported window sizes,
  down to 1024×768.
- Labels remain visible and understandable in English, Spanish, and Catalan.
- A test fails when a task name in the Java menu definition is missing from the
  catalogue without an explicit exclusion, and when a catalogue entry names a
  task that does not exist.
- The existing administration destinations continue to open and behave as
  before when reached from the new screen.
