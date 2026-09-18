The price-tag mark, in the variants each output needs — never redraw or recolour it outside these:

- `retail-pos-mark.svg` — light-background icon, drawn in `brand` (`#b8481f`) with a transparent punched hole. For any surface close to `surface-0`/`surface-100`.
- `retail-pos-mark-on-dark.svg` — the same mark in the dark theme's `brand` (`#e2764c`), for a surface close to `surface-0`(dark)/`surface-100`(dark). Never place the light-variant mark on a dark surface or vice-versa.
- `retail-pos-lockup.svg` / `retail-pos-lockup-on-dark.svg` — mark plus the "Retail POS" wordmark (Manrope ExtraBold), light- and dark-background pairs respectively. The wordmark's ink is `ink` / `ink` (dark theme value) to match.
- `retail-pos-app-icon.svg` — the mark on a filled `brand`-coloured square, for the window/taskbar/`.ico` icon, where a transparent background isn't an option.
- `retail-pos-ticket-logo.svg` — pure black-on-white, no accent colour, for `Printer.Ticket.Logo` and `Window.Logo`: an ESC/POS thermal printer can't render the accent colour or a soft edge, only 1-bit black.

Single-ink note (icons and marks shown only via `<img>` can't inherit colour): each SVG's fill is baked in as listed above, not `currentColor`.
