# Redesign the main menu sidebar and drop SwingX

Captured: 2026-09-25

Related: `tickets/todo/audit-unused-code-and-dependencies.md`

## Goal

The expanded main menu is rebuilt with plain Swing components styled from the Retail POS design system, and `org.swinglabs:swingx:0.9.5` is gone from the build.

After this, the sidebar looks like the rest of the application, every menu entry is a real touch target, and the operator can see which screen is currently open.

## Context

SwingX is used in exactly one place: the expanded left menu of the main window, in `src-pos/com/openbravo/pos/forms/JPrincipalApp.java`.

- `import org.jdesktop.swingx.JXTaskPane` / `JXTaskPaneContainer` (lines 39-40)
- `ScriptMenu` holds a `JXTaskPaneContainer` (lines 243-262)
- `ScriptGroup` holds a `JXTaskPane` (lines 287-345)
- `build.gradle.kts` declares the dependency (line 86)
- `licensing/Openbravo POS notice.txt` carries its notice

Nothing else in the tree imports SwingX. No `.form` file references it. The compact icon rail (`ScriptMenu.getRailMenu()`) is already plain Swing; only the expanded menu depends on the library.

The API surface actually used is small, which is what makes the replacement cheap:

- `JXTaskPaneContainer`: constructor, `add(JXTaskPane)`, `applyComponentOrientation`
- `JXTaskPane`: constructor, `setTitle`, `setVisible`, `setFocusable`, `setRequestFocusEnabled`, `add(Action)` returning the created `Component`

Collapsing, animation and the SwingX look-and-feel addons are never used programmatically.

### What is wrong with the current screen

SwingX paints its Metal-era style, so the sidebar is the last part of the main window that predates the design system:

- Each group is a bordered, rounded box with a grey gradient header and a decorative chevron in a circle. The rest of the application is flat, with `retailpos.*` surfaces.
- Menu entries are hyperlink-style rows roughly 33px tall with 16px icons. `design-system/swing-development.md` sets a hard 48px minimum for touch targets, and the toolbar next to the sidebar already uses large icons.
- Group titles are rendered at the same visual weight as the entries, so there is no hierarchy between "Main" and "Sales".
- Nothing indicates which panel is currently open.
- The rail toggle is a floating tab pinned outside the panel by `jPanel2.add(Box.createVerticalStrut(50), 0)`, unrelated to any row in the menu.
- The collapse state of a group is not persisted, so the collapse affordance costs a click and buys nothing across restarts.

`design-system/swing-development.md` section 6 already names "replace SwingX components" as follow-up work for a later pass. This is that pass.

## Design

### Decisions

1. **No boxed groups.** The sidebar becomes one flat surface with full-width rows, not a stack of framed widgets. The base surface is `retailpos.surface0` so it matches the main content area.
2. **Groups are static section labels, not collapsible panes.** Each group holds three to eight entries, permissions already hide empty groups, and the collapsed state was never saved. Dropping collapse removes the header button, the chevron and its state. If this turns out to be wrong, the header is the only place that has to change back.
3. **The open panel is highlighted.** The sidebar gains a selected row, driven from the task name that `showTask()` already receives.
4. **Group labels are rendered in upper case.** This is styling, not copy: the label is still `AppLocal.getIntString("Menu.Main")`, and no message in `locales/` changes. `MenuSidebarGroup` upper-cases the string when it paints the header, the same way a stylesheet would, using `toUpperCase(AppLocal.getLocale())` rather than the default locale so a Turkish or Azerbaijani install does not mangle a dotted i. Pair it with a small tracking increase, since upper-case at 13px sets tight.

### Wireframe — today

```
  ┌ sidebar ──────────────────────────────┐
  │  ┌─────────────────────────────────┐  │
  │  │ Main                       (^)  │  │  <- boxed group, grey gradient header, 22px
  │  ├─────────────────────────────────┤  │
  │  │ ▫ Sales                         │  │  <- 33px row, 16px icon, hyperlink style
  │  │ ▫ Edit sales                    │  │
  │  │ ▫ Replenishment and orders      │  │
  │  │ ▫ Cash movements                │  │
  │  │ ▫ Close cash                    │  │
  │  └─────────────────────────────────┘  │ ┌─┐
  │                                       │ │‹│  <- rail toggle floats outside the
  │  ┌─────────────────────────────────┐  │ └─┘     panel, pinned by a 50px strut
  │  │ System                     (^)  │  │
  │  ├─────────────────────────────────┤  │
  │  │ ▫ Change password               │  │
  │  │ ▫ Exit                          │  │
  │  └─────────────────────────────────┘  │
  └───────────────────────────────────────┘
```

### Wireframe — proposed

