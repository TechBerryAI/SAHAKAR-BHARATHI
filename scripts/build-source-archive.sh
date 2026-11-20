#!/usr/bin/env bash
# Build a source ZIP from the current HEAD commit only (no untracked files,
# no working-tree dirt). Output: sahakar-bharathi-source-<shortsha>.zip
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
sha="$(git rev-parse --short HEAD)"
out="sahakar-bharathi-source-${sha}.zip"
rm -f "$out"
git archive --format=zip --output="$out" HEAD
echo "Wrote $out"
echo "$out"
