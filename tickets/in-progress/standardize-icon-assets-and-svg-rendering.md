# Standardize icons on Lucide and render SVG in Swing

Captured: 2026-09-29

Related: `tickets/done/redesign-main-menu-sidebar.md`

## Goal

The menu toggle clearly communicates opening and closing the sidebar rather than going back. New POS icons have a consistent visual language and render sharply at supported window sizes and display scales in light and dark themes.

## Context

- The sidebar toggle in `JPrincipalApp` uses `menu-left.png` and `menu-right.png` through `HiDpiIcon`. The expanded control has localized "Collapse menu" text; the compact rail is icon-only. A lone chevron can be mistaken for back navigation.
- Choose **Lucide** as the primary source of UI glyphs. Its 24×24, 2px outlined style matches the existing design-system rules more closely than Phosphor's multiple weights, and it provides `panel-left-close` / `panel-left-open`. Keep selected upstream SVG assets locally; do not add a JavaScript package or icon font to the Swing app.
- Use SVG as the source and runtime format for newly adopted icons. Java 21 alone does not render SVG in Swing; add `com.formdev:flatlaf-extras` at the same version as the existing FlatLaf (`3.7.2` currently) and use `FlatSVGIcon` via a small shared loading/sizing convention. Start with the menu toggle and the six design-system icons, not a mass conversion of legacy PNGs or user/product images.
- Replace every replaceable design-system icon with the corresponding Lucide glyph: `print`, `card-payment`, `discount-tag`, and `search` become `printer`, `credit-card`, `tag`, and `search`. Retain `barcode-scan` (scan corners) and `cash-drawer` as documented POS-specific exceptions: Lucide `barcode` omits the scan affordance, and there is no exact cash-drawer glyph. Keep exceptions visually aligned with Lucide's outline weight and geometry. This does not convert legacy product/user images or unrelated screen-specific PNGs.
- Use `panel-left-close` when the sidebar is expanded and `panel-left-open` when it is compact. Mirror the action correctly in right-to-left orientation and provide localized action text/tooltip in both states; do not rely on the icon alone. Preserve the `ui.menu.state` preference, touch target, and existing navigation behavior.
- Keep the 24×24 source geometry, 2px stroke, rounded caps and joins. Choose displayed sizes appropriate to context (e.g. 16, 24, 32 or 48 logical pixels) without maintaining separate 1x/2x PNG exports. Integrate icon colours with the shared light/dark theme tokens; verify the chosen SVG colour mechanism works with `FlatSVGIcon` instead of assuming browser `currentColor` support. Keep any POS-specific semantic colour intentional.
- Update `design-system/README.md`, `design-system/assets/Icons/README.md`, and `design-system/swing-development.md`: Lucide becomes the reference family, domain-specific exceptions are allowed, and SVG replaces the documented PNG-export recommendation. Record asset provenance and Lucide's ISC licence in the appropriate licensing documentation.

## Done when

- Opening and closing the sidebar uses distinct panel icons and correctly localized labels/tooltips in English, Spanish and Catalan; neither state reads as back navigation. Toggling, persistence, compact/expanded layouts, keyboard/focus behavior, and right-to-left orientation are checked on the running app.
- The complete replaceable design-system icon set and menu toggle render at the intended logical sizes without blur or clipping at normal and HiDPI scales, in both light and dark themes. The custom scan and cash-drawer meanings remain clear.
- SVG resources load from the packaged application (not just from the development tree); any retained PNGs continue to work. Missing or unsupported SVG assets do not silently produce blank critical controls.
- The design-system rules and licensing records reflect the actual icon source, exceptions, runtime rendering path, and theme-colour strategy.
- After Java changes, `./gradlew spotlessApply` and the narrowest relevant checks pass; run broader verification where required for the shared icon loader and packaging.

## Implementation Notes

- Lucide is now the source for `printer`, `credit-card`, `tag`, `search`, `panel-left-open`, and `panel-left-close`. The scan and cash-drawer SVGs remain local POS-specific exceptions and use the same `currentColor`, 24x24, 2px outline convention.
- `PosIcons` is the shared loader. It validates resources from the packaged classpath, derives logical sizes with `FlatSVGIcon`, and mirrors the sidebar glyph for right-to-left layouts.
- Legacy product, user, and screen-specific PNGs were intentionally left unchanged. The request to standardize all icons is implemented for the design-system family, not for those image assets or historical database migrations.

## Verification

- Passed `./gradlew spotlessApply`.
- Passed `./gradlew compileJava` and `./gradlew test`.
- Passed `./gradlew ciCheck` with PostgreSQL and MySQL containers; containers were stopped afterwards with `docker compose down -v`.
- Confirmed all eight runtime SVG resources are present in `build/jar/retail-pos.jar`.
- Manual running-app checks for focus, resize, light/dark display, translated labels, and RTL remain to be performed before moving this ticket to `done/`.