```
  ┌ sidebar, retailpos.surface100 ────────┐│  <- 1px trailing border in
  │                                       ││     retailpos.border, against the
  │  ‹  Collapse menu                     ││     surface0 content area
  │                                       ││
  │  MAIN                                 ││  <- caption 13px, retailpos.inkMuted,
  │                                       ││     upper-cased at paint time
  │ ╭───────────────────────────────────╮ ││
  │ │ ▣  Sales                          │ ││  <- selected row: brandSubtle fill,
  │ ╰───────────────────────────────────╯ ││     ink text, arc 10
  │   ▣  Edit sales                       ││  <- 48px row, 24px icon, body-lg 17
  │   ▣  Replenishment and orders         ││
  │   ▣  Cash movements                   ││
  │   ▣  Close cash                       ││
  │                                       ││
  │  SYSTEM                               ││
  │   ▣  Change password                  ││
  │   ▣  Exit                             ││
  │                                       ││
  └───────────────────────────────────────┘│
```

The rail keeps its current structure and gains the same icon size and selected treatment, so toggling between the two does not move or resize anything but the labels. The expanded toggle also includes a small localized label (`Collapse menu` / `Reducir el menú` / `Redueix el menú`):

```
  ┌────┐
  │ ›  │  <- toggle, 56px
  │╭──╮│
  ││▣ ││  <- selected, same brandSubtle pill as the expanded sidebar
  │╰──╯│
  │ ▣  │  <- 56px, 24px icon
  │ ▣  │
  └────┘
```

### Measurements

| | Today | Proposed | Source |
|---|---|---|---|
| Entry row height | ~33px | 48px | `swing-development.md` §4, hard minimum |
| Entry icon | 16px | 24px | `swing-development.md` §5 |
| Entry label | default 15 | body-lg 17 | `tokens.json` type groups |
| Group label | same as entry | caption 13, `inkMuted`, upper case | `swing-development.md` §3 |
| Group container | bordered box, gradient header | none | |
| Sidebar background | look-and-feel default | `retailpos.surface100` | `RetailPOSColors` |
| Selected row | none | `retailpos.brandSubtle` on `retailpos.ink`, arc 10 | `FlatLightLaf.properties` |
| Hover row | underline | `retailpos.surface200` | `RetailPOSColors` |
| Padding | SwingX defaults | `space-3` around, `space-2` icon gap | `swing-development.md` §4 |

### New components

Two new classes next to `JPrincipalApp`, in `com.openbravo.pos.forms`:

`MenuSidebar extends JPanel` — replaces `JXTaskPaneContainer`.

- `BoxLayout(Y_AXIS)`, background `RetailPOSColors.surface100()`, `space-3` padding, a one pixel trailing border in `RetailPOSColors.border()` to separate it from the `surface0` content area.
- Implements `Scrollable` with `getScrollableTracksViewportWidth()` returning `true`. `m_jPanelLeft` is a `JScrollPane`, and this is what keeps a long label from producing a horizontal scrollbar; SwingX does it for us today.
- `add(MenuSidebarGroup)`, plus `setSelectedTask(String taskName)` which forwards to every group.

`MenuSidebarGroup extends JPanel` — replaces `JXTaskPane`.

- `BoxLayout(Y_AXIS)`: a caption header label, then one row per action.
- Header: `JLabel` at the `caption` style (Manrope Medium 13) in `RetailPOSColors.inkMuted()`, upper-cased for the active locale, `space-3` above and `space-1` below. Not focusable, not clickable.
- `setTitle(String)`, `setVisible(boolean)` and `Component add(Action)` keep the same contract as the SwingX pane, so `ScriptGroup.addAction` does not change.
- `setSelectedTask(String)` marks the matching row and clears the others, comparing against `AppUserView.ACTION_TASKNAME`.

Row buttons (created inside `MenuSidebarGroup.add(Action)`):

- `JButton(action)`, `setHorizontalAlignment(LEADING)`, 24px icon, `iconTextGap` of `space-2`, label at `body-lg` (17).
- Preferred and minimum height 48, `setMaximumSize(new Dimension(Integer.MAX_VALUE, 48))` so `BoxLayout` cannot stretch it.
- `setFocusPainted(false)`, `setFocusable(false)`, `setRequestFocusEnabled(false)`, matching the rail.
- Idle: no border, background of the sidebar. Hover: `retailpos.surface200`. Selected: `retailpos.brandSubtle` background with `retailpos.ink` text, the same pair as `List.selectionBackground` / `List.selectionForeground`, at `@arc` 10.
- Style through `putClientProperty("FlatLaf.style", ...)` in the shape of `RetailPOSColors.primaryButton`, so hover and pressed states come from FlatLaf rather than custom paint code. Colours are read via `RetailPOSColors`, never hardcoded hex, so the dark theme follows for free.

### Changes in `JPrincipalApp`

