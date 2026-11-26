#!/usr/bin/env bash
# Fail if a source archive contains secrets, build products, or VCS metadata.
# Usage: scripts/check-archive.sh <zip>
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: scripts/check-archive.sh <zip>" >&2
  exit 2
fi

zip="$1"
if [[ ! -f "$zip" ]]; then
  echo "check-archive: missing file: $zip" >&2
  exit 2
fi

mapfile -t entries < <(unzip -Z1 "$zip")
status=0
for entry in "${entries[@]}"; do
  base="$(basename "$entry")"
  case "$entry" in
    *.env.example) continue ;;
  esac
  if [[ "$base" == .env || "$base" == .env.* || "$entry" == */.env || "$entry" == */.env.* ]]; then
    echo "check-archive: forbidden env file: $entry"
    status=1
  fi
  case "$base" in
    local.properties|*.apk|*.aab|*.jks|*.keystore)
      echo "check-archive: forbidden artifact: $entry"
      status=1
      ;;
  esac
  case "$entry" in
    */node_modules/*|node_modules/*|\
    */.venv/*|.venv/*|\
    */build/*|build/*|\
    */dist/*|dist/*|\
    */.gradle/*|.gradle/*|\
    */.kotlin/*|.kotlin/*|\
    */.android-launch/*|.android-launch/*|\
    */.git/*|.git/*)
      echo "check-archive: forbidden path: $entry"
      status=1
      ;;
  esac
done

if [[ $status -ne 0 ]]; then
  echo "check-archive: FAILED ($zip)"
else
  echo "check-archive: ok ($zip, ${#entries[@]} entries)"
fi
exit $status
