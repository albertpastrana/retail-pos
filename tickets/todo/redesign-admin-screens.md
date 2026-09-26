# Redesign the administration maintenance screens (users, roles, resources)

Captured: 2026-09-26

Design reference (wireframe, not binding on implementation details):
https://claude.ai/artifact/XjjQYq1vqX1RJo1tAxyHhE

Mockups: `tickets/todo/redesign-admin-screens/Main.html` (Users),
`tickets/todo/redesign-admin-screens/Roles.html` (Roles, permissions as a
grouped checklist), `tickets/todo/redesign-admin-screens/Resources.html`
(Resources, with a ticket preview).

Related: `tickets/in-progress/redesign-categories-screen.md`,
`tickets/in-progress/remove-swing-layout-antipatterns.md`,
`tickets/todo/administration-welcome-screen.md`,
`tickets/todo/review-table-search-actions.md`,
`tickets/done/redesign-products-screen.md`

## Goal

Bring the three administration maintenance screens in line with the chrome the
products and categories screens already use — no legacy record toolbar, a
searchable list, one primary action, one Save — and, on Roles, replace the raw
permissions XML with a checklist a shop owner can actually read.

## Context

The three screens are the last untouched `JPanelTable` master/detail pairs:

- `src-pos/com/openbravo/pos/admin/PeoplePanel.java` /
  `PeopleView.java` — `PeoplePanel.getListCellRenderer()` L71 renders column 1
  (`NAME`) only; `PeopleView.initComponents()` is generated `GroupLayout` with
  fixed 90px label columns and a trailing `addGap(246, 246, 246)`.
- `src-pos/com/openbravo/pos/admin/RolesPanel.java` L69-79 /
  `RolesView.java` — the editor is a `JTextField` for the name plus a
  `JTextArea` in `DialogInput 12` holding the permissions XML verbatim
  (`initComponents()`, `m_jText`).
- `src-pos/com/openbravo/pos/admin/ResourcesPanel.java` L80-90 /
  `ResourcesView.java` — name field, type combo, and a `CardLayout`
  (`m_jContainer`, `showView()`) switching between a text area and
  `JImageEditor`.

Precedent to follow, as the categories ticket does: `ProductsPanel.getFilter()`
L94 for the header, `showToolbar()` L117, `getListComponent()` L132,
`confirmDelete()` L174; `JPanelTable` exposes those hooks at L82, L115, L135,
L183, L207. Colours come from
`src-pos/com/openbravo/pos/theme/RetailPOSColors.java` L43.

Permission data: `AppUser.fillPermissions()` L139 loads
`DataLogicSystem.findRolePermissions()` and SAX-parses `<class name="…"/>`
entries into a `HashSet`; `hasPermission()` L180 is a plain set lookup with an
administration-mode filter. The canonical role definitions live in
`src-pos/com/openbravo/pos/templates/Role.*.xml` —
Administrator 58 classes, Manager 52, Seller 21, Employee 17, Guest 1 — and are
re-applied by migrations (`V19__canonical_roles.java`, `V32`, `V44`).

Resource data: `DataLogicSystem` L108-116 reads and writes `RESOURCES` by name.
The 42 files in `src-pos/com/openbravo/pos/templates/` are the shipped
originals for the resources that have one.

### Problems observed

1. **The roles editor is a raw XML text area.** Editing a role means hand-writing
   `<class name="com.openbravo.pos.forms.JPanelWelcome"/>` lines with no
   validation, no list of what is available, and no feedback when a class name
   is wrong — a typo silently removes a permission. Nothing on screen says how
   many permissions a role has or which users it affects.
2. **Legacy toolbar chrome on all three screens.** First/prev/next/last, reload,
   magnifier, sort and the new/delete/save icon buttons, already dropped from
   products and categories.
3. **No search on any of the three lists**, and each list renders the bare name
   with no secondary information — a user's role, a role's permission count, a
   resource's type.
4. **Unlabelled icon buttons in the users editor.** `PeopleView` L189/L214/L220
   wires `fileclose.png` and `color_line16.png` buttons with no text and no
   tooltip; a user has to press one to find out it generates or clears an
   access-card number.
5. **The access card reads like a customer record.** The field is labelled
   `label.card` with no explanation, next to an image and a visible flag, so it
   is easy to mistake for a loyalty card. It is the swipe alternative to the
   password (`Hashcypher`/`StringUtils.getCardNumber()`).
6. **Resources give no context.** The list is 42 bare names; the editor shows
   raw XML with no indication of which resource is a modified copy of a shipped
   template, no way back to the original, and no way to see what a ticket
   template produces without printing one.
7. **Fixed-width generated layouts.** All three editors use generated
   `GroupLayout` with hard-coded label columns and preferred sizes, which clip
   in Catalan and Spanish.

## Proposed screen changes

### Users

1. Remove the toolbar (`showToolbar() → false`); header with a single
   `+ Nou usuari` secondary button and `Desa l'usuari` as the one primary in
   the editor footer.
2. Searchable list (name and card number) with avatar, role, and a visible/
   hidden status badge, replacing the flat name list.