- Remove both `org.jdesktop.swingx` imports and the dead `l2fprod` comments on lines 37-38.
- `ScriptMenu.taskPane`: `JXTaskPaneContainer` becomes `MenuSidebar`. `getTaskPane()` changes return type; the only caller assigns it to a `Component`.
- `ScriptGroup.taskGroup`: `JXTaskPane` becomes `MenuSidebarGroup`.
- `showTask()` tells the sidebar and the rail which task is now open, so both show the selection.
- `getRailMenu()`: icons move to 24px and the selected state uses the same treatment, so switching between rail and expanded does not change the icon size.
- Move the rail toggle to the top of the sidebar as a full-width row of the same height as a menu entry, and drop the `Box.createVerticalStrut(50)` spacer and the `jPanel2` side strip that hold the floating tab today.

`buildMenu()`, the permission check in `addAction`, `ScriptSubmenu`, `JPanelMenu`, and the `ui.menu.state` rail preference are unchanged.

### Chevron and other icons

The rail toggle keeps the existing `menu-left.png` / `menu-right.png` pair through `HiDpiIcon`. No new image assets are needed: with collapsible groups gone, there is no group chevron to draw.

### Removing the dependency

Once the menu no longer imports SwingX:

- Delete `"org.swinglabs:swingx:0.9.5"` from `build.gradle.kts`.
- Delete the SwingX entry from `licensing/Openbravo POS notice.txt`.
- Tick the SwingX line in `tickets/todo/audit-unused-code-and-dependencies.md`.

## Scope

Included:

- The two new sidebar components and the `JPrincipalApp` changes above.
- Selected-entry highlighting in both the expanded sidebar and the rail.
- Touch-target, spacing, type and colour alignment with `design-system/swing-development.md`.
- Removing the dependency, its licence notice and the audit entry.

Excluded:

- The submenu screens rendered by `JPanelMenu` (stock, sales and maintenance landing pages). They are a separate surface with their own grid layout.
- The menu structure itself: no entry is added, removed, renamed or regrouped. The upper-case group headers are a rendering choice in the component; only the toggle label is added to `locales/pos_messages*.properties`.
- `m_jPanelTitle`, which is built with a hardcoded `SansSerif` font and a dark grey matte border but is set invisible in the constructor. Leave it alone; removing it is a different ticket.
- Persisting a per-group open or closed state, which the decision above removes rather than reimplements.
- Any change to the rail preference key `ui.menu.state` or to how it is saved.

## Verification

Manual checks on the running application:

- Log in as a user whose role grants only part of the menu: groups with no permitted entry stay hidden, exactly as before.
- Open each entry in turn: the sidebar row for the open panel is highlighted, and the previous one is cleared.
- Toggle the rail with the menu button, in both directions, and confirm the preference survives a restart.
- Start with the window at 800px wide or narrower and confirm it still opens in rail mode.
- Switch to a right-to-left locale and confirm icons, labels and the toggle mirror correctly.
- Switch between the light and dark look and feel in `Configuration → General` and confirm the sidebar follows both themes.
- Give a group a long label and confirm no horizontal scrollbar appears in the sidebar.
- Measure a menu row: at least 48px tall.

Required commands before completion:

```sh
./gradlew spotlessApply
./gradlew ciCheck
```

## Done when

- The expanded main menu is built from `MenuSidebar` and `MenuSidebarGroup`, with no `org.jdesktop` import left in the tree.
- The sidebar matches the proposed wireframe: no group boxes, caption group labels, 48px rows with 24px icons, and the rail toggle as the first row.
- The entry for the open panel is visibly selected, in both the expanded sidebar and the rail.
- Sidebar colours, type and spacing come from `RetailPOSColors` and the design system's tokens, with no hardcoded hex in the new components.
- Permissions, right-to-left orientation, the rail toggle and the `ui.menu.state` preference behave exactly as before.
- `org.swinglabs:swingx` is absent from `build.gradle.kts` and from the licensing notice, and the application starts and runs without it.
- `./gradlew ciCheck` passes.

## Shipped

- Replaced SwingX with `MenuSidebar` and `MenuSidebarGroup`, including themed 48px action rows, selected-task state, scrolling, orientation-aware borders, and rail selection.
- Moved the rail toggle into the first row of the expanded sidebar and added the matching toggle row to the compact rail.
- Added a small localized label to the expanded toggle and enlarged the navigation chevron without changing other icon scaling.
- Removed the SwingX dependency and notice, and recorded the removal in the dependency audit ticket.
- Kept the rounded row shape through the existing `Button.arc` theme token; unsupported `arc` client styles are not applied to FlatLaf buttons.
- Verification: `./gradlew spotlessApply ciCheck` passed with PostgreSQL and MySQL containers running; containers were stopped afterwards with `docker compose down -v`.
