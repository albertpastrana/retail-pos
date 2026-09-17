#!/usr/bin/env bash
set -euo pipefail

platform="${1:?platform is required: linux, windows, or macos}"
version="${2:-${GITHUB_REF_NAME:-local}}"
version="${version#v}"

case "$platform" in
  linux) native_lib="Linux/x86_64-unknown-linux-gnu"; archive_suffix="tar.gz" ;;
  windows) native_lib="Windows/i368-mingw32"; archive_suffix="zip" ;;
  macos) native_lib="Mac_OS_X"; archive_suffix="tar.gz" ;;
  *) echo "Unsupported platform: $platform" >&2; exit 2 ;;
esac

if [ "$platform" = "linux" ]; then
  runtime_note="This package uses Java installed on the system."
else
  runtime_note="This package includes its Java runtime."
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
dist="$root/dist"
staging="$dist/RetailPOS-${platform}-x86_64"
rm -rf "$staging"
mkdir -p "$staging/runtime-libs" "$staging/lib/$(dirname "$native_lib")" "$dist"

for file in retail-pos.jar locales.jar reports.jar; do
  cp "$root/build/jar/$file" "$staging/$file"
done
cp -R "$root/build/runtime-libs/." "$staging/runtime-libs/"
cp "$root/logging.properties" "$root/start.sh" "$root/start.bat" "$root/scripts/update.sh" "$root/scripts/update.bat" "$staging/"
cp -R "$root/licensing" "$staging/"
cp -R "$root/lib/$native_lib" "$staging/lib/$native_lib"

if [ "$platform" != "linux" ]; then
  jlink="${JAVA_HOME:-}/bin/jlink"
  if [ ! -x "$jlink" ]; then jlink="$(command -v jlink)"; fi
  "$jlink" \
    --add-modules ALL-MODULE-PATH \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output "$staging/runtime"
fi

cat > "$staging/README.txt" <<EOF
Retail POS ${version}

Run start.sh on Linux or macOS, and start.bat on Windows.
${runtime_note} Application configuration remains
external; see the project documentation for database and printer settings.
EOF
chmod +x "$staging/start.sh"
chmod +x "$staging/update.sh"

archive="$dist/RetailPOS-${version}-${platform}-x86_64.${archive_suffix}"
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
