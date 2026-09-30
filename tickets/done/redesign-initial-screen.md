# Redesign the initial screen

Captured: 2026-09-27

Related: `tickets/done/administration-welcome-screen.md`,
`tickets/done/redesign-main-menu-sidebar.md`,
`tickets/done/login-vs-seller.md`

Design reference (wireframes, not binding on implementation details):
`tickets/todo/redesign-initial-screen/` — each wireframe is there both as a
standalone HTML file to open in a browser and as a PNG rendered at 2x from it.
Editable canvas: https://claude.ai/artifact/8zup4R2SciEkg8SKQo8Bzt

| File | Shows |
| --- | --- |
| `Actual.html` / `Actual.png` | The screen as it is today at 1024×768, with the administrator grid open |
| `Main.html` / `Main.png` | Step 1: till state, one primary action, keyboard focus ring |
| `Administracio.html` / `Administracio.png` | Step 2: the administrator picker replacing step 1, reflowing grid, roles shown |
| `Fosc.html` / `Fosc.png` | Dark theme with the cash session closed, and what that changes in the primary action |

## Goal

The first screen of the day states which till it is and whether the cash
session is open, offers one obvious primary action, and can be driven by touch
or keyboard.

## Context

The screen is the `login` card of `JRootApp`: a 64px header, a centred column
with a heading and two mode buttons, and a footer with About and Close.

### What to change

1. **Two competing primaries.** Both mode buttons are 460×82 with the same
   weight. Sales is `brand`; Administration is
   `ADMIN_BUTTON = new Color(0x374151)` (`JRootApp.java:64`), a screen-local
   colour outside `design-system/tokens.json`. Administration becomes the
   `secondary` variant (`surface-100` + `border` + `ink`). Same problem in the
   demo badge, which hardcodes `0x78350F` / `0xFDE68A` / `0xF59E0B`
   (`JRootApp.java:112`) where `RetailPOSColors.warning()` already exists.
2. **Step 2 stacks under step 1.** Pressing Administration keeps both mode
   buttons and grows the user grid below them, inside a fixed 510×301 well
   (`LOGIN_GRID_WIDTH` / `LOGIN_GRID_HEIGHT`) with its own scrollbar, always two
   columns however wide the window is. It fits at 1024×768 with nothing to
   spare. Step 2 should replace step 1, with an explicit Back and Esc, and the
   grid should reflow by width instead of scrolling in a fixed well.
3. **A dead label.** `initComponents()` sets the heading to `Label.WhoWorks`
   ("Who is working?") and `showLogin()` overwrites it with `Label.ChooseMode`
   before the screen is ever shown. The string is translated in all three
   languages and unreachable. Remove it, or use it as step 2's heading in place
   of `Label.ChooseAdministrator`.
4. **Till state is not shown.** `initApp()` creates or resumes the cash session
   silently. Show shop, till and open/closed state; when the session is closed,
   the primary action promises a new session instead. Decision (2026-09-28):
   remove the session information card entirely, including the session number,
   opening time and ticket count. Put the open/closed wording beside the till
   name as a quiet line of text, without a badge or card.
5. **No keyboard.** Every button on this screen — modes, users, About, Close —
   is built with `setFocusable(false)`. `AGENTS.md` asks for keyboard and
   scanner input. Give the screen a tab order and a `border-strong` focus ring.
6. **Close quits the application** and is a muted text label next to About.
   Make it a `danger` secondary with its full label and the exit-door arrow icon
   shown in the wireframe; About
   becomes a text link rather than a bordered button.
7. **User cards carry no role**, so two people with the same first name are
   indistinguishable.

Minor, same pass: set the clock and figures in IBM Plex Mono with tabular
figures, and derive the mode-button and grid sizes from content rather than the
current fixed 460×82 / 510×301.

Decision (2026-09-28): both mode buttons have a trailing chevron, and the
Sales explanatory line uses the smaller hint size, as in the wireframe.

Correction (2026-09-28): the Sales basket must include its full curved handle
and tapered rim, and the Close icon is the wireframe's exit-door arrow, not a
power symbol. Reproduce the actual 24×24 SVG path geometry in Swing using
theme tokens for the strokes.

Picker revision (2026-09-28): Back has the wireframe's left chevron. At
supported till widths and at full screen, the administrator cards form two
columns in a centred grid with generous side margins instead of spanning the
whole window. At unusually narrow widths, one column prevents clipped cards.
The picker header (Back, title and hint) must use the same centred width and
left edge as the cards, including after a resize.

Out of scope: the administration landing screen behind Administration
(`tickets/done/administration-welcome-screen.md`) and the sales screen.

### A design-system gap this surfaces

In the dark theme, `danger` (`#c9463a`) on `surface-100` does not reach 4.5:1 at
the 12px `label` size a `StatusBadge` uses. The dark wireframe works around it
with `ink` text and a `danger` border. Worth fixing in the design system rather
than per screen.

