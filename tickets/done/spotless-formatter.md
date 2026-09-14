# Spotless formatter (one-shot)

Captured: 2026-09-14

Updated: 2026-09-14

## Goal

Spotless formats Java in this repo. Agents apply it. CI fails if someone skips it.

## Context

`chore/dependency-maintenance` is on `origin/main` (PR #2). Open GitHub PRs are Dependabot (Gradle, Actions, Compose), not Java trees.

Work from `origin/main`. Do not use `ratchetFrom`. Exclude `launch4j/`. Eclipse JDT via Spotless (not google-java-format): layout only. No `importOrder` / `removeUnusedImports`. Confirmed: import order unchanged (CRLF/whitespace only); SQL and other string literals kept intact.

## Done when

- Format commit is on `main` (or a PR onto it) and `./gradlew check` is green.
- Agents have an always-on rule and README mentions `spotlessApply` / `spotlessCheck`.
- GitHub runs `spotlessCheck` on push and pull request.
- `chore/dependency-maintenance` is merged, rebased onto the format commit, or dropped — not left to collide later.

## Shipped

- Spotless 8.10.1 on the root Gradle project; `eclipse()` on Java source sets; `launch4j/` excluded; `spotlessJava` `mustRunAfter` `syncRunJars` so `check` is valid with Gradle 8.
- Tree-wide `spotlessApply` on the Java source sets.
- Always-on Cursor rule `.cursor/rules/spotless.mdc`. README build table lists `spotlessApply` / `spotlessCheck`.
- GitHub `spotless` job runs `./gradlew spotlessCheck` without Compose. `check` still includes `spotlessCheck`.
- `chore/dependency-maintenance` already merged on `origin/main`.
