# Using this system on the future website

Nothing here is Swing-specific: `tokens.json` is the same source for the till and for a marketing page, a docs site, or an admin dashboard — build the website from it directly rather than eyeballing values off a screenshot of the app.

## Loading the tokens

This artifact compiles `tokens.json` into `tokens.css` (CSS custom properties, one block per theme, switched by `[data-theme="light"|"dark"]` on `html`). Read it by its `project/` path and ship it as-is — never copy hex values into a stylesheet by hand, so a later token edit here doesn't quietly drift out of sync with the site:

```html
<link rel="stylesheet" href="tokens.css">
<link rel="stylesheet" href="components/bundle.css">
<html data-theme="light">
```

Fonts: `fonts/*.woff2` are the real Manrope and IBM Plex Mono files (OFL-licensed, `fonts/Manrope-OFL-LICENSE.txt` / `fonts/IBMPlexMono-OFL-LICENSE.txt`) — self-host them next to the site rather than pulling from Google Fonts' CDN, so the site works the same offline demo a shop owner might see as it does live, and so there's one fewer third-party request. `@font-face` blocks for all eight weights are generated into `tokens.css` already; just make sure the `fonts/` folder ships alongside it.

## What the site is for, and what that means for tone

The till's voice (imperative, plain, no exclamation marks — see `README.md` → "Content fundamentals") is for someone mid-shift. A website talking to a shop owner deciding whether to install this can be a little warmer and more descriptive without breaking the system — headlines can use `display-lg`/`display-sm` at sizes the till never needs (a hero at 56–64px, scaling `display-lg`'s weight and tracking rather than introducing a new style), and body copy can run longer than a till's helper text ever does. Keep the same rules that don't change with the medium: one `brand`-coloured action per view (the install/download button, not every link), semantic colour only for its fixed meaning, no icon font other than this system's own set, no emoji.

## Components that carry over directly

`Button`, `StatusBadge` and `ProductTile` (bundled with live previews under `components/`) are written as plain HTML/CSS, so they render identically in the till's own web-based screens (if any) and on the marketing/docs site — a pricing table's "most popular" tag is `StatusBadge` with `warning`, a feature-comparison tile is `ProductTile`'s layout with different content. `NumericKey` is till-only by nature (it exists to be tapped by a cashier); skip it on the website rather than forcing a use for it.

## Building new components for the site

A website will eventually need things the till never will — a nav bar, a footer, a pricing table, a docs sidebar. Build those from the same tokens (surfaces, `brand` as the one accent, the type and spacing scales) rather than adding new colours or a second font pairing "just for the web" — the whole point of one token set is that a shop owner's first impression on the website and their cashier's daily screen come from the same hand. Add genuinely new components to `components/<Name>/` here as they're built, with a `README.md` and `preview.html` each, so the site and the till stay drawing from one place instead of two design languages slowly drifting apart.
