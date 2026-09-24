---
name: release-verification
description: Verify builds, CI, packaging, updates, and platform-specific Retail POS releases before publication.
---

# Release verification

Use this skill for CI failures, release candidates, packaging, update delivery,
or requests to commit and push a release-related change.

## Procedure

1. Inspect the current branch, worktree, recent commits, workflow files, and
   version/tag state. Separate the requested release from unrelated changes.
2. Run the narrow failing check first, then the project verification required by
   the affected path. Do not infer GitHub CI status from a local build.
3. Verify the runtime assumptions separately from compilation: Java bytecode
   target, bundled or system JRE, dependencies, Flyway drivers, native device
   libraries, resources, launch scripts, and configuration preservation.
4. For Linux, Windows, and macOS packages, verify the package on the target
   platform or state exactly what could not be tested. Check filenames,
   permissions, launchers, checksums, and update rollback behaviour.
5. For an updater, test the no-update, available-update, failed-download,
   checksum-mismatch, failed-replacement, and rollback paths without replacing
   the live installation.
6. Report the exact artifact, verification commands, CI result, and remaining
   platform risks. Ask before committing, tagging, pushing, or publishing.
