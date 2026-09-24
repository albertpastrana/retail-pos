---
name: pos-ui-review
description: Review or implement Retail POS Swing UI changes with operator-first, localization, layout, and manual validation checks.
---

# POS UI review

Use this skill for Swing screens, dialogs, controls, design-system work,
layouts, labels, keyboard input, screenshots, or visual regressions.

## Review procedure

1. Identify the concrete screen, entry path, shared components, and all call
   sites before editing.
2. Check the relevant `design-system/` documentation and existing screens that
   already implement the desired pattern.
3. Prefer layout managers and `pack()` over `null` layouts, `setBounds`, and
   arbitrary pixel dimensions. Preserve intentional touch targets and document
   exceptions.
4. Keep the operator flow short and explicit. Buttons should describe the
   action, destructive actions should be distinguishable and safe by default,
   and Enter must not trigger an unexpected destructive operation.
5. Keep scanner input, physical keyboard input, focus, decimal/comma input,
   resize behaviour, and touch targets working.
6. Update English, Spanish, and Catalan resources for every new user-visible
   string. Check long labels and the supported display scales.

## Verification

- Run `./gradlew spotlessApply` after Java edits.
- Compile and run the relevant tests.
- Manually open the concrete affected flow, not just the application home
  screen. Check normal, empty, error, and translated states.
- For shared beans or theme changes, verify at least one existing screen for
  every materially different usage.
- Do not declare a screenshot-based issue fixed until the user has confirmed
  the visual result when the task requires manual review.
