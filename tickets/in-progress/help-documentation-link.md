# Add documentation help link

Captured: 2026-10-11

## Goal

Give operators a direct Help link from the POS entry screen to the published operator documentation.

## Context

The entry screen currently exposes About and Close application actions but no documentation link. The published documentation is available at `https://retail-pos-docs.netlify.app/`. The link must use the system browser, remain usable when browser support is unavailable, and have English, Spanish, and Catalan labels.

## Done when

- The entry screen has a clearly labelled Help action next to About.
- Activating Help opens the documentation URL in the system browser when supported.
- Unsupported browser or invalid URL failures are reported without crashing the POS.
- English, Spanish, and Catalan labels are present.
- Focus and keyboard activation work for the new action.
- Relevant Java formatting and tests pass.
