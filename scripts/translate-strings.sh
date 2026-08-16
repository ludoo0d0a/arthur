#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${GK_TOOLS:-$(cd "$ROOT/../.." && pwd)/geoking-tools}"
# Fallback sibling layouts: android/arthur/Untitled -> android/geoking-tools
if [ ! -d "$TOOLS/translate" ]; then
  TOOLS="$(cd "$ROOT/../../geoking-tools" && pwd)"
fi
if [ ! -d "$TOOLS/translate" ]; then
  TOOLS="/Users/ludovic/dev/android/geoking-tools"
fi
exec "$TOOLS/translate/translate.sh" "$@"
