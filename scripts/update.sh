#!/usr/bin/env bash
set -euo pipefail

package="${1:?package is required}"
install_dir="${2:?install directory is required}"
pid="${3:?process id is required}"
staging="$(mktemp -d "${TMPDIR:-/tmp}/retail-pos-update.XXXXXX")"
backup="${install_dir}.previous"
cleanup() { rm -rf "$staging"; }
trap cleanup EXIT

for _ in $(seq 1 60); do
  if ! kill -0 "$pid" 2>/dev/null; then break; fi
  sleep 1
done

tar -xzf "$package" -C "$staging"
new_dir=""
for candidate in "$staging"/*; do
  if [ -d "$candidate" ]; then new_dir="$candidate"; break; fi
done
[ -n "$new_dir" ] || { echo "Package has no application directory" >&2; exit 1; }
rm -rf "$backup"
mv "$install_dir" "$backup"
if mv "$new_dir" "$install_dir"; then
  rm -rf "$backup"
  exec "$install_dir/start.sh"
fi
mv "$backup" "$install_dir"
exit 1
