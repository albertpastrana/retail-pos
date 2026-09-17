#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

sha256sum -c migration-checksums.sha256
tracked="$(sed -E 's/^[^ ]+[ ]+//' migration-checksums.sha256 | sort)"
actual="$(find src-pos/db/migration -maxdepth 1 -type f -name 'V[0-9]*__*' -print | sort)"
if [ "$tracked" != "$actual" ]; then
  printf '%s\n' "Migration checksum manifest is out of date; add every versioned migration." >&2
  diff -u <(printf '%s\n' "$tracked") <(printf '%s\n' "$actual") >&2 || true
  exit 1
fi
