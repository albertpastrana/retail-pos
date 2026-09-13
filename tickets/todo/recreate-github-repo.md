# Recreate the GitHub repo (drop leaked TSV history)

Captured: 2026-09-14

## Goal

GitHub no longer serves the old `data/` TSV blobs. The public history matches the rewritten local history.

## Context

Delete and recreate `albertpastrana/retail-pos`. Local history is already rewritten (`data/` TSVs are in no commit) and was force-pushed, but GitHub still serves orphan commits by SHA. Example: `gh api "repos/albertpastrana/retail-pos/contents/data/import-products.tsv?ref=72f7217a"` still returns the file.

A new empty repo plus a push of current history is the only reliable way to drop them without waiting on GitHub GC.

This is an ops step. Do not rewrite git history again unless the user asks.

## Done when

That SHA (and other orphan TSV commits) 404 on GitHub, and the default branch is the rewritten history.
