# Spotless formatter (one-shot)

Captured: 2026-09-14

Parked: 2026-09-14

## Goal

Apply Spotless once across the tree in a single commit. No `ratchetFrom`.

## Context

No `.form` files. `initComponents` / `GEN-BEGIN` is frozen Swing; reformatting does not break the till. Risk is `blame` and conflicts with open branches, not runtime.

Exclude `launch4j/`. Check that the formatter does not reorder imports or break string literals. `./gradlew check` afterwards.

## Done when

One formatter commit is on the branch, `./gradlew check` is green, and later edits stay formatted.
