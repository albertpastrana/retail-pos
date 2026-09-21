#!/usr/bin/env bash
set -euo pipefail

package="${1:?package is required}"
install_dir="${2:?install directory is required}"
pid="${3:?process id is required}"
log_file="${install_dir}.update.log"
exec >>"$log_file" 2>&1
log() { printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { log "ERROR: $*"; exit 1; }
log "Update started: package=$package install_dir=$install_dir pid=$pid"
staging="$(mktemp -d "${TMPDIR:-/tmp}/retail-pos-update.XXXXXX")"
backup="${install_dir}.previous"
cleanup() { rm -rf "$staging"; }
trap cleanup EXIT

for _ in $(seq 1 60); do
  if ! kill -0 "$pid" 2>/dev/null; then break; fi
  sleep 1
done
if kill -0 "$pid" 2>/dev/null; then fail "Timed out waiting for application process $pid"; fi

# The updater is launched from the installation directory. Leave it before
# replacing that directory, otherwise the restarted JVM inherits a deleted cwd.
cd / || fail "Could not change to a stable working directory"

case "$package" in
  *.tar.gz) tar -xzf "$package" -C "$staging" || fail "Could not extract package" ;;
  *.zip) unzip -q "$package" -d "$staging" || fail "Could not extract package" ;;
  *) fail "Unsupported package format: $package" ;;
esac
new_dir=""
for candidate in "$staging"/*; do
  if [ -d "$candidate" ]; then new_dir="$candidate"; break; fi
done
[ -n "$new_dir" ] || fail "Package has no application directory"
rm -rf "$backup"
mv "$install_dir" "$backup" || fail "Could not move current installation to backup"
if mv "$new_dir" "$install_dir"; then
  log "Application files replaced successfully"
  rm -rf "$backup"
  chmod +x "$install_dir/start.sh" "$install_dir/update.sh" 2>/dev/null || true
  log "Restarting application"
  exec "$install_dir/start.sh"
fi
log "ERROR: Could not install new application; restoring backup"
mv "$backup" "$install_dir" || log "ERROR: Could not restore backup"
exit 1