## Done when

- The initial screen names the till and states whether the cash session is open
  in a quiet line of text; there is no session-information card.
- Sales is the only `brand`-coloured action on the screen; Administration reads
  as secondary, and no colour on the screen comes from outside the tokens.
- Choosing Administration replaces the mode choice with the administrator
  picker; Back and Esc return to it, and nothing on the screen moves under the
  pointer when it does.
- The administrator grid reflows by window width and does not scroll inside a
  fixed well at 1024×768. It has two centred columns at supported widths and
  full screen, and one column if the viewport cannot fit two readable cards.
- Back has the left-pointing chevron from the wireframe.
- The picker header lines up with the left edge of the centred card grid at
  1024×768 and full screen.
- Each administrator is shown with their role.
- The whole screen can be reached and operated with Tab and Enter, with a
  visible focus ring.
- Closing the application is visibly distinct from About.
- About is a text link, Close has an icon, both mode buttons have trailing
  chevrons, and the Sales hint reads smaller than its action label.
- Labels remain visible and understandable in English, Spanish and Catalan at
  1024×768 and full screen, in both light and dark themes.

## Slices

1. Tokens and hierarchy: Administration as `secondary`, the demo badge on
   `warning()`, the close action as `danger`, monospaced clock.
2. The till-state line, and the closed-session wording of the primary action.
3. Two steps instead of a stack: Back, Esc, reflowing grid, roles on the cards.
4. Keyboard focus and tab order, and label checks in the three languages.

## Implementation and verification (2026-09-28; awaiting live till check)

- The login screen swaps a mode-choice view for a width-reflowing administrator picker. Focusable rounded buttons use the shared colour tokens, including a contrast-aware danger label on dark `surface-100`; card-swipe input remains available from the picker.
- Closed sessions use closed wording and a new-session sales hint. Startup still follows the existing cash-session creation/resumption behaviour.
- `InitialScreenLayoutTest` renders both steps at 1024×768 in English, Spanish and Catalan in light and dark, plus wide and narrow Catalan layouts. Rendered previews live in `build/reports/initial-screen/`. Earlier previews covered the session card; regenerate and re-review after removing it and applying the follow-up visual changes.
- A live till check of Tab/Enter, Esc, scanner cards, resize and display scaling is still outstanding; rendered fixtures do not exercise the database-backed operator flow.
- Follow-up (2026-09-28): removed the session-information card and its ticket-count query; the till and open/closed state now sit on one quiet line. About is a transparent text link with a touch-sized target; Close has an icon; both mode buttons have trailing chevrons, and their hints are smaller, regular-weight text. The shared mode width follows the window, never goes below the translated content width, and caps at 560px on large displays to keep the wireframe's centred column rather than stretching across the till. The regenerated light/dark preview PNGs were checked against the reference; `./gradlew spotlessApply integrationTest --tests com.openbravo.pos.forms.InitialScreenLayoutTest`, `./gradlew check`, and `git diff --check` passed. Live till interaction remains to be checked.
- The basket's previous straight strokes looked like a cut-off handle and its rectangular body did not match the SVG. Replaced the strokes with the original tapered basket and curved handle path. Replaced the power icon on Close with the wireframe's exit-door arrow in both themes. The other mode icon remains within its paint bounds. Re-rendered and visually compared `light-ca-choice.png` and `dark-ca-choice.png`; `./gradlew spotlessApply integrationTest --tests com.openbravo.pos.forms.InitialScreenLayoutTest` and `git diff --check` pass. Live till interaction remains to be checked.
- Picker revision (2026-09-28): Back uses the wireframe's left-pointing chevron. The centred grid displays two columns in `light-ca-administrators.png`, `dark-ca-administrators.png`, and the full-screen preview, with wider side margins at 1024×768 and 800×768; one column is reserved for exceptionally narrow viewports. `InitialScreenLayoutTest` asserts the column count and margins in all three languages and both themes. `./gradlew spotlessApply integrationTest --tests com.openbravo.pos.forms.InitialScreenLayoutTest`, `./gradlew check` and `git diff --check` passed. Live till interaction remains to be checked.
- Header alignment correction (2026-09-28): Back, heading and hint now occupy the same centred column as the cards, with matching left edges. The layout test checks this in every locale and theme at 1024×768, plus 800px and full-screen widths; reviewed the rendered PNGs for all three sizes. `./gradlew spotlessApply integrationTest --tests com.openbravo.pos.forms.InitialScreenLayoutTest`, `./gradlew check`, and `git diff --check` passed. Live till interaction remains outstanding.

## Shipped

Implemented in commit `d7d57181` and its follow-up changes. The initial screen
now has the revised mode hierarchy, till state, administrator picker, keyboard
focus treatment, responsive layout, translated labels, and light/dark previews.
The ticket is marked done for the shipped scope; live till interaction,
scanner, resize, and display-scale checks remain follow-up verification.
