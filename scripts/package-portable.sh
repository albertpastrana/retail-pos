#!/usr/bin/env bash
set -euo pipefail

platform="${1:?platform is required: linux, windows, or macos}"
version="${2:-${GITHUB_REF_NAME:-local}}"
architecture="${3:-x86_64}"
version="${version#v}"

case "$architecture" in
  x86_64|arm64) ;;
  *) echo "Unsupported architecture: $architecture" >&2; exit 2 ;;
esac

case "$platform" in
  linux) archive_suffix="tar.gz" ;;
  windows) archive_suffix="zip" ;;
  macos) archive_suffix="tar.gz" ;;
  *) echo "Unsupported platform: $platform" >&2; exit 2 ;;
esac

runtime_note="This package includes its Java 21 runtime."

root="$(cd "$(dirname "$0")/.." && pwd)"
dist="$root/dist"
staging="$dist/RetailPOS-${platform}-${architecture}"
rm -rf "$staging"
mkdir -p "$staging/runtime-libs" "$dist"

for file in retail-pos.jar locales.jar; do
  cp "$root/build/jar/$file" "$staging/$file"
done
cp -R "$root/build/runtime-libs/." "$staging/runtime-libs/"
cp "$root/logging.properties" "$root/start.sh" "$root/start.bat" "$root/scripts/update.sh" "$root/scripts/update.bat" "$staging/"
cp -R "$root/licensing" "$staging/"

jlink="${JAVA_HOME:-}/bin/jlink"
if [ ! -x "$jlink" ]; then jlink="$(command -v jlink)"; fi
jlink_version="$($jlink --version 2>&1)"
case "$jlink_version" in
  21.*) ;;
  *) echo "Java 21 jlink is required; found: $jlink_version" >&2; exit 1 ;;
esac
"$jlink" \
  --add-modules ALL-MODULE-PATH \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "$staging/runtime"

if [ "$platform" != "windows" ]; then
  jspawnhelper="$staging/runtime/lib/jspawnhelper"
  if [ ! -f "$jspawnhelper" ]; then
    echo "Bundled runtime is missing jspawnhelper: $jspawnhelper" >&2
    exit 1
  fi
  chmod 755 "$jspawnhelper"
  if [ ! -x "$jspawnhelper" ]; then
    echo "Bundled runtime jspawnhelper is not executable: $jspawnhelper" >&2
    exit 1
  fi
fi

cat > "$staging/README.txt" <<EOF
Retail POS ${version}

Run start.sh on Linux or macOS, and start.bat on Windows.
${runtime_note} Application configuration remains
external; see the project documentation for database and printer settings.
EOF
chmod +x "$staging/start.sh"
chmod +x "$staging/update.sh"

archive="$dist/RetailPOS-${version}-${platform}-${architecture}.${archive_suffix}"
if [ "$archive_suffix" = "tar.gz" ]; then
  tar -czf "$archive" -C "$dist" "$(basename "$staging")"
else
  jar --create --file "$archive" -C "$dist" "$(basename "$staging")"
fi

if command -v sha256sum >/dev/null 2>&1; then
  (cd "$dist" && sha256sum "$(basename "$archive")" > "$(basename "$archive").sha256")
else
  (cd "$dist" && shasum -a 256 "$(basename "$archive")" > "$(basename "$archive").sha256")
fi
rm -rf "$staging"
echo "$archive"