3. Editor: name, role (with a line pointing at the Roles screen for permissions),
   access card, password, image, visible flag.
4. Card field labelled **Targeta d'accés** with a one-line explanation, and the
   two icon buttons replaced with named actions (`Genera'n una de nova`,
   `Treu la targeta`).
5. Footer states unsaved changes and offers `Descarta` / `Desa l'usuari`.

### Roles

6. **Permissions as a grouped checklist** instead of the XML text area: areas
   (sales, maintenance, reports, system), a count per area and per role, a
   search box over permission names, and `Marca-ho tot` / `Buida-ho tot`.
7. **Keep the XML reachable** behind an `Edita el XML` action, so anything the
   checklist does not model is still editable and existing role definitions are
   never silently rewritten.
8. Role list shows users assigned and permissions active (`13/18` style).
9. Footer states the impact of the change ("Has tret 2 permisos. Afectarà 1
   usuari.") before saving.

### Resources

10. Type filter chips (all / text / image / binary) and a searchable list with a
    type icon and a short description per row.
11. Mark resources that differ from their shipped template, and offer
    `Restaura l'original` for those that have one.
12. Text content in a monospaced editor with line numbers; image and binary keep
    their current editors.
13. Ticket-template preview beside the editor, plus a `Prova d'impressió`
    action.

## Scope decisions

- **The permission catalogue is the real work here.** The wireframe shows 18
  permissions in 4 groups as an illustration; the actual catalogue is ~58
  permission keys (`Role.Administrator.xml`), a mix of panel class names
  (`com.openbravo.pos.forms.JPanelWelcome`) and action keys (`sales.EditLines`,
  `Menu.Replenishment.Add`). Deriving that catalogue — a stable list of keys,
  a human label and an area for each — is a prerequisite, not a detail, and it
  is the part most likely to need its own ticket.
- A role's stored XML stays the source of truth. The checklist reads and writes
  it; keys it does not know about are preserved untouched and surfaced (a
  "others" group or a note), never dropped on save.
- The "modified vs original" marker only applies to the resources that have a
  file in `templates/`; the rest simply do not show it.
- The ticket preview (change 13) is the most speculative piece and depends on
  how far the printing template engine can render off-device. If it does not
  fit, ship the rest and capture the preview separately.
- No schema change: `PEOPLE`, `ROLES` and `RESOURCES` stay as they are.

## Implementation notes (2026-09-26; awaiting manual verification)

- Started from `origin/main` (`4f9e91c7`) and fast-forwarded to the newer
  `33092c24` on `redesign-admin-screens`.
- The canonical administrator XML is the checklist catalogue; its keys are
  grouped, labelled with existing menu strings or translated action labels,
  searchable, and selectable. Custom keys are shown read-only in Other. Saving
  an unchanged checklist preserves the original XML bytes; changing modelled
  keys preserves unknown XML nodes. The raw XML editor validates its input.
- The three screens now have local searchable lists, New actions and editor
  Save actions rather than the shared toolbar. Users expose labelled card and
  image actions; resources expose type filters, source comparison, restore and
  text line numbers. Existing Delete actions have a confirmation in the footer.
- A faithful ticket preview and test print need a representative print context;
  deferred under `tickets/todo/preview-administration-ticket-template.md` as
  allowed by the scope decision.
- Automated checks: `./gradlew spotlessApply`, `./gradlew compileJava`,
  `./gradlew integrationTest --tests 'com.openbravo.pos.admin.*'`,
  `./gradlew test`, `git diff --check` passed. The role tests cover unmodified
  XML byte preservation, known/unknown permissions, and the catalogue across
  shipped roles; layout checks instantiate editors in English, Spanish and
  Catalan at the target editor width.
- Still to verify manually before Done: Users (name/card search, role, image,
  password, visible flag and save/discard), Roles (checklist/XML/unknown keys,
  impact and save), Resources (filters, line numbers, restore and image/binary
  editors), each at 1024x768 and in English, Spanish and Catalan, including
  keyboard focus and resizing. Also open/save an existing role in a live DB and
  compare granted permissions before/after; check one other JPanelTable screen.

## Done when

- None of the three screens shows the first/prev/next/last/reload/search/sort
  toolbar; each has one primary action and one Save.
- Each list is searchable and each row carries its secondary information (role,
  permission count, resource type).
- A role's permissions can be set without touching XML, the XML remains
  reachable, and permission keys the checklist does not model survive a save
  unchanged.
- Every button in the users editor has a visible label; the card field is named
  and explained.
- Resources that differ from their shipped template are marked and can be
  restored.
- No editor uses a generated fixed-width `GroupLayout`; labels and buttons are
  not clipped in English, Catalan and Spanish, and the screens stay usable at
  `1024x768`.
- Existing role definitions in a live database still grant exactly the same
  permissions after being opened and saved through the new editor.
- No other `JPanelTable` screen regresses.

## Out of scope (unless separately approved)

- Changing what any permission key means, or adding new ones.
- Per-user permission overrides (permissions stay a property of the role).
- Password policy, expiry, or authentication changes.
- A resource upload/import flow beyond the current image and binary editors.
