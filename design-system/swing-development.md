# Building the Swing UI from these tokens

Retail POS already runs FlatLaf 3.7.2 (`Configuration → General`, `com.formdev:flatlaf` in `build.gradle.kts`). That's the right foundation — FlatLaf themes from a `.properties` file, so the tokens above map onto it directly instead of hand-styling each `JComponent`. This guide is the bridge: it doesn't ask you to replace anything working, only to point FlatLaf at these values instead of its stock Light/Dark palette, screen by screen.

## 1. A custom FlatLaf theme file, not a custom LookAndFeel

Don't subclass `FlatLightLaf`/`FlatDarkLaf` or override paint code. FlatLaf reads a `.properties` file of UI colour keys and applies it on top of a base theme — that file *is* the token mapping. `FlatLaf.registerCustomDefaultsSource(packageName)` looks up a file named after the active look and feel's simple class name in that package, so the two theme files have to be named to match, not just "one per theme":

```
src-pos/com/openbravo/pos/theme/FlatLightLaf.properties
src-pos/com/openbravo/pos/theme/FlatDarkLaf.properties
```

`FlatLightLaf.properties` (values copied verbatim from `tokens.json`'s `light` theme — if a token's value here ever disagrees with `tokens.json`, `tokens.json` is the source of truth and this file is stale):

```properties
# Surfaces
@background = #f5f1ea
Panel.background = #f5f1ea
@componentBackground = #fffdf9
List.background = #fffdf9
Table.background = #fffdf9
TextField.background = #e5dac5
Spinner.background = #e5dac5

# Text
@foreground = #241c14
Label.disabledForeground = #6b6154
@disabledForeground = #6b6154

# Borders and focus
@borderColor = #96896f
Component.focusColor = #6b5f47
Component.focusWidth = 2
Button.focusedBorderColor = #6b5f47

# Brand accent — the ONE primary action per screen, per README.md "Colour"
@accentColor = #b8481f
Button.default.background = #b8481f
Button.default.foreground = #ffffff
Button.default.hoverBackground = #8f3216
Button.background = #fffdf9
Button.hoverBackground = #fffdf9
CheckBox.icon.background = #e5dac5
CheckBox.icon.borderColor = #96896f
CheckBox.icon.selectedBackground = #b8481f
List.selectionBackground = #e7b8a4
List.selectionForeground = #241c14
Table.selectionBackground = #e7b8a4
Table.selectionForeground = #241c14

# Semantic — do not repurpose these keys for anything but their token's meaning
retailpos.success = #1f6b5c
retailpos.warning = #7a5300
retailpos.danger = #8a2a22
retailpos.info = #3c6e8f

# Shape — radius-md (buttons, keys, tiles); components needing radius-lg or
# radius-sm set arc explicitly per component below, this is only the default
@arc = 10
Button.arc = 10
Component.arc = 10
TextComponent.arc = 6

# Type
defaultFont = Manrope 15
```

`FlatDarkLaf.properties` is the same key list with the `dark` column of `tokens.json` (`@background = #1b1712`, `@accentColor = #e2764c`, `Button.default.foreground = #2b1206`, and so on) — never invert the light file's values with a filter or "darken by X%": copy them from the token, the same discipline as the light file.

Loaded once, at startup, before `UIManager.setLookAndFeel(...)` — see `com.openbravo.pos.theme.RetailPOSTheme.registerDefaultsSource()`, called from both `StartPOS.main` and `JFrmConfig.main`:

```java
FlatLaf.registerCustomDefaultsSource("com.openbravo.pos.theme");
UIManager.setLookAndFeel(config.getProperty("swing.defaultlaf")); // FlatLaf Light or FlatLaf Dark,
                                                                    // same place Configuration → General
                                                                    // already switches Light/Dark
```

(`FlatLaf.registerCustomDefaultsSource(packageName)` is the supported hook — FlatLaf finds `FlatLightLaf.properties`-style overrides on the classpath at that package and layers them automatically; that's the cleanest way to slot these two files in without touching the setup call at all.)

Disabled controls keep their normal background and foreground; FlatLaf's standard disabled painting supplies the approximately 45% attenuation documented in the design system. Do not set `TextField.disabledBackground` or a component-specific `disabledBackground` to a solid token, especially not to the same colour as the enabled state.

## 2. Fonts: register them, don't look them up by name

The real Manrope and IBM Plex Mono files are in this system's `fonts/` folder as `.woff2` (for the web side). Swing needs `.ttf`/`.otf`, so convert once and commit the result — do not ship `.woff2` next to the `.jar`:

```sh
pip install fonttools brotli
python -m fontTools.ttLib.woff2 decompress -o Manrope-Regular.ttf   fonts/Manrope-Regular.woff2
# repeat for Medium, SemiBold, Bold, ExtraBold, and the two IBM Plex Mono weights
```

**Gotcha, found while building this system:** the Manrope files served this way carry the *internal* font family name `Manrope ExtraLight` (Medium/SemiBold/Bold/ExtraBold each append their own weight, e.g. `Manrope ExtraLight SemiBold`) — an artifact of how the upstream variable font was split into static weights, not a mistake in this token set. `new Font("Manrope", ...)` will silently fall back to a system font because that string doesn't match. Register and keep the `Font` object instead of relying on the family-name string:

```java
private static Font loadFont(String resourcePath, float size) throws Exception {
    try (InputStream in = ...class.getResourceAsStream(resourcePath)) {
        Font base = Font.createFont(Font.TRUETYPE_FONT, in);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
        return base.deriveFont(size);
    }
}
// one loadFont() call per weight, held as static Font fields — never re-resolved by name
UIManager.put("defaultFont", loadFont("/fonts/Manrope-Medium.ttf", 15f));
UIManager.put("Table.font", loadFont("/fonts/IBMPlexMono-Regular.ttf", 15f));
```

IBM Plex Mono's files carry clean names (`IBM Plex Mono`, `IBM Plex Mono Medium`, …), so that half is safe to reference by name if you'd rather — but for consistency, load every weight of both families through `loadFont()` and never mix the two approaches.

## 3. Mapping text styles to Swing font sizes

`tokens.json`'s `type.groups` are CSS-first (`fontSize`/`lineHeight` in px). On a fixed-DPI till screen, px and Java's point-based `Font` size are close enough to use directly — take the numeral and drop the unit:

| Token | Swing usage |
|---|---|
| `display-lg` / `display-sm` | The "Till closed" screen, day-end total dialog — `deriveFont(40f)` / `deriveFont(28f)`, Manrope ExtraBold weight |
| `heading` | `JDialog`/panel titles — `deriveFont(20f)`, Manrope Bold |
| `body-lg` | Product names, ticket lines, button and keypad labels — `deriveFont(17f)`, Manrope Medium |
| `body` | Default `defaultFont` for forms and settings — `deriveFont(15f)`, Manrope Regular |
| `caption` | Table headers, timestamps — `deriveFont(13f)`, Manrope Medium, painted in `retailpos.ink-muted` (`#6b6154` / `#b2a695`) |
| `price-lg` / `price` / `price-sm` | The running total; ticket line prices and keypad entry; unit prices — IBM Plex Mono at 32 / 17 / 13, **with tabular figures**: IBM Plex Mono's digits are fixed-width by design, so no OpenType feature toggle is needed the way it would be for a proportional face |

## 4. Spacing and touch targets in existing layouts

Whatever layout manager a screen already uses (`GridBagLayout`, SwingX, absolute positioning in the older dialogs), carry the numbers over as `insets`/gaps rather than eyeballing new ones: `space-4` (16px) for panel and dialog padding, `space-2`–`space-3` (8–12px) between related controls, `space-8` (32px) between the ticket panel and the product grid. The one hard rule, not a suggestion: **no keypad key, product tile or primary button gets a preferred height under `space-12` (48px)**. That's the number that matters for a touch screen — retrofit it into a screen before touching its colours or fonts if the two ever compete for time.

## 5. Icons: PNG today, SVG as the next step

The existing templates (`Button.OpenDrawer.png`, `Button.Print.png`, the coin/banknote set) are flat PNGs loaded as `ImageIcon` — this system's six SVG icons follow the same visual family (24×24, 2px stroke, single ink colour) so they drop into that exact pattern: export each as PNG at the sizes those templates already use (typically 1x + a 2x for high-DPI displays) rather than inventing a new loading path. `com.formdev:flatlaf-extras` (not yet a dependency) adds `FlatSVGIcon`, which paints the actual SVG at any size and is the natural next step once more than a handful of icons exist — worth adding when the icon set grows past what hand-exported PNGs can keep up with, not before.

## 6. What to leave alone for now

This maps tokens onto FlatLaf; it does not ask you to rebuild a screen's layout, replace SwingX components, or chase every Nimbus-era visual left in the app in one pass. Apply the theme file globally first (colours and type shift everywhere at once, for free), then work screen by screen on spacing and touch targets where the old layout fights the new numbers — the sales screen and its keypad first, since that's where a customer is standing and watching.
